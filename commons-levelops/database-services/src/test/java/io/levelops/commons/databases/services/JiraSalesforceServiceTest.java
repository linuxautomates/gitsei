package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitSalesforce;
import io.levelops.commons.databases.models.database.combined.SalesforceWithJira;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSalesforceCase;
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
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.response.AcrossUniqueKey;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraSalesforceServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraSalesforceService jiraSalesforceService;
    private static JiraIssueService jiraIssueService;
    private static SalesforceCaseService salesforceCaseService;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        salesforceCaseService = new SalesforceCaseService(dataSource);
        jiraSalesforceService = new JiraSalesforceService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), salesforceCaseService);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);

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
        userIdentityService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
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
    }

    @Test
    public void test() throws SQLException, JsonProcessingException {
        List<DbAggregationResult> caseTicketsCount = jiraSalesforceService.groupJiraTicketsByStatus(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of("1", "2", "3"),
                        false,
                        false, Map.of(), null)
                .getRecords();

        Map<String, Long> caseIssuesCount = new HashMap<>();
        caseTicketsCount.forEach(ct -> caseIssuesCount.put(ct.getKey(), ct.getTotalTickets()));

        assertThat(caseIssuesCount.get("IN PROGRESS")).isNull();

        List<DbAggregationResult> caseTicketsCountWithCommit = jiraSalesforceService.groupJiraTicketsByStatus(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true,
                        false, Map.of(), null)
                .getRecords();

        // FIXME
//        final DbAggregationResult dbAggregationResult = caseTicketsCountWithCommit.stream().findFirst().orElseThrow();
//        assertThat(dbAggregationResult.getKey()).isEqualTo("DONE");
//        assertThat(dbAggregationResult.getTotalTickets()).isEqualTo(2);

        List<DbAggregationResult> caseTicketsCountCommitCount = jiraSalesforceService.groupJiraTicketsByStatus(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of("1", "2", "3"),
                        true,
                        true, Map.of(), null)
                .getRecords();

        assertThat((int) caseTicketsCountCommitCount.stream().map(DbAggregationResult::getKey).count()).isEqualTo(0);
//        assertThat(caseTicketsCountCommitCount.stream().findFirst().orElseThrow().getTotalTickets()).isEqualTo(2); // FIXME

        List<DbAggregationResult> cases = jiraSalesforceService.groupSalesforceCasesWithJiraLinks(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(), Map.of(), null)
                .getRecords();

//        assertThat(cases.stream().findFirst().orElseThrow().getKey()).isEqualTo("CLOSED"); // FIXME
//        assertThat(caseTicketsCountCommitCount.stream().findFirst().orElseThrow().getTotalTickets()).isEqualTo(2);// FIXME

        cases = jiraSalesforceService.groupSalesforceCasesWithJiraLinks(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(), Map.of("status", SortingOrder.ASC), null)
                .getRecords();
        // FIXME
//        assertThat(cases.get(0).getKey()).isEqualTo("CLOSED");
//        assertThat(cases.get(1).getKey()).isEqualTo("ESCALATED");

        cases = jiraSalesforceService.groupSalesforceCasesWithJiraLinks(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(), Map.of("status", SortingOrder.DESC), null)
                .getRecords();
        // FIXME
//        assertThat(cases.get(0).getKey()).isEqualTo("ESCALATED");
//        assertThat(cases.get(1).getKey()).isEqualTo("CLOSED");

        List<SalesforceWithJira> caseTicketsList = jiraSalesforceService.listSalesforceCases(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(), 0, 10, Map.of(), null).getRecords();

        // FIXME
//        assertThat(
//                caseTicketsList.stream()
////                        .filter(a -> a.getCaseId().equals("5003h000003wkweAAA"))
//                        .findFirst()
//                        .orElseThrow()
//                        .getJiraIssues())
//                .isEqualTo(List.of("LEV-995", "LEV-998"));

        caseTicketsList = jiraSalesforceService.listSalesforceCases(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(), 0, 3,
                Map.of("sf_created_at", SortingOrder.ASC), null).getRecords();

        // FIXME
//        assertThat(caseTicketsList.get(0).getCaseCreatedAt()).isBefore(caseTicketsList.get(1).getCaseCreatedAt());

        caseTicketsList = jiraSalesforceService.listSalesforceCases(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(), 0, 3,
                Map.of("sf_created_at", SortingOrder.DESC), null).getRecords();

        // FIXME
//        assertThat(caseTicketsList.get(0).getCaseCreatedAt()).isAfter(caseTicketsList.get(1).getCaseCreatedAt());

        List<JiraWithGitSalesforce> jiraWithoutCommitList = jiraSalesforceService.listJiraTickets(company, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                List.of(),
                false, 0, 10, Map.of(), null).getRecords();

        Map<String, List<String>> jiraWithoutCommitListMap = new HashMap<>();
        jiraWithoutCommitList.forEach(ct -> jiraWithoutCommitListMap.put(ct.getKey(), ct.getSalesforceCases()));

        assertThat(jiraWithoutCommitListMap.size()).isEqualTo(0);
//        assertThat(jiraWithoutCommitListMap.get("LEV-995").size()).isEqualTo(1); // FIXME

        List<JiraWithGitSalesforce> jiraWithCommitList = jiraSalesforceService.listJiraTickets(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of(), true, 0, 10, Map.of(), null)
                .getRecords();

        Map<String, List<String>> jiraWithCommitListMap = new HashMap<>();
        jiraWithCommitList.forEach(ct -> jiraWithCommitListMap.put(ct.getKey(), ct.getSalesforceCases()));

        assertThat(jiraWithCommitListMap.size()).isEqualTo(0);
//        assertThat(jiraWithCommitListMap.get("LEV-997").size()).isEqualTo(1); // FIXME
//        assertThat(jiraWithCommitListMap.get("LEV-998").size()).isEqualTo(2);

        List<CommitWithJira> commits = jiraSalesforceService.listCommits(company, JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                        List.of(), 0, 10, Map.of(), null)
                .getRecords();
        assertThat(commits).hasSize(0);

        List<DbScmFile> scmFiles = jiraSalesforceService.listFiles(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 10, Map.of(), null).getRecords();

        // FIXME
//        DbScmFile firstFile = scmFiles.get(0);
//        assertThat(firstFile.getTotalChanges()).isEqualTo(338);
//        assertThat(firstFile.getNumCommits()).isEqualTo(4);
//        assertThat(firstFile.getNumCases()).isEqualTo(2);
//
//        DbScmFile secondFile = scmFiles.get(scmFiles.size() - 1);
//        assertThat(secondFile.getTotalChanges()).isEqualTo(100);
//        assertThat(secondFile.getNumCommits()).isEqualTo(1);
//        assertThat(secondFile.getNumCases()).isEqualTo(1);

        scmFiles = jiraSalesforceService.listFiles(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .module("src")
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 10, Map.of(), null).getRecords();
        assertThat(scmFiles.size()).isEqualTo(0);

        scmFiles = jiraSalesforceService.listFiles(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .module("src/utils")
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 10, Map.of(), null).getRecords();
        assertThat(scmFiles.size()).isEqualTo(0);

        DbListResponse<DbAggregationResult> filesReport = jiraSalesforceService.filesReport(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .listFiles(true)
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), Map.of(), null);
        List<DbAggregationResult> records = filesReport.getRecords();
