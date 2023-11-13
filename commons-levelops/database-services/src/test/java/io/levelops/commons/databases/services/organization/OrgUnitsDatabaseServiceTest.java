package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUnitDashboardMapping;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
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
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class OrgUnitsDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static TagItemDBService tagItemDbService;
    private static OrgUnitsDatabaseService orgUnitsService;
    private static OrgUsersDatabaseService usersService;
    private static UserService userService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;
    private static DashboardWidgetService dws;
    private static String dashboardId,dashboardId2;
    private static String userId;
    private static ProductService productService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        usersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        usersService.ensureTableExistence(company);
        // tagItemDbService = new TagItemDBService(dataSource);
        // tagItemDbService.ensureTableExistence(company);
        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, null, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemDbService, usersService, orgVersionsService, dashboardWidgetService);
        orgUnitsService.ensureTableExistence(company);

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

        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        dws = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        dws.ensureTableExistence(company);
        userId = userService.insert(company, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId)
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                        .displayInfo(Map.of("description", "test"))
                        .build()))
                .build();
        dashboardId = dws.insert(company, dashboard1);
        Dashboard dashboard2 = Dashboard.builder()
                .name("name2")
                .type("type2")
                .ownerId(userId)
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                        .displayInfo(Map.of("description", "test"))
                        .build()))
                .build();
        dashboardId2 = dws.insert(company, dashboard2);


    }

    @Test
    public void tesSimpleUnits() throws SQLException {
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));
        Product product = Product.builder().name("Test Prod")
                .description("test prod")
                .integrationIds(Set.of(integrationId1, integrationId2))
                .key("key")
                .ownerId("Own").build();
        String productId = productService.insert(company, product);
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId1 = Integer.valueOf(usersService.insert(company, orgUser1));

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = Integer.valueOf(usersService.insert(company, orgUser2));
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .workspaceId(Integer.parseInt(productId))
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        var unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("The unit1")
                .active(true)
                .path("google/youtube")
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .workspaceId(Integer.parseInt(productId))
                .noOfDashboards(0)
                //     .managers(Set.of())
                .managers(Set.of(
                        OrgUserId.builder().refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                        OrgUserId.builder().refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .sections(Set.of(
                        DBOrgContentSection.builder().users(Set.of(1, 2)).dynamicUsers(Map.of("city", "chicago")).build(),
                        DBOrgContentSection.builder().users(Set.of(2)).dynamicUsers(Map.of("city", "LA")).defaultSection(true).build()))
                .build();
        var unitId1 = orgUnitsService.insertForId(company, unit1);

        orgUnitsService.update(company, unitId1.getLeft(), true);
        DBOrgUnit dbUnit1 = orgUnitsService.get(company, unitId1.getRight()).get();
        unit1 = unit1.toBuilder().id(dbUnit1.getId())
                .refId(dbUnit1.getRefId())
                .parentRefId(null)
                .sections(unit1.getSections().stream()
                        .map(section -> dbUnit1.getSections().stream()
                                .map(db -> {
                                    if (section.getDynamicUsers().get("city").toString().equalsIgnoreCase(db.getDynamicUsers().get("city").toString())) {
                                        return section.toBuilder().id(db.getId()).build();
                                    }
                                    return null;
                                })
                                .filter(item -> item != null)
                                .findFirst()
                        )
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet())
                )
                .managers(Set.of(
                        OrgUserId.builder().id(dbUnit1.getManagers().stream().filter(m -> m.getRefId() == userId1).map(m -> m.getId()).findFirst().orElse(UUID.randomUUID())).refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                        OrgUserId.builder().id(dbUnit1.getManagers().stream().filter(m -> m.getRefId() == userId2).map(m -> m.getId()).findFirst().orElse(UUID.randomUUID())).refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .versions(Set.of(1))
                .path("/unit1")
                .createdAt(dbUnit1.getCreatedAt())
                .updatedAt(dbUnit1.getUpdatedAt())
                .defaultDashboardId(0)
                .build();
        Assertions.assertThat(dbUnit1).isEqualToIgnoringGivenFields(unit1,"admins");

        var results = orgUnitsService.filter(company, null, 0, 10);
        Assertions.assertThat(results.getRecords()).hasSize(1);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);

        results = orgUnitsService.filter(company, QueryFilter.builder().count(false).build(), 0, 10);
        Assertions.assertThat(results.getRecords()).hasSize(1);
        Assertions.assertThat(results.getTotalCount()).isNull();

        var unit2 = DBOrgUnit.builder()
                .name("The unit2")
                .description("My unit2")
                .active(true)
                .managers(Set.of())
                .admins(Set.of())
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .workspaceId(Integer.parseInt(productId))
                .noOfDashboards(0)
                .path("/google/Youtube")
                //     .managers(Set.of(
                //         OrgUserId.builder().refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                //         OrgUserId.builder().refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .sections(Set.of(
                        DBOrgContentSection.builder().integrationId(integrationId1).users(Set.of(1, 2)).integrationType(IntegrationType.fromString(integration1.getApplication())).integrationName(integration1.getName()).build(),
                        DBOrgContentSection.builder().integrationId(integrationId2).integrationFilters(Map.of("test", "ok")).users(Set.of(1, 2)).integrationType(IntegrationType.fromString(integration2.getApplication())).integrationName(integration2.getName()).build()))
                .build();
        var unitId2 = orgUnitsService.insertForId(company, unit2);
        orgUnitsService.update(company, unitId2.getLeft(), true);

        DBOrgUnit dbUnit2 = orgUnitsService.get(company, unitId2.getRight()).get();
        unit2 = unit2.toBuilder().id(dbUnit2.getId())
                .refId(dbUnit2.getRefId())
                .sections(unit2.getSections().stream()
                        .map(section -> dbUnit2.getSections().stream()
                                .filter(s -> s.getIntegrationId() == section.getIntegrationId())
                                .map(s -> section.toBuilder().id(s.getId()).build())
                                .findFirst().get())
                        .collect(Collectors.toSet()))
                .versions(Set.of(1))
                .ouCategoryId(dbUnit2.getOuCategoryId())
                .defaultDashboardId(0)
                .noOfDashboards(0)
                .path("/The unit2")
                .parentRefId(null)
                .createdAt(dbUnit2.getCreatedAt())
                .updatedAt(dbUnit2.getUpdatedAt())
                .build();
        Assertions.assertThat(dbUnit2).isEqualTo(unit2);

        var versions = orgUnitsService.getVersions(company, unitId2.getRight());
        Assertions.assertThat(versions.size()).isEqualTo(1);

        var values = orgUnitsService.getValues(company, "name", null, 0, 10);
        Assertions.assertThat(values.getTotalCount()).isEqualTo(2);

        var valueSearch = orgUnitsService.getValues(company, "name", QueryFilter.builder().partialMatch("name", "The").build(), 0, 10);
        Assertions.assertThat(valueSearch.getTotalCount()).isEqualTo(1);

        var search = orgUnitsService.filter(company, QueryFilter.builder().partialMatch("name", "The").build(), 0, 10);
        Assertions.assertThat(search.getTotalCount()).isEqualTo(1);

        var search2 = orgUnitsService.filter(company, QueryFilter.builder().strictMatch("name", "The unit2").build(), 0, 10);
        Assertions.assertThat(search2.getTotalCount()).isEqualTo(1);

        var search3 = orgUnitsService.filter(company,
                QueryFilter.builder().strictMatch("name", Set.of("The unit2")).build(), 0, 10);
        Assertions.assertThat(search3.getTotalCount()).isEqualTo(1);
        var search4 = orgUnitsService.filter(company, QueryFilter.builder().strictMatch("path", "/google/Youtube").build(), 0, 10);
        Assertions.assertThat(search4.getTotalCount()).isEqualTo(0);
        var search5 = orgUnitsService.filter(company, QueryFilter.builder().strictMatch("ou_group_id", orgGroupId1).build(), 0, 10);
        Assertions.assertThat(search5.getTotalCount()).isEqualTo(2);
        var search6 = orgUnitsService.filter(company, QueryFilter.builder().partialMatch("path", "google").build(), 0, 10);
        Assertions.assertThat(search6.getTotalCount()).isEqualTo(0);
        DbListResponse<DBOrgUser> dbOrgUsers = usersService.filter(company, QueryFilter.builder().strictMatch("email", Set.of("email1","email2")).build(), 0, 10);
        List<UUID> dbOrgUserIds = dbOrgUsers.getRecords().stream().sorted(Comparator.comparingInt(OrgUserId::getRefId)).map(DBOrgUser::getId).collect(Collectors.toList());
        List<UUID> actualOrgUserIds = orgUnitsService.getActiveMembersByOuId(company, dbUnit1.getId(), 0, 10);
        Assertions.assertThat(actualOrgUserIds).isEqualTo(dbOrgUserIds);
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .workspaceId(Integer.parseInt(productId))
                .isPredefined(true)
                .enabled(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        productService.deleteOuIntegration(company, Integer.parseInt(productId), List.of(integrationId2));

    }

    //     @Test
    public void test() throws SQLException {
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));

        var integration3 = Integration.builder()
                .description("description1")
                .name("integ3")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId3 = Integer.valueOf(integrationService.insert(company, integration3));

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId1 = UUID.fromString(usersService.insert(company, orgUser1));

        DBOrgUser dbUser1 = usersService.get(company, userId1).get();
        Assertions.assertThat(dbUser1).isEqualToComparingFieldByField(orgUser1.toBuilder().id(userId1).refId(dbUser1.getRefId()).build());
        DBOrgUser dbUser1a = usersService.get(company, dbUser1.getRefId()).get();
        Assertions.assertThat(dbUser1).isEqualToComparingFieldByField(dbUser1a);

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = UUID.fromString(usersService.insert(company, orgUser2));
        var results = usersService.filter(company, null, 0, 10);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");
        Assertions.assertThat(results.getRecords().stream().flatMap(user -> user.getIds().stream()).map(LoginId::getUsername).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("cloudId", "cloudId2");

        userIdentityService.batchUpsert(company, List.of(
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("cloudId")
                        .displayName("sample-Cog-1")
                        .originalDisplayName("sample-Cog-1")
                        .build(),
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("cloudId2")
                        .displayName("sample-Cog-2")
                        .originalDisplayName("sample-Cog-2")
                        .build()));

        results = usersService.filter(company, null, 0, 10);

        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");
        Assertions.assertThat(results.getRecords().stream().flatMap(user -> user.getIds().stream()).map(LoginId::getUsername).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2");

        results = usersService.filter(company, QueryFilter.builder().strictMatch("full_name", "fullName1").build(), 0, 10);

        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().get(0).getFullName()).isEqualTo("fullName1");

        var userId2a = UUID.fromString(usersService.insert(company, orgUser2));

        Assertions.assertThat(userId2a).isEqualTo(userId2);

        results = usersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId1).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");

        var versionId = orgVersionsService.insert(company, OrgAssetType.USER);

        var currentVersion = orgVersionsService.getActive(company, OrgAssetType.USER).get().getId();
        Assertions.assertThat(versionId).isNotEqualTo(currentVersion);

        orgVersionsService.update(company, versionId, true);
        orgVersionsService.update(company, currentVersion, false);

        var userId2b = UUID.fromString(usersService.insert(company, orgUser2));
        Assertions.assertThat(userId2b).isNotEqualTo(userId2a);

        results = usersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId1).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        results = usersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId2).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(0);

        var ver1 = orgVersionsService.get(company, currentVersion).get();
        var ver2 = orgVersionsService.get(company, versionId).get();

        results = usersService.filter(company, QueryFilter.builder().strictMatch("version", ver1.getVersion()).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");

        results = usersService.filter(company, QueryFilter.builder().strictMatch("version", ver2.getVersion()).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        var v3Id = orgVersionsService.insert(company, OrgAssetType.USER);
        var v3 = orgVersionsService.get(company, v3Id).get();

        results = usersService.filter(company, QueryFilter.builder().strictMatch("version", v3.getVersion()).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(0);

        var exclusions = Set.<Integer>of();
        usersService.upgradeUsersVersion(company, v3.getVersion(), ver2.getVersion(), exclusions);

        results = usersService.filter(company, QueryFilter.builder().strictMatch("version", v3.getVersion()).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        var v4Id = orgVersionsService.insert(company, OrgAssetType.USER);
        var v4 = orgVersionsService.get(company, v4Id).get();
        var user2 = usersService.get(company, userId2).get();
        exclusions = Set.<Integer>of(user2.getRefId());
        usersService.upgradeUsersVersion(company, v4.getVersion(), ver1.getVersion(), exclusions);

        results = usersService.filter(company, QueryFilter.builder().strictMatch("version", v4.getVersion()).build(), 0, 10);
        Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1");
    }

    @Test
    public void testOuDashboardMappings() throws SQLException {
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit11")
                .description("My unit1")
                .active(true)
                .refId(1)
                .versions(Set.of(1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationFilters(Map.of("cicd_user_ids", List.of("testread", "SYSTEM")))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();
        Pair<UUID, Integer> uuidIntegerPair = orgUnitsService.insertForId(company, unit1);
        new OrgUnitHelper(orgUnitsService, integrationService).activateVersion(company, uuidIntegerPair.getLeft());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair.getLeft())
                .dashboardId(Integer.parseInt(dashboardId))
                .build());
        orgUnitsService.insertOuDashboardMapping(company, OrgUnitDashboardMapping.builder()
                .orgUnitId(uuidIntegerPair.getLeft())
                .dashboardId(Integer.parseInt(dashboardId2))
                .build());
        DbListResponse<DBOrgUnit> dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 2)
                .build(), 0, 1000);

        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("unit11");
        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
                .strictMatch("dashboard_id", 3)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().stream().map(DBOrgUnit::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("unit11");
        dbOrgUnitDbListResponse = orgUnitsService.filter(company, QueryFilter.builder()
            .strictMatch("dashboard_id", 11)
                .build(), 0, 1000);
        Assertions.assertThat(dbOrgUnitDbListResponse.getRecords().size()).isEqualTo(0);

    }

    @Test
    public void testOuFirstLevelChild() throws SQLException {
        var integration1 = Integration.builder()
                .description("description-A1")
                .name("integA1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("description-A2")
                .name("integA2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));
        Product product = Product.builder().name("Test Prod A")
                .description("test prod A")
                .integrationIds(Set.of(integrationId1, integrationId2))
                .key("prod-A")
                .ownerId("Own").build();
        String productId = productService.insert(company, product);
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId1 = Integer.valueOf(usersService.insert(company, orgUser1));

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = Integer.valueOf(usersService.insert(company, orgUser2));
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .workspaceId(Integer.parseInt(productId))
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        var unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("The unit1")
                .active(true)
                .path("google/youtube")
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .workspaceId(Integer.parseInt(productId))
                .noOfDashboards(0)
                //     .managers(Set.of())
                .managers(Set.of(
                        OrgUserId.builder().refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                        OrgUserId.builder().refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .sections(Set.of(
                        DBOrgContentSection.builder().users(Set.of(1, 2)).dynamicUsers(Map.of("city", "BLR")).build(),
                        DBOrgContentSection.builder().users(Set.of(2)).dynamicUsers(Map.of("city", "DEL")).defaultSection(true).build()))
                .build();
        var unitId1 = orgUnitsService.insertForId(company, unit1);

        orgUnitsService.update(company, unitId1.getLeft(), true);
        DBOrgUnit dbUnit1 = orgUnitsService.get(company, unitId1.getRight()).get();
        var unit2 = DBOrgUnit.builder()
                .name("unit1")
                .description("The unit1")
                .active(true)
                .path("google/youtube")
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .workspaceId(Integer.parseInt(productId))
                .noOfDashboards(0)
                .parentRefId(dbUnit1.getRefId())
                .managers(Set.of(
                        OrgUserId.builder().refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                        OrgUserId.builder().refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .sections(Set.of(
                        DBOrgContentSection.builder().users(Set.of(1, 2)).dynamicUsers(Map.of("city", "BLR")).build(),
                        DBOrgContentSection.builder().users(Set.of(2)).dynamicUsers(Map.of("city", "DEL")).defaultSection(true).build()))
                .build();
        var unitId2 = orgUnitsService.insertForId(company, unit2);

        orgUnitsService.update(company, unitId1.getLeft(), true);
        DBOrgUnit dbUnit2 = orgUnitsService.get(company, unitId1.getRight()).get();
        var unit3 = DBOrgUnit.builder()
                .name("unit1")
                .description("The unit1")
                .active(true)
                .path("google/youtube")
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .workspaceId(Integer.parseInt(productId))
                .noOfDashboards(0)
                .parentRefId(dbUnit2.getRefId())
                .managers(Set.of(
                        OrgUserId.builder().refId(userId1).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build(),
                        OrgUserId.builder().refId(userId2).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build()))
                .sections(Set.of(
                        DBOrgContentSection.builder().users(Set.of(1, 2)).dynamicUsers(Map.of("city", "BLR")).build(),
                        DBOrgContentSection.builder().users(Set.of(2)).dynamicUsers(Map.of("city", "DEL")).defaultSection(true).build()))
                .build();
        var unitId3 = orgUnitsService.insertForId(company, unit3);

        orgUnitsService.update(company, unitId3.getLeft(), true);
        DBOrgUnit dbUnit3 = orgUnitsService.get(company, unitId1.getRight()).get();
        Set<Integer> orgRefList=orgUnitsService.getFirstLevelChildrenRefIds(company,List.of(dbUnit1.getRefId()));
        Assertions.assertThat(orgRefList.size()).isEqualTo(1);
    }

}
