package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@Log4j2
public class UserIdentityServiceTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static UserIdentityService userIdentityService;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static String gitHubIntegrationId2;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static DataSource dataSource;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .originalDisplayName("viraj-levelops")
            .build();
    final DbScmUser testScmUser2 = DbScmUser.builder()
            .integrationId(gitHubIntegrationId2)
            .cloudId("ivan-levelops")
            .displayName("ivan-levelops")
            .originalDisplayName("ivan-levelops")
            .build();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        gitHubIntegrationId2 = integrationService.insert(company, Integration.builder()
                .application("github-2")
                .name("github test-2")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
    }

    public void clear() throws SQLException {
        dataSource.getConnection().prepareStatement("DELETE FROM test.scm_pullrequests;").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM test.scm_commits;").execute();
        dataSource.getConnection().prepareStatement("DELETE FROM test.integration_users;").execute();
    }

    public void setUpdatedAt(String uuid, long updatedAt) throws SQLException {
        dataSource.getConnection().prepareStatement("UPDATE test.integration_users SET updated_at = " + updatedAt + " WHERE id = '" + uuid + "';").execute();
    }

    private void verifyRecord(DbScmUser expected, DbScmUser actual) {
        assertThat(expected.getIntegrationId()).isEqualTo(actual.getIntegrationId());
        assertThat(expected.getCloudId()).isEqualTo(actual.getCloudId());
        assertThat(expected.getDisplayName()).isEqualTo(actual.getDisplayName());
        assertThat(expected.getOriginalDisplayName()).isEqualTo(actual.getOriginalDisplayName());
    }

    @Test
    public void testUserInput() throws SQLException {
        scmAggService.insertCommit(company, DbScmCommit.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .changes(3)
                .additions(2).deletions(2).filesCt(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .vcsType(VCS_TYPE.GIT)
                .committedAt(System.currentTimeMillis())
                .createdAt(System.currentTimeMillis())
                .ingestedAt(System.currentTimeMillis())
                .build());
        assertThat(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")).isNotNull();

        Optional<DbScmUser> opt = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "viraj-levelops");
        assertThat(opt.isPresent()).isTrue();
        opt = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "VIRAJ-LEVELOPS");
        assertThat(opt.isPresent()).isTrue();
        opt = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "Viraj-Levelops");
        assertThat(opt.isPresent()).isTrue();
        opt = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "dummy");
        assertThat(opt.isEmpty()).isTrue();

        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("meghana-levelops")
                        .originalDisplayName("meghana-levelops")
                        .cloudId("qwerty")
                        .build());
        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("sample-cog")
                        .originalDisplayName("sample-cog")
                        .cloudId("qwertySample")
                        .build());
        assertThat(uuidInserted1).isNotEmpty();
        assertThat(uuidInserted2).isNotEmpty();
        Stream.of(uuidInserted1, uuidInserted2).forEach(uuid -> {
            try {
                assertThat(List.of("meghana-levelops", "sample-cog")
                        .contains(userIdentityService.get(company, uuid).stream().findFirst().orElseThrow().getDisplayName())).isTrue();
                assertThat(List.of("meghana-levelops", "sample-cog")
                        .contains(userIdentityService.get(company, uuid).stream().findFirst().orElseThrow().getOriginalDisplayName())).isTrue();
                assertThat(List.of("qwerty", "qwertySample")
                        .contains(userIdentityService.get(company, uuid).stream().findFirst().orElseThrow().getCloudId())).isTrue();
            } catch (SQLException e) {
                log.error("Error while inserting user...{0}" + e.getMessage(), e);
            }
        });
        long now = Instant.now().getEpochSecond();
        // List<String> listOfTeamMembers = teamMembersDatabaseService.list(company, 0, 100)
        //         .getRecords().stream().map(DBTeamMember::getFullName).collect(Collectors.toList());
        // assertThat(listOfTeamMembers.contains("meghana-levelops")).isTrue();
        // assertThat(listOfTeamMembers.contains("sample-cog")).isTrue();
        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("1").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339289").reviewer("viraj-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("2").displayName("viraj1-levelops").originalDisplayName("viraj1-levelops").build())
                .reviewId("543339290").reviewer("viraj1-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        scmAggService.insert(company, DbScmPullRequest.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).number("164").integrationId(gitHubIntegrationId2)
                .project("levelops/ingestion-levelops")
                .creator("viraj-levelops").mergeSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(testScmUser2)
                .title("LEV-1983").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build());
        assertThat(userIdentityService.getUser(company, gitHubIntegrationId2, "ivan-levelops")).isNotNull();

        uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("harsh-levelops")
                        .originalDisplayName("harsh-levelops")
                        .cloudId("cloudid1")
                        .build());
        uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId2)
                        .displayName("ivan-levelops")
                        .originalDisplayName("ivan-levelops")
                        .cloudId("cloudid2")
                        .build());
        assertThat(uuidInserted1).isNotEmpty();
        assertThat(uuidInserted2).isNotEmpty();
        Stream.of(uuidInserted1, uuidInserted2).forEach(uuid -> {
            try {
                assertThat(List.of("harsh-levelops", "ivan-levelops")
                        .contains(userIdentityService.get(company, uuid).stream().findFirst().orElseThrow().getDisplayName())).isTrue();
                assertThat(List.of("cloudid1", "cloudid2")
                        .contains(userIdentityService.get(company, uuid).stream().findFirst().orElseThrow().getCloudId())).isTrue();
            } catch (SQLException e) {
                log.error("Error updating the DB: {}", e.getMessage(), e);
            }
        });
    }

    // TODO: This test needs to be refactored because it depends on state created
    // by the other tests. This means that any new tests added to this class will
    // break this test.
