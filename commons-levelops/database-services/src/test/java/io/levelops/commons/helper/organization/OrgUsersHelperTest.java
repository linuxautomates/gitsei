package io.levelops.commons.helper.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Log4j2
public class OrgUsersHelperTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgUsersHelper usersHelper;
    private static OrgVersionsDatabaseService versionsService;
    private static OrgUsersDatabaseService usersService;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
            "CREATE SCHEMA IF NOT EXISTS " + company,
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
        .forEach(template.getJdbcTemplate()::execute);

        versionsService = new OrgVersionsDatabaseService(dataSource);
        versionsService.ensureTableExistence(company);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        usersService = new OrgUsersDatabaseService(dataSource, mapper, versionsService, userIdentityService);
        usersService.ensureTableExistence(company);

        OrgUsersLockService orgUsersLockService = mock(OrgUsersLockService.class);
        when(orgUsersLockService.lock(anyString(), anyInt())).thenReturn(true);
        when(orgUsersLockService.unlock(anyString())).thenReturn(true);
        usersHelper = new OrgUsersHelper(usersService, versionsService, orgUsersLockService);
    }

    @Test
    public void test() throws SQLException {
        var integrationId = Integer.parseInt(integrationService.insert(company, Integration.builder()
            .name("name")
            .application("test")
            .description("description")
            .authentication(Authentication.API_KEY)
            .metadata(Map.of())
            .satellite(false)
            .tags(List.of())
            .status("ok")
            .build()));
        usersHelper.activateVersion(company, 1);

        Stream<DBOrgUser> users = List.<DBOrgUser>of(DBOrgUser.builder()
            .fullName("fullName")
            .email("email")
            .ids(Set.of(LoginId.builder()
                .integrationId(integrationId)
                .integrationType("integrationType")
                .username("username")
                .cloudId("cloudId")
                .build()))
            .active(true)
            .build()).stream();
        usersHelper.insertNewVersionUsers(company, users);

        var v1 = versionsService.getActive(company, OrgAssetType.USER);

        Assertions.assertThat(v1).isPresent();
        Assertions.assertThat(v1.get().getVersion()).isGreaterThan(0);
    }

    @Test
    public void testUpdateUsers() throws NumberFormatException, SQLException{
        // Insert integration to be used in the test
        var integrationId = Integer.parseInt(integrationService.insert(company, Integration.builder()
            .name("Test Integration 1 for Org User Update")
            .application(IntegrationType.JIRA.toString())
            .description("description")
            .authentication(Authentication.API_KEY)
            .metadata(Map.of())
            .satellite(false)
            .tags(List.of())
            .status("ok")
            .build()));

        // set of users for the user directory
        var user1 = DBOrgUser.builder()
            .active(true)
            .customFields(Map.of())
            .email("user1@test.test")
            .fullName("User 1")
            .ids(Set.of(LoginId.builder()
                .cloudId("update_test_user1")
                .username("update_test_user1")
                .integrationId(integrationId)
                .integrationType(IntegrationType.JIRA.toString())
                .build()))
            .build();
        var user2 = DBOrgUser.builder()
            .active(true)
            .customFields(Map.of())
            .email("user2@test.test")
            .fullName("User 2")
            .ids(Set.of(LoginId.builder()
                .cloudId("update_test_user2")
                .username("update_test_user2")
                .integrationId(integrationId)
                .integrationType(IntegrationType.JIRA.toString())
                .build()))
            .build();
        var user3 = DBOrgUser.builder()
            .active(true)
            .customFields(Map.of())
            .email("user3@test.test")
            .fullName("User 3")
            .ids(Set.of(LoginId.builder()
                .cloudId("update_test_user3")
                .username("update_test_user3")
                .integrationId(integrationId)
                .integrationType(IntegrationType.JIRA.toString())
                .build()))
            .build();

        Set<DBOrgUser> users = Set.of(user1, user2, user3);

        // insert the original version of the directory
        var userIds = usersHelper.insertNewVersionUsers(company, users.stream());
        var directoryVersion = versionsService.getLatest(company, OrgAssetType.USER).get();

        // update a few users from the directory
        var updatedUserRefId = userIds.iterator().next();
        var updatedUser = usersService.get(company, updatedUserRefId).get().toBuilder().customFields(Map.of("team", "ok1")).build();
        usersHelper.updateUsers(company, Set.of(updatedUser).stream());
        // validate that the users directory has a new version
        var updatedDirectoryVersion = versionsService.getLatest(company, OrgAssetType.USER).get();
        Assertions.assertThat(updatedDirectoryVersion).isNotEqualTo(directoryVersion);

        // validate that the non-updated users are present in the new directory version as well as in the previous version
        var updatedUserFromDB = usersService.get(company, updatedUserRefId).get();
        // validate that the updated users are not duplicated and only available in
        Assertions.assertThat(updatedUserFromDB.getVersions()).containsExactly(updatedDirectoryVersion.getVersion());
        // the new directory version with a new UUID and only the new version in their versions list.
        userIds.stream().filter(id -> id != updatedUserRefId)
            .forEach(id -> {
                try {
                    var user = usersService.get(company, id);
                    Assertions.assertThat(user).isPresent();
                    Assertions.assertThat(user.get().getVersions()).containsAll(Set.of(directoryVersion.getVersion(), updatedDirectoryVersion.getVersion()));
                } catch (SQLException e) {
                    log.error("Unable to retrieve user with ref_id '{}'...", id, e);
                }
            });

        updatedUser = updatedUser.toBuilder().email("email-updated").build();
        usersHelper.updateUsers(company, Set.of(updatedUser).stream());
        // validate that the users directory has a new version
        var updatedDirectoryVersion1 = versionsService.getLatest(company, OrgAssetType.USER).get();
        Assertions.assertThat(updatedDirectoryVersion).isNotEqualTo(directoryVersion);

        // validate that the non-updated users are present in the new directory version as well as in the previous version
        updatedUserFromDB = usersService.get(company, updatedUserRefId).get();
        // validate that the updated users are not duplicated and only available in
        Assertions.assertThat(updatedUserFromDB.getVersions()).containsExactly(updatedDirectoryVersion1.getVersion());
        // the new directory version with a new UUID and only the new version in their versions list.
        userIds.stream().filter(id -> id != updatedUserRefId)
                .forEach(id -> {
                    try {
                        var user = usersService.get(company, id);
                        Assertions.assertThat(user).isPresent();
                        Assertions.assertThat(user.get().getVersions()).containsAll(Set.of(directoryVersion.getVersion(), updatedDirectoryVersion1.getVersion()));
                    } catch (SQLException e) {
                        log.error("Unable to retrieve user with ref_id '{}'...", id, e);
                    }
                });

        // validate that we only get one record for the current version
        var results = usersService.filter(company, QueryFilter.builder().strictMatch("ref_id", updatedUserRefId).build(), 0, 10);
        Assertions.assertThat(results.getCount()).isEqualTo(1);
    }

    @Test
    public void testUserSelect() throws JsonProcessingException{
        var params = new HashMap<String, Object>();
        var userSelectors = Map.<String, Object>of(
            "email", "user1@test.test",
            "custom_field_location", "India"
            );
        var query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
            + "o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location "
            + "AND o_u.email ILIKE :org_user_email "
            + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
            "org_user_selection_version_type", "USER",
            "org_user_email", "user1@test.test",
            "org_user_condition_key_location", "location",
            "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India"
        ));

        params = new HashMap<String, Object>();
        userSelectors = Map.of(
            "custom_field_location", "India",
            "custom_field_designation", Map.of("$begins", "Senior"),
            "custom_field_start_date", Map.of("$age", Duration.ofDays(30).getSeconds())
            );
        query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
                +"((o_u.custom_fields->>:org_user_condition_key_start_date)::bigint >= (EXTRACT(EPOCH FROM now()) - :o_u_c_o_u_custom_fields_org_user_condition_key_start_date)) "
            + "AND (o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins) "
            + "AND o_u.custom_fields->>:org_user_condition_key_location = :o_u_c_o_u_custom_fields_org_user_condition_key_location "
            + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)");
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
            "org_user_selection_version_type", "USER",
            "org_user_condition_key_location", "location",
            "o_u_c_o_u_custom_fields_org_user_condition_key_location", "India",
            "org_user_condition_key_designation", "designation",
            "o_u_c_o_u_custom_fields_org_user_condition_key_designation_begins", "Senior%",
            "org_user_condition_key_start_date", "start_date",
            "o_u_c_o_u_custom_fields_org_user_condition_key_start_date", Long.valueOf(Duration.ofDays(30).getSeconds()).intValue()
        ));

        params = new HashMap<String, Object>();
        userSelectors = Map.of(
            "custom_field_designation", Map.of("$ends", "Senior"),
            "custom_field_start_date", Map.of("$gt", Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(), "$lt", Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond()),
            "partial_match", Map.of("custom_field_location", Map.of("$begins","Ind"),"email",Map.of("$begins","email1"))
            );
        query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
            + "(o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_ends) "
            + "AND (o_u.custom_fields->>:org_user_condition_key_location SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_location_begins) "
            + "AND (o_u.custom_fields->>:org_user_condition_key_start_date < :o_u_c_o_u_custom_fields_org_user_condition_key_start_date_lt AND o_u.custom_fields->>:org_user_condition_key_start_date > :o_u_c_o_u_custom_fields_org_user_condition_key_start_date_gt) "
            + "AND (o_u.email SIMILAR TO :o_u_o_u_email_begins) "
            + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)" );
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.of(
            "org_user_selection_version_type", "USER",
            "org_user_condition_key_location", "location",
            "o_u_c_o_u_custom_fields_org_user_condition_key_location_begins", "Ind%",
            "o_u_o_u_email_begins","email1%",
            "org_user_condition_key_designation", "designation",
            "o_u_c_o_u_custom_fields_org_user_condition_key_designation_ends", "%Senior",
            "org_user_condition_key_start_date", "start_date",
            "o_u_c_o_u_custom_fields_org_user_condition_key_start_date_gt", Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(),
            "o_u_c_o_u_custom_fields_org_user_condition_key_start_date_lt", Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond()
        ));

        params = new HashMap<String, Object>();
        userSelectors = Map.of(
            "email", List.of("tes1@test.test", "test2@test.test"),
            "custom_field_designation", Map.of("$ends", "Senior"),
            "custom_field_manager", List.of("Manager1", "Manager2", "Manager3"),
            "custom_field_start_date", Map.of("$gt", Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(), "$lt", Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond())
            );
        query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
            + "(o_u.custom_fields->>:org_user_condition_key_designation SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_designation_ends) "
            + "AND (o_u.custom_fields->>:org_user_condition_key_start_date < :o_u_c_o_u_custom_fields_org_user_condition_key_start_date_lt AND o_u.custom_fields->>:org_user_condition_key_start_date > :o_u_c_o_u_custom_fields_org_user_condition_key_start_date_gt) "
            + "AND o_u.custom_fields->>:org_user_condition_key_manager IN (:o_u_c_o_u_custom_fields_org_user_condition_key_manager) "
            + "AND o_u.email IN (:org_user_email) "
            + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)" );
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.<String, Object>of(
            "org_user_email", List.of("tes1@test.test", "test2@test.test"),
            "org_user_selection_version_type", "USER",
            "org_user_condition_key_designation", "designation",
            "o_u_c_o_u_custom_fields_org_user_condition_key_designation_ends", "%Senior",
            "org_user_condition_key_start_date", "start_date",
            "org_user_condition_key_manager", "manager",
            "o_u_c_o_u_custom_fields_org_user_condition_key_manager", List.of("Manager1", "Manager2", "Manager3"),
            "o_u_c_o_u_custom_fields_org_user_condition_key_start_date_gt", Instant.parse("2021-07-01T00:00:00-00:00").getEpochSecond(),
            "o_u_c_o_u_custom_fields_org_user_condition_key_start_date_lt", Instant.parse("2021-11-01T00:00:00-00:00").getEpochSecond()
        ));


        params = new HashMap<String, Object>();
        userSelectors = Map.of(
                "exclude",Map.of("custom_field_manager", List.of("Manager1", "Manager2", "Manager3")));
        query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
                + "NOT (o_u.custom_fields->>:org_user_condition_key_manager IN (:o_u_c_o_u_custom_fields_org_user_condition_key_manager)) "
                + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)" );
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.<String, Object>of(
                "org_user_selection_version_type", "USER",
                "org_user_condition_key_manager", "manager",
                "o_u_c_o_u_custom_fields_org_user_condition_key_manager", List.of("Manager1", "Manager2", "Manager3")
        ));

        params = new HashMap<String, Object>();
        userSelectors = Map.of(
                "partial_match", Map.of("custom_field_manager", Map.of("$begins","man")));
        query = OrgUsersHelper.getOrgUsersSelectQuery(company, userSelectors, params);
        Assertions.assertThat(query).isEqualTo("SELECT id, ref_id, versions FROM test.org_users o_u WHERE "
                + "(o_u.custom_fields->>:org_user_condition_key_manager SIMILAR TO :o_u_c_o_u_custom_fields_org_user_condition_key_manager_begins) "
                + "AND o_u.versions @> ARRAY(SELECT version FROM test.org_version_counter WHERE type = :org_user_selection_version_type AND active = true)" );
        Assertions.assertThat(params).containsExactlyInAnyOrderEntriesOf(Map.<String, Object>of(
                "org_user_selection_version_type", "USER",
                "org_user_condition_key_manager", "manager",
                "o_u_c_o_u_custom_fields_org_user_condition_key_manager_begins","man%"
        ));
    }
}
