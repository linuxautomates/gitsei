package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUnitDashboardMapping;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class OrgUnitCategoryDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static ProductService workspaceDatabaseService;
    private static OrgUnitsDatabaseService orgUnitsService;
    private static OrgUnitHelper orgUnitHelper;
    private static Integration integration;
    private static UserService userService;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService  devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;



    private static Pair<UUID, Integer> uuidIntegerPair1;
    private static Pair<UUID, Integer> uuidIntegerPair2;
    private static Pair<UUID, Integer> uuidIntegerPair3;
    private static OrgUnitCategory orgGroup1, orgGroup2, orgGroup3, orgGroup4, orgGroup5, orgGroup6, orgGroup7,
            orgGroup8;
    private static String orgGroupId1, orgGroupId2, orgGroupId3, orgGroupId4, orgGroupId5, orgGroupId6, orgGroupId7,
            orgGroupId8;
    private static String dashBoardId1, dashBoardId2, dashBoardId3;
    private static String workSpaceId1, workSpaceId2, workSpaceId3;
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
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        OrgVersionsDatabaseService orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        workspaceDatabaseService = new ProductService(dataSource);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        workspaceDatabaseService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, null, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, new TagItemDBService(dataSource), usersService, orgVersionsService, new DashboardWidgetService(dataSource, mapper));
        orgUnitsService.ensureTableExistence(company);
        orgUnitHelper = new OrgUnitHelper(orgUnitsService, integrationService);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,mapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,mapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,mapper);
        devProductivityProfileDbService.ensureTableExistence(company);


        var firstVersion = orgVersionsService.insert(company, OrgVersion.OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        Integration integration2 = Integration.builder()
                .name("integration-name-" + 5)
                .status("status-" + 0).application("azure_devops").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId2 = integrationService.insert(company, integration2);

        Integration integration3 = Integration.builder()
                .name("integration-name-" + 6)
                .status("status-" + 0).application("jenkins").url("http://www.dummy.com")
                .satellite(false).build();
        String integrationId3 = integrationService.insert(company, integration3);
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("testread").username("cloudId").integrationType(integration2.getApplication())
                        .integrationId(Integer.parseInt(integrationId2)).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("SCMTrigger").integrationId(Integer.parseInt(integrationId2)).build()))
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
        var userId3 = usersService.upsert(company, orgUser3);
        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1,
                manager2
        );
        insertWorkSpaces();
        insertOrgGroups();
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .refId(1)
                .versions(Set.of(1))
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId2))
                        .integrationFilters(Map.of("cicd_user_ids", List.of("testread", "SYSTEM")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();
        uuidIntegerPair1 = orgUnitsService.insertForId(company, unit1);
        orgUnitHelper.activateVersion(company, uuidIntegerPair1.getLeft());
        DBOrgUnit unit2 = DBOrgUnit.builder()
                .name("unit2")
                .description("My unit2")
                .active(true)
                .refId(4)
                .versions(Set.of(2))
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId3))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 2))
                        .build()))
                .ouCategoryId(UUID.fromString(orgGroupId2))
                .build();
        uuidIntegerPair2 = orgUnitsService.insertForId(company, unit2);
        orgUnitHelper.activateVersion(company, uuidIntegerPair2.getLeft());
        DBOrgUnit unit3 = DBOrgUnit.builder()
                .name("unit3")
                .description("My unit3")
                .active(true)
                .refId(3)
                .versions(Set.of(2))
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId3))
                        .integrationFilters(Map.of())
                        .defaultSection(false)
                        .users(Set.of(1, 3))
                        .build()))
                .ouCategoryId(UUID.fromString(orgGroupId3))
                .build();
        uuidIntegerPair3 = orgUnitsService.insertForId(company, unit3);
        orgUnitHelper.activateVersion(company, uuidIntegerPair3.getLeft());
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

        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        insertOuDashBoardMappings();
    }

    @Test
    public void testCRUDOperations() throws SQLException {
        String idInserted = orgUnitCategoryDatabaseService.insert(company, OrgUnitCategory.builder()
                .name("Sample")
                .isPredefined(true)
                .enabled(true)
                .createdAt(Instant.now())
                .build());
        Optional<OrgUnitCategory> orgUnitCategory1 = orgUnitCategoryDatabaseService.get(company, idInserted);
        Assertions.assertThat(orgUnitCategory1.isPresent()).isTrue();
        orgUnitCategoryDatabaseService.delete(company, idInserted);

        idInserted = orgUnitCategoryDatabaseService.insert(company, OrgUnitCategory.builder()
                .name("Sample-1")
                .isPredefined(true)
                .enabled(true)
                .createdAt(Instant.now().plusSeconds(33030303L))
                .build());
        Optional<OrgUnitCategory> orgUnitCategory2 = orgUnitCategoryDatabaseService.get(company, idInserted);
        Assertions.assertThat(orgUnitCategory2.isPresent()).isTrue();

        orgUnitCategoryDatabaseService.delete(company, idInserted);

        Optional<OrgUnitCategory> orgUnitGroup = orgUnitCategoryDatabaseService.get(company, orgGroupId1);
        Assertions.assertThat(orgUnitGroup.isPresent()).isTrue();
        Assertions.assertThat(orgUnitGroup.get().getName()).isEqualTo("TEAM A");
        Assertions.assertThat(orgUnitGroup.get().getDescription()).isEqualTo("Sample team");

        Boolean update = orgUnitCategoryDatabaseService.update(
                company, OrgUnitCategory.builder()
                        .name("TEAM A")
                        .description("Sample team with updated description")
                        .build());
        Assertions.assertThat(update).isTrue();

        orgUnitGroup = orgUnitCategoryDatabaseService.get(company, orgGroupId1);
        Assertions.assertThat(orgUnitGroup.isPresent()).isTrue();
        Assertions.assertThat(orgUnitGroup.get().getName()).isEqualTo("TEAM A");
        Assertions.assertThat(orgUnitGroup.get().getDescription()).isEqualTo("Sample team");

        DbListResponse<OrgUnitCategory> orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(8);

        Boolean deletedOrgGroup = orgUnitCategoryDatabaseService.delete(company, orgGroupId2);
        Assertions.assertThat(deletedOrgGroup).isTrue();
        orgUnitGroup = orgUnitCategoryDatabaseService.get(company, orgGroupId2);
        Assertions.assertThat(orgUnitGroup.isPresent()).isFalse();

        /*
            Test List API
         */
        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("name", Set.of("TEAM A")).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TEAM A", "TEAM A", "TEAM A");
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getOusCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1, 0, 0);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getOuRefIds).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(List.of(1), List.of(), List.of());
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("is_predefined", false).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(6);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("enabled", true).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(5);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("enabled", false).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(2);
        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("enabled", null).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(7);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("workspace_id", Integer.parseInt(workSpaceId2)).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("SPRINT A");

        orgUnitGroupDbListResponse = orgUnitCategoryDatabaseService.list(company, QueryFilter.builder()
                .strictMatch("workspace_id", Integer.parseInt(workSpaceId3)).build(), 0, 10);
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse).isNotNull();
        Assertions.assertThat(orgUnitGroupDbListResponse.getTotalCount()).isEqualTo(4);
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getWorkspaceId).collect(Collectors.toList()))
                .isNotEmpty();
        Assertions.assertThat(orgUnitGroupDbListResponse.getRecords().stream().map(OrgUnitCategory::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("TEAM Z", "TEAM XXX", "TEAM YYY", "TEAM A");

        DbListResponse<DBOrgUnit> dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter
                .fromRequestFilters(Map.of("name", "Ou-Number-1", "active", false)), 0, 100);
        Assertions.assertThat(dbOrgUnitDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Ou-Number-1");

        /*
            Test Ou Group to Ou mappings
         */
        DbListResponse<DBOrgUnit> orgUnits = orgUnitsService.getOusForGroupId(company, UUID.fromString(orgGroupId1));
        Assertions.assertThat(orgUnits).isNotNull();
        Assertions.assertThat(orgUnits.getTotalCount()).isEqualTo(1);

        orgUnits = orgUnitsService.getOusForGroupId(company, UUID.fromString(orgGroupId5));
        Assertions.assertThat(orgUnits).isNotNull();
        Assertions.assertThat(orgUnits.getTotalCount()).isEqualTo(0);

        /*
            Test Ou Category creation upsert producing multiple Root ous
         */

        // Test for Update
        Optional<OrgUnitCategory> orgUnitCategory = orgUnitCategoryDatabaseService.get(company, UUID.fromString(orgGroupId7));
        Assertions.assertThat(orgGroupId7).isEqualTo(orgGroupId8);
        Assertions.assertThat(orgUnitCategory.get().getDescription()).isEqualTo("Updated Description");

        // Test for Single Root Ou produced
        DbListResponse<DBOrgUnit> rootOrgUnits = orgUnitsService.getOusForGroupId(company, UUID.fromString(orgGroupId8));
        Assertions.assertThat(rootOrgUnits).isNotNull();
        Assertions.assertThat(rootOrgUnits.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(rootOrgUnits.getRecords().get(0).getName()).isEqualTo("Ou-Number-2");

        /*
            Test Dashboard to Ou mappings
         */
        DbListResponse<OUDashboard> dashboardsIdsForOuId = orgUnitsService.getDashboardsIdsForOuId(company, uuidIntegerPair1.getLeft());
        Assertions.assertThat(dashboardsIdsForOuId).isNotNull();
        Assertions.assertThat(dashboardsIdsForOuId.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(dashboardsIdsForOuId.getRecords().stream().map(OUDashboard::getDashboardId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2, 3, 4);

        dashboardsIdsForOuId = orgUnitsService.getDashboardsIdsForOuId(company, uuidIntegerPair2.getLeft());
        Assertions.assertThat(dashboardsIdsForOuId).isNotNull();
        Assertions.assertThat(dashboardsIdsForOuId.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dashboardsIdsForOuId.getRecords().stream().map(OUDashboard::getDashboardId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2);

        DbListResponse<OrgUnitCategory> unitCategoryDbListResponse = orgUnitCategoryDatabaseService.filterByDashboard(company, Integer.parseInt(workSpaceId1), Integer.parseInt(dashBoardId1), QueryFilter.builder().strictMatch("enabled", true).build(), 0, 20);
        Assertions.assertThat(unitCategoryDbListResponse.getRecords().size()).isEqualTo(1);
    }

    @Test
    public void testDashboardIdsFilter() throws SQLException {
        DbListResponse<DBOrgUnit> dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 2)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(2);

        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 3)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("unit1");

        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 4)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("unit1");

        //Negative Test
        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 33443)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(0);

        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 4)
                .strictMatch("parent_ref_id", null)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("unit1");

        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 4)
                .strictMatch("parent_ref_id", "1")
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(0);


    }

    @Test
    public void testWorkSpaceOuMappingsByOuRefIds() throws SQLException {
        Map<String,List<String>> workspaceOuMappings = orgUnitCategoryDatabaseService.getWorkspaceOuRefIdMappingsByOuRefIds(company,List.of(uuidIntegerPair1.getRight(),uuidIntegerPair3.getRight()));
        Assertions.assertThat(workspaceOuMappings).isNotEmpty();
        Assertions.assertThat(workspaceOuMappings).containsAllEntriesOf(Map.of("1", List.of("1"), "2", List.of("3"), "3", List.of("1")));
    }

    private static void insertOuDashBoardMappings() {
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair1.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId1))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair1.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId3))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair1.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId2))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair2.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId1))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair1.getLeft())
                .dashboardId(Integer.parseInt(dashBoardId3))
                .build());
    }

    private static void insertOrgGroups() throws SQLException {
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .workspaceId(Integer.parseInt(workSpaceId1))
                .isPredefined(true)
                .enabled(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        orgGroup2 = OrgUnitCategory.builder()
                .name("TEAM B")
                .description("Sample team")
                .isPredefined(true)
                .workspaceId(Integer.parseInt(workSpaceId1))
                .enabled(false)
                .build();
        orgGroupId2 = orgUnitCategoryDatabaseService.insert(company, orgGroup2);

        orgGroup3 = OrgUnitCategory.builder()
                .name("SPRINT A")
                .description("Sample team")
                .isPredefined(false)
                .workspaceId(Integer.parseInt(workSpaceId2))
                .enabled(true)
                .build();
        orgGroupId3 = orgUnitCategoryDatabaseService.insert(company, orgGroup3);

        orgGroup4 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team -1")
                .isPredefined(false)
                .enabled(false)
                .workspaceId(Integer.parseInt(workSpaceId3))
                .build();
        orgGroupId4 = orgUnitCategoryDatabaseService.insert(company, orgGroup4);

        orgGroup5 = OrgUnitCategory.builder()
                .name("TEAM Z")
                .enabled(true)
                .description("Sample team")
                .workspaceId(Integer.parseInt(workSpaceId3))
                .isPredefined(false)
                .build();
        orgGroupId5 = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgGroup5).getId().toString();

        orgGroup6 = OrgUnitCategory.builder()
                .name("TEAM XXX")
                .enabled(true)
                .rootOuName("Ou-Number-1")
                .description("Sample team with root Ou name")
                .workspaceId(Integer.parseInt(workSpaceId3))
                .isPredefined(false)
                .build();
        orgGroupId6 = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgGroup6).getId().toString();

        orgGroup7 = OrgUnitCategory.builder()
                .name("TEAM YYY")
                .enabled(true)
                .rootOuName("Ou-Number-2")
                .description("Sample team with root Ou name")
                .workspaceId(Integer.parseInt(workSpaceId3))
                .isPredefined(false)
                .build();
        orgGroupId7 = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgGroup7).getId().toString();

        orgGroup8 = OrgUnitCategory.builder()
                .name("TEAM YYY")
                .enabled(true)
                .rootOuName("Ou-Number-3")
                .description("Updated Description")
                .workspaceId(Integer.parseInt(workSpaceId3))
                .isPredefined(false)
                .build();
        orgGroupId8 = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgGroup8).getId().toString();
    }


    private static void insertWorkSpaces() throws SQLException {
        User user = User.builder()
                .email("email")
                .company(company)
                .firstName("firstName")
                .lastName("lastName")
                .metadata(Map.of())
                .mfaEnabled(false)
                .mfaEnforced(false)
                .passwordAuthEnabled(false)
                .samlAuthEnabled(false)
                .scopes(Map.of())
                .userType(RoleType.ADMIN)
                .bcryptPassword("bcryptPassword")
                .build();
        var userId = userService.insert(company, user);
        Product build = Product.builder().name("WorkSpace-1")
                .description("This is a sample workspace description")
                .integrationIds(Set.of(1))
                .createdAt(Instant.EPOCH.getEpochSecond())
                .key("w1")
                .ownerId(userId)
                .build();
        workSpaceId1 = workspaceDatabaseService.insert(company, build);

        build = Product.builder().name("WorkSpace-2")
                .description("This is a sample workspace-2 description")
                .integrationIds(Set.of(1))
                .createdAt(Instant.EPOCH.getEpochSecond())
                .key("w2")
                .ownerId(userId)
                .build();
        workSpaceId2 = workspaceDatabaseService.insert(company, build);

        build = Product.builder().name("WorkSpace-3")
                .description("This is a sample workspace-3 description")
                .integrationIds(Set.of(1))
                .createdAt(Instant.EPOCH.getEpochSecond())
                .key("w3")
                .ownerId(userId)
                .build();
        workSpaceId3 = workspaceDatabaseService.insert(company, build);
    }

}
