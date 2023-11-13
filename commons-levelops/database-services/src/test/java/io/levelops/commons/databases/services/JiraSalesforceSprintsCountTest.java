package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.combined.JiraWithGitSalesforce;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCaseHistory;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
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

import static org.assertj.core.api.Assertions.assertThat;

public class JiraSalesforceSprintsCountTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JiraSalesforceService jiraSalesforceService;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        final ObjectMapper m = DefaultObjectMapper.get();

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        ScmAggService scmAggService = new ScmAggService(dataSource, userIdentityService);
        JiraIssueService jiraIssueService = jiraTestDbs.getJiraIssueService();
        SalesforceCaseService salesforceCaseService = new SalesforceCaseService(dataSource);
        jiraSalesforceService = new JiraSalesforceService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), salesforceCaseService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());

        integrationService.insert(company, Integration.builder()
                .application("sf")
                .name("sf test")
                .status("enabled")
                .build());

        integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());

        jiraIssueService.ensureTableExistence(company);
        salesforceCaseService.ensureTableExistence(company);

        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020_sf_custom.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                List<IntegrationConfig.ConfigEntry> entries = List.of(new IntegrationConfig.ConfigEntry("case field",
                        "customfield_10042",
                        null));
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime,
                        JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
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

        final String caseInput = ResourceUtils.getResourceAsString("json/databases/salesforce_case.json");
        final String caseHistoryInput = ResourceUtils.getResourceAsString("json/databases/salesforce_case_history.json");

        List<DbSalesforceCase> cases = m.readValue(caseInput, m.getTypeFactory()
                .constructCollectionType(List.class, DbSalesforceCase.class));
        List<DbSalesforceCaseHistory> caseHistories = m.readValue(caseHistoryInput, m.getTypeFactory()
                .constructCollectionType(List.class, DbSalesforceCaseHistory.class));

        for (DbSalesforceCase sfCase : cases) {
            try {
                sfCase = sfCase.toBuilder()
                        .integrationId("2")
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build();
                salesforceCaseService.insert(company, sfCase);
                if (salesforceCaseService.get(company, sfCase.getCaseId(), sfCase.getIntegrationId(), sfCase.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("The case must exist");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        String input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "3"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), "3", null);
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
            repos.add(DbRepository.fromGithubRepository(repo, "3"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), "3");
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(company, tmp);
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "3"));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), "3",
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            if ("6b2ba5c76236894e03a73cb4d73d9d7cdca3087b".equals(tmp.getCommitSha())) {
                                tmp = tmp.toBuilder()
                                        .issueKeys(List.of("LEV-997"))
                                        .build();
                            }
                            if ("8fd86afa79c8e803c729b1b6668226a13ed28174".equals(tmp.getCommitSha())) {
                                tmp = tmp.toBuilder()
                                        .issueKeys(List.of("LEV-997", "LEV-998"))
                                        .build();
                            }
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), "3", currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });

        caseHistories.forEach(sfCaseHistory ->
                salesforceCaseService.insertCaseHistory(company, sfCaseHistory, currentTime.toInstant().getEpochSecond()));

        List<DbJiraSprint> sprints = List.of(
                DbJiraSprint.builder().name("Sprint 1").sprintId(1).state("ACTIVE").integrationId(1).startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 2").sprintId(2).state("ACTIVE").integrationId(1).startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 3").sprintId(3).state("CLOSED").integrationId(1).startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build()
        );
        sprints.forEach(s -> jiraIssueService.insertJiraSprint(company, s));

        List<DbJiraIssue> dbIssues = List.of(
                DbJiraIssue.builder().key("UN-35").integrationId("1").project("TS").summary("Remove access for user").components(List.of())
                        .labels(List.of()).versions(List.of()).fixVersions(List.of()).descSize(51).priority("MEDIUM").reporter("").status("TO DO")
                        .issueType("Task").bounces(1).hops(1).numAttachments(1).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L)
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).sprintIds(List.of(1, 2, 3)).customFields(Map.of())
                        .salesforceFields(Map.of("customfield_10042", List.of("00001006"))).build(),
                DbJiraIssue.builder().key("UN-13").integrationId("1").project("LEV").summary("Summary").components(List.of())
                        .labels(List.of()).versions(List.of()).fixVersions(List.of()).descSize(51).priority("MEDIUM").reporter("").status("DONE")
                        .issueType("Epic").bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L)
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).sprintIds(List.of(1, 2)).customFields(Map.of())
                        .salesforceFields(Map.of("customfield_10042", List.of("00001006"))).build(),
                DbJiraIssue.builder().key("UN-20").integrationId("1").project("LEV").summary("Summary").components(List.of())
                        .labels(List.of()).versions(List.of()).fixVersions(List.of()).descSize(51).priority("LOW").reporter("").status("IN PROGRESS")
                        .issueType("Epic").bounces(0).hops(0).numAttachments(0).issueCreatedAt(1613658414L).issueUpdatedAt(1613658414L)
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).sprintIds(List.of(3)).customFields(Map.of())
                        .salesforceFields(Map.of("customfield_10042", List.of("00001006"))).build()
        );
        dbIssues.forEach(issue -> {
            try {
                jiraIssueService.insert(company, issue);
            } catch (SQLException ignored) {
            }
        });
    }


    /**
     * UN-35 - {1, 2, 3}
     * UN-13 - {1, 2}
     * UN-20 - {3}
     */

    @Test
    public void testSprintCount() {
        List<DbAggregationResult> records = jiraSalesforceService.groupJiraTicketsByStatus(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .excludeSprintIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of("1", "2", "3"),
                        false,
                        false, Map.of(), null)
                .getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getTotalTickets()).isEqualTo(1);

        records = jiraSalesforceService.groupJiraTicketsByStatus(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .sprintCount(1)
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of("1", "2", "3"),
                        false,
                        false, Map.of(), null)
                .getRecords();

        assertThat(records.size()).isEqualTo(2);
        assertThat(records.get(0).getTotalTickets()).isEqualTo(1);
        assertThat(records.get(1).getTotalTickets()).isEqualTo(1);

        records = jiraSalesforceService.groupSalesforceCasesWithJiraLinks(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .sprintCount(1)
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(), Map.of(), null)
                .getRecords();

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getTotalTickets()).isEqualTo(1);

        List<JiraWithGitSalesforce> jiraTicketsRecords = jiraSalesforceService.listJiraTickets(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                List.of(),
                false, 0, 10, Map.of(), null).getRecords();

        assertThat(jiraTicketsRecords.size()).isEqualTo(3);
    }
}
