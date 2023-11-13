package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmAggServicePRTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static JiraIssueService jiraIssueService;
    private static ZendeskTicketService zendeskTicketService;
    private static ScmJiraZendeskService scmJiraZendeskService;
    private static String gitHubIntegrationId;
    private static String jiraIntegrationId;
    private static String zendeskIntegrationId;
    private static ZendeskFieldService zendeskFieldService;
    private static GitRepositoryService repositoryService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ScmIssueMgmtService scmIssueMgmtService;
    private static Date currentTime;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .build();

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        IntegrationService integrationService = jiraTestDbs.getIntegrationService();
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);
        scmIssueMgmtService = new ScmIssueMgmtService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), workItemFieldsMetaService);
        repositoryService = new GitRepositoryService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        zendeskTicketService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);

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

        repositoryService.batchUpsert(company, repos);
        dataSource.getConnection().prepareStatement("update test.scm_files set filetype = COALESCE(substring(filename from '\\.([^\\.]*)$'),'NA');")
                .execute();

        String arrayCatAgg = "DROP AGGREGATE IF EXISTS array_cat_agg(anyarray); CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();

        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }

    @Test
    public void testScmPrList() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .projects(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .partialMatch(Map.of("title", Map.of("$contains", "Added time series PD")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("title", Map.of("$ends", "a")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("title", Map.of("$contains", "ADded")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("title", Map.of("$begins", "ADded")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("title", Map.of("$regex", "Added time series PD")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .titles(List.of("Bump commons - Fixed issue with JsonUnwrapped"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludeTitles(List.of("Bump commons - Fixed issue with JsonUnwrapped"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .projects(List.of("levelops/does-not-exist"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(10);
    }

    @Test
    public void testExcludePartialMatchPRState() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("state", Map.of("$begins", "open")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("state", Map.of("$contains", "lose")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("state", Map.of("$ends", "pen")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("state", Map.of("$regex", "close")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testExcludePartialMatchPRCreator() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("creator", Map.of("$begins", "viraj")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("creator", Map.of("$contains", "levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("creator", Map.of("$ends", "ops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("creator", Map.of("$regex", "harsh")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
    }

    @Test
    public void testExcludePartialMatchPRSourceBranch() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("source_branch", Map.of("$begins", "tenable")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("source_branch", Map.of("$contains", "lev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("source_branch", Map.of("$ends", "ins")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("source_branch", Map.of("$regex", "jen")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
    }

    @Test
    public void testExcludePartialMatchPRTargetBranch() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("target_branch", Map.of("$begins", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("target_branch", Map.of("$contains", "lev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("target_branch", Map.of("$ends", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("target_branch", Map.of("$regex", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testExcludePartialMatchPRProject() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("project", Map.of("$begins", "levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("project", Map.of("$contains", "lev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("project", Map.of("$ends", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("project", Map.of("$regex", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
    }

    @Test
    public void testExcludePartialMatchPRAssignees() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("assignees", Map.of("$begins", "vir")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(8);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("assignees", Map.of("$contains", "viraj-")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(8);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("assignees", Map.of("$ends", "dev")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("assignees", Map.of("$regex", "levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(8);
    }

    @Test
    public void testExcludePartialMatchPRLabels() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("labels", Map.of("$begins", "imp")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("labels", Map.of("$contains", "important")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("labels", Map.of("$ends", "ant")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("labels", Map.of("$regex", "mpor")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(9);
    }

    @Test
    public void testExcludePartialMatchPRRepoId() throws SQLException {
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("repo_id", Map.of("$begins", "levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("repo_id", Map.of("$contains", "aggregations")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("repo_id", Map.of("$ends", "ant")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("repo_id", Map.of("$regex", "mpor")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .excludePartialMatch(Map.of("repo_id", Map.of("$regex", "mpor"),
                                        "title", Map.of("$regex", "ADd")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(10);
    }

    @Test
    public void testPrJiraMappingPrUuidColumn() throws SQLException {
        var rs = dataSource.getConnection().prepareStatement("SELECT EXISTS (SELECT 1 \n" +
                "FROM information_schema.columns \n" +
                "WHERE table_schema='test' AND table_name='scm_pullrequests_jira_mappings' AND column_name='pr_uuid');")
                .executeQuery();
        rs.next();
        Assertions.assertThat(rs.getBoolean(1)).isTrue();
    }
}
