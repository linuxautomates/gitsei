package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.OuOrgUserMappings;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
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
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OUOrgUserMappingV2DatabaseServiceTest {
    private static final Integer n = 3;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    String company = "test";
    JdbcTemplate template;

    TagItemDBService tagItemService;
    OrgVersionsDatabaseService orgVersionsService;
    OrgUnitsDatabaseService orgUnitsDatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    DashboardWidgetService dashboardWidgetService;
    UserService userService;
    OUOrgUserMappingV2DatabaseService ouOrgUserMappingV2DatabaseService;

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
        ouOrgUserMappingV2DatabaseService = new OUOrgUserMappingV2DatabaseService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        orgVersionsService.ensureTableExistence(company);
        orgUsersDatabaseService.ensureTableExistence(company);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);
        ouOrgUserMappingV2DatabaseService.ensureTableExistence(company);
    }


    @Test
    public void test() throws SQLException {
        List<OuOrgUserMappings> expectedOU0 = List.of(
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(0).build(),
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(1).build(),
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(2).build()
        );
        ImmutablePair<Integer, Integer> result = ouOrgUserMappingV2DatabaseService.syncMappings(company,0, List.of(0,1,2));
        Assert.assertEquals(3, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        DbListResponse<OuOrgUserMappings> resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(0), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());

        result = ouOrgUserMappingV2DatabaseService.syncMappings(company,0, List.of(0,1,2));
        Assert.assertEquals(0, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(0), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());


        List<OuOrgUserMappings> expectedOU1 = List.of(
                OuOrgUserMappings.builder().ouRefId(1).ouRefId(11).build(),
                OuOrgUserMappings.builder().ouRefId(1).ouRefId(12).build(),
                OuOrgUserMappings.builder().ouRefId(1).ouRefId(13).build(),
                OuOrgUserMappings.builder().ouRefId(1).ouRefId(14).build()
        );
        result = ouOrgUserMappingV2DatabaseService.syncMappings(company,1, List.of(11,12, 13, 14));
        Assert.assertEquals(4, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size() + expectedOU1.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(0), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(1), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU1.size());

        expectedOU0 = List.of(
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(0).build(),
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(2).build(),
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(3).build(),
                OuOrgUserMappings.builder().ouRefId(0).ouRefId(4).build()
        );
        result = ouOrgUserMappingV2DatabaseService.syncMappings(company,0, List.of(0,2,3,4));
        Assert.assertEquals(2, result.getLeft().intValue());
        Assert.assertEquals(1, result.getRight().intValue());

        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size() + expectedOU1.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(0), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(1), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU1.size());

        expectedOU0 = List.of();
        result = ouOrgUserMappingV2DatabaseService.syncMappings(company,0, List.of());
        Assert.assertEquals(0, result.getLeft().intValue());
        Assert.assertEquals(4, result.getRight().intValue());

        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size() + expectedOU1.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(0), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU0.size());
        resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, List.of(1), null);
        Assert.assertEquals(resp.getRecords().size(), expectedOU1.size());
    }

    @Test
    public void testBulkInsert() throws SQLException {
        List<Integration> integrations = IntegrationUtils.createIntegrations(integrationService, company, n);

        //100 mappings
        List<Integer> orgUserRefIds = OrgUsersDatabaseServiceTestUtils.createDBOrgUsers(orgUsersDatabaseService, company, 10, integrations.get(0)).stream().map(u -> u.getRefId()).collect(Collectors.toList());
        List<DBOrgUnit> dbOrgUnits = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, n, integrations.get(0), integrations.get(1)).stream().collect(Collectors.toList());;

        UUID ouId1 = dbOrgUnits.get(0).getId();
        Integer ouRefId1 = dbOrgUnits.get(0).getRefId();
        List<OuOrgUserMappings> expected = orgUserRefIds.stream().map(orgUserRefId -> OuOrgUserMappings.builder()
                .ouId(ouId1).ouRefId(ouRefId1)
                .orgUserRefId(orgUserRefId)
                .build()).collect(Collectors.toList());
        ImmutablePair<Integer, Integer> result = ouOrgUserMappingV2DatabaseService.syncMappings(company,ouRefId1,orgUserRefIds);
        DbListResponse<OuOrgUserMappings> resp = ouOrgUserMappingV2DatabaseService.listByFilter(company, 0, 100, null, null, null);
        Assert.assertEquals(orgUserRefIds.size(), resp.getRecords().size());

      /*  orgUsers = createNOrguserIds(1000);
        expected = orgUsers.stream().map(orguserId -> OuOrgUserMappings.builder()
                .ouId(ouId1).orgUserId(orguserId)
                .build()).collect(Collectors.toList());
        inserted = ouOrgUserMappingDatabaseService.upsertOUOrgUserMappingsBulk(company,ouId1,orgUsers);
        resp = ouOrgUserMappingDatabaseService.listByFilter(company, 0, 1000, null, null, null);
        Assert.assertEquals(1000, resp.getRecords().size());

        orgUsers = createNOrguserIds(2000);
        expected = orgUsers.stream().map(orguserId -> OuOrgUserMappings.builder()
                .ouId(ouId1).orgUserId(orguserId)
                .build()).collect(Collectors.toList());
        inserted = ouOrgUserMappingDatabaseService.upsertOUOrgUserMappingsBulk(company,ouId1,orgUsers);
        resp = ouOrgUserMappingDatabaseService.listByFilter(company, 0, 2000, null, null, null);
        Assert.assertEquals(2000, resp.getRecords().size());*/
    }

    private List<UUID> createNOrguserIds(int n){
        List<UUID> uniqueIds = new ArrayList<>();
        for(int i = 0; i < n; i++){
            uniqueIds.add(UUID.randomUUID());
        }
        return uniqueIds;
    }

}