package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import static org.assertj.core.api.Assertions.assertThat;
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

public class ScmAggServiceIssueTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static UserIdentityService userIdentityService;

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
        List<DbRepository> repos = new ArrayList<>();
        String input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
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
    }

    @Test
    public void testIssuesFilter() throws SQLException {
        assertThat(scmAggService.list(company,
                ScmIssueFilter
                        .builder()
                        .integrationIds(List.of("1"))
                        .issueClosedRange(ImmutablePair.of(1595044152L, 1595045652L))
                        .extraCriteria(List.of())
                        .build(), Map.of(), null, 0, 10000).getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testIssuesAggInterval() throws SQLException {
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.week)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.month)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.year)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testResolutionTimeReport() throws SQLException {
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                        .across(ScmIssueFilter.DISTINCT.issue_closed)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                        .across(ScmIssueFilter.DISTINCT.issue_updated)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(3);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(8);
    }

    @Test
    public void testResponseTimeReport() throws SQLException {
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .across(ScmIssueFilter.DISTINCT.repo_id)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .across(ScmIssueFilter.DISTINCT.issue_closed)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .across(ScmIssueFilter.DISTINCT.issue_updated)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(3);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .calculation(ScmIssueFilter.CALCULATION.response_time)
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(8);
    }

    @Test
    public void testAcross() throws SQLException {
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.label)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(8);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.state)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(2);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.issue_closed)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(1);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(8);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.issue_updated)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(3);
        assertThat(scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter
                        .builder()
                        .across(ScmIssueFilter.DISTINCT.first_comment)
                        .integrationIds(List.of("1"))
                        .aggInterval(AGG_INTERVAL.day)
                        .extraCriteria(List.of())
                        .build(), null).getTotalCount()).isEqualTo(4);
    }
}
