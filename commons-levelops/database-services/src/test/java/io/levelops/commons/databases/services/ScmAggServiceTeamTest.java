package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
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
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.databases.services.organization.TeamsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
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

public class ScmAggServiceTeamTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static Date currentTime;
    private static String teamId1;
    private static String gitHubIntegrationId;
    private static final String company = "sample";

    @BeforeClass
    public static void setup() throws Exception {
        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        JiraIssueService jiraIssueService = jiraTestDbs.getJiraIssueService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

        ObjectMapper mapper = DefaultObjectMapper.get();

        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);

        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        TeamMembersDatabaseService teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, mapper);
        teamMembersDatabaseService.ensureTableExistence(company);

        TeamsDatabaseService teamsDatabaseService = new TeamsDatabaseService(dataSource, mapper);
        teamsDatabaseService.ensureTableExistence(company);

        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = mapper.readValue(jiraIn,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());

                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }

                jiraIssueService.insert(company, tmp);
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("This issue should exist.");
                }
                if (jiraIssueService.get(company, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = mapper.readValue(input,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), "1", null);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = mapper.readValue(input,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), "1");
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(company, tmp);
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = mapper.readValue(input,
                mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), "1",
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), "1", currentTime.toInstant().getEpochSecond())
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
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testTeamFilter() throws SQLException {
        Assertions.assertThat(scmAggService.list(
                company,
                ScmPrFilter.builder()
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .build(),
                Map.of(),
                null,
                0,
                10).getTotalCount()).isEqualTo(10);

        Assertions.assertThat(scmAggService.list(
                company,
                ScmPrFilter.builder()
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .creators(List.of("team_id:" + teamId1))
                        .build(),
                Map.of(),
                null,
                0,
                10).getTotalCount()).isEqualTo(9);

        Assertions.assertThat(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .build(), null).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .across(ScmPrFilter.DISTINCT.pr_merged)
                        .build(), null).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .creators(List.of("team_id:" + teamId1))
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .across(ScmPrFilter.DISTINCT.pr_merged)
                        .build(), null).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .authors(List.of("team_id:" + teamId1))
                        .committers(List.of("team_id:" + teamId1))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getTotalCount()).isEqualTo(0);

        String user1 = userIdentityService.getUser(company, gitHubIntegrationId, "piyushkantm");
        Assertions.assertThat(scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .authors(List.of(user1))
                        .committers(List.of("team_id:" + teamId1))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getTotalCount()).isEqualTo(0);

        // FIXME
//        String user2 = userIdentityService.getUser(company, gitHubIntegrationId, "meghana-levelops");
//        Assertions.assertThat(scmAggService.listCommits(
//                company,
//                ScmCommitFilter.builder()
//                        .committers(List.of(user2))
//                        .build(),
//                Map.of(),
//                null,
//                0,
//                1000).getTotalCount()).isEqualTo(113);

//        Assertions.assertThat(scmAggService.listCommits(
//                company,
//                ScmCommitFilter.builder()
//                        .committers(List.of("team_id:" + teamId1))
//                        .build(),
//                Map.of(),
//                null,
//                0,
//                1000).getTotalCount()).isEqualTo(23);

//        Assertions.assertThat(scmAggService.listCommits(
//                company,
//                ScmCommitFilter.builder()
//                        .committers(List.of("team_id:" + teamId1, user2))
//                        .build(),
//                Map.of(),
//                null,
//                0,
//                1000).getTotalCount()).isEqualTo(136);
    }
}
