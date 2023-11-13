package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDevProdProfileMappings;
import io.levelops.commons.databases.models.database.dev_productivity.OuOrgUserMappings;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.organization.*;
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

public class OrgUserDevProductivityProfileMappingDatabaseServiceTest {
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
    OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService;

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
        orgUserDevProductivityProfileMappingDatabaseService = new OrgUserDevProductivityProfileMappingDatabaseService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        orgVersionsService.ensureTableExistence(company);
        orgUsersDatabaseService.ensureTableExistence(company);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);
        orgUserDevProductivityProfileMappingDatabaseService.ensureTableExistence(company);
    }


    @Test
    public void test() throws SQLException {
        UUID devProdParentProfile1 = UUID.randomUUID();
        UUID devProdParentProfile2 = UUID.randomUUID();
        UUID devProdParentProfile3 = UUID.randomUUID();

        UUID devProdProfileId1 = UUID.randomUUID();
        UUID devProdProfileId2 = UUID.randomUUID();
        UUID devProdProfileId3 = UUID.randomUUID();

        List<OrgUserDevProdProfileMappings> expectedUser0 = List.of(
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile1).devProductivityProfileId(devProdProfileId1).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile2).devProductivityProfileId(devProdProfileId2).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile3).devProductivityProfileId(devProdProfileId3).build()
        );

        ImmutablePair<Integer, Integer> result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,null, expectedUser0);
        Assert.assertEquals(3, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        DbListResponse<OrgUserDevProdProfileMappings> resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(0), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, List.of(devProdParentProfile1), null);
        Assert.assertEquals(resp.getRecords().size(), 1);
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, null, List.of(devProdProfileId2));
        Assert.assertEquals(resp.getRecords().size(), 1);

        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,null, expectedUser0);
        Assert.assertEquals(0, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(0), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());

        UUID devProdParentProfileId4 = UUID.randomUUID();
        UUID devProdParentProfileId5 = UUID.randomUUID();
        UUID devProdParentProfileId6 = UUID.randomUUID();

        UUID devProdProfileId4 = UUID.randomUUID();
        UUID devProdProfileid5 = UUID.randomUUID();
        UUID devProdProfileId6 = UUID.randomUUID();

        List<OrgUserDevProdProfileMappings> expectedUser1 = List.of(
                OrgUserDevProdProfileMappings.builder().orgUserRefId(1).devProductivityParentProfileId(devProdParentProfileId4).devProductivityProfileId(devProdProfileId4).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(1).devProductivityParentProfileId(devProdParentProfileId5).devProductivityProfileId(devProdProfileid5).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(1).devProductivityParentProfileId(devProdParentProfileId6).devProductivityProfileId(devProdProfileId6).build()
        );
        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,null,expectedUser1);
        Assert.assertEquals(3, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size() + expectedUser1.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(0), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(1), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser1.size());

        UUID devProdParentProfileId7 = UUID.randomUUID();
        UUID devProdParentProfileId8 = UUID.randomUUID();

        UUID devProdProfileId7 = UUID.randomUUID();
        UUID devProdProfileId8 = UUID.randomUUID();

        expectedUser0 = List.of(
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfileId7).devProductivityProfileId(devProdProfileId7).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile2).devProductivityProfileId(devProdProfileId2).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile3).devProductivityProfileId(devProdProfileId3).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityProfileId(devProdProfileId8).devProductivityParentProfileId(devProdParentProfileId8).build()
        );
        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,null, expectedUser0);
        Assert.assertEquals(2, result.getLeft().intValue());
        Assert.assertEquals(0, result.getRight().intValue());

        //Same parentProfileId different profileId
        devProdProfileId1 = UUID.randomUUID();
        List<OrgUserDevProdProfileMappings> expectedParentProfile1 = List.of(
                OrgUserDevProdProfileMappings.builder().orgUserRefId(0).devProductivityParentProfileId(devProdParentProfile1).devProductivityProfileId(devProdProfileId1).build()
        );
        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,devProdParentProfile1, expectedParentProfile1);
        Assert.assertEquals(1, result.getLeft().intValue());
        Assert.assertEquals(1, result.getRight().intValue());

        //Same parentProfileId different users
        expectedParentProfile1 = List.of(
                OrgUserDevProdProfileMappings.builder().orgUserRefId(2).devProductivityParentProfileId(devProdParentProfile1).devProductivityProfileId(devProdProfileId1).build(),
                OrgUserDevProdProfileMappings.builder().orgUserRefId(3).devProductivityParentProfileId(devProdParentProfile1).devProductivityProfileId(devProdProfileId1).build()
        );

        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,devProdParentProfile1,expectedParentProfile1);
        Assert.assertEquals(2, result.getLeft().intValue());
        Assert.assertEquals(1, result.getRight().intValue());

        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, null, null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size() + expectedUser1.size() + expectedParentProfile1.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(0), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser0.size());
        resp = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company, 0, 100, null, List.of(1), null, null);
        Assert.assertEquals(resp.getRecords().size(), expectedUser1.size());

        expectedUser0 = List.of();
        result = orgUserDevProductivityProfileMappingDatabaseService.syncMappings(company,devProdParentProfile1, List.of());
        Assert.assertEquals(0, result.getLeft().intValue());
        Assert.assertEquals(2, result.getRight().intValue());

    }

}