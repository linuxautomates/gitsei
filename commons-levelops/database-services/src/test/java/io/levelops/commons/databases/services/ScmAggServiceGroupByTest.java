package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmAggServiceGroupByTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static Date currentTime;

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
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
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
        teamMembersDatabaseService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

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
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    private void validateSortedItems(DbListResponse<DbAggregationResult> response,
                                     SortingOrder sortOrder,
                                     Function<? super DbAggregationResult, ? extends String> mapper) {
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getRecords());
        if (sortOrder.equals(SortingOrder.ASC)) {
            assertThat(response.getRecords()).isSortedAccordingTo(Comparator.comparing(
                    mapper, String.CASE_INSENSITIVE_ORDER));
        } else {
            assertThat(response.getRecords()).isSortedAccordingTo(Comparator.comparing(
                    mapper, String.CASE_INSENSITIVE_ORDER).reversed());
        }
    }

    private void validateSortedKeys(DbListResponse<DbAggregationResult> response, SortingOrder sortOrder) {
        validateSortedItems(response, sortOrder, DbAggregationResult::getKey);
    }

    private void validateSortedAdditionalKeys(DbListResponse<DbAggregationResult> response, SortingOrder sortOrder) {
        validateSortedItems(response, sortOrder, DbAggregationResult::getAdditionalKey);
    }

    private void validateSortedStats(DbListResponse<DbAggregationResult> response,
                                     SortingOrder sortOrder,
                                     Function<? super DbAggregationResult, ? extends Long> mapper) {
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getRecords());
        if (sortOrder.equals(SortingOrder.ASC)) {
            assertThat(response.getRecords().stream().map(mapper).collect(Collectors.toList())).isSorted();
        } else {
            assertThat(response.getRecords().stream().map(mapper).collect(Collectors.toList()))
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    private void validateSortedMedian(DbListResponse<DbAggregationResult> response, SortingOrder sortOrder) {
        validateSortedStats(response, sortOrder, DbAggregationResult::getMedian);
    }

    private void validateSortedCount(DbListResponse<DbAggregationResult> response, SortingOrder sortOrder) {
        validateSortedStats(response, sortOrder, DbAggregationResult::getCount);
    }

    @Test
    public void testPrsCountGroupBy() throws SQLException {
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.branch)
                        .sort(Map.of("branch", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.branch)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .sort(Map.of("creator", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_reviewed)
                        .sort(Map.of("pr_reviewed", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_reviewed)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_reviewed)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .sort(Map.of("repo_id", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.repo_id)
                        .sort(Map.of("repo_id", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.label)
                        .sort(Map.of("label", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.label)
                        .sort(Map.of("label", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.assignee)
                        .sort(Map.of("assignee", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.assignee)
                        .sort(Map.of("assignee", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
    }

    @Test
    public void testPrsDurationGroupBy() throws SQLException {
        validateSortedKeys(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of("pr_created", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of("count", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedMedian(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of("count", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/aggregations-levelops"))
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .build(), null), SortingOrder.ASC);
    }

    @Test
    public void testCommitsGroupBy() {
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .sort(Map.of("committer", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .sort(Map.of("count", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .sort(Map.of("trend", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .sort(Map.of("count", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.project)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .sort(Map.of("repo_id", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .sort(Map.of("repo_id", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
    }

    @Test
    public void testIssuesGroupBy() {
        validateSortedCount(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of())
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of())
                        .build(), null), SortingOrder.ASC);
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("creator", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedAdditionalKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("creator", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("count", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("resolution_time", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("response_time", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.creator)
                        .sort(Map.of("response_time", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .sort(Map.of("issue_created", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .sort(Map.of("issue_created", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of("first_comment", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of("first_comment", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of("response_time", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedMedian(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .sort(Map.of("response_time", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.assignee)
                        .sort(Map.of("assignee", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.assignee)
                        .sort(Map.of("assignee", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.label)
                        .sort(Map.of("label", SortingOrder.ASC))
                        .build(), null), SortingOrder.ASC);
        validateSortedKeys(scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.no_response))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmIssueFilter.DISTINCT.label)
                        .sort(Map.of("label", SortingOrder.DESC))
                        .build(), null), SortingOrder.DESC);
    }

    @Test
    public void testFilesGroupBy() {
        validateSortedKeys(scmAggService.groupByAndCalculateFiles(
                company, ScmFilesFilter.builder()
                        .across(ScmFilesFilter.DISTINCT.repo_id)
                        .sort(Map.of("repo_id", SortingOrder.ASC))
                        .build()), SortingOrder.ASC);
        validateSortedCount(scmAggService.groupByAndCalculateFiles(
                company, ScmFilesFilter.builder()
                        .across(ScmFilesFilter.DISTINCT.repo_id)
                        .sort(Map.of("count", SortingOrder.DESC))
                        .build()), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateFiles(
                company, ScmFilesFilter.builder()
                        .across(ScmFilesFilter.DISTINCT.repo_id)
                        .sort(Map.of())
                        .build()), SortingOrder.DESC);
        validateSortedCount(scmAggService.groupByAndCalculateFiles(
                company, ScmFilesFilter.builder()
                        .across(ScmFilesFilter.DISTINCT.repo_id)
                        .build()), SortingOrder.DESC);
    }
}