//        assertThat(records.get(0).getKey().equalsIgnoreCase("src"));
//        assertThat(records.get(0).getTotalCases() == 6); // FIXME

        // files report with nested module
        filesReport = jiraSalesforceService.filesReport(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .module("src")
                        .listFiles(true)
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), Map.of(), null);
//        List<DbAggregationResult> expected = List.of(
//                DbAggregationResult.builder().key("assessments").repoId("levelops/ui-levelops").totalCases(2L).build(),
//                DbAggregationResult.builder().key("redux").repoId("levelops/ui-levelops").totalCases(2L).build(),
//                DbAggregationResult.builder().key("shared-resources").repoId("levelops/ui-levelops").totalCases(2L).build(),
//                DbAggregationResult.builder().key("utils").repoId("levelops/ui-levelops").totalCases(2L).build(),
//                DbAggregationResult.builder().key("workitems").repoId("levelops/ui-levelops").totalCases(1L).build()
//        );
//        verifyRecords(filesReport.getRecords(), expected, false); // FIXME

        filesReport = jiraSalesforceService.filesReport(company,
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .module("src/assessments/components/comments")
                        .listFiles(false)
                        .build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), Map.of(), null);
        assertThat(filesReport.getRecords().size()).isEqualTo(0);

        assertThat(jiraSalesforceService.resolvedTicketsTrendReport(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        false, Map.of(), null)
                .getTotalCount()).isEqualTo(0);
        assertThat(jiraSalesforceService.resolvedTicketsTrendReport(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(0);

        assertThat(jiraSalesforceService.resolvedTicketsTrendReport(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("4"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(0);

        assertThat(jiraSalesforceService.resolvedTicketsTrendReport(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder().integrationIds(List.of("4"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        false, Map.of(), null)
                .getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testEscalationReport() {
        DbListResponse<DbAggregationResult> report = jiraSalesforceService.getCaseEscalationTimeReport(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .across(SalesforceCaseFilter.DISTINCT.trend)
                        .build(), Map.of(), null);
        assertThat(report.getTotalCount()).isEqualTo(0);
        assertThat(jiraSalesforceService.listCasesWithEscalationTime(company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                0, 100, Map.of(), null).getTotalCount()).isEqualTo(0);
        assertThat(jiraSalesforceService.listCasesWithEscalationTime(company,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        SalesforceCaseFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        0, 100, Map.of(), null).getRecords()
                .stream()
                .filter(sf -> sf.getEscalationTime() == null)
                .findAny()).isEmpty();
    }

    @Test
    public void testTopCommitters() {
        DbListResponse<DbAggregationResult> topCommitters = jiraSalesforceService.getTopCommitters(
                company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                List.of("1", "2", "3"), Map.of(),
                null
        );
        assertThat(
                topCommitters.getTotalCount())
                .isEqualTo(0);

        // FIXME
//        assertThat(topCommitters.getRecords().get(0).getCount()).isEqualTo(16);
//        assertThat(topCommitters.getRecords().get(5).getCount()).isEqualTo(4);

        List<DbAggregationResult> topCommittersList = jiraSalesforceService.getTopCommitters(
                company,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .hygieneCriteriaSpecs(Map.of())
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                SalesforceCaseFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                List.of("1", "2", "3"), Map.of("no_of_commits", SortingOrder.ASC),
                null
        ).getRecords();

        // FIXME
//        assertThat(topCommittersList.get(0).getCount()).isEqualTo(4);
//        assertThat(topCommittersList.get(5).getCount()).isEqualTo(16);
    }

    private void verifyRecords(List<DbAggregationResult> a, List<DbAggregationResult> e, boolean ignoreKey) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<Object, DbAggregationResult> actualMap = convertListToMap(a, ignoreKey);
        Map<Object, DbAggregationResult> expectedMap = convertListToMap(e, ignoreKey);
        for (Object key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key), ignoreKey);
        }
    }

    private Map<Object, DbAggregationResult> convertListToMap(List<DbAggregationResult> lst, boolean ignoreKey) {
        Map<Object, DbAggregationResult> map = new HashMap<>();
        for (int i = 0; i < lst.size(); i++) {
            if (ignoreKey) {
                map.put(i, lst.get(i));
            } else {
                map.put(AcrossUniqueKey.fromDbAggregationResult(lst.get(i)), lst.get(i));
            }
        }
        return map;
    }

    private void verifyRecord(DbAggregationResult a, DbAggregationResult e, boolean ignoreKey) {
        Assert.assertEquals((e == null), (a == null));
        if (e == null) {
            return;
        }
        if (!ignoreKey) {
            Assert.assertEquals(a.getKey(), e.getKey());
        }
        Assert.assertEquals(a.getAdditionalKey(), e.getAdditionalKey());
        Assert.assertEquals(a.getMedian(), e.getMedian());
        Assert.assertEquals(a.getMin(), e.getMin());
        Assert.assertEquals(a.getMax(), e.getMax());
        Assert.assertEquals(a.getCount(), e.getCount());
        Assert.assertEquals(a.getSum(), e.getSum());
        Assert.assertEquals(a.getRepoId(), e.getRepoId());
        Assert.assertEquals(a.getTotalCases(), e.getTotalCases());
        verifyRecords(a.getStacks(), e.getStacks(), false);
    }

    @Test
    public void testInsert() throws SQLException {
        String integrationId = "1";
        long ingestedAt = currentTime.getTime();

        String key = "maxime-1";
        DbJiraIssue.DbJiraIssueBuilder builder = DbJiraIssue.builder()
                .integrationId(integrationId)
                .ingestedAt(ingestedAt)
                .key(key)
                .project("maxime")
                .issueType("bug")
                .priority("over9000")
                .reporter("rep")
                .status("todo")
                .issueCreatedAt(0L)
                .issueUpdatedAt(0L)
                // ----
                .descSize(0)
                .hops(0)
                .bounces(0)
                .numAttachments(0);

        String id = jiraIssueService.insert(company, builder
                .salesforceFields(Map.of("f1", List.of("a1", "b1")))
                .build());
        assertThat(id).isNotNull();

        BiFunction<String, String, DbJiraSalesforceCase> caseBuilder = (k, v) -> DbJiraSalesforceCase.builder()
                .issueKey(key)
                .integrationId(1)
                .fieldKey(k)
                .fieldValue(v)
                .build();

        Map<Pair<String, String>, List<DbJiraSalesforceCase>> cases = jiraIssueService.getSalesforceCaseForIssues(company, List.of(key), List.of(1));
        assertThat(cases).hasSize(1);
        assertThat(cases).containsKey(Pair.of(integrationId, key));
        assertThat(cases.get(Pair.of(integrationId, key))).containsExactly(
                caseBuilder.apply("f1", "a1"),
                caseBuilder.apply("f1", "b1"));

        jiraIssueService.insert(company, builder
                .salesforceFields(Map.of("f2", List.of("a2", "b2")))
                .build());
        cases = jiraIssueService.getSalesforceCaseForIssues(company, List.of(key), List.of(1));
        assertThat(cases).hasSize(1);
        assertThat(cases.get(Pair.of(integrationId, key))).containsExactly(
                caseBuilder.apply("f2", "a2"),
                caseBuilder.apply("f2", "b2"));

        jiraIssueService.insert(company, builder
                .salesforceFields(Map.of("f1", List.of("c1")))
                .build());
        cases = jiraIssueService.getSalesforceCaseForIssues(company, List.of(key), List.of(1));
        assertThat(cases).hasSize(1);
        assertThat(cases.get(Pair.of(integrationId, key))).containsExactly(
                caseBuilder.apply("f1", "c1")
        );

        jiraIssueService.insert(company, builder
                .salesforceFields(null)
                .build());
        cases = jiraIssueService.getSalesforceCaseForIssues(company, List.of(key), List.of(1));
        assertThat(cases).hasSize(0);
    }
}