//    @Test
    public void testUsersList() throws SQLException {
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(10);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(6);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .integrationIds(List.of("2"))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(4);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .integrationIds(List.of("1", "2"))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(10);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .cloudIds(List.of("viraj-levelops", "cloudid1"))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(2);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .cloudIds(List.of())
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(10);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .partialMatch(Map.of("display_name", Map.of("$ends", "levelops")))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(7);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .partialMatch(Map.of("display_name", Map.of("$begins", "ivan")))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(2);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .partialMatch(Map.of("display_name", Map.of("$contains", "i")))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(5);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .partialMatch(Map.of("display_name",
                                                Map.of("$contains", "i", "$begins", "ivan")))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(2);
        assertThat(
                userIdentityService.list(company,
                                UserIdentitiesFilter.builder()
                                        .partialMatch(Map.of("display_name",
                                                Map.of("$contains", "z", "$begins", "ivan")))
                                        .build(), 0, 100)
                        .getTotalCount()
        ).isEqualTo(0);
        validateSortedDisplayNames(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("display_name", SortingOrder.ASC))
                                .build(), 0, 100),
                SortingOrder.ASC);
        validateSortedDisplayNames(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("display_name", SortingOrder.DESC))
                                .build(), 0, 100),
                SortingOrder.DESC);
        validateSortedCreatedAt(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("created_at", SortingOrder.ASC))
                                .build(), 0, 100),
                SortingOrder.ASC);
        validateSortedCreatedAt(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("created_at", SortingOrder.DESC))
                                .build(), 0, 100),
                SortingOrder.DESC);
        validateSortedUpdatedAt(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("updated_at", SortingOrder.ASC))
                                .build(), 0, 100),
                SortingOrder.ASC);
        validateSortedUpdatedAt(
                userIdentityService.list(company,
                        UserIdentitiesFilter.builder()
                                .sort(Map.of("updated_at", SortingOrder.DESC))
                                .build(), 0, 100),
                SortingOrder.DESC);
    }

    @Test
    public void listByAssigneeIdTest() throws SQLException {
        scmAggService.insertCommit(company, DbScmCommit.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .changes(3)
                .additions(2).deletions(2).filesCt(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .vcsType(VCS_TYPE.GIT)
                .committedAt(System.currentTimeMillis())
                .createdAt(System.currentTimeMillis())
                .ingestedAt(System.currentTimeMillis())
                .build());

        assertThat(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")).isNotNull();
        String dbScmUserId = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        log.info("dbscmuser: {}", dbScmUserId);

        assertThat(
                userIdentityService.list(company,
                                List.of(dbScmUserId))
                        .getRecords().get(0).getDisplayName()
        ).isEqualTo("viraj-levelops");

        List<DbScmUser> users = userIdentityService.listUserByDisplayNames(company, List.of(gitHubIntegrationId),
                List.of("viraj-levelops")).getRecords();
        assertNotNull(users);
        users.forEach(dbScmUser -> {
            assertThat(dbScmUser.getDisplayName()).isEqualTo("viraj-levelops");
            assertThat(dbScmUser.getIntegrationId()).isEqualTo(gitHubIntegrationId);
        });
    }

    private void validateSortedItems(DbListResponse<DbScmUser> response,
                                     SortingOrder sortOrder,
                                     Function<? super DbScmUser, ? extends String> mapper) {
        assertNotNull(response);
        assertNotNull(response.getRecords());
        if (sortOrder.equals(SortingOrder.ASC)) {
            assertThat(response.getRecords()).isSortedAccordingTo(Comparator.comparing(
                    mapper, String.CASE_INSENSITIVE_ORDER));
        } else {
            assertThat(response.getRecords()).isSortedAccordingTo(Comparator.comparing(
                    mapper, String.CASE_INSENSITIVE_ORDER).reversed());
        }
    }

    private void validateSortedDisplayNames(DbListResponse<DbScmUser> response, SortingOrder sortOrder) {
        validateSortedItems(response, sortOrder, DbScmUser::getDisplayName);
    }

    private void validateSortedTemporalValues(DbListResponse<DbScmUser> response,
                                              SortingOrder sortOrder,
                                              Function<? super DbScmUser, ? extends Long> mapper) {
        assertNotNull(response);
        assertNotNull(response.getRecords());
        if (sortOrder.equals(SortingOrder.ASC)) {
            assertThat(response.getRecords().stream().map(mapper).collect(Collectors.toList())).isSorted();
        } else {
            assertThat(response.getRecords().stream().map(mapper).collect(Collectors.toList()))
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    private void validateSortedCreatedAt(DbListResponse<DbScmUser> response, SortingOrder sortOrder) {
        validateSortedTemporalValues(response, sortOrder, DbScmUser::getCreatedAt);
    }

    private void validateSortedUpdatedAt(DbListResponse<DbScmUser> response, SortingOrder sortOrder) {
        validateSortedTemporalValues(response, sortOrder, DbScmUser::getUpdatedAt);
    }

    @Test
    public void listByOriginalDisplayName() throws SQLException {
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Satish")
                        .originalDisplayName("Satish")
                        .cloudId("cloudid1")
                        .build());
        Optional<String> uuidInserted2 = userIdentityService.getUserByOriginalDisplayName(company, gitHubIntegrationId, "Satish");
        assertThat(uuidInserted1).isEqualTo(uuidInserted2.get());
    }

    @Test
    public void testGetUserByEmail() throws SQLException {
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Harvey Specter")
                        .originalDisplayName("Harvey Specter")
                        .cloudId("harvey.specter")
                        .emails(List.of("harvey.specter@law.com"))
                        .build());
        Optional<DbScmUser> retrievedUser1 = userIdentityService.getUserByEmail(company, gitHubIntegrationId, "harvey.specter@law.com");
        assertThat(uuidInserted1).isEqualTo(retrievedUser1.get().getId());

        Optional<DbScmUser> retrievedUser2 = userIdentityService.getUserByEmail(company, gitHubIntegrationId, "a");
        assertThat(retrievedUser2).isEmpty();

        Optional<DbScmUser> retrievedUser3 = userIdentityService.getUserByEmail(company, gitHubIntegrationId, "harvey.s.specter@law.com");
        assertThat(retrievedUser3).isEmpty();
    }

    @Test
    public void testEmails() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of("GOAT@nba.com"))
                        .build());
        var retrieved = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved.getId()).isEqualTo(uuidInserted1);
        assertThat(retrieved.getEmails()).containsExactly("GOAT@nba.com");
        assertThat(retrieved.getOriginalDisplayName()).isEqualTo("Steph Curry");
        assertThat(retrieved.getCloudId()).isEqualTo("stephcurry30");

        // Set emails to null. The returned emails should be an empty list
        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(null)
                        .build());
        var retrieved2 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved2.getEmails()).isEmpty();
        assertThat(retrieved2.getOriginalDisplayName()).isEqualTo("Steph Curry");

        // Set the emails to an empty list explicitly. This should reflect in the DB
        String uuidInserted3 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of())
                        .build());
        var retrieved3 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved3.getEmails()).isEmpty();
        assertThat(retrieved3.getOriginalDisplayName()).isEqualTo("Steph Curry");

        // Set email to GOAT@nba.com again
        String uuidInserted4 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of("GOAT@nba.com"))
                        .build());
        var retrieved4 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved4.getId()).isEqualTo(uuidInserted4);
        assertThat(retrieved4.getEmails()).containsExactly("GOAT@nba.com");

        // Change the display name and set the email to null. This time the email should change
        String uuidInserted5 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Stephen Curry (Changed)")
                        .originalDisplayName("Stephen Curry (Changed)")
                        .cloudId("stephcurry30")
                        .emails(null)
                        .build());
        var retrieved5 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved5.getId()).isEqualTo(uuidInserted5);
        assertThat(retrieved5.getDisplayName()).isEqualTo("Stephen Curry (Changed)");
        assertThat(retrieved5.getEmails()).isEmpty();
    }

    @Test
    public void testInsertNoUpsert() throws SQLException {
        clear();
        var steph = DbScmUser.builder()
                .integrationId(gitHubIntegrationId)
                .displayName("Steph Curry")
                .originalDisplayName("Steph Curry")
                .cloudId("stephcurry30")
                .emails(List.of("GOAT@nba.com"))
                .build();
        String uuidInserted1 = userIdentityService.upsert(company, steph);
        var retrieved = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved.getId()).isEqualTo(uuidInserted1);
        assertThat(retrieved.getEmails()).containsExactly("GOAT@nba.com");
        assertThat(retrieved.getOriginalDisplayName()).isEqualTo("Steph Curry");
        assertThat(retrieved.getCloudId()).isEqualTo("stephcurry30");

        var insertNoUpsertId = userIdentityService.insertNoUpsert(company, steph.toBuilder().originalDisplayName("Goat Curry").build());
        assertThat(insertNoUpsertId).isNull();
        var retrieved2 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved2.getId()).isEqualTo(uuidInserted1);
        assertThat(retrieved2.getOriginalDisplayName()).isEqualTo("Steph Curry");
        assertThat(retrieved2.getCloudId()).isEqualTo("stephcurry30");
    }

    @Test
    public void testBulkInsertWithEmails() throws SQLException {
        clear();
        userIdentityService.batchUpsert(company, List.of(
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of("GOAT@nba.com"))
                        .build(),
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("game6klay")
                        .emails(List.of("game6klay@nba.com"))
                        .build()
        ));
        var klayRetrieved = userIdentityService.getUserByCloudId(
                company,
                gitHubIntegrationId,
                "game6klay"
        ).get();
        var stephRetrieved = userIdentityService.getUserByCloudId(
                company,
                gitHubIntegrationId,
                "stephcurry30"
        ).get();
        assertThat(klayRetrieved.getEmails()).containsExactly("game6klay@nba.com");
        assertThat(stephRetrieved.getEmails()).containsExactly("GOAT@nba.com");
    }

    @Test
    public void testBulkInsertIgnoreEmails() throws SQLException {
        clear();
        userIdentityService.batchUpsertIgnoreEmail(company, List.of(
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of("GOAT@nba.com"))
                        .build(),
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("game6klay")
                        .emails(List.of("game6klay@nba.com"))
                        .build()
        ));
        var klayRetrieved = userIdentityService.getUserByCloudId(
                company,
                gitHubIntegrationId,
                "game6klay"
        ).get();
        var stephRetrieved = userIdentityService.getUserByCloudId(
                company,
                gitHubIntegrationId,
                "stephcurry30"
        ).get();
        assertThat(klayRetrieved.getEmails()).isEmpty();
        assertThat(stephRetrieved.getEmails()).isEmpty();
    }


    @Test
    public void testUpsertIgnoreEmail() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of("GOAT@nba.com"))
                        .build());
        var retrieved = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved.getId()).isEqualTo(uuidInserted1);
        assertThat(retrieved.getEmails()).containsExactly("GOAT@nba.com");
        assertThat(retrieved.getOriginalDisplayName()).isEqualTo("Steph Curry");
        assertThat(retrieved.getCloudId()).isEqualTo("stephcurry30");

        // Change the display name and set the email to null. This time the email should not change because we are ignoring it
        String uuidInserted2 = userIdentityService.upsertIgnoreEmail(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Stephen Curry (Changed)")
                        .originalDisplayName("Stephen Curry (Changed)")
                        .cloudId("stephcurry30")
                        .emails(null)
                        .build());
        var retrieved2 = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrieved2.getId()).isEqualTo(uuidInserted2);
        assertThat(retrieved2.getDisplayName()).isEqualTo("Stephen Curry (Changed)");
        assertThat(retrieved2.getEmails()).containsExactly("GOAT@nba.com");
    }

    @Test
    public void testEmptyEmailFilter() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .emails(List.of())
                        .build());

        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("iceklay")
                        .emails(List.of("klay@baddy.com"))
                        .build());

        String uuidInserted3 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("DrayDray")
                        .originalDisplayName("DrayDray")
                        .cloudId("killerDray")
                        .emails(List.of("dray@baddy.com"))
                        .build());

        String uuidInserted4 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("pooleparty")
                        .originalDisplayName("pooleparty")
                        .cloudId("poolepoole")
                        .build());

        var emptyEmailUsers = userIdentityService.stream(company, UserIdentitiesFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .emptyEmails(true)
                        .build())
                .collect(Collectors.toList());
        assertThat(emptyEmailUsers).map(DbScmUser::getDisplayName).containsExactlyInAnyOrder("pooleparty", "Steph Curry");

        var nonEmptyEmailUsers = userIdentityService.stream(company, UserIdentitiesFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .emptyEmails(false)
                        .build())
                .collect(Collectors.toList());
        assertThat(nonEmptyEmailUsers).map(DbScmUser::getDisplayName).containsExactlyInAnyOrder("DrayDray", "Klay Thompson");
    }

    @Test
    public void testMappingStatus() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .emails(List.of())
                        .build());

        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("iceklay")
                        .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                        .emails(List.of("klay@baddy.com"))
                        .build());

        String uuidInserted4 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("pooleparty")
                        .originalDisplayName("pooleparty")
                        .cloudId("poolepoole")
                        .build());
        var retrievedCurry = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrievedCurry.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.AUTO);

        var retrievedKlay = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "iceklay").get();
        assertThat(retrievedKlay.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.MANUAL);

        // Default mapping status is AUTO
        var retrievedPoole = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "poolepoole").get();
        assertThat(retrievedPoole.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.AUTO);
    }

    @Test
    public void testCloudIdsToMarkAsOverridden() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .emails(List.of())
                        .build());

        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("iceklay")
                        .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                        .emails(List.of("klay@baddy.com"))
                        .build());

        String uuidInserted4 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("pooleparty")
                        .originalDisplayName("pooleparty")
                        .cloudId("poolepoole")
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .build());

        userIdentityService.batchUpdateMappingStatus(company, List.of(
                DBOrgUser.LoginId.builder()
                        .cloudId("stephcurry30")
                        .integrationId(Integer.parseInt(gitHubIntegrationId))
                        .build(),
                DBOrgUser.LoginId.builder()
                        .cloudId("iceklay")
                        .integrationId(Integer.parseInt(gitHubIntegrationId))
                        .build(),
                DBOrgUser.LoginId.builder()
                        .cloudId("poolepoole")
                        .integrationId(Integer.parseInt(gitHubIntegrationId))
                        .build()
        ), DbScmUser.MappingStatus.MANUAL);

        var retrievedCurry = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "stephcurry30").get();
        assertThat(retrievedCurry.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.MANUAL);

        var retrievedKlay = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "iceklay").get();
        assertThat(retrievedKlay.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.MANUAL);

        var retrievedPoole = userIdentityService.getUserByCloudId(company, gitHubIntegrationId, "poolepoole").get();
        assertThat(retrievedPoole.getMappingStatus()).isEqualTo(DbScmUser.MappingStatus.MANUAL);
    }

    @Test
    public void testMappingStatusFilter() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .emails(List.of())
                        .build());

        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("iceklay")
                        .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                        .emails(List.of("klay@baddy.com"))
                        .build());

        var response = userIdentityService.list(company, UserIdentitiesFilter.builder()
                .mappingStatus(DbScmUser.MappingStatus.AUTO)
                .build(), 0, 10);
        assertThat(response.getRecords()).map(r -> r.getDisplayName()).containsExactlyInAnyOrder("Steph Curry");

        response = userIdentityService.list(company, UserIdentitiesFilter.builder()
                .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                .build(), 0, 10);
        assertThat(response.getRecords()).map(r -> r.getDisplayName()).containsExactlyInAnyOrder("Klay Thompson");
    }

    @Test
    public void testStableOrderStream() throws SQLException {
        clear();
        String uuidInserted1 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Steph Curry")
                        .originalDisplayName("Steph Curry")
                        .cloudId("stephcurry30")
                        .mappingStatus(DbScmUser.MappingStatus.AUTO)
                        .emails(List.of())
                        .build());

        String uuidInserted2 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Klay Thompson")
                        .originalDisplayName("Klay Thompson")
                        .cloudId("iceklay")
                        .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                        .emails(List.of("klay@baddy.com"))
                        .build());
        String uuidInserted3 = userIdentityService.upsert(company,
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Dray")
                        .originalDisplayName("Dray")
                        .cloudId("draydray")
                        .mappingStatus(DbScmUser.MappingStatus.MANUAL)
                        .emails(List.of("dray@baddy.com"))
                        .build());

        setUpdatedAt(uuidInserted1, 1L);
        setUpdatedAt(uuidInserted2, 1L);
        setUpdatedAt(uuidInserted3, 1L);

        var expectedReponseOrder = Stream.of(uuidInserted1, uuidInserted2, uuidInserted3)
                .sorted()
                .collect(Collectors.toList());

        //  Run this 5 times because the order is not guaranteed. So we decrease the probability of a false positive
        for (int i = 0; i < 5; i++) {
            streamUsersAndAssertOrder(UserIdentitiesFilter.builder()
                    .sort(Map.of("updated_at", SortingOrder.ASC))
                    .build(), expectedReponseOrder);
        }
    }

    private void streamUsersAndAssertOrder(UserIdentitiesFilter filter, List<String> expectedOrder) throws SQLException {
        var response = userIdentityService.stream(company, filter);
        assertThat(response.map(DbScmUser::getId).collect(Collectors.toList())).isEqualTo(expectedOrder);
    }
}
