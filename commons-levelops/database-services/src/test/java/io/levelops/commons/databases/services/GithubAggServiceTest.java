package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class GithubAggServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static Date currentTime;


    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        IntegrationService integrationService = new IntegrationService(dataSource);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
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
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = new Date();
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), "1");
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty())
                            scmAggService.insertIssue(company, tmp);
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
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
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                    commit, repo.getId(), "1", currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(company, repos).size();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void test() throws SQLException, JsonProcessingException {
        for (ScmPrFilter.DISTINCT a : List.of(ScmPrFilter.DISTINCT.pr_updated,
                ScmPrFilter.DISTINCT.pr_merged,
                ScmPrFilter.DISTINCT.pr_created)) {
            System.out.println("hmm1: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder().across(a).build(), null)));
            System.out.println("hmm2: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder()
                                            .calculation(ScmPrFilter.CALCULATION.first_review_time)
                                            .across(a)
                                            .build(), null)));
            System.out.println("hmm3: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrsDuration(
                                    company, ScmPrFilter.builder()
                                            .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                                            .across(a)
                                            .build(), null)));
        }
        for (ScmPrFilter.DISTINCT a : List.of(ScmPrFilter.DISTINCT.values())) {
            System.out.println("a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrs(
                                    company, ScmPrFilter.builder().across(a).build(), null)));
        }
        // FIXME
//        Assertions.assertThat(
//                scmAggService.groupByAndCalculatePrs(
//                        company, ScmPrFilter.builder()
//                                .integrationIds(List.of("1"))
//                                .reviewers(List.of(userIdentityService.getUser(company, "1", "viraj-levelops")))
//                                .across(ScmPrFilter.DISTINCT.creator)
//                                .build(), null).getTotalCount())
//                .isEqualTo(1);

        for (ScmCommitFilter.DISTINCT a : List.of(ScmCommitFilter.DISTINCT.values())) {
            System.out.println("a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateCommits(
                                    company, ScmCommitFilter.builder()
                                            .integrationIds(List.of("1"))
                                            .across(a)
                                            .build(), null)));
        }

        for (ScmFilesFilter.DISTINCT a : List.of(ScmFilesFilter.DISTINCT.values())) {
            System.out.println("files a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateFiles(
                                    company, ScmFilesFilter.builder()
                                            .integrationIds(List.of("1"))
                                            .across(a)
                                            .build())));
        }
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder().build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(6);
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder()
                        .repoIds(List.of("levelops/ui-levelops"))
                        .integrationIds(List.of("1"))
                        .commitStartTime(currentTime.toInstant().getEpochSecond() - 86400)
                        .commitEndTime(currentTime.toInstant().getEpochSecond() + 86400)
                        .build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(6);

        for (ScmIssueFilter.DISTINCT a : List.of(ScmIssueFilter.DISTINCT.values())) {
            for (ScmIssueFilter.EXTRA_CRITERIA criterion : List.of(ScmIssueFilter.EXTRA_CRITERIA.values())) {
                System.out.println("issues DATA: " + a + " data: " +
                        m.writeValueAsString(
                                scmAggService.groupByAndCalculateIssues(
                                        company, ScmIssueFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .extraCriteria(List.of(criterion))
                                                .across(a)
                                                .build(), null)));
                System.out.println("issues_resp DATA: " + a + " data: " +
                        m.writeValueAsString(
                                scmAggService.groupByAndCalculateIssues(
                                        company, ScmIssueFilter.builder()
                                                .calculation(ScmIssueFilter.CALCULATION.response_time)
                                                .integrationIds(List.of("1"))
                                                .extraCriteria(List.of(criterion))
                                                .across(a)
                                                .build(), null)));
            }
            System.out.println("issues DATA: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateIssues(
                                    company, ScmIssueFilter.builder()
                                            .extraCriteria(List.of())
                                            .integrationIds(List.of("1"))
                                            .across(a)
                                            .build(), null)));
            System.out.println("issues_resp DATA: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateIssues(
                                    company, ScmIssueFilter.builder()
                                            .calculation(ScmIssueFilter.CALCULATION.response_time)
                                            .integrationIds(List.of("1"))
                                            .extraCriteria(List.of())
                                            .across(a)
                                            .build(), null)));
        }
        System.out.println(m.writeValueAsString(scmAggService.list(
                company,
                ScmIssueFilter.builder()
                        .integrationIds(List.of("1"))
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                Map.of(),
                null,
                0,
                1000)));
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmIssueFilter.builder()
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isGreaterThan(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmIssueFilter.builder()
                                .integrationIds(List.of("1"))
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isGreaterThan(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .reviewers(List.of(userIdentityService.getUser(company, "1", "viraj-levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .reviewers(List.of(userIdentityService.getUser(company, "1", "viraj-levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(2);
        String user = userIdentityService.getUser(company, "1", "viraj-levelops");
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .committers(List.of(user))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .authors(List.of(user))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
    }

}
