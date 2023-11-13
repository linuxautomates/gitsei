package io.levelops.commons.databases.services.precalculation;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WidgetPrecalculatedResultsDBServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    String company = "test";
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    JdbcTemplate template;
    TagItemDBService tagItemService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static OrgUnitsDatabaseService orgUnitsDatabaseService;
    private static OrgUsersDatabaseService orgUsersDatabaseService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static DashboardWidgetService dashboardWidgetService;
    private static UserService userService;

    private static WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);

        tagItemService = new TagItemDBService(dataSource);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        dashboardWidgetService = new DashboardWidgetService(dataSource,mapper);
        userService = new UserService(dataSource,mapper);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, orgVersionsService, dashboardWidgetService);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, null, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        orgVersionsService.ensureTableExistence(company);
        orgUsersDatabaseService.ensureTableExistence(company);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);

        widgetPrecalculatedResultsDBService = new WidgetPrecalculatedResultsDBService(dataSource, mapper);
        widgetPrecalculatedResultsDBService.ensureTableExistence(company);

    }

    @Test
    public void test() throws SQLException, JsonProcessingException {
        List<Integration> integrations = IntegrationUtils.createIntegrations(integrationService, company, 2);
        List<UUID> orgUsers = OrgUsersDatabaseServiceTestUtils.createDBOrgUsers(orgUsersDatabaseService, company, 1, integrations.get(0)).stream().map(DBOrgUser::getId).collect(Collectors.toList());
        List<DBOrgUnit> dbOrgUnits = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 1, integrations.get(0), integrations.get(1)).stream().collect(Collectors.toList());;



        Widget w = Widget.builder()
                .name("w1")
                .type("issue_lead_time")
                .build();
        Dashboard d = Dashboard.builder()
                .name("test 1").type("abc")
                .build();
        String dashboardId =  dashboardWidgetService.insert(company, d);
        String widgetId = dashboardWidgetService.insertWidget(company, w, dashboardId);

//        String id = orgUnitsDatabaseService.insert(company, DBOrgUnit.builder().refId(1).name("ou1").active(true).versions(Set.of(1)).build());
//        System.out.println(id);

        //DBOrgUnit ouFromDB = orgUnitsDatabaseService.get(company, Integer.parseInt(id), null, null).get();
        DBOrgUnit dbOrgUnit = dbOrgUnits.get(0);

        String reportString = "[{\"key\": \"Lead time to First Commit\", \"p90\": 0, \"p95\": 6024, \"mean\": 78161.18969825101, \"count\": 5203, \"median\": 0, \"additional_key\": \"First Commit\", \"velocity_stage_result\": {\"rating\": \"GOOD\", \"lower_limit_unit\": \"SECONDS\", \"upper_limit_unit\": \"SECONDS\", \"lower_limit_value\": 864000, \"upper_limit_value\": 2592000}}, {\"key\": \"PR Creation Time\", \"p90\": 0, \"p95\": 0, \"mean\": 25773.89294637709, \"count\": 5203, \"median\": 0, \"additional_key\": \"Pull Request Created\", \"velocity_stage_result\": {\"rating\": \"GOOD\", \"lower_limit_unit\": \"SECONDS\", \"upper_limit_unit\": \"SECONDS\", \"lower_limit_value\": 864000, \"upper_limit_value\": 2592000}}, {\"key\": \"Time to First Comment\", \"p90\": 0, \"p95\": 775, \"mean\": 36835.54276379012, \"count\": 5203, \"median\": 0, \"additional_key\": \"Pull Request Review Started\", \"velocity_stage_result\": {\"rating\": \"GOOD\", \"lower_limit_unit\": \"SECONDS\", \"upper_limit_unit\": \"SECONDS\", \"lower_limit_value\": 864000, \"upper_limit_value\": 2592000}}, {\"key\": \"Approval Time\", \"p90\": 0, \"p95\": 0, \"mean\": 779.7780126849894, \"count\": 5203, \"median\": 0, \"additional_key\": \"Pull Request Approved\", \"velocity_stage_result\": {\"rating\": \"GOOD\", \"lower_limit_unit\": \"SECONDS\", \"upper_limit_unit\": \"SECONDS\", \"lower_limit_value\": 864000, \"upper_limit_value\": 2592000}}, {\"key\": \"Merge Time\", \"p90\": 0, \"p95\": 3836, \"mean\": 49426.205266192585, \"count\": 5203, \"median\": 0, \"additional_key\": \"Pull Request Merged\", \"velocity_stage_result\": {\"rating\": \"GOOD\", \"lower_limit_unit\": \"SECONDS\", \"upper_limit_unit\": \"SECONDS\", \"lower_limit_value\": 864000, \"upper_limit_value\": 2592000}}]";
        List<DbAggregationResult> reportObj = mapper.readValue(reportString, mapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));

        WidgetPrecalculatedReport report = WidgetPrecalculatedReport.builder()
                .widgetId(UUID.fromString(widgetId))
                .widget(w)
                .listRequest(DefaultListRequest.builder().build())
                .ouRefId(dbOrgUnit.getRefId())
                .ouID(dbOrgUnit.getId())
                .reportSubType("velocity")
                .report(reportObj).calculatedAt(Instant.now())
                .interval("default")
                .build();

        UUID reportId = widgetPrecalculatedResultsDBService.upsert(company, report);
        System.out.println(reportId);

        WidgetPrecalculatedReport readBack = widgetPrecalculatedResultsDBService.get(company, reportId.toString()).get();
        List<DbAggregationResult> reportObjRead = mapper.readValue(readBack.getReport().toString(), mapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, List.of(reportId), null, null, null, null, null).getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null, null, null, null).getRecords().size());

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(UUID.fromString(widgetId)), null, null, null, null).getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(UUID.randomUUID()), null, null, null, null).getRecords().size());

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, List.of(dbOrgUnit.getRefId()), null, null, null).getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, List.of(Integer.MAX_VALUE), null, null, null).getRecords().size());

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, List.of("velocity"), null, null).getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, List.of(UUID.randomUUID().toString()), null, null).getRecords().size());

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, null, List.of("default"), null).getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, null, List.of(UUID.randomUUID().toString()), null).getRecords().size());

        Assert.assertEquals(1, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, null, null, "velo").getRecords().size());
        Assert.assertEquals(0, widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, null, null, null, null, UUID.randomUUID().toString()).getRecords().size());
    }
}