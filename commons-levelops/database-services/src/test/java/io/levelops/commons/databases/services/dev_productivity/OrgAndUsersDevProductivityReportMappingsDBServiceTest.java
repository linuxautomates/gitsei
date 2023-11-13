package io.levelops.commons.databases.services.dev_productivity;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseServiceTestUtils;
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
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseServiceTestUtils.createDevProdProfiles;

public class OrgAndUsersDevProductivityReportMappingsDBServiceTest {
    private static final Integer n = 3;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;

    String company = "test";
    JdbcTemplate template;

    TagItemDBService tagItemService;
    OrgVersionsDatabaseService orgVersionsService;
    OrgUnitsDatabaseService orgUnitsDatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketService;

    OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService;
    UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    OrgAndUsersDevProductivityReportMappingsDBService orgAndUsersDevProductivityReportMappingsDBService;

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
        new UserService(dataSource, mapper).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, orgVersionsService, dashboardWidgetService);
        ticketService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, mapper);
        orgDevProductivityReportDatabaseService = new OrgDevProductivityReportDatabaseService(dataSource, mapper);
        userDevProductivityReportDatabaseService = new UserDevProductivityReportDatabaseService(dataSource, mapper);
        orgAndUsersDevProductivityReportMappingsDBService = new OrgAndUsersDevProductivityReportMappingsDBService(dataSource);

        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
//        tagItemService.ensureTableExistence(company);
        orgVersionsService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, null, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUsersDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);
        ticketService.ensureTableExistence(company);
        devProductivityProfileDatabaseService.ensureTableExistence(company);
        orgDevProductivityReportDatabaseService.ensureTableExistence(company);
        userDevProductivityReportDatabaseService.ensureTableExistence(company);
        orgAndUsersDevProductivityReportMappingsDBService.ensureTableExistence(company);
    }


    @Test
    public void test() throws SQLException {
        List<Integration> integrations = IntegrationUtils.createIntegrations(integrationService, company, n);
        DBOrgUser orgUser = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integrations.get(0));
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        List<UUID> profiles = createDevProdProfiles(devProductivityProfileDatabaseService, company, scheme.getId(), 3).stream().map(DevProductivityProfile::getId).collect(Collectors.toList());
        List<UUID> orgUsers = OrgUsersDatabaseServiceTestUtils.createDBOrgUsers(orgUsersDatabaseService, company, n, integrations.get(0)).stream().map(DBOrgUser::getId).collect(Collectors.toList());
        List<DBOrgUnit> dbOrgUnits = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, n, integrations.get(0), integrations.get(1)).stream().collect(Collectors.toList());
        //List<UUID> orgUnits = dbOrgUnits.stream().map(DBOrgUnit::getId).collect(Collectors.toList());

        ReportIntervalType interval1 = ReportIntervalType.LAST_MONTH;
        ReportIntervalType interval2 = ReportIntervalType.LAST_QUARTER;
        ReportIntervalType interval3 = ReportIntervalType.LAST_TWO_QUARTERS;

        Map<Integer, List<OrgAndUsersDevProductivityReportMappings>> ouRefIdToMappingMap = new HashMap<>();

        OrgAndUsersDevProductivityReportMappings mapping1 = OrgAndUsersDevProductivityReportMappings.builder()
                .devProductivityProfileId(profiles.get(0)).interval(interval1).ouID(dbOrgUnits.get(0).getId()).orgUserIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .build();
        String id = orgAndUsersDevProductivityReportMappingsDBService.upsert(company, mapping1);
        mapping1 = mapping1.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(1, orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null, null, null, null, null).getRecords().size());

        mapping1 = mapping1.toBuilder().orgUserIds(orgUsers).build();
        id = orgAndUsersDevProductivityReportMappingsDBService.upsert(company, mapping1);
        Assert.assertEquals(id, mapping1.getId().toString());
        mapping1 = mapping1.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(1, orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null, null, null, null, null).getRecords().size());
        ouRefIdToMappingMap.computeIfAbsent(dbOrgUnits.get(0).getRefId(), k -> new ArrayList<>()).add(mapping1);


        OrgAndUsersDevProductivityReportMappings mapping2 = OrgAndUsersDevProductivityReportMappings.builder()
                .devProductivityProfileId(profiles.get(0)).interval(interval2).ouID(dbOrgUnits.get(1).getId()).orgUserIds(List.of(orgUsers.get(0), orgUsers.get(1)))
                .build();
        id = orgAndUsersDevProductivityReportMappingsDBService.upsert(company, mapping2);
        mapping2 = mapping2.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(2, orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null, null, null, null, null).getRecords().size());
        ouRefIdToMappingMap.computeIfAbsent(dbOrgUnits.get(1).getRefId(), k -> new ArrayList<>()).add(mapping2);

        OrgAndUsersDevProductivityReportMappings mapping3 = OrgAndUsersDevProductivityReportMappings.builder()
                .devProductivityProfileId(profiles.get(0)).interval(interval3).ouID(dbOrgUnits.get(2).getId()).orgUserIds(List.of(orgUsers.get(0)))
                .build();
        id = orgAndUsersDevProductivityReportMappingsDBService.upsert(company, mapping3);
        mapping3 = mapping3.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(3, orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null, null, null, null, null).getRecords().size());
        ouRefIdToMappingMap.computeIfAbsent(dbOrgUnits.get(2).getRefId(), k -> new ArrayList<>()).add(mapping3);

        OrgAndUsersDevProductivityReportMappings mapping4 = OrgAndUsersDevProductivityReportMappings.builder()
                .devProductivityProfileId(profiles.get(1)).interval(interval1).ouID(dbOrgUnits.get(0).getId()).orgUserIds(List.of(orgUsers.get(1)))
                .build();
        id = orgAndUsersDevProductivityReportMappingsDBService.upsert(company, mapping4);
        mapping4 = mapping4.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(4, orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null, null, null, null, null).getRecords().size());
        ouRefIdToMappingMap.computeIfAbsent(dbOrgUnits.get(0).getRefId(), k -> new ArrayList<>()).add(mapping4);

        DbListResponse<OrgAndUsersDevProductivityReportMappings> res = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnits.get(0).getId()), List.of(profiles.get(0)), List.of(interval1), null);
        Assert.assertEquals(1, res.getTotalCount().intValue());
        Assert.assertEquals(1, res.getRecords().size());
        verifyRecord(mapping1, res.getRecords().get(0));

        res = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnits.get(1).getId()), List.of(profiles.get(0)), List.of(interval2), null);
        Assert.assertEquals(1, res.getTotalCount().intValue());
        Assert.assertEquals(1, res.getRecords().size());
        verifyRecord(mapping2, res.getRecords().get(0));

        res = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnits.get(2).getId()), List.of(profiles.get(0)), List.of(interval3), null);
        Assert.assertEquals(1, res.getTotalCount().intValue());
        Assert.assertEquals(1, res.getRecords().size());
        verifyRecord(mapping3, res.getRecords().get(0));

        res = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnits.get(0).getId()), List.of(profiles.get(1)), List.of(interval1), null);
        Assert.assertEquals(1, res.getTotalCount().intValue());
        Assert.assertEquals(1, res.getRecords().size());
        verifyRecord(mapping4, res.getRecords().get(0));

        for (Map.Entry<Integer, List<OrgAndUsersDevProductivityReportMappings>> e : ouRefIdToMappingMap.entrySet()) {
            res = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 100, null,
                    null, null, null, List.of(e.getKey()));
            Assert.assertEquals(e.getValue().size(), res.getTotalCount().intValue());
            Assert.assertEquals(e.getValue().size(), res.getRecords().size());
            verifyRecords(e.getValue(), res.getRecords());
        }
    }

    private void verifyRecords(List<OrgAndUsersDevProductivityReportMappings> a, List<OrgAndUsersDevProductivityReportMappings> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, OrgAndUsersDevProductivityReportMappings> actualMap = a.stream().collect(Collectors.toMap(OrgAndUsersDevProductivityReportMappings::getId, x -> x));
        Map<UUID, OrgAndUsersDevProductivityReportMappings> expectedMap = e.stream().collect(Collectors.toMap(OrgAndUsersDevProductivityReportMappings::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void verifyRecord(OrgAndUsersDevProductivityReportMappings expected, OrgAndUsersDevProductivityReportMappings actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getDevProductivityProfileId(), actual.getDevProductivityProfileId());
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getOuID(), actual.getOuID());
        Assert.assertEquals(expected.getOrgUserIds(), actual.getOrgUserIds());
        Assert.assertNotNull(actual.getCreatedAt());
        Assert.assertNotNull(actual.getUpdatedAt());
    }
}