package io.levelops.etl.jobs.user_id_consolidation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserIdConsolidationStageTest {
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
    private static UserIdConsolidationStage userIdConsolidationStage;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;


    private static Integration integration1, integration2, integration3;
    private static Integer integrationId1, integrationId2, integrationId3;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                        "CREATE SCHEMA IF NOT EXISTS " + company,
                        "CREATE SCHEMA IF NOT EXISTS _levelops_etl",
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
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        var tenantConfigService = new TenantConfigService(dataSource);
        tenantConfigService.ensureTableExistence(company);
        tenantConfigService.insert(company, TenantConfig.builder()
                .name("AUTO_USER_ID_CONSOLIDATION_ENABLED")
                .value("true")
                .build());
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobDefinitionDatabaseService.ensureTableExistence();

        userIdConsolidationStage = new UserIdConsolidationStage(
                userIdentityService,
                orgUsersService,
                orgVersionsService,
                integrationService,
                orgUsersHelper,
                tenantConfigService,
                jobDefinitionDatabaseService,
                orgUsersLockService
        );

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

    @Before
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
    public void testGetOrgUsersToUpdateNoPreviousMappings() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createUser(List.of("email1")),
                createUser(List.of("email1")),
                createUser(List.of("email2")),
                createUser(List.of("email2")),
                createUser(List.of("email3"))
        );
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1)
                        ), Set.of(
                                allUsers.get(2),
                                allUsers.get(3)
                        ), Set.of(
                                allUsers.get(4)
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).hasSize(3);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email2", "email3");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId()),
                                Set.of(allUsers.get(2).getCloudId(), allUsers.get(3).getCloudId()),
                                Set.of(allUsers.get(4).getCloudId())
                        )
                );
    }

    @Test
    public void testGetOrgUsersToUpdateDifferentEmails() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createUser(List.of("email1")),
                createUser(List.of("email1")),
                createUser(List.of("email2")),
                createUser(List.of("email1", "email2")),
                createUser(List.of("email3"))
        );
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1),
                                allUsers.get(2),
                                allUsers.get(3)
                        ), Set.of(
                                allUsers.get(4)
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).hasSize(2);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email3");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId(), allUsers.get(2).getCloudId(), allUsers.get(3).getCloudId()),
                                Set.of(allUsers.get(4).getCloudId())
                        )
                );
    }

    @Test
    public void testGetOrgUsersToUpdateExistingMappings() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createUser(List.of("email1")), // 0
                createUser(List.of("email1")), // 1
                createUser(List.of("email1")), // 2
                createUser(List.of("email1")), // 3
                createUser(List.of("email2")), // 4
                createUser(List.of("email2")), // 5
                createUser(List.of("email3")) // 6
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1))),
                createOrgUser("email2", List.of(allUsers.get(5))),
                createOrgUser("email3", List.of(allUsers.get(6)))
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1),
                                allUsers.get(2),
                                allUsers.get(3)
                        ), Set.of(
                                allUsers.get(4),
                                allUsers.get(5)
                        ), Set.of(
                                allUsers.get(6)
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).hasSize(3);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email2", "email3");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId(), allUsers.get(2).getCloudId(), allUsers.get(3).getCloudId()),
                                Set.of(allUsers.get(4).getCloudId(), allUsers.get(5).getCloudId()),
                                Set.of(allUsers.get(6).getCloudId())
                        )
                );
    }

    @Test
    public void testNothingChanges() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email1")), // 2
                createAndInsert(List.of("email1")), // 3
                createAndInsert(List.of("email2")), // 4
                createAndInsert(List.of("email2")), // 5
                createAndInsert(List.of("email3")) // 6
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1), allUsers.get(2), allUsers.get(3))),
                createOrgUser("email2", List.of(allUsers.get(5), allUsers.get(4))),
                createOrgUser("email3", List.of(allUsers.get(6)))
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1),
                                allUsers.get(2),
                                allUsers.get(3)
                        ), Set.of(
                                allUsers.get(4),
                                allUsers.get(5)
                        ), Set.of(
                                allUsers.get(6)
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).isEmpty();
    }

    @Test
    public void testRemoveMapping() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email1")), // 2
                createAndInsert(List.of("email2")) // 3  - this will be removed
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1), allUsers.get(2), allUsers.get(3)))
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                Set.of(
                        allUsers.get(0),
                        allUsers.get(1),
                        allUsers.get(2)
                ), Set.of(
                        allUsers.get(3) // This is added
                )
        ), Optional.of(version));
        assertThat(usersToUpdate).hasSize(2);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email2");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId(), allUsers.get(2).getCloudId()),
                                Set.of(allUsers.get(3).getCloudId())
                        )
                );
    }

    @Test
    public void testSomeGroupsChanging() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email1")), // 2
                createAndInsert(List.of("email1")), // 3
                createAndInsert(List.of("email2")), // 4
                createAndInsert(List.of("email2")), // 5
                createAndInsert(List.of("email3")), // 6
                createAndInsert(List.of("email4")) // 7
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1), allUsers.get(2))), // user 3 mapping doesn't exist but will be added
                createOrgUser("email2", List.of(allUsers.get(5), allUsers.get(4))),
                createOrgUser("email3", List.of(allUsers.get(6), allUsers.get(7))) // user 7 mapping will be removed
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1),
                                allUsers.get(2),
                                allUsers.get(3) // This is added
                        ), Set.of(
                                allUsers.get(4), // This group remains the same
                                allUsers.get(5)
                        ), Set.of(
                                allUsers.get(6) // User 7 is removed from this group
                        ), Set.of(
                                allUsers.get(7) // New org user will be created here
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).hasSize(3);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email3", "email4");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId(), allUsers.get(2).getCloudId(), allUsers.get(3).getCloudId()),
                                Set.of(allUsers.get(6).getCloudId()),
                                Set.of(allUsers.get(7).getCloudId())
                        )
                );
    }

    @Test
    public void testDifferentVersions() throws SQLException {
        OrgVersion version = insertNewUserVersionAndActivate();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email2")), // 2
                createAndInsert(List.of("email3")) // 3
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1))),
                createOrgUser("email2", List.of(allUsers.get(2))),
                createOrgUser("email3", List.of(allUsers.get(3)))
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1)
                        ), Set.of(
                                allUsers.get(2)
                        ), Set.of(
                                allUsers.get(3)
                        )
                ),
                Optional.of(version));
        assertThat(usersToUpdate).isEmpty();

        // Disable previous version and create a new version
        orgVersionsService.update(company, version.getId(), false);
        OrgVersion newVersion = insertNewUserVersionAndActivate();

        // Reset all mappings in the new versions
        orgUsers.stream()
                .map(orgUser -> orgUser.toBuilder().ids(Set.of()).build())
                .forEach(orgUser -> {
                    try {
                        orgUsersService.upsert(company, orgUser);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        usersToUpdate = userIdConsolidationStage.getOrgUsersToUpdate(company, List.of(
                        Set.of(
                                allUsers.get(0),
                                allUsers.get(1)
                        ), Set.of(
                                allUsers.get(2)
                        ), Set.of(
                                allUsers.get(3)
                        )
                ),
                Optional.of(newVersion));
        // Now we should get all users back because we're only looking at the mappings of the current version
        assertThat(usersToUpdate).hasSize(3);
        assertThat(usersToUpdate).map(DBOrgUser::getEmail).containsExactlyInAnyOrder("email1", "email2", "email3");
        assertThat(usersToUpdate).map(DBOrgUser::getIds).map(ids -> ids.stream().map(id -> id.getCloudId()).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(
                                Set.of(allUsers.get(0).getCloudId(), allUsers.get(1).getCloudId()),
                                Set.of(allUsers.get(2).getCloudId()),
                                Set.of(allUsers.get(3).getCloudId())
                        )
                );
    }

    @Test
    public void testNoEmails() throws SQLException, JsonProcessingException {
        OrgVersion version = insertNewUserVersionAndActivate();
        DbJobDefinition jobDefinition = createAndInsertJobDefinition();
        var allUsers = List.of(
                createAndInsert(List.of()), // 0
                createAndInsert(List.of()), // 1
                createAndInsert(List.of()), // 2
                createAndInsert(List.of())  // 3
        );

        var orgUsers = List.of(
                createOrgUser("email1", List.of(allUsers.get(0), allUsers.get(1))),
                createOrgUser("email2", List.of(allUsers.get(2))),
                createOrgUser("email3", List.of(allUsers.get(3)))
        );
        for (DBOrgUser orgUser : orgUsers) {
            orgUsersService.insert(company, orgUser);
        }
        var initialMappings = orgUsersService.streamOrgUserCloudIdMappings(
                        company, OrgUsersDatabaseService.OrgUserCloudIdMappingFilter.builder().build())
                .collect(Collectors.toList());
        assertThat(initialMappings).hasSize(4);

        userIdConsolidationStage.process(
                JobContext.builder()
                        .jobInstanceId(JobInstanceId.builder().jobDefinitionId(jobDefinition.getId()).instanceId(1).build())
                        .jobScheduledStartTime(new Date(2023, Calendar.DECEMBER, 10))
                        .tenantId(company)
                        .stageProgressMap(new HashMap<>())
                        .stageProgressDetailMap(new HashMap<>())
                        .gcsRecords(List.of())
                        .etlProcessorName("TestJobDefinition")
                        .isFull(false)
                        .jobType(JobType.GENERIC_TENANT_JOB)
                        .build(),
                new UserIdConsolidationState()
        );

        var orgUsersByEmail = orgUsersService.stream(company, QueryFilter.builder().build(), 100)
                .collect(Collectors.toMap(u -> u.getEmail(), u -> u));
        assertThat(orgUsersByEmail).hasSize(3);
        assertThat(orgUsersByEmail.get("email1").getIds()).map(l -> l.getCloudId()).containsExactlyInAnyOrder(
                allUsers.get(0).getCloudId(),
                allUsers.get(1).getCloudId()
        );
        assertThat(orgUsersByEmail.get("email2").getIds()).map(l -> l.getCloudId()).containsExactlyInAnyOrder(
                allUsers.get(2).getCloudId()
        );
        assertThat(orgUsersByEmail.get("email3").getIds()).map(l -> l.getCloudId()).containsExactlyInAnyOrder(
                allUsers.get(3).getCloudId()
        );
        var finalMappings = orgUsersService.streamOrgUserCloudIdMappings(
                        company, OrgUsersDatabaseService.OrgUserCloudIdMappingFilter.builder().build())
                .collect(Collectors.toList());
        assertThat(finalMappings).hasSize(4);
    }

    @Test
    public void testDedupeVersion() throws SQLException, JsonProcessingException {
        OrgVersion version = insertNewUserVersionAndActivate();
        DbJobDefinition jobDefinition = createAndInsertJobDefinition();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email2")), // 2
                createAndInsert(List.of("email3")) // 3
        );

        JobContext context = createJobContext(jobDefinition.getId());
        userIdConsolidationStage.process(context, new UserIdConsolidationState());

        // This should create 3 org users, 4 mappings and a new version
        OrgVersion version2 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version2.getVersion()).isEqualTo(version.getVersion() + 1);
        var allOrgUsers = orgUsersService.list(company, 0, 100).getRecords();
        assertThat(allOrgUsers).hasSize(3);
        assertThat(orgUsersService.streamOrgUserCloudIdMappings(company, OrgUsersDatabaseService.OrgUserCloudIdMappingFilter.builder().build())
                .collect(Collectors.toList())).hasSize(4);

        // Create a couple new integration users and run the job again
        createAndInsert(List.of("email3"));
        createAndInsert(List.of("email2"));
        userIdConsolidationStage.process(context, new UserIdConsolidationState());

        // New mappings should be created, but no new version should be created
        OrgVersion version3 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version2.getVersion()).isEqualTo(version3.getVersion());
        allOrgUsers = orgUsersService.list(company, 0, 100).getRecords();
        assertThat(allOrgUsers).hasSize(3);
        allOrgUsers.stream().map(u -> u.getVersions()).forEach(versions -> assertThat(versions).contains(version3.getVersion()));

        // Create a new integration user this time with a new email
        createAndInsert(List.of("email4"));
        userIdConsolidationStage.process(context, new UserIdConsolidationState());
        OrgVersion version4 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version4.getVersion()).isEqualTo(version3.getVersion());
        allOrgUsers = orgUsersService.list(company, 0, 100).getRecords();
        assertThat(allOrgUsers).hasSize(4);
        allOrgUsers.stream().map(u -> u.getVersions()).forEach(versions -> assertThat(versions).contains(version4.getVersion()));

        // Now bump up the version and run the job again, and a new version should be created
        orgUsersHelper.updateUsers(company, Stream.of());
        createAndInsert(List.of("email4"));
        userIdConsolidationStage.process(context, new UserIdConsolidationState());
        OrgVersion version5 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version5.getVersion()).isEqualTo(version4.getVersion() + 2);
    }

    @Test
    public void testDedupeWithRollback() throws SQLException, JsonProcessingException {
        OrgVersion version = insertNewUserVersionAndActivate();
        DbJobDefinition jobDefinition = createAndInsertJobDefinition();
        var allUsers = List.of(
                createAndInsert(List.of("email1")), // 0
                createAndInsert(List.of("email1")), // 1
                createAndInsert(List.of("email2")), // 2
                createAndInsert(List.of("email3")) // 3
        );

        JobContext context = createJobContext(jobDefinition.getId());
        userIdConsolidationStage.process(context, new UserIdConsolidationState());

        // This should create 3 org users, 4 mappings and a new version
        OrgVersion version2 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version2.getVersion()).isEqualTo(version.getVersion() + 1);

        // Create new version
        orgUsersHelper.updateUsers(company, Stream.of());
        OrgVersion version3 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version3.getVersion()).isEqualTo(version2.getVersion() + 1);

        // Rollback
        orgUsersHelper.activateVersion(company, version2.getId());
        OrgVersion version4 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version4.getVersion()).isEqualTo(version2.getVersion());

        createAndInsert(List.of("email3"));
        createAndInsert(List.of("email4"));
        userIdConsolidationStage.process(context, new UserIdConsolidationState());
        OrgVersion version5 = orgVersionsService.getActive(company, OrgVersion.OrgAssetType.USER).get();
        assertThat(version5.getVersion()).isEqualTo(version2.getVersion());
        var allOrgUsers = orgUsersService.list(company, 0, 100).getRecords();
        assertThat(allOrgUsers).hasSize(4);
        allOrgUsers.stream().map(u -> u.getVersions()).forEach(versions -> assertThat(versions).contains(version2.getVersion()));
    }

    private JobContext createJobContext(UUID jobDefinitionId) {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(jobDefinitionId).instanceId(1).build())
                .jobScheduledStartTime(new Date(2023, Calendar.DECEMBER, 10))
                .tenantId(company)
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .gcsRecords(List.of())
                .etlProcessorName("TestJobDefinition")
                .isFull(false)
                .jobType(JobType.GENERIC_TENANT_JOB)
                .build();
    }

    private DBOrgUser createOrgUser(String email, List<DbScmUser> integrationUsers) throws SQLException {
        var orgUser = DBOrgUser.builder()
                .email(email)
                .fullName("name")
                .ids(integrationUsers
                        .stream()
                        .map(this::toLoginId)
                        .collect(Collectors.toSet()))
                .active(true)
                .build();
        return orgUser;
    }

    private DbJobDefinition createAndInsertJobDefinition() throws JsonProcessingException {
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .tenantId(company)
                .integrationId(integrationId1.toString())
                .jobType(JobType.GENERIC_TENANT_JOB)
                .aggProcessorName("TestProcessor")
                .defaultPriority(JobPriority.HIGH)
                .isActive(true)
                .build();
        var id = jobDefinitionDatabaseService.insert(jobDefinition);
        return jobDefinition.toBuilder()
                .id(id)
                .build();
    }

    private DBOrgUser.LoginId toLoginId(DbScmUser user) {
        return DBOrgUser.LoginId.builder()
                .cloudId(user.getCloudId())
                .integrationId(Integer.parseInt(user.getIntegrationId()))
                .username(user.getDisplayName())
                .integrationType("application")
                .build();
    }

    private OrgVersion insertNewUserVersionAndActivate() throws SQLException {
        var version = orgVersionsService.insert(company, OrgVersion.OrgAssetType.USER);
        orgVersionsService.update(company, version, true);
        return orgVersionsService.get(company, version).get();
    }

    private DbScmUser createUser(List<String> emails) {
        return DbScmUser.builder()
                .id(UUID.randomUUID().toString())
                .integrationId("1")
                .cloudId(RandomStringUtils.random(10, "abcdefgh123456"))
                .displayName(emails.get(0).toLowerCase())
                .emails(emails)
                .build();
    }

    private DbScmUser createAndInsert(List<String> emails) throws SQLException {
        var name = emails.size() > 0 ? emails.get(0).toLowerCase() : RandomStringUtils.random(10, "abcdefgh123456");
        var dbScmUser = DbScmUser.builder()
                .id(UUID.randomUUID().toString())
                .integrationId("1")
                .cloudId(RandomStringUtils.random(10, "abcdefgh123456"))
                .displayName(name)
                .originalDisplayName(name)
                .mappingStatus(DbScmUser.MappingStatus.AUTO)
                .emails(emails)
                .build();
        var id = userIdentityService.upsert(company, dbScmUser);
        return dbScmUser.toBuilder()
                .id(id)
                .build();
    }

}