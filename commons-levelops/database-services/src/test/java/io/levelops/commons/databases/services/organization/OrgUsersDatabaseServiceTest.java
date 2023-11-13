package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.DBOrgUserCloudIdMapping;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService.OrgUserCloudIdMappingFilter;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class OrgUsersDatabaseServiceTest {

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
    private static OrgUsersHelper orgUsersHelper;

    private static Integration integration1, integration2, integration3;
    private static Integer integrationId1, integrationId2, integrationId3;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
                )
                .forEach(template.getJdbcTemplate()::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        orgUsersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        OrgUsersLockService orgUsersLockService = mock(OrgUsersLockService.class);
        when(orgUsersLockService.lock(anyString(), anyInt())).thenReturn(true);
        when(orgUsersLockService.unlock(anyString())).thenReturn(true);
        orgUsersHelper = new OrgUsersHelper(orgUsersService, orgVersionsService, orgUsersLockService);
        orgUsersService.ensureTableExistence(company);
        UserService userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);

        OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, null, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService);
        orgUnitsService.ensureTableExistence(company);

        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);

        integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        integration2 = Integration.builder()
                .description("description1")
                .name("integ2")
                .url("url")
                .application("application")
                .status("active")
                .build();
        integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));

        integration3 = Integration.builder()
                .description("description1")
                .name("integ3")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId3 = Integer.valueOf(integrationService.insert(company, integration3));
    }

    @Test
    public void test() throws SQLException {
        resetDb();
        Assert.assertEquals(orgUsersService.getAllContributorRoles(company).size(),9);
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        Map<String, Object> map = new HashMap<>();
        map.put("test_name", "test1");
        map.put("bu", "testBU");
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(map)
                .ids(Set.of(LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = Integer.valueOf(orgUsersService.insert(company, orgUser1));

        DBOrgUser dbUser1 = orgUsersService.get(company, userId1).get();
        assertThat(dbUser1).isEqualTo(orgUser1.toBuilder()
                .id(dbUser1.getId())
                .refId(userId1)
                .createdAt(dbUser1.getCreatedAt())
                .updatedAt(dbUser1.getUpdatedAt())
                .build());
        DBOrgUser dbUser1a = orgUsersService.get(company, dbUser1.getRefId()).get();
        assertThat(dbUser1).isEqualToComparingFieldByField(dbUser1a);


        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId2 = Integer.valueOf(orgUsersService.insert(company, orgUser2));
        var results = orgUsersService.filter(company, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");
        assertThat(results.getRecords().stream().flatMap(user -> user.getIds().stream()).map(LoginId::getUsername).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("cloudId", "cloudId2");

        String insertedId1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("cloudId")
                        .displayName("sample-Cog-1")
                        .originalDisplayName("sample-Cog-1")
                        .build());
        String insertedId2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("cloudId2")
                        .displayName("sample-Cog-2")
                        .originalDisplayName("sample-Cog-2")
                        .build());

        results = orgUsersService.filter(company, null, 0, 10);

        assertThat(results).isNotNull();
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");
        assertThat(results.getRecords().stream().flatMap(user -> user.getIds().stream()).map(LoginId::getUsername).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("sample-Cog-1", "sample-Cog-2");
        assertThat(results.getTotals()).isEqualTo(Map.of("dangling", 0));

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("full_name", "fullName1").build(), 0, 10);

        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().get(0).getFullName()).isEqualTo("fullName1");

        Map<String, Object> data =new HashMap<>();
        data.put("custom_field_bu","BU");
        QueryFilter filters=QueryFilter.builder().partialMatches(data).build();
        results = orgUsersService.filter(company, QueryFilter.builder().partialMatches(data).build(), 0, 10);

        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().get(0).getCustomFields().get("bu")).isEqualTo("testBU");

        var userId2a = Integer.valueOf(orgUsersService.insert(company, orgUser2));

        assertThat(userId2a).isEqualTo(userId2);

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId1).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(2);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");

        var versionId = orgVersionsService.insert(company, OrgAssetType.USER);

        var currentVersion = orgVersionsService.getActive(company, OrgAssetType.USER).get().getId();
        assertThat(versionId).isNotEqualTo(currentVersion);

        orgVersionsService.update(company, versionId, true);
        orgVersionsService.update(company, currentVersion, false);

        var userId2b = Integer.valueOf(orgUsersService.insert(company, orgUser2));
        assertThat(userId2b).isEqualTo(userId2a);

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId1).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("integration_id", integrationId2).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(0);

        var ver1 = orgVersionsService.get(company, currentVersion).get();
        var ver2 = orgVersionsService.get(company, versionId).get();

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", ver1.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(2);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "fullName2");

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", ver2.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        var v3Id = orgVersionsService.insert(company, OrgAssetType.USER);
        var v3 = orgVersionsService.get(company, v3Id).get();

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", v3.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(0);

        var exclusions = Set.<Integer>of();
        orgUsersService.upgradeUsersVersion(company, v3.getVersion(), ver2.getVersion(), exclusions);

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", v3.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2");

        var v4Id = orgVersionsService.insert(company, OrgAssetType.USER);

        var v4 = orgVersionsService.get(company, v4Id).get();
        var user2 = orgUsersService.get(company, userId2).get();
        exclusions = Set.<Integer>of(user2.getRefId());
        orgUsersService.upgradeUsersVersion(company, v4.getVersion(), ver1.getVersion(), exclusions);

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", v4.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1");

        var values = orgUsersService.getValues(company, "email", null, 0, 10);
        assertThat(values).isNotNull();
        assertThat(values.getTotalCount()).isEqualTo(1);

        var values2 = orgUsersService.getValues(company, "custom_field_location", null, 0, 10);
        assertThat(values2).isNotNull();
        assertThat(values2.getTotalCount()).isEqualTo(0);

        var values3 = orgUsersService.getValues(company, "custom_field_sample_name", null, 0, 10);
        assertThat(values3).isNotNull();
        assertThat(values3.getTotalCount()).isEqualTo(1);

        var login1 = DbScmUser.builder().cloudId("cloud_user1").displayName("Cloud User1").originalDisplayName("Cloud User1").integrationId(integrationId1.toString()).build();
        var loginId = userIdentityService.upsert(company, login1);
        assertThat(loginId).isNotBlank();

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", v4.getVersion()).build(), 0, 10);
        assertThat(results.getTotalCount()).isEqualTo(1);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1");

        results = orgUsersService.filter(company, QueryFilter.builder().build(), 0, 10, true);
        assertThat(results.getTotalCount()).isEqualTo(3);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName2", "Cloud User1", "sample-Cog-1");

        results = orgUsersService.filter(company, QueryFilter.builder().strictMatch("version", v4.getVersion()).build(), 0, 10, true);
        assertThat(results.getTotalCount()).isEqualTo(3);
        assertThat(results.getRecords().stream().map(DBOrgUser::getFullName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("fullName1", "Cloud User1", "sample-Cog-1");

        insertedId1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("cloudId3")
                        .displayName("sample-Cog-3")
                        .originalDisplayName("sample-Cog-3")
                        .build());
        insertedId2 = userIdentityService.upsert(company, DbScmUser.builder()
                .integrationId(String.valueOf(integrationId2))
                .cloudId("cloudId4")
                .displayName("sample-Cog-4")
                .originalDisplayName("sample-Cog-4")
                .originalDisplayName("sample-Cog-4")
                .build());
        var resultsTotals = orgUsersService.filter(company, null, 0, 10);
        assertThat(resultsTotals.getCount()).isEqualTo(1);
        assertThat(resultsTotals.getTotals()).isEqualTo(Map.of("dangling", 4));

        resultsTotals = orgUsersService.filter(company, null, 0, 10, true);
        assertThat(resultsTotals.getCount()).isEqualTo(5);
        assertThat(resultsTotals.getTotals()).isEqualTo(Map.of("dangling", 4));
    }

    @Test
    public void testSchema() throws DataAccessException, JsonProcessingException {
        var fields = Set.<OrgUserSchema.Field>of(OrgUserSchema.Field.builder().build());
        var version = orgUsersService.insertUsersSchema(company, fields);

        var dbResponse = orgUsersService.getUsersSchemas(company, version);
        assertThat(fields).isEqualTo(dbResponse.get().getFields());

        var fields2 = Set.<OrgUserSchema.Field>of(
                OrgUserSchema.Field.builder()
                        .index(1)
                        .key("location")
                        .description("Location")
                        .displayName("Location")
                        .type(OrgUserSchema.Field.FieldType.STRING)
                        .build(),
                OrgUserSchema.Field.builder()
                        .index(2)
                        .key("department")
                        .description("OU")
                        .displayName("Department")
                        .type(OrgUserSchema.Field.FieldType.STRING)
                        .build());
        var version2 = orgUsersService.insertUsersSchema(company, fields2);

        var dbResponse2 = orgUsersService.getUsersSchemas(company, version2);
        assertThat(fields2).isEqualTo(dbResponse2.get().getFields());

        var results = orgUsersService.listUsersSchemas(company);
        assertThat(results).isPresent();
        assertThat(results.get()).hasSize(2);
        assertThat(results.get().stream()
                .map(item -> item.toBuilder().createdAt(null).build())
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrder(
                        OrgUserSchema.builder().version(version).fields(fields).build(),
                        OrgUserSchema.builder().version(version2).fields(fields2).build());
    }

    @Test
    public void testSchemaSerialization() throws JsonMappingException, JsonProcessingException {
        var sample1 = "{\"index\": 1, \"type\": \"string\", \"key\": \"location\", \"display_name\": \"Location\"}";
        var schema = DefaultObjectMapper.get().readValue(sample1, OrgUserSchema.Field.class);

        assertThat(schema).isEqualTo(OrgUserSchema.Field.builder()
                .index(1)
                .key("location")
                .displayName("Location")
                .type(OrgUserSchema.Field.FieldType.STRING)
                .build());

        var sample2 = "{\"index\": 1, \"type\": \"string\", \"key\": \"location\", \"display_name\": \"Location\", \"description\": \"My Location\", \"system_field\": true}";
        var schema2 = DefaultObjectMapper.get().readValue(sample2, OrgUserSchema.Field.class);

        assertThat(schema2).isEqualTo(OrgUserSchema.Field.builder()
                .index(1)
                .key("location")
                .displayName("Location")
                .description("My Location")
                .type(OrgUserSchema.Field.FieldType.STRING)
                .systemField(true)
                .build());
    }

    public void resetDb() throws SQLException {
        List<String> tables = List.of("org_users", "integration_users", "org_user_cloud_id_mapping", "org_version_counter");
        tables.forEach(table -> {
            try {
                dataSource.getConnection().prepareStatement("DELETE FROM " + "test." + table).execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testDanglingPreviousVersions() throws SQLException {
        resetDb();
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId0").username("cloudId0").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = Integer.valueOf(orgUsersService.insert(company, orgUser1));

        var cloudUserId1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("cloudId1")
                        .displayName("sample-Cog-1")
                        .originalDisplayName("sample-Cog-1")
                        .build());

        var cloudUserId2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("cloudId2")
                        .displayName("sample-Cog-2")
                        .originalDisplayName("sample-Cog-2")
                        .build());


        // We have 2 dangling users + 1 org user
        var results = orgUsersService.filter(company, null, 0, 10, true);
        assertThat(results.getRecords()).map(DBOrgUser::getFullName).containsExactlyInAnyOrder("fullName1", "sample-Cog-1", "sample-Cog-2");


        // Now let's increase the version of the org user and make that the active version
        var newVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, newVersion, true);
        orgVersionsService.update(company, firstVersion, false);
        orgUsersService.upgradeUsersVersion(company, newVersion);

        // Remove the first cloud id from the mapping
        var upsertedOrgUserId = orgUsersService.upsert(company, orgUser1.toBuilder().ids(Set.of()).build());

        // This time we should return cloudId0 because it is now dangling and not part of the active version
        var orgUser = orgUsersService.get(company, upsertedOrgUserId.getRefId()).get();
        results = orgUsersService.filter(company, null, 0, 10, true);
        assertThat(results.getRecords()).map(DBOrgUser::getFullName).containsExactlyInAnyOrder("fullName1", "sample-Cog-1", "sample-Cog-2", "cloudId0");
    }

    private List<Map<String, Object>> getAllCloudIdMappings() {
        return template.queryForList("SELECT * FROM test.org_user_cloud_id_mapping", Map.of());
    }

    @Test
    public void testCloudIdMapping() throws SQLException {
        resetDb();
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);

        // Insert 2 integration users with the same email
        var stephIntegrationUser = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("stephcurry")
                        .displayName("Stephen Curry")
                        .originalDisplayName("Stephen Curry")
                        .emails(List.of("steph@warriors.com"))
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        var stephIntegrationUser2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("stephcurry")
                        .displayName("Stephen Curry")
                        .originalDisplayName("Stephen Curry")
                        .emails(List.of("steph@warriors.com"))
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        var orgUserInserted = orgUsersService.insert(company, DBOrgUser.builder()
                .email("step@warriors.com")
                .fullName("Stephen Curry")
                .customFields(Map.of("test_name", "ChefCurry"))
                .ids(Set.of(
                        LoginId.builder()
                                .cloudId("stephcurry")
                                .username("stephcurry")
                                .integrationType(integration1.getApplication())
                                .integrationId(integrationId1)
                                .build(),
                        LoginId.builder()
                                .cloudId("stephcurry")
                                .username("stephcurry")
                                .integrationType(integration2.getApplication())
                                .integrationId(integrationId2)
                                .build()
                ))
                .versions(Set.of(1))
                .build()
        );
        DBOrgUser stephOrgUser = orgUsersService.getByUser(company, "Stephen Curry").get();
        assertThat(stephOrgUser.getIds()).hasSize(2);


        var result = orgUsersService.filterOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder()
                .integrationUserIds(List.of(UUID.fromString(stephIntegrationUser)))
                .build(), 0, 100);
        assertThat(result.getRecords()).map(DBOrgUserCloudIdMapping::getIntegrationUserId).containsExactlyInAnyOrder(UUID.fromString(stephIntegrationUser));

        result = orgUsersService.filterOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder()
                .orgUserIds(List.of(stephOrgUser.getId()))
                .build(), 0, 100);
        assertThat(result.getRecords()).map(DBOrgUserCloudIdMapping::getIntegrationUserId)
                .containsExactlyInAnyOrder(UUID.fromString(stephIntegrationUser), UUID.fromString(stephIntegrationUser2));

        result = orgUsersService.filterOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder()
                .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                .build(), 0, 100);
        assertThat(result.getRecords()).map(DBOrgUserCloudIdMapping::getIntegrationUserId)
                .containsExactlyInAnyOrder(UUID.fromString(stephIntegrationUser), UUID.fromString(stephIntegrationUser2));

        result = orgUsersService.filterOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder()
                .mappingStatus(DbScmUser.MappingStatus.AUTO)
                .build(), 0, 100);
        assertThat(result.getRecords()).map(DBOrgUserCloudIdMapping::getIntegrationUserId).isEmpty();
    }

    @Test
    public void testLinkCloudIds() throws SQLException {
        resetDb();
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        var version = orgVersionsService.get(company, firstVersion).get();
        orgVersionsService.update(company, firstVersion, true);

        // Insert 2 integration users with the same email
        var stephIntegrationUser = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId1))
                        .cloudId("stephcurry")
                        .displayName("Stephen Curry")
                        .originalDisplayName("Stephen Curry")
                        .emails(List.of("steph@warriors.com"))
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        var stephIntegrationUser2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("stephcurry")
                        .displayName("Stephen Curry")
                        .originalDisplayName("Stephen Curry")
                        .emails(List.of("steph@warriors.com"))
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        var klayIntegrationUser = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(String.valueOf(integrationId2))
                        .cloudId("klaythompson")
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .emails(List.of("klay@warriors.com"))
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        orgUsersService.upsertAuto(company, DBOrgUser.builder()
                        .email("step@warriors.com")
                        .fullName("Stephen Curry")
                        .customFields(Map.of("test_name", "ChefCurry"))
                        .ids(Set.of(
                                LoginId.builder()
                                        .cloudId("stephcurry")
                                        .username("stephcurry")
                                        .integrationType(integration1.getApplication())
                                        .integrationId(integrationId1)
                                        .build(),
                                LoginId.builder()
                                        .cloudId("stephcurry")
                                        .username("stephcurry")
                                        .integrationType(integration2.getApplication())
                                        .integrationId(integrationId2)
                                        .build()
                        ))
                        .versions(Set.of(1))
                        .build(),
                Set.of(version.getVersion())
        );
        var allMappings = orgUsersService.streamOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder().build())
                .collect(Collectors.toList());
        assertThat(allMappings).map(DBOrgUserCloudIdMapping::getMappingStatus).containsExactlyInAnyOrderElementsOf(
                List.of(DbScmUser.MappingStatus.AUTO, DbScmUser.MappingStatus.AUTO)
        );

        DBOrgUser stephOrgUser = orgUsersService.getByUser(company, "Stephen Curry").get();
        assertThat(stephOrgUser.getIds()).hasSize(2);

        // Insert new version of user. This is the same function that is called with the CSV import in replace mode
        var newUsers = orgUsersHelper.insertNewVersionUsers(company, Stream.of(stephOrgUser));
        var newUserRetrieved = orgUsersService.get(company, newUsers.stream().findFirst().get()).get();

        assertThat(newUserRetrieved.getId()).isNotEqualTo(stephOrgUser.getId());
        assertThat(stephOrgUser.getVersions().stream().max(Comparator.comparingInt(a -> a)).get()).isEqualTo(1);
        assertThat(newUserRetrieved.getVersions().stream().max(Comparator.comparingInt(a -> a)).get()).isEqualTo(2);

        // Since we have not added any new mappings everything should still be on auto
        assertIntegrationUserMappingStatus("stephcurry", integrationId1.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("stephcurry", integrationId2.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("klaythompson", integrationId2.toString(), DbScmUser.MappingStatus.AUTO);
        assertMappingStatus(UUID.fromString(stephIntegrationUser), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);
        assertMappingStatus(UUID.fromString(stephIntegrationUser2), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);

        // Now let's add an integration user mapping manually, and check that the correct mapping statuses are updated.
        var newIntegrationIds = new HashSet<>(newUserRetrieved.getIds());
        newIntegrationIds.add(LoginId.builder()
                .cloudId("klaythompson")
                .username("Klay Thompson")
                .integrationType(integration2.getApplication())
                .integrationId(integrationId2)
                .build());
        orgUsersHelper.insertNewVersionUsers(company, Stream.of(newUserRetrieved.toBuilder()
                .ids(newIntegrationIds)
                .build()));
        assertIntegrationUserMappingStatus("stephcurry", integrationId1.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("stephcurry", integrationId2.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("klaythompson", integrationId2.toString(), DbScmUser.MappingStatus.MANUAL);
        assertMappingStatus(UUID.fromString(klayIntegrationUser), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.MANUAL);
        assertMappingStatus(UUID.fromString(stephIntegrationUser), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);
        assertMappingStatus(UUID.fromString(stephIntegrationUser2), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);

        // Run insertNewVersions again on the same data, and make sure everything is still the same
        newUserRetrieved = orgUsersService.get(company, newUsers.stream().findFirst().get()).get();
        newUsers = orgUsersHelper.insertNewVersionUsers(company, Stream.of(newUserRetrieved));
        assertIntegrationUserMappingStatus("stephcurry", integrationId1.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("stephcurry", integrationId2.toString(), DbScmUser.MappingStatus.AUTO);
        assertIntegrationUserMappingStatus("klaythompson", integrationId2.toString(), DbScmUser.MappingStatus.MANUAL);
        assertMappingStatus(UUID.fromString(klayIntegrationUser), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.MANUAL);
        assertMappingStatus(UUID.fromString(stephIntegrationUser), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);
        assertMappingStatus(UUID.fromString(stephIntegrationUser2), newUserRetrieved.getRefId(), DbScmUser.MappingStatus.AUTO);
    }

    private void assertMappingStatus(UUID integrationUserId, int refId, DbScmUser.MappingStatus mappingStatus) throws SQLException {
        // Get latest org user
        var orgUser = orgUsersService.get(company, refId).get();
        var mappings = orgUsersService.streamOrgUserCloudIdMappings(company, OrgUserCloudIdMappingFilter.builder()
                        .orgUserIds(List.of(orgUser.getId()))
                        .integrationUserIds(List.of(integrationUserId))
                        .build())
                .collect(Collectors.toList());
        assertThat(mappings).hasSize(1);
        assertThat(mappings.get(0).getMappingStatus()).isEqualTo(mappingStatus);
    }

    private void assertIntegrationUserMappingStatus(String cloudId, String integrationId, DbScmUser.MappingStatus mappingStatus) {
        assertThat(userIdentityService.getUserByCloudId(company, integrationId, cloudId).get().getMappingStatus()).isEqualTo(mappingStatus);
    }

    @Test
    public void testOrgUserIdFilter() throws SQLException {
        resetDb();
        var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        orgVersionsService.update(company, firstVersion, true);
        var version = orgVersionsService.get(company, firstVersion).get();
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId").username("cloudId").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();

        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .customFields(Map.of("sample_name", "sample"))
                .ids(Set.of(LoginId.builder().cloudId("cloudId2").integrationId(integrationId1).build()))
                .versions(Set.of(2, 3))
                .build();
        var userId1 = orgUsersService.upsert(company, orgUser1);
        var userId2 = orgUsersService.upsert(company, orgUser2);

        var result = orgUsersService.stream(company, QueryFilter.builder()
                        .strictMatch("org_user_id", List.of(userId1.getId(), userId2.getId()))
                        .build(), 10)
                .collect(Collectors.toList());
        assertThat(result).hasSize(2);

        // Increase the version
        var secondVersionId = orgVersionsService.insert(company, OrgAssetType.USER);
        var secondVersion = orgVersionsService.get(company, secondVersionId).get();
        orgVersionsService.update(company, secondVersionId, true);
        orgVersionsService.update(company, firstVersion, false);
        result = orgUsersService.stream(company, QueryFilter.builder()
                        .build(), 10)
                .collect(Collectors.toList());
        assertThat(result).isEmpty();

    }
}
