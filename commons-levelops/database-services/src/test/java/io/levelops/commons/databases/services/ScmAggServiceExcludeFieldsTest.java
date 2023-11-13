package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.organization.TeamMemberId;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmAggServiceExcludeFieldsTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static String gitHubIntegrationId;
    private static Date currentTime;
    private static String teamId1;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        ObjectMapper mapper = DefaultObjectMapper.get();
        TeamMembersDatabaseService teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        teamMembersDatabaseService.ensureTableExistence(company);
        TeamsDatabaseService teamsDatabaseService = new TeamsDatabaseService(dataSource, mapper);
        teamsDatabaseService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), gitHubIntegrationId, null);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), gitHubIntegrationId);
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), gitHubIntegrationId,
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), gitHubIntegrationId, currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos);
        String scmUserIdViraj = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        String scmUserIdKush = userIdentityService.getUser(company, gitHubIntegrationId, "kush-prof");
        var dbScmUsers = List.of(
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("Viraj")
                        .originalDisplayName("Viraj")
                        .cloudId(scmUserIdViraj)
                        .build(),
                DbScmUser.builder()
                        .integrationId(gitHubIntegrationId)
                        .displayName("kush")
                        .originalDisplayName("kush")
                        .cloudId(scmUserIdKush)
                        .build()
        );
        List<String> newUuidsInserted = List.of(
                userIdentityService.upsert(company, dbScmUsers.get(0))
//                userIdentityService.upsert(company, dbScmUsers.get(1))
        );
        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(0).getDisplayName()).build(), UUID.fromString(newUuidsInserted.get(0)));
