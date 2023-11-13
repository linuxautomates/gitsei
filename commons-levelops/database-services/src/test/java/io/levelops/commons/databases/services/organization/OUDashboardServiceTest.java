/*
package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitDashboardMapping;
import io.levelops.commons.databases.models.database.organization.OrgUnitGroup;
import io.levelops.commons.databases.models.database.organization.OrgUnitGroupToOrgUnitMapping;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class OUDashboardServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitsDatabaseService orgUnitsService;
    private static OUDashboardService ouDashboardService;
    private static OrgUnitHelper unitsHelper;
    private static Pair<UUID, Integer> uuidIntegerPair1;
    private static Pair<UUID, Integer> uuidIntegerPair2;
    private static Pair<UUID, Integer> uuidIntegerPair3;
    private static Pair<UUID, Integer> uuidIntegerPair4;
    private static OrgUnitGroup orgGroup1, orgGroup2, orgGroup3, orgGroup4, orgGroup5;
    private static String orgGroupId1;
    private static String orgGroupId2;
    private static String orgGroupId5;
    private static String dashBoardId1, dashBoardId2, dashBoardId3, dashBoardId4;
    private static String integrationId1, integrationId2;
    private static Set<OrgUserId> managers;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException {

        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);

        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        OrgVersionsDatabaseService orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        UserService userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, new TagItemDBService(dataSource), usersService, orgVersionsService, new DashboardWidgetService(dataSource, mapper));
        ouDashboardService = new OUDashboardService(dataSource, mapper, orgUnitsService);
        orgUnitsService.ensureTableExistence(company);
        ouDashboardService.ensureTableExistence(company);
        ouDashboardService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, orgUnitsService);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        unitsHelper = new OrgUnitHelper(orgUnitsService, integrationService);

        var firstVersion = orgVersionsService.insert(company, OrgVersion.OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        Integration integration2 = Integration.builder()
                .name("integration-name-" + 5)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId1 = integrationService.insert(company, integration2);

        Integration integration3 = Integration.builder()
                .name("integration-name-" + 6)
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        integrationId2 = integrationService.insert(company, integration3);
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("testread").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integrationId1)).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SCMTrigger").integrationId(Integer.parseInt(integrationId1)).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = usersService.upsert(company, orgUser2);
        var orgUser3 = DBOrgUser.builder()
                .email("email3")
                .fullName("fullName3")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SYSTEM").username("cloudId").integrationType(integration3.getApplication())
                        .integrationId(Integer.parseInt(integrationId2)).build()))
                .versions(Set.of(1))
                .build();
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        managers = Set.of(
                manager1,
                manager2
        );
        insertOrgGroups();
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId1))
                        .integrationFilters(Map.of("cicd_user_ids", List.of("testread", "SYSTEM")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .path("/unit1")
                .ouCategoryId(List.of(UUID.fromString(orgGroupId1), UUID.fromString(orgGroupId5)))
                .build();
        uuidIntegerPair1 = orgUnitsService.insertForId(company, unit1);
        orgUnitsService.update(company, uuidIntegerPair1.getLeft(), true);

        DBOrgUnit unit2 = DBOrgUnit.builder()
                .name("unit2")
                .description("My unit2")
                .active(true)
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId2))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .path("/unit1/unit2")
                .parentRefId(uuidIntegerPair1.getRight())
                .ouCategoryId(List.of(UUID.fromString(orgGroupId1), UUID.fromString(orgGroupId5)))
                .build();
        uuidIntegerPair2 = orgUnitsService.insertForId(company, unit2);
        orgUnitsService.update(company, uuidIntegerPair2.getLeft(), true);

        DBOrgUnit unit3 = DBOrgUnit.builder()
                .name("unit3")
                .description("My unit3")
                .active(true)
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId2))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 3))
                        .build()))
                .path("/unit1/unit2/unit3")
                .parentRefId(uuidIntegerPair2.getRight())
                .build();
        uuidIntegerPair3 = orgUnitsService.insertForId(company, unit3);
        orgUnitsService.update(company, uuidIntegerPair3.getLeft(), true);

        DBOrgUnit unit4 = DBOrgUnit.builder()
                .name("unit4")
                .description("My unit4")
                .active(true)
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId2))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 3))
                        .build()))
                .path("/unit1/unit2/unit3/unit4")
                .parentRefId(uuidIntegerPair3.getRight())
                .build();
        uuidIntegerPair4 = orgUnitsService.insertForId(company, unit4);
        orgUnitsService.update(company, uuidIntegerPair4.getLeft(), true);

        dashBoardId1 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 1")
                .firstName("DASH")
                .lastName("BOARD")
                .isDefault(true)
                .type("TYPE 1")
                .email("example@levelops.io")
                .build());

        dashBoardId2 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 2")
                .firstName("DASH1")
                .lastName("BOARD2")
                .isDefault(false)
                .type("TYPE 2")
                .email("example@levelops.io")
                .build());

        dashBoardId3 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 3")
                .firstName("DASH1")
                .lastName("BOARD2")
                .isDefault(false)
                .type("TYPE 3")
                .email("example@levelops.io")
                .build());

        dashBoardId4 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 4")
                .firstName("DASH1")
                .lastName("BOARD2")
                .isDefault(false)
                .type("TYPE 4")
                .email("example@levelops.io")
                .build());
        insertOuDashBoardMappings();
    }

    private static void insertOuDashBoardMappings() {
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair1.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId1))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair2.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId2))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair3.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId3))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair4.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId4))
                .build());
    }

    private static void insertOuGroupOuIdMappings() throws SQLException {
        orgUnitCategoryDatabaseService.insert(company, OrgUnitGroupToOrgUnitMapping.builder()
                .orgUnitGroupId(UUID.fromString(orgGroupId1))
                .ouRefId("1")
                .orgUnitId(uuidIntegerPair1.getLeft())
                .build());
        orgUnitCategoryDatabaseService.insert(company, OrgUnitGroupToOrgUnitMapping.builder()
                .orgUnitGroupId(UUID.fromString(orgGroupId1))
                .ouRefId("2")
                .orgUnitId(uuidIntegerPair2.getLeft())
                .build());
        orgUnitCategoryDatabaseService.insert(company, OrgUnitGroupToOrgUnitMapping.builder()
                .orgUnitGroupId(UUID.fromString(orgGroupId5))
                .orgUnitId(uuidIntegerPair2.getLeft())
                .ouRefId("2")
                .build());
        orgUnitCategoryDatabaseService.insert(company, OrgUnitGroupToOrgUnitMapping.builder()
                .orgUnitGroupId(UUID.fromString(orgGroupId5))
                .orgUnitId(uuidIntegerPair1.getLeft())
                .ouRefId("1")
                .build());
    }

    private static void insertOrgGroups() throws SQLException {
        orgGroup1 = OrgUnitGroup.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        orgGroup2 = OrgUnitGroup.builder()
                .name("TEAM B")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId2 = orgUnitCategoryDatabaseService.insert(company, orgGroup2);

        orgGroup3 = OrgUnitGroup.builder()
                .name("SPRINT A")
                .description("Sample team")
                .isPredefined(false)
                .build();
        orgUnitCategoryDatabaseService.insert(company, orgGroup3);

        orgGroup4 = OrgUnitGroup.builder()
                .name("TEAM A")
                .description("Sample team -1")
                .isPredefined(false)
                .build();
        orgUnitCategoryDatabaseService.insert(company, orgGroup4);

        orgGroup5 = OrgUnitGroup.builder()
                .name("TEAM Z")
                .description("Sample team")
                .isPredefined(false)
                .build();
        orgGroupId5 = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgGroup5).getId().toString();
    }

    @Test
    public void ouDashboardMapping() throws SQLException {
        DBOrgUnit dbUnit1 = orgUnitsService.get(company, uuidIntegerPair1.getRight()).get();
        Assertions.assertThat(dbUnit1.getRefId()).isEqualTo(uuidIntegerPair1.getRight());

        var results = orgUnitsService.filter(company, null, 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(6);

        DBOrgUnit dbUnit2 = orgUnitsService.get(company, uuidIntegerPair2.getRight()).get();
        Assertions.assertThat(dbUnit2.getRefId()).isEqualTo(uuidIntegerPair2.getRight());
        OUDashboardService.DashboardFilter dashboardFilter = OUDashboardService.DashboardFilter.builder().ouId(uuidIntegerPair2.getLeft()).build();
        DbListResponse<OUDashboard> dbOUDashboardList = ouDashboardService.listByOuFilters(company, dashboardFilter, 0, 10, null);
        Assertions.assertThat(dbOUDashboardList.getTotalCount()).isGreaterThan(0);
        List<OUDashboard> ouDashBoards = new ArrayList<OUDashboard>();
        OUDashboard ouDashboard1 = OUDashboard.builder().dashboardId(Integer.parseInt(dashBoardId1)).ouId(uuidIntegerPair1.getLeft()).dashboardOrder(1).isDefault(false).build();
        OUDashboard ouDashboard2 = OUDashboard.builder().dashboardId(Integer.parseInt(dashBoardId1)).ouId(uuidIntegerPair2.getLeft()).dashboardOrder(1).isDefault(false).build();
        ouDashBoards.add(ouDashboard1);
        ouDashBoards.add(ouDashboard2);
        ouDashboardService.updateOuMapping(company, ouDashBoards, uuidIntegerPair2.getLeft());
        DbListResponse<OUDashboard> dbOUDashboardList1 = ouDashboardService.listByOuFilters(company, dashboardFilter, 0, 10, null);
        Assertions.assertThat(dbOUDashboardList1.getTotalCount()).isEqualTo(1);
        List<OUDashboard> ouDashBoards1 = new ArrayList<OUDashboard>();
        ouDashBoards1.add(ouDashboard1);
        ouDashboardService.updateOuMapping(company, ouDashBoards1, uuidIntegerPair2.getLeft());
        DbListResponse<OUDashboard> dbOUDashboardList2 = ouDashboardService.listByOuFilters(company, dashboardFilter, 0, 10, null);
        OUDashboard dbDashboard = dbOUDashboardList2.getRecords().get(0);
        Assertions.assertThat(dbDashboard.getDashboardId()).isEqualTo(ouDashboard1.getDashboardId());
        Assertions.assertThat(dbDashboard.getOuId()).isEqualTo(ouDashboard2.getOuId());
        Assertions.assertThat(dbDashboard.getDashboardOrder()).isEqualTo(ouDashboard2.getDashboardOrder());
        Assertions.assertThat(dbOUDashboardList1.getTotalCount()).isEqualTo(1);
        ouDashboardService.delete(company, uuidIntegerPair2.getLeft());
        DbListResponse<OUDashboard> dbOUDashboardListAfterDel = ouDashboardService.listByOuFilters(company, dashboardFilter, 0, 10, null);
        Assertions.assertThat(dbOUDashboardListAfterDel.getTotalCount()).isEqualTo(0);

    }

    @Test
    public void testInheritedDashboard() throws SQLException {
        DbListResponse<OUDashboard> ouDashboardDbListResponse = ouDashboardService.listByOuFilters(company,
                OUDashboardService.DashboardFilter.builder()
                        .inherited(true)
                        .ouId(uuidIntegerPair2.getLeft())
                        .build(), 0, 100, null);
        Assertions.assertThat(ouDashboardDbListResponse.getRecords().size()).isEqualTo(1);

        ouDashboardDbListResponse = ouDashboardService.listByOuFilters(company,
                OUDashboardService.DashboardFilter.builder()
                        .inherited(true)
                        .ouId(uuidIntegerPair3.getLeft())
                        .build(), 0, 100, null);
        Assertions.assertThat(ouDashboardDbListResponse.getRecords().size()).isEqualTo(2);
    }

    @Test
    public void testDashboardAssociationWithNewerVersion() throws SQLException {
        DbListResponse<OUDashboard> olderResponse = ouDashboardService.listByOuFilters(company,
                OUDashboardService.DashboardFilter.builder()
                        .name("Dashboard 4")
                .build(), 0, 100, null);

        DBOrgUnit unit4 = DBOrgUnit.builder()
                .name("unit4")
                .description("My new unit4")
                .active(true)
                .refId(4)
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId2))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .path("/unit1/unit2/unit3/unit4")
                .parentRefId(uuidIntegerPair3.getRight())
                .build();

        unitsHelper.updateUnits(company, Stream.of(unit4));

        DbListResponse<OUDashboard> newerResponse = ouDashboardService.listByOuFilters(company,
                OUDashboardService.DashboardFilter.builder()
                        .name("Dashboard 4")
                        .build(), 0, 100, null);

        Assertions.assertThat(olderResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(newerResponse.getRecords().size()).isEqualTo(2);
        Assertions.assertThat(olderResponse.getRecords().get(0).getDashboardId()).isEqualTo(newerResponse.getRecords().get(0).getDashboardId());
    }

}
*/
