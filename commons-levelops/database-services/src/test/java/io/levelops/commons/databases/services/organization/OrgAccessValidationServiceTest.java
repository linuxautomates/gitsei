package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class OrgAccessValidationServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgUsersDatabaseService orgUsersService;
    private static OrgUnitsDatabaseService orgUnitsService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static OrgAccessValidationService orgAccessValidationService;
    private static OrgUnitHelper orgUnitHelper;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;

    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static UserService userService;

    private static final String GET_OUID_QUERY = "SELECT id FROM test.ous WHERE ref_id = :ref_id";
    private static final String GET_ORG_USER_ID_QUERY = "SELECT id FROM test.org_users";

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
        orgUsersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        orgUsersService.ensureTableExistence(company);
        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource,  new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService);
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

        var firstVersion = orgVersionsService.insert(company, OrgVersion.OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        orgUnitHelper = new OrgUnitHelper(orgUnitsService, integrationService);
        orgAccessValidationService = new OrgAccessValidationService(dataSource, orgVersionsService, orgUnitHelper);
    }


    @Test
    public void testValidation() throws Exception {

        Map<String, Object> param = Maps.newHashMap();
        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var orgUser1 = DBOrgUser.builder()
                .email("manager1@levelops.io")
                .fullName("manager1")
                //.refId(1)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("manager1").username("manager1").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var orgUser2 = DBOrgUser.builder()
                .email("dev1@levelops.io")
                .fullName("dev1-levelops")
                //.refId(2)
                .customFields(Map.of("test_name", "test1", "manager", "manager1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("dev1").username("dev1").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var orgUser3 = DBOrgUser.builder()
                .email("dev2@levelops.io")
                .fullName("developer2")
                //.refId(2)
                .customFields(Map.of("test_name", "test1", "manager", "manager1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("dev2").username("dev2").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();

        var orgUser4 = DBOrgUser.builder()
                .email("dev3@levelops.io")
                .fullName("dev3")
                //.refId(2)
                .customFields(Map.of("test_name", "test1", "manager", "manager2"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("dev3").username("dev3").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();

        var orgUser5 = DBOrgUser.builder()
                .email("dev4@levelops.io")
                .fullName("developer4")
                //.refId(2)
                .customFields(Map.of("test_name", "test1", "manager", "manager2"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("dev4").username("dev4").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();

        var orgUser6 = DBOrgUser.builder()
                .email("manager2@levelops.io")
                .fullName("manager2")
                //.refId(1)
                .customFields(Map.of("test_name", "test1", "manager", "manager1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("manager2").username("manager2").integrationType("application").integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();

        var userId1 = Integer.valueOf(orgUsersService.insert(company, orgUser1));
        var userId2 = Integer.valueOf(orgUsersService.insert(company, orgUser2));
        var userId3 = Integer.valueOf(orgUsersService.insert(company, orgUser3));
        var userId4 = Integer.valueOf(orgUsersService.insert(company, orgUser4));
        var userId5 = Integer.valueOf(orgUsersService.insert(company, orgUser5));
        var userId6 = Integer.valueOf(orgUsersService.insert(company, orgUser6));

        DBOrgUser dbUser1 = orgUsersService.get(company, userId1).get();
        DBOrgUser dbUser2 = orgUsersService.get(company, userId6).get();
        DBOrgContentSection dbOrgContentSection1 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of(1, 2))
                .defaultSection(true)
                .dynamicUsers(Map.of("email", List.of("dev1@levelops.io"),
                        "full_name", List.of("developer3")))
                .build();

        DBOrgContentSection dbOrgContentSection2 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of())
                .defaultSection(true)
                .dynamicUsers(Map.of("email", List.of("dev2@levelops.io", "dev3@levelops.io"),
                        "full_name", List.of("developer2")))
                .build();

        DBOrgContentSection dbOrgContentSection3 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of())
                .defaultSection(true)
                //.dynamicUsers(Map.of("full_name", List.of("developer4")))
                .build();

        DBOrgContentSection dbOrgContentSection4 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of())
                .defaultSection(true)
                .dynamicUsers(Map.of("full_name", List.of("xyz")))
                .build();

        DBOrgContentSection dbOrgContentSection5 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of())
                .defaultSection(true)
                .dynamicUsers(Map.of("partial_match",  Map.of("custom_field_manager", Map.of("$begins","man"))))
                .build();

        DBOrgContentSection dbOrgContentSection6 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of())
                .defaultSection(true)
                .dynamicUsers(Map.of("exclude",Map.of("custom_field_manager", List.of("manager1"))))
                .build();

        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);

        DBOrgUnit unit1 = DBOrgUnit.builder()
                .refId(1)
                .name("ou-1")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection1))
                .versions(Set.of(1))
                .managers(Set.of(dbUser1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        DBOrgUnit unit2 = DBOrgUnit.builder()
                .refId(2)
                .name("ou-2")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection2))
                .versions(Set.of(1))
                .managers(Set.of(dbUser1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        DBOrgUnit unit3 = DBOrgUnit.builder()
                .refId(3)
                .name("ou-3")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection3))
                .versions(Set.of(1))
                .managers(Set.of(dbUser2))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        DBOrgUnit unit4 = DBOrgUnit.builder()
                .refId(4)
                .name("ou-4")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection4))
                .versions(Set.of(1))
                //.managers(Set.of(dbUser2))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        DBOrgUnit unit5 = DBOrgUnit.builder()
                .refId(5)
                .name("ou-5")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection5))
                .versions(Set.of(1))
                //.managers(Set.of(dbUser2))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        DBOrgUnit unit6 = DBOrgUnit.builder()
                .refId(6)
                .name("ou-6")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection6))
                .versions(Set.of(1))
                //.managers(Set.of(dbUser2))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        String refId1 = orgUnitsService.insert(company, unit1);
        String refId2 = orgUnitsService.insert(company, unit2);
        String refId3 = orgUnitsService.insert(company, unit3);
        String refId4 = orgUnitsService.insert(company, unit4);
        String refId5 = orgUnitsService.insert(company, unit5);
        String refId6 = orgUnitsService.insert(company, unit6);

        param.put("ref_id", Integer.parseInt(refId1));
        UUID ouId1 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);
        param.put("ref_id", Integer.parseInt(refId2));
        UUID ouId2 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);
        param.put("ref_id", Integer.parseInt(refId3));
        UUID ouId3 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);
        param.put("ref_id", Integer.parseInt(refId4));
        UUID ouId4 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);
        param.put("ref_id", Integer.parseInt(refId5));
        UUID ouId5 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);
        param.put("ref_id", Integer.parseInt(refId6));
        UUID ouId6 = template.queryForObject(GET_OUID_QUERY, param, UUID.class);

        //activate ou-1 and ou-2
        orgUnitsService.update(company, ouId1, true);
        orgUnitsService.update(company, ouId2, true);

        String propeloUserId1 = userService.insert(company, User.builder()
                .firstName("f")
                .lastName("l")
                .bcryptPassword("p")
                .email("propelo-user1@email.com")
                .userType(RoleType.ORG_ADMIN_USER)
                .passwordAuthEnabled(true)
                .samlAuthEnabled(false)
                .mfaEnabled(true)
                .mfaEnrollmentEndAt(Instant.EPOCH)
                .mfaResetAt(Instant.EPOCH)
                .managedOURefIds(List.of(1,2))
                .build());

        String propeloUserId2 = userService.insert(company, User.builder()
                .firstName("f")
                .lastName("l")
                .bcryptPassword("p")
                .email("manager1@levelops.io")
                .userType(RoleType.ADMIN)
                .passwordAuthEnabled(true)
                .samlAuthEnabled(false)
                .mfaEnabled(true)
                .mfaEnrollmentEndAt(Instant.EPOCH)
                .mfaResetAt(Instant.EPOCH)
                .managedOURefIds(List.of(1,2))
                .build());

        boolean isValidUser = orgAccessValidationService.validateAccess(company, orgUser1.getEmail(), "1", ouId1);
        Assertions.assertThat(isValidUser).isTrue();

        List<UUID> orgUserIdList = template.queryForList(GET_ORG_USER_ID_QUERY, param, UUID.class);

        List<DBOrgUnit> managedOUs = orgAccessValidationService.getOrgsManagedByAdminUsingAdminsEmail(company,"propelo-user1@email.com", 0, 10);
        Assertions.assertThat(managedOUs).isNotNull();
        Assertions.assertThat(managedOUs.size()).isEqualTo(2);

        //requestorEmail is org_admin
        DBOrgAccessUsers orgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, "propelo-user1@email.com", "1", IdType.OU_USER_IDS, orgUserIdList);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(3);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(3);

        //This is a manager + org_admin
        orgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, orgUser1.getEmail(), "1", IdType.OU_USER_IDS, orgUserIdList);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(3);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(3);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, orgUser6.getEmail(), "1", IdType.OU_USER_IDS, orgUserIdList);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(0);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(6);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, orgUser3.getEmail(), "1", IdType.OU_USER_IDS, orgUserIdList);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(0);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(6);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", List.of(ouId1,ouId2));
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(3);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId2);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(1);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId3, false);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(6);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

        //Org has no user selection, strictlyExplicit = true, return all users
        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId3, true);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(6);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

        //Org has no user criteria but no matches, strictlyExplicit = true, return 0 users
        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId4, true);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(0);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId4, false);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(6);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);


        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId5, true);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(5);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);


        orgAccessUsers = orgAccessValidationService.getAllAccessUsersByOuId(company, "1", ouId6, true);
        Assertions.assertThat(orgAccessUsers).isNotNull();
        Assertions.assertThat(orgAccessUsers.getAuthorizedUserList().size()).isEqualTo(2);
        Assertions.assertThat(orgAccessUsers.getUnAuthorizedUserList().size()).isEqualTo(0);

    }
}