//        teamMembersDatabaseService.upsert(company, DBTeamMember.builder().fullName(dbScmUsers.get(1).getDisplayName()).build(), UUID.fromString(newUuidsInserted.get(1)));

        List<Optional<TeamMemberId>> teamMemberIds = newUuidsInserted.stream()
                .map(uuidInserted -> teamMembersDatabaseService.getId(company, UUID.fromString(uuidInserted))).collect(Collectors.toList());
        DBTeam team1 = DBTeam.builder()
                .name("name")
                .description("description")
                .managers(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build()))
                .members(Set.of(DBTeam.TeamMemberId.builder().id(UUID.fromString(teamMemberIds.get(0).get().getTeamMemberId())).build()))
                .build();
        teamId1 = teamsDatabaseService.insert(company, team1);

        String arrayCatAgg = "CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testScmPRsList() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .projects(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .excludeProjects(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .excludeRepoIds(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeStates(List.of("closed"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeSourceBranches(List.of("jen", "lev-1222"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeAssignees(List.of("viraj-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeReviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeApprovers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .creators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeCreators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeCreators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmCommitsList() {
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .projects(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeProjects(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .repoIds(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeRepoIds(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .commitShas(List.of("8616d2d52d51135065b7deab9e9718d09b6c18ab",
                                        "6cf4e8122baa2bec1383ab96ddd7c0c0817d9f49",
                                        "3b3be7e8bd328f8aa6e0d2c87d326046b896c970",
                                        "fd6caa2f13b2f8b491a4b6c869feab895c89eb77"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .commitShas(List.of("8616d2d52d51135065b7deab9e9718d09b6c18ab",
                                        "6cf4e8122baa2bec1383ab96ddd7c0c0817d9f49",
                                        "3b3be7e8bd328f8aa6e0d2c87d326046b896c970",
                                        "fd6caa2f13b2f8b491a4b6c869feab895c89eb77"))
                                .excludeCommitShas(List.of("8616d2d52d51135065b7deab9e9718d09b6c18ab",
                                        "6cf4e8122baa2bec1383ab96ddd7c0c0817d9f49",
                                        "3b3be7e8bd328f8aa6e0d2c87d326046b896c970",
                                        "fd6caa2f13b2f8b491a4b6c869feab895c89eb77"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .commitShas(List.of("8616d2d52d51135065b7deab9e9718d09b6c18ab",
                                        "6cf4e8122baa2bec1383ab96ddd7c0c0817d9f49",
                                        "3b3be7e8bd328f8aa6e0d2c87d326046b896c970",
                                        "fd6caa2f13b2f8b491a4b6c869feab895c89eb77"))
                                .excludeCommitShas(List.of("8616d2d52d51135065b7deab9e9718d09b6c18ab",
                                        "6cf4e8122baa2bec1383ab96ddd7c0c0817d9f49"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of(
//                                        userIdentityService.getUser(company, gitHubIntegrationId, "meghana-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .excludeCommitters(List.of(
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .excludeCommitters(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeCommitters(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .excludeAuthors(List.of(
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .excludeAuthors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeAuthors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmIssuesList() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .projects(List.of("hashicorp/vault"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .excludeProjects(List.of("hashicorp/vault"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .repoIds(List.of("hashicorp/vault"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .excludeRepoIds(List.of("hashicorp/vault"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .states(List.of("closed", "open"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .excludeStates(List.of("closed", "open"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .excludeStates(List.of("closed"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .excludeLabels(List.of("docs-cherrypick", "feature-request,secret/pki"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .assignees(List.of("calvn", "noelledaley", "chelshaw", "vishalnayak"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .assignees(List.of("calvn", "noelledaley", "chelshaw", "vishalnayak"))
                                .excludeAssignees(List.of("calvn", "noelledaley", "chelshaw", "vishalnayak"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .assignees(List.of("calvn", "noelledaley", "chelshaw", "vishalnayak"))
                                .excludeAssignees(List.of("calvn", "noelledaley"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .creators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "jwitko"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "mberhault"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "SledgeHammer01")))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat
                (scmAggService.list(company,
                        ScmIssueFilter.builder()
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .creators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "jwitko"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "mberhault"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "SledgeHammer01")))
                                .excludeCreators(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "gwilym"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "riuvshyn"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops"),
                                        userIdentityService.getUser(company, gitHubIntegrationId, "upodroid")))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(3);
    }

    @Test
    public void testScmFilesList() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmFilesFilter.builder()
                                .projects(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), 0, 1000)
                        .getTotalCount())
                .isEqualTo(6);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmFilesFilter.builder()
                                .excludeProjects(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company, ScmFilesFilter.builder()
                                .repoIds(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), 0, 1000)
                        .getTotalCount())
                .isEqualTo(6);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmFilesFilter.builder()
                                .excludeRepoIds(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmReposList() {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeProjects(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .excludeProjects(List.of("levelops/ui-levelops"))
                                .excludeRepoIds(List.of("levelops/ui-levelops", "levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        String user = userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm");
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .committers(List.of(user))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .excludeCommitters(List.of(user))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .committers(List.of(user))
                                .excludeCommitters(List.of(user))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .excludeAuthors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeAuthors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .authors(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
    }

    @Test
    public void testScmCommittersList() {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder().includeIssues(true).build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(3);
        String user = userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm");
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .committers(List.of(user))
                                .includeIssues(true)
                                .projects(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .excludeCommitters(List.of(user))
                                .includeIssues(true)
                                .projects(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .committers(List.of(user))
                                .excludeCommitters(List.of(user))
                                .includeIssues(true)
                                .projects(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .authors(List.of(user))
                                .projects(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .excludeAuthors(List.of(user))
                                .includeIssues(true)
                                .projects(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .authors(List.of(user))
                                .excludeAuthors(List.of(user))
                                .includeIssues(true)
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .projects(List.of("levelops/ui-levelops"))
                                .includeIssues(true)
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .excludeProjects(List.of("levelops/ui-levelops"))
                                .includeIssues(true)
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .repoIds(List.of("levelops/ui-levelops"))
                                .includeIssues(true)
                                .integrationIds(List.of(gitHubIntegrationId))
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .excludeRepoIds(List.of("levelops/ui-levelops"))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .includeIssues(true)
                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
    }

    @Test
    public void testExcludeTeamFilter() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .creators(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .excludeCreators(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .creators(List.of("team_id:" + teamId1))
                                .excludeCreators(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .creators(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeCreators(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeCommitters(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of("team_id:" + teamId1))
                                .excludeCommitters(List.of("team_id:" + teamId1))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.listCommits(company,
                        ScmCommitFilter.builder()
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmReposFilter.builder()
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .includeIssues(true)
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .includeIssues(true)
                                .committers(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(company,
                        ScmContributorsFilter.builder()
                                .includeIssues(true)
                                .excludeCommitters(List.of("team_id:" + teamId1,
                                        userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .integrationIds(List.of(gitHubIntegrationId))
                                .build(),
                        Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(2);
    }

}
