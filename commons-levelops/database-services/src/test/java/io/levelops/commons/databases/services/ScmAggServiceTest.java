package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitZendesk;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScmAggServiceTest {

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
            .originalDisplayName("viraj-levelops")
            .build();

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
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
        jiraIntegrationId = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        zendeskIntegrationId = integrationService.insert(company, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        zendeskTicketService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, jiraIntegrationId, currentTime, JiraIssueParser.JiraParserConfig.builder()
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
        //start zendesk zone
        String input = ResourceUtils.getResourceAsString("json/databases/zendesk-tickets.json");
        PaginatedResponse<Ticket> zendeskTickets = m.readValue(input, m.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Ticket.class));
        List<DbZendeskTicket> tickets = zendeskTickets.getResponse().getRecords().stream()
                .map(ticket -> DbZendeskTicket
                        .fromTicket(ticket, zendeskIntegrationId, currentTime, Collections.emptyList(), Collections.emptyList()))
                .collect(Collectors.toList());
        List<DbZendeskTicket> zfTickets = new ArrayList<>();
        for (DbZendeskTicket ticket : tickets) {
            Date ticketUpdatedAt;
            ticketUpdatedAt = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(15));
            zfTickets.addAll(List.of(
                    ticket.toBuilder().ingestedAt(currentTime).ticketUpdatedAt(ticketUpdatedAt).build()));
        }
        zfTickets.sort(Comparator.comparing(DbZendeskTicket::getTicketUpdatedAt));
        for (DbZendeskTicket dbZendeskTicket : zfTickets) {
            try {
                final int integrationId = NumberUtils.toInt(dbZendeskTicket.getIntegrationId());
                zendeskTicketService.insert(company, dbZendeskTicket);
                if (zendeskTicketService.get(company, dbZendeskTicket.getTicketId(), integrationId,
                        dbZendeskTicket.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("The ticket must exist: " + dbZendeskTicket);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //end zendesk zone
        input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
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
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(company, tmp);
                        }
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
    public void partialMatchConditionTest() throws SQLException {
        List<DbScmFile> scmFiles = scmIssueMgmtService.listScmFiles(
                company,
                ScmFilesFilter.builder()
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(),
                null,
                0,
                100).getRecords();
        Assertions.assertThat(scmFiles.size()).isEqualTo(0);

        scmFiles = scmIssueMgmtService.listScmFiles(
                company,
                ScmFilesFilter.builder()
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of("filename", SortingOrder.ASC),
                null,
                0,
                100).getRecords();

        // FIXME
//        assertThat(scmFiles.get(0).getFilename()).isEqualTo("src/redux/reducers/restapiReducer.js");
//        assertThat(scmFiles.get(4).getFilename()).isEqualTo("src/services/restapiService.js");

        scmFiles = scmIssueMgmtService.listScmFiles(
                company,
                ScmFilesFilter.builder()
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of("filename", SortingOrder.DESC),
                null,
                0,
                100).getRecords();

        // FIXME
//        assertThat(scmFiles.get(0).getFilename()).isEqualTo("src/services/restapiService.js");
//        assertThat(scmFiles.get(4).getFilename()).isEqualTo("src/redux/reducers/restapiReducer.js");

        scmFiles = scmIssueMgmtService.listScmFiles(
                company,
                ScmFilesFilter.builder()
                        .partialMatch(Map.of("filename", Map.of("$contains", "' OR true OR filename LIKE '")))
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(),
                null,
                0,
                100).getRecords();
        Assertions.assertThat(scmFiles.size()).isEqualTo(0);

        List<DbAggregationResult> records = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("repo_id", Map.of("$contains", "aggrega")))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .build(), null).getRecords();
        Assertions.assertThat(records.size()).isEqualTo(2);
        List<String> keys = records.stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Assertions.assertThat(keys.size()).isEqualTo(2);
        List<String> additionalKeys = records.stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList());
        Assertions.assertThat(keys.size()).isEqualTo(2);
        Assertions.assertThat(additionalKeys).isEqualTo(List.of("viraj-levelops", "ivan-levelops"));

        records = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("repo_id", Map.of("$contains", "%' or true AND 'dummy' = '")))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .build(), null).getRecords();
        Assertions.assertThat(records.size()).isEqualTo(0);
    }

    @Test
    public void testCommitBranchFilter() {
        List<DbAggregationResult> records;
        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("commit_branch", Map.of("$begins", "PROP")))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(1, records.size());

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("commit_branch", Map.of("$contains", "ev")))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(1, records.size());

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .commitBranches(List.of("PROP-123", "dev"))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(2, records.size());

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .excludeCommitBranches(List.of("PROP-123", "dev"))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(0, records.size());

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .excludeCommitBranches(List.of("PROP-123"))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(1, records.size());

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .excludeCommitBranches(List.of("ABC"))
                        .across(ScmCommitFilter.DISTINCT.commit_branch)
                        .build(), null).getRecords();
        Assert.assertEquals(2, records.size());
    }

    @Test
    public void partialMatchRegexTest() {
        List<DbAggregationResult> records;
        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("project", Map.of("$regex", "^l.*_.*")))
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null).getRecords();

        for (DbAggregationResult record : records) {
            Assert.assertTrue(record.getKey().matches("^l.*_.*"));
        }

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("project", Map.of("$regex", "^doesNotContain")))
                        .across(ScmCommitFilter.DISTINCT.project)
                        .build(), null).getRecords();

        assertThat(records.size()).isEqualTo(0);

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("repo_id", Map.of("$regex", "^r")))
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .build(), null).getRecords();
        for (DbAggregationResult record : records) {
            Assert.assertTrue(record.getKey().matches("^r"));
        }

        records = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .partialMatch(Map.of("repo_id", Map.of("$regex", ".*s$")))
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .build(), null).getRecords();
        for (DbAggregationResult record : records) {
            Assert.assertTrue(record.getKey().matches(".*s$"));
        }
    }

    @Test
    public void testCollabReport() throws SQLException {
        DbListResponse<DbAggregationResult> stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .build(), null);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(2);

        stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .collabStates(List.of("unapproved"))
                .build(), null);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(stackedCollabReport.getRecords().stream().map(DbAggregationResult::getCollabState))
                .containsOnly("unapproved");

        stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .collabStates(List.of("unassigned-peer-approved"))
                .build(), null);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(stackedCollabReport.getRecords().stream().map(DbAggregationResult::getCollabState))
                .isEmpty();

        stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .collabStates(List.of("unassigned-peer-approved"))
                .creators(List.of("sample"))
                .build(), null);
        Assertions.assertThat(stackedCollabReport.getRecords()).isEmpty();

        stackedCollabReport = scmAggService.getCollaborationReport(company, ScmPrFilter.builder().build(), null, false);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(2);

        stackedCollabReport = scmAggService.getCollaborationReport(company, ScmPrFilter.builder()
                .collabStates(List.of("unapproved"))
                .build(), null, false);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(stackedCollabReport.getRecords().stream().map(DbAggregationResult::getCollabState))
                .containsOnly("unapproved");
        String ivanUserid = userIdentityService.getUserByDisplayName(company, gitHubIntegrationId, "ivan-levelops").orElse(StringUtils.EMPTY);
        stackedCollabReport = scmAggService.getCollaborationReport(company, ScmPrFilter.builder()
                .creators(List.of(ivanUserid))
                .collabStates(List.of("unapproved"))
                .build(), null, false);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(1);

        stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .creators(List.of(ivanUserid))
                .collabStates(List.of("unapproved"))
                .build(), null);
        Assertions.assertThat(stackedCollabReport.getRecords()).isNotNull();
        Assertions.assertThat(stackedCollabReport.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(stackedCollabReport.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("ivan-levelops");
        Assertions.assertThat(stackedCollabReport.getRecords()
                .stream()
                .map(DbAggregationResult::getCollabState)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("unapproved");
        Assertions.assertThat(stackedCollabReport
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NONE");
        stackedCollabReport = scmAggService.getStackedCollaborationReport(company, ScmPrFilter.builder()
                .creators(List.of(ivanUserid))
                .collabStates(List.of("unapproved"))
                .build(), null);
        DbListResponse<DbScmPullRequest> dbListResponse = scmAggService.list(company, ScmPrFilter.builder()
                .collabStates(List.of("unapproved"))
                .creators(List.of(ivanUserid))
                .build(), Map.of(), null, 0, 1000);
        Assertions.assertThat(Long.valueOf(dbListResponse.getTotalCount())).isEqualTo(stackedCollabReport.getRecords()
                .stream().map(DbAggregationResult::getCount).collect(Collectors.toList()).get(0));
    }

    @Test
    public void test() throws SQLException, JsonProcessingException {
//        Assertions.assertThat(
//                scmAggService.list(
//                        company,
//                        ScmReposFilter.builder()
//                                .integrationIds(List.of(gitHubIntegrationId))
//                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
//                                .build(),
//                        Map.of(),
//                        null,
//                        0,
//                        1000)
//                        .getTotalCount())
//                .isEqualTo(2); // FIXME flaky
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .repoIds(List.of("levelops/ui-levelops"))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        String user = userIdOf("piyushkantm");
        DbListResponse<DbScmRepoAgg> dbListResponseScmRepo = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/ui-levelops"))
                        .authors(List.of(user))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponseScmRepo.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbListResponseScmRepo.getRecords().get(0)).isEqualTo(DbScmRepoAgg.builder()
                .id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3).numPrs(0).numAdditions(219).numDeletions(177)
                .numChanges(396).numJiraIssues(1).numWorkitems(1)
                .build());
        dbListResponseScmRepo = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/ui-levelops", "levelops/aggregations-levelops"))
                        .authors(List.of(user))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponseScmRepo.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dbListResponseScmRepo.getRecords().get(0)).isEqualTo(DbScmRepoAgg.builder()
                .id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3).numPrs(0).numAdditions(219).numDeletions(177)
                .numChanges(396).numJiraIssues(1).numWorkitems(1)
                .build());
        dbListResponseScmRepo = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/ui-levelops"))
                        .authors(List.of("does_not_exist"))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponseScmRepo.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .committers(List.of(user))
                                                .includeIssues(true)
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(ScmContributorsFilter.builder()
                        .committers(List.of("piyushkantm"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build()
                        .generateCacheHash())
                .isEqualTo(ScmContributorsFilter.builder()
                        .committers(List.of("piyushkantm"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build().generateCacheHash());
        Assertions.assertThat(ScmContributorsFilter.builder()
                        .committers(List.of("piyushkantm"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build()
                        .generateCacheHash())
                .isNotEqualTo(ScmContributorsFilter.builder()
                        .committers(List.of("puoiaoisd"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build().generateCacheHash());

        dbListResponseScmRepo = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);

        List<DbScmRepoAgg> expected = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3).numAdditions(219).numDeletions(177)
                        .numPrs(0).numJiraIssues(1).numWorkitems(1).numChanges(396).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numCommits(0)
                        .numAdditions(0).numDeletions(0).numPrs(10).numJiraIssues(1).numWorkitems(1).numChanges(0).build()
        );
//        Assertions.assertThat(dbListResponseScmRepo.getRecords()).containsExactlyInAnyOrder(expected.toArray(new DbScmRepoAgg[0]));// FIXME flaky

        dbListResponseScmRepo = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of("repo", SortingOrder.ASC),
                null,
                0,
                1000);

        expected = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3)
                        .numAdditions(219).numDeletions(177).numPrs(0).numJiraIssues(1).numWorkitems(1).numChanges(396).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops")
                        .numAdditions(0).numDeletions(0).numCommits(0).numPrs(10).numJiraIssues(1).numWorkitems(1).numChanges(0).build()
        );
//        Assertions.assertThat(dbListResponseScmRepo.getRecords()).containsExactlyInAnyOrder(expected.toArray(new DbScmRepoAgg[0])); // FIXME flaky

        DbListResponse<String> allRepoNames = scmAggService.listAllRepoNames(company, ScmReposFilter.builder()
                .integrationIds(List.of(gitHubIntegrationId))
                .build(), Map.of(), null, 0, 10);
        // Assertions.assertThat(allRepoNames.getRecords().size()).isEqualTo(2);  // FIXME


        DbListResponse<DbScmContributorAgg> dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .authors(List.of(user))
                        .includeIssues(true)
                        .committers(List.of(user))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        compareAggResults(dbListResponse.getRecords(), List.of(DbScmContributorAgg.builder()
                .id(userIdOf("piyushkantm")).name("piyushkantm").fileTypes(List.of("js", "jsx")).repoBreadth(List.of("levelops/ui-levelops"))
                .techBreadth(List.of("Javascript/Typescript")).numRepos(1).numCommits(3).numAdditions(219).numDeletions(177).numPrs(0).numChanges(396).numJiraIssues(1).numWorkitems(1)
                .build()));

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .authors(List.of("does_not_exist"))
                        .committers(List.of("piyushkantm"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of(user))
                        .committers(List.of(user))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);
        compareAggResults(dbListResponse.getRecords(), List.of(DbScmContributorAgg.builder()
                .id(userIdOf("piyushkantm"))
                .name("piyushkantm")
                .fileTypes(List.of("jsx", "js"))
                .techBreadth(List.of("Javascript/Typescript"))
                .repoBreadth(List.of("levelops/ui-levelops"))
                .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()));

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of("does_not_exist"))
                        .includeIssues(true)
                        .committers(List.of("piyushkantm"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of("committer", SortingOrder.DESC),
                null,
                0,
                1000);

        List<DbScmContributorAgg> expectedContributorList = List.of(
                DbScmContributorAgg.builder()
                        .id(userIdOf("viraj-levelops"))
                        .name("viraj-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(9).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(0).numWorkitems(0).build(),
                DbScmContributorAgg.builder()
                        .id(userIdOf("piyushkantm"))
                        .name("piyushkantm")
                        .fileTypes(List.of("jsx", "js"))
                        .techBreadth(List.of("Javascript/Typescript"))
                        .repoBreadth(List.of("levelops/ui-levelops"))
                        .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build(),
                DbScmContributorAgg.builder()
                        .id(userIdOf("ivan-levelops"))
                        .name("ivan-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(1).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(1).numWorkitems(1).build()
        );
        DefaultObjectMapper.prettyPrint(dbListResponse);
//        compareAggResults(dbListResponse.getRecords(), expectedContributorList); // FIXME flaky

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of("committer", SortingOrder.ASC),
                null,
                0,
                1000);

        expectedContributorList = List.of(
                DbScmContributorAgg.builder()
                        .id(userIdOf("viraj-levelops"))
                        .name("viraj-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(9).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(0).numWorkitems(0).build(),
                DbScmContributorAgg.builder()
                        .id(userIdOf("piyushkantm"))
                        .name("piyushkantm")
                        .fileTypes(List.of("jsx", "js"))
                        .techBreadth(List.of("Javascript/Typescript"))
                        .repoBreadth(List.of("levelops/ui-levelops"))
                        .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build(),
                DbScmContributorAgg.builder()
                        .id(userIdOf("ivan-levelops"))
                        .name("ivan-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(1).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(1).numWorkitems(1).build()
        );

        // FIXME
        /*
        DefaultObjectMapper.prettyPrint(dbListResponse.getRecords());
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$contains", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder()
                        .id(userIdOf("viraj-levelops"))
                        .name("viraj-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(9).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(0).numWorkitems(0).build(),
                DbScmContributorAgg.builder()
                        .id(userIdOf("ivan-levelops"))
                        .name("ivan-levelops")
                        .fileTypes(List.of())
                        .techBreadth(List.of())
                        .repoBreadth(List.of())
                        .numRepos(0).numCommits(0).numPrs(1).numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(1).numWorkitems(1).build()
                );
        DefaultObjectMapper.prettyPrint(dbListResponse.getRecords());
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$begins", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        // FIXME
//        expectedContributorList = List.of();
//                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "jsx", "tsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
//                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
//                        .techBreadth(List.of()).numRepos(0).numAdditions(0).numDeletions(0).numCommits(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of())
//                        .techBreadth(List.of()).numRepos(0).numAdditions(0).numDeletions(0).numCommits(0).numPrs(15).numChanges(0).numJiraIssues(0).numWorkitems(0).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of())
//                        .techBreadth(List.of()).numRepos(0).numAdditions(0).numDeletions(0).numCommits(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build()
//        );
//        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
//        expectedContributorList = List.of(
//                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "jsx", "tsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
//                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of("levelops/devops-levelops", "levelops/api-levelops", "levelops/integrations-levelops", "levelops/ingestion-levelops"))
//                        .techBreadth(List.of()).numRepos(4).numCommits(3).numAdditions(6).numDeletions(6).numPrs(15).numChanges(3).numJiraIssues(0).numWorkitems(0).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of())
//                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build(),
//                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
//                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build()
//        );
//        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        // // FIXME

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$contains", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "jsx", "tsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("web-flow")).name("web-flow").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numCommits(6).numAdditions(2614).numDeletions(520).numPrs(0).numChanges(3134).numJiraIssues(4).numWorkitems(34).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(15).numChanges(0).numJiraIssues(0).numWorkitems(0).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build()
        );
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .includeIssues(true)
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$begins", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "jsx", "tsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("web-flow")).name("web-flow").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numCommits(6).numAdditions(2614).numDeletions(520).numPrs(0).numChanges(3134).numJiraIssues(4).numWorkitems(34).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(15).numChanges(0).numJiraIssues(0).numWorkitems(0).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build()

        );
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .includeIssues(true)
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("web-flow")).name("web-flow").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numCommits(6).numAdditions(2614).numDeletions(520).numPrs(0).numChanges(3134).numJiraIssues(4).numWorkitems(34).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of("levelops/devops-levelops", "levelops/api-levelops", "levelops/integrations-levelops", "levelops/ingestion-levelops"))
                        .techBreadth(List.of()).numRepos(4).numCommits(3).numAdditions(6).numDeletions(6).numPrs(15).numChanges(3).numJiraIssues(0).numWorkitems(0).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build()
        );
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .includeIssues(true)
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_repos", SortingOrder.ASC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of()).techBreadth(List.of())
                        .numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("js", "svg", "jsx", "tsx", "json", "scss", "ts")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("web-flow")).name("web-flow").fileTypes(List.of("js", "svg", "jsx", "tsx", "json", "scss", "ts")).repoBreadth(List.of())
                        .techBreadth(List.of("Javascript/Typescript")).numRepos(1).numCommits(6).numAdditions(2614).numDeletions(520).numPrs(0).numChanges(3134).numJiraIssues(4).numWorkitems(34).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of()).techBreadth(List.of())
                        .numRepos(4).numCommits(3).numAdditions(6).numDeletions(6).numPrs(15).numChanges(3).numJiraIssues(0).numWorkitems(0).repos(null).build()
        );
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("meghana-levelops", "web-flow", "viraj-levelops", "ivan-levelops", "harsh-levelops");
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getNumRepos).collect(Collectors.toList()))
                .isSorted();
        assertThat(dbListResponse.getRecords().size()).isEqualTo(expectedContributorList.size());

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of("levelops/devops-levelops", "levelops/api-levelops", "levelops/integrations-levelops", "levelops/ingestion-levelops"))
                        .techBreadth(List.of()).numRepos(4).numCommits(3).numAdditions(6).numDeletions(6).numPrs(15).numChanges(3).numJiraIssues(0).numWorkitems(0).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("web-flow")).name("web-flow").fileTypes(List.of("scss", "tsx", "jsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numCommits(6).numAdditions(2614).numDeletions(520).numPrs(0).numChanges(3134).numJiraIssues(4).numWorkitems(34).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of()).techBreadth(List.of())
                        .numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build()
                );
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_commits", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedContributorList = List.of(
                DbScmContributorAgg.builder().id(userIdOf("meghana-levelops")).name("meghana-levelops").fileTypes(List.of("scss", "jsx", "tsx", "svg", "ts", "js", "json")).repoBreadth(List.of("levelops/ui-levelops"))
                        .techBreadth(List.of("HTML/CSS", "JSON", "Javascript/Typescript")).numRepos(1).numAdditions(9480).numDeletions(3639).numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("viraj-levelops")).name("viraj-levelops").fileTypes(List.of()).repoBreadth(List.of("levelops/devops-levelops", "levelops/api-levelops", "levelops/integrations-levelops", "levelops/ingestion-levelops"))
                        .techBreadth(List.of()).numRepos(4).numCommits(3).numAdditions(6).numDeletions(6).numPrs(15).numChanges(3).numJiraIssues(0).numWorkitems(0).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("ivan-levelops")).name("ivan-levelops").fileTypes(List.of()).repoBreadth(List.of())
                        .techBreadth(List.of()).numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(2).numChanges(0).numJiraIssues(1).numWorkitems(1).repos(null).build(),
                DbScmContributorAgg.builder().id(userIdOf("harsh-levelops")).name("harsh-levelops").fileTypes(List.of()).repoBreadth(List.of()).techBreadth(List.of())
                        .numRepos(0).numCommits(0).numAdditions(0).numDeletions(0).numPrs(7).numChanges(0).numJiraIssues(1).numWorkitems(3).repos(null).build()

        );
        compareAggResults(dbListResponse.getRecords(), expectedContributorList);

        DbListResponse<DbScmRepoAgg> dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$contains", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        List<DbScmRepoAgg> expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(9480).numDeletions(3639)
                        .numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assert.assertEquals(expectedReposList, dbScmRepoAggDbListResponse.getRecords());

        dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$begins", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(9480).numDeletions(3639)
                        .numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assert.assertEquals(expectedReposList, dbScmRepoAggDbListResponse.getRecords());

        dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(9480).numDeletions(3639)
                        .numCommits(113).numPrs(0).numChanges(13119).numJiraIssues(8).numWorkitems(29).build(),
                DbScmRepoAgg.builder().id("levelops/devops-levelops").name("levelops/devops-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/integrations-levelops").name("levelops/integrations-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/ingestion-levelops").name("levelops/ingestion-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/api-levelops").name("levelops/api-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assertions.assertThat(dbScmRepoAggDbListResponse.getRecords()).containsExactlyInAnyOrder(expectedReposList.toArray(new DbScmRepoAgg[0]));

        dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$contains", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(12094).numDeletions(4159)
                        .numCommits(119).numPrs(0).numChanges(16253).numJiraIssues(8).numWorkitems(37).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assert.assertEquals(expectedReposList, dbScmRepoAggDbListResponse.getRecords());

        dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$begins", "meghana-")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(12094).numDeletions(4159)
                        .numCommits(119).numPrs(0).numChanges(16253).numJiraIssues(8).numWorkitems(37).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assert.assertEquals(expectedReposList, dbScmRepoAggDbListResponse.getRecords());

        dbScmRepoAggDbListResponse = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_changes", SortingOrder.DESC),
                null,
                0,
                1000);
        expectedReposList = List.of(
                DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numAdditions(12094).numDeletions(4159)
                        .numCommits(119).numPrs(0).numChanges(16253).numJiraIssues(8).numWorkitems(37).build(),
                DbScmRepoAgg.builder().id("levelops/devops-levelops").name("levelops/devops-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/integrations-levelops").name("levelops/integrations-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/ingestion-levelops").name("levelops/ingestion-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/api-levelops").name("levelops/api-levelops").numAdditions(2).numDeletions(2)
                        .numCommits(1).numPrs(0).numChanges(1).numJiraIssues(0).numWorkitems(0).build(),
                DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numAdditions(0).numDeletions(0)
                        .numCommits(0).numPrs(24).numChanges(0).numJiraIssues(2).numWorkitems(4).build()
        );
        Assertions.assertThat(dbScmRepoAggDbListResponse.getRecords()).containsExactlyInAnyOrder(expectedReposList.toArray(new DbScmRepoAgg[0]));
*/

        Assertions.assertThat(scmJiraZendeskService.listZendeskTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                0,
                1000, Map.of(), null).getTotalCount()).isEqualTo(2);

        List<ZendeskWithJira> zendeskTickets = scmJiraZendeskService.listZendeskTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                0,
                1000, Map.of("status", SortingOrder.ASC), null).getRecords();

        assertThat(zendeskTickets.get(0).getStatus()).isEqualTo("NEW");
        assertThat(zendeskTickets.get(0).getStatus()).isEqualTo("NEW");

        Assertions.assertThat(scmJiraZendeskService.listJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                false,
                0,
                1000, Map.of(), null).getTotalCount()).isEqualTo(2);
        Assertions.assertThat(scmJiraZendeskService.listJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                true,
                0,
                1000, Map.of(), null).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(scmJiraZendeskService.listCommits(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                0,
                1000, Map.of(), null).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(scmJiraZendeskService.groupJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                true,
                false, Map.of(), null).getTotalCount()).isEqualTo(0);

        Assertions.assertThat(scmJiraZendeskService.groupJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                false,
                false, Map.of(), null).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmJiraZendeskService.groupJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                false,
                true, Map.of(), null).getTotalCount()).isEqualTo(0);

        /*
        Assertions.assertThat(
                scmIssueMgmtService.listScmFiles(
                        company,
                        ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                        JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                                .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        Map.of(),
                        null,
                        0,
                        100)
                        .getTotalCount())
                .isEqualTo(27);

        List<DbScmFile> escalatedFiles = scmJiraZendeskService.listEscalatedFiles(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                0,
                100, Map.of(), null).getRecords();

        Assertions.assertThat(escalatedFiles.size()).isEqualTo(27);
        Assertions.assertThat(escalatedFiles.get(0).getNumCommits()).isEqualTo(1);
        Assertions.assertThat(escalatedFiles.get(0).getNumTickets()).isEqualTo(2);
        Assertions.assertThat(escalatedFiles.get(0).getZendeskTicketIds()).isEqualTo(List.of(gitHubIntegrationId, zendeskIntegrationId));
        Assertions.assertThat(escalatedFiles.get(escalatedFiles.size() - 1).getNumCommits()).isEqualTo(1);
        // TODO fix test - unreliable
//        Assertions.assertThat(escalatedFiles.get(escalatedFiles.size() - 1).getNumTickets()).isEqualTo(2);
//        Assertions.assertThat(escalatedFiles.get(escalatedFiles.size() - 1).getZendeskTicketIds()).isEqualTo(List.of(gitHubIntegrationId, zendeskIntegrationId));

        escalatedFiles = scmJiraZendeskService.listEscalatedFiles(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).module("wiremock").build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                0,
                100, Map.of(), null).getRecords();

        Assertions.assertThat(escalatedFiles.size()).isEqualTo(2);

        List<DbAggregationResult> fileReportRecods = scmJiraZendeskService.getZendeskEscalationFileReport(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).listFiles(true).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(), Map.of(), null).getRecords();

        Assertions.assertThat(fileReportRecods.get(0).getKey()).isEqualTo("src");
        Assertions.assertThat(fileReportRecods.get(0).getTotalTickets()).isEqualTo(2);

        fileReportRecods = scmJiraZendeskService.getZendeskEscalationFileReport(
                company,
                ScmFilesFilter.builder().module("src/services").integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).listFiles(true).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(), Map.of(), null).getRecords();

        Assertions.assertThat(fileReportRecods.get(0).getKey()).isEqualTo("restapi");
        Assertions.assertThat(fileReportRecods.get(0).getTotalTickets()).isEqualTo(2);

        Assertions.assertThat(fileReportRecods.get(1).getKey()).isEqualTo("restapiService.js");
        Assertions.assertThat(fileReportRecods.get(1).getTotalTickets()).isEqualTo(2);

        fileReportRecods = scmJiraZendeskService.getZendeskEscalationFileReport(
                company,
                ScmFilesFilter.builder().module("src/services").integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).listFiles(false).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(), Map.of(), null).getRecords();
        Assertions.assertThat(fileReportRecods.get(0).getKey()).isEqualTo("restapi");
        Assertions.assertThat(fileReportRecods.get(0).getTotalTickets()).isEqualTo(2);

        System.out.println("OUTPUT: " + m.writeValueAsString(scmIssueMgmtService.listScmFiles(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(),
                null,
                0,
                100)));
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
            Assertions.assertThat(ScmPrFilter.builder()
                    .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                    .integrationIds(List.of(gitHubIntegrationId))
                    .labels(List.of("asd"))
                    .build()
                    .generateCacheHash())
                    .isEqualTo(ScmPrFilter.builder()
                            .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                            .integrationIds(List.of(gitHubIntegrationId))
                            .labels(List.of("asd"))
                            .build()
                            .generateCacheHash());
            Assertions.assertThat(ScmPrFilter.builder()
                    .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                    .integrationIds(List.of(gitHubIntegrationId))
                    .labels(List.of("asd"))
                    .build()
                    .generateCacheHash())
                    .isNotEqualTo(ScmPrFilter.builder()
                            .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                            .integrationIds(List.of(gitHubIntegrationId))
                            .labels(List.of("bsd"))
                            .build()
                            .generateCacheHash());
        }
        for (ScmPrFilter.DISTINCT a : List.of(ScmPrFilter.DISTINCT.values())) {
            System.out.println("a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculatePrs(
                                    company, ScmPrFilter.builder().across(a).build(), null)));
        }
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(2);

        List<DbAggregationResult> records = scmIssueMgmtService.listScmModules(
                company,
                ScmFilesFilter.builder()
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .listFiles(true)
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(), null).getRecords();
        Assertions.assertThat(records.get(0).getKey().equalsIgnoreCase("src"));
        Assertions.assertThat(records.get(0).getTotalIssues() == 1);
        Assertions.assertThat(records.get(0).getRepoId().equalsIgnoreCase("levelops/ui-levelops"));

        records = scmIssueMgmtService.listScmModules(
                company,
                ScmFilesFilter.builder()
                        .module("src")
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .listFiles(true)
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(), null).getRecords();
        Assertions.assertThat(records.size()).isEqualTo(2);
        Assertions.assertThat(records.get(0).getKey()).isEqualTo("redux");
        Assertions.assertThat(records.get(0).getRepoId()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(records.get(0).getTotalIssues()).isEqualTo(1);
        Assertions.assertThat(records.get(1).getKey()).isEqualTo("services");
        Assertions.assertThat(records.get(1).getRepoId()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(records.get(1).getTotalIssues()).isEqualTo(1);
        records = scmIssueMgmtService.listScmModules(
                company,
                ScmFilesFilter.builder()
                        .module("src/services")
                        .partialMatch(Map.of("filename", Map.of("$contains", "restapi")))
                        .listFiles(false)
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                Map.of(), null).getRecords();

        Assertions.assertThat(records.get(0).getKey()).isEqualTo("restapi");
        Assertions.assertThat(records.get(0).getRepoId()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(records.get(0).getTotalIssues()).isEqualTo(1);

        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .prCreatedRange(ImmutablePair.of(0L, 1L))
                                .build(), null).getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                                .partialMatch(Map.of("repo_id", Map.of("$contains", "aggrega")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "notcorrect")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "notcorrect")))
                                .across(ScmPrFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "notcorrect")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("assignees", Map.of("$contains", "raj")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("assignees", Map.of("$contains", "van")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "viraj")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "doesnotexist")))
                                .across(ScmPrFilter.DISTINCT.creator)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "viraj")))
                                .across(ScmPrFilter.DISTINCT.pr_created)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "doesnotexist")))
                                .across(ScmPrFilter.DISTINCT.pr_created)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "vir")))
                                .across(ScmPrFilter.DISTINCT.pr_updated)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "doesnotexist")))
                                .across(ScmPrFilter.DISTINCT.pr_updated)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("approvers", Map.of("$contains", "ivan")))
                                .across(ScmPrFilter.DISTINCT.pr_reviewed)
                                .build(), null).getTotalCount())
                .isEqualTo(6);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "doesnotexist")))
                                .across(ScmPrFilter.DISTINCT.pr_reviewed)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "vir")))
                                .across(ScmPrFilter.DISTINCT.pr_merged)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("reviewer", Map.of("$contains", "doesnotexist")))
                                .across(ScmPrFilter.DISTINCT.pr_merged)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("repo_id", Map.of("$contains", "evelops/ui-levelop")))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .build(), null).getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("levelops/ui-levelops"))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .build(), null).getTotalCount()).isGreaterThan(0);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("repo_id", Map.of("$contains", "bbbbb")))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .build(), null).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .repoIds(List.of("aaa"))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .build(), null).getTotalCount()).isEqualTo(0);

        for (ScmCommitFilter.DISTINCT a : List.of(ScmCommitFilter.DISTINCT.values())) {
            System.out.println("a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateCommits(
                                    company, ScmCommitFilter.builder()
                                            .integrationIds(List.of(gitHubIntegrationId))
                                            .partialMatch(Map.of("repo_id", Map.of("$contains", "aggrega")))
                                            .across(a)
                                            .build(), null)));
            Assertions.assertThat(ScmCommitFilter.builder()
                    .integrationIds(List.of(gitHubIntegrationId))
                    .across(a)
                    .build().generateCacheHash())
                    .isEqualTo(ScmCommitFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(a)
                            .build().generateCacheHash());
            Assertions.assertThat(ScmCommitFilter.builder()
                    .integrationIds(List.of(gitHubIntegrationId))
                    .across(a)
                    .build().generateCacheHash())
                    .isNotEqualTo(ScmCommitFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(a)
                            .calculation(ScmCommitFilter.CALCULATION.count)
                            .build().generateCacheHash());
        }

        for (ScmFilesFilter.DISTINCT a : List.of(ScmFilesFilter.DISTINCT.values())) {
            System.out.println("files a: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateFiles(
                                    company, ScmFilesFilter.builder()
                                            .integrationIds(List.of(gitHubIntegrationId))
                                            .across(a)
                                            .build())));
            Assertions.assertThat(ScmFilesFilter.builder()
                    .integrationIds(List.of(gitHubIntegrationId))
                    .across(a)
                    .build().generateCacheHash())
                    .isEqualTo(ScmFilesFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(a)
                            .build().generateCacheHash());
            Assertions.assertThat(ScmFilesFilter.builder()
                    .integrationIds(List.of(gitHubIntegrationId))
                    .across(a)
                    .build().generateCacheHash())
                    .isNotEqualTo(ScmFilesFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(a)
                            .calculation(ScmFilesFilter.CALCULATION.count)
                            .build().generateCacheHash());
        }

        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ui-levelops"))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ui-levelops"))
                                .partialMatch(Map.of("repo_id", Map.of("$contains", "ui-levelops")))
                                .across(ScmCommitFilter.DISTINCT.trend)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .partialMatch(Map.of("repo_id", Map.of("$contains", "ui-levelops")))
                                .across(ScmCommitFilter.DISTINCT.trend)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("repo_id", Map.of("$contains", "ui-levelops")))
                                .across(ScmCommitFilter.DISTINCT.author)
                                .build(), null).getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .committers(List.of(user))
                                .partialMatch(Map.of("repo_id", Map.of("$contains", "ui-levelops")))
                                .across(ScmCommitFilter.DISTINCT.committer)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .committers(List.of(user))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "levelops")))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ui-levelops"))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "notcorrect")))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .committers(List.of(user))
                                .partialMatch(Map.of("repo_id", Map.of("$begins", "notcorrect")))
                                .across(ScmCommitFilter.DISTINCT.committer)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("author", Map.of("$contains", "piyushkantm")))
                                .across(ScmCommitFilter.DISTINCT.author)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("author", Map.of("$contains", "doesnotexist")))
                                .across(ScmCommitFilter.DISTINCT.author)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("message", Map.of("$begins", "LEV-889")))
                                .across(ScmCommitFilter.DISTINCT.author)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .partialMatch(Map.of("author", Map.of("$begins", "doesnotexist")))
                                .across(ScmCommitFilter.DISTINCT.author)
                                .build(), null).getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder().build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(420);
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder()
                        .repoIds(List.of("levelops/ui-levelops"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .commitStartTime(currentTime.toInstant().getEpochSecond() - 86400)
                        .commitEndTime(currentTime.toInstant().getEpochSecond() + 86400)
                        .build(), Map.of(), 0, 1000)
                .getTotalCount())
                .isEqualTo(420);

        List<DbAggregationResult> scmRecords =
                scmAggService.listModules(company, ScmFilesFilter.builder().listFiles(true).commitStartTime(0L).build(), Map.of()).getRecords();
        Assertions.assertThat(scmRecords.get(0).getKey().equalsIgnoreCase("src"));
        Assertions.assertThat(scmRecords.get(0).getCount() == 1176);
        Assertions.assertThat(scmRecords.get(1).getKey().equalsIgnoreCase("wiremock"));
        Assertions.assertThat(scmRecords.get(1).getCount() == 244);
        Assertions.assertThat(scmRecords.get(2).getKey().equalsIgnoreCase("assets"));
        Assertions.assertThat(scmRecords.get(2).getCount() == 2);

        scmRecords = scmAggService.listModules(company,
                ScmFilesFilter.builder().module("wiremock").listFiles(true).commitStartTime(0L).build(), Map.of()).getRecords();
        Assertions.assertThat(scmRecords.get(0).getKey().equalsIgnoreCase("mappings_old"));
        Assertions.assertThat(scmRecords.get(0).getCount() == 193);
        Assertions.assertThat(scmRecords.get(1).getKey().equalsIgnoreCase("mappings"));
        Assertions.assertThat(scmRecords.get(1).getCount() == 51);

        scmRecords = scmAggService.listModules(company,
                ScmFilesFilter.builder().module("wiremock/mappings").commitStartTime(0L).commitEndTime(3000000000L).listFiles(false).build(), Map.of()).getRecords();
        Assertions.assertThat(scmRecords.size() == 0);

        scmRecords = scmAggService.listModules(company,
                ScmFilesFilter.builder().module("wiremock/mappings").commitStartTime(0L).commitEndTime(3000000000L).listFiles(true).build(), Map.of()).getRecords();
        Assertions.assertThat(scmRecords.size() == 47);

        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder()
                        .partialMatch(Map.of("repo_id", Map.of("$ends", "ui-levelops"),
                                "filename", Map.of("$contains", "dashboard")))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .commitStartTime(currentTime.toInstant().getEpochSecond() - 86400)
                        .commitEndTime(currentTime.toInstant().getEpochSecond() + 86400)
                        .build(), Map.of(), 0, 1000).getTotalCount()).isEqualTo(43);

        for (ScmIssueFilter.DISTINCT a : List.of(ScmIssueFilter.DISTINCT.values())) {
            for (ScmIssueFilter.EXTRA_CRITERIA criterion : List.of(ScmIssueFilter.EXTRA_CRITERIA.values())) {
                System.out.println("issues DATA: " + a + " data: " +
                        m.writeValueAsString(
                                scmAggService.groupByAndCalculateIssues(
                                        company, ScmIssueFilter.builder()
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .extraCriteria(List.of(criterion))
                                                .across(a)
                                                .build(), null)));
                System.out.println("issues_resp DATA: " + a + " data: " +
                        m.writeValueAsString(
                                scmAggService.groupByAndCalculateIssues(
                                        company, ScmIssueFilter.builder()
                                                .calculation(ScmIssueFilter.CALCULATION.response_time)
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .extraCriteria(List.of(criterion))
                                                .across(a)
                                                .build(), null)));
                Assertions.assertThat(ScmIssueFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .extraCriteria(List.of(criterion))
                        .across(a)
                        .build().generateCacheHash())
                        .isEqualTo(ScmIssueFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .extraCriteria(List.of(criterion))
                                .across(a)
                                .build().generateCacheHash());

            }
            Assertions.assertThat(ScmIssueFilter.builder()
                    .calculation(ScmIssueFilter.CALCULATION.response_time)
                    .integrationIds(List.of(gitHubIntegrationId))
                    .extraCriteria(List.of())
                    .across(a)
                    .build().generateCacheHash())
                    .isNotEqualTo(ScmIssueFilter.builder()
                            .calculation(ScmIssueFilter.CALCULATION.count)
                            .integrationIds(List.of(gitHubIntegrationId))
                            .extraCriteria(List.of())
                            .across(a)
                            .build().generateCacheHash());
            System.out.println("issues DATA: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateIssues(
                                    company, ScmIssueFilter.builder()
                                            .extraCriteria(List.of())
                                            .integrationIds(List.of(gitHubIntegrationId))
                                            .across(a)
                                            .build(), null)));
            System.out.println("issues_resp DATA: " + a + " data: " +
                    m.writeValueAsString(
                            scmAggService.groupByAndCalculateIssues(
                                    company, ScmIssueFilter.builder()
                                            .calculation(ScmIssueFilter.CALCULATION.response_time)
                                            .integrationIds(List.of(gitHubIntegrationId))
                                            .extraCriteria(List.of())
                                            .across(a)
                                            .build(), null)));
        }

        Assertions.assertThat(scmAggService.list(
                company,
                ScmIssueFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("creator", Map.of("$begins", "jeff")))
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getTotalCount()).isEqualTo(2);

        System.out.println(m.writeValueAsString(scmAggService.list(
                company,
                ScmIssueFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
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
                                .integrationIds(List.of(gitHubIntegrationId))
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
                                .integrationIds(List.of(gitHubIntegrationId))
                                .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                                .issueCreatedRange(ImmutablePair.of(0L, 1L))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmIssueFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
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
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "maxime-levelops")))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(2);

        Assertions.assertThat(scmAggService.list(
                company,
                ScmPrFilter.builder()
                        .partialMatch(Map.of("source_branch", Map.of("$contains", "LEV")))
                        .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "maxime-levelops")))
                        .build(),
                Map.of(),
                null,
                0,
                10).getTotalCount()).isEqualTo(1);

        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .reviewers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "maxime-levelops")))
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
                                .repoIds(List.of("levelops/aggregations-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(24);
        String user2 = userIdOf("meghana-levelops");
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .committers(List.of(user2))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(113);

        Assertions.assertThat(scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .partialMatch(Map.of("message", Map.of("$ends", "mappings")))
                        .committers(List.of(user2))
                        .build(),
                Map.of(),
                null,
                0,
                10).getTotalCount()).isEqualTo(2);

        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .authors(List.of(user2))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(119);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .authors(List.of(user2))
                                .committedAtRange(ImmutablePair.of(0L, 1L))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);

        long now = Instant.now().getEpochSecond();
        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339289").reviewer("viraj-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339290").reviewer("viraj1-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmPullRequest pr = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).number("164").integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .creator("viraj-levelops").mergeSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(testScmUser)
                .title("LEV-1983").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        String prId = scmAggService.insert(company, pr);
        Assert.assertNotNull(prId);
        DbScmPullRequest prRead = scmAggService.getPr(company, "164", List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"), gitHubIntegrationId).get();
        validate(pr, prRead);
        List<DbScmReview> prReviews = scmAggService.getPrReviews(company, prRead.getId());
        Assertions.assertThat(prReviews.size()).isEqualTo(2);

        now = Instant.now().getEpochSecond();
        DbScmCommit commit = DbScmCommit.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.GIT)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit);
        DbScmCommit commitRead = scmAggService.getCommit(company, "ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2",
                List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"), gitHubIntegrationId).get();
        Assert.assertEquals(VCS_TYPE.GIT, commitRead.getVcsType());
        validateCommit(commit, commitRead);

        now = Instant.now().getEpochSecond();
        DbScmCommit commit1 = DbScmCommit.builder()
                .repoIds(List.of("levelops/devops-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/devops-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.TFVC)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit1);
        DbScmCommit commitRead1 = scmAggService.getCommit(company, "ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2",
                "levelops/devops-levelops", gitHubIntegrationId).get();
        Assert.assertEquals(VCS_TYPE.TFVC, commitRead1.getVcsType());
        validateCommit(commit1, commitRead1);

        now = Instant.now().getEpochSecond();
        DbScmCommit commit2 = DbScmCommit.builder()
                .repoIds(List.of("levelops/api-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("80091a3200b99700b55208a225e842e6f1e4c1af")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.PERFORCE)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit2);
        DbScmCommit commitRead2 = scmAggService.getCommit(company, "80091a3200b99700b55208a225e842e6f1e4c1af",
                List.of("levelops/api-levelops"), gitHubIntegrationId).get();
        Assert.assertEquals(VCS_TYPE.PERFORCE, commitRead2.getVcsType());
        validateCommit(commit2, commitRead2);

        List<DbScmCommit> scmCommits = scmAggService.getCommits(company, List.of(commitRead.getCommitSha(), commitRead1.getCommitSha(), commitRead2.getCommitSha()), gitHubIntegrationId);
        Assert.assertEquals(3, scmCommits.size());
        validateCommits(List.of(commitRead, commitRead1, commitRead2), scmCommits);

        //Searching by invalid commit shas
        scmCommits = scmAggService.getCommits(company, List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()), gitHubIntegrationId);
        Assert.assertTrue(CollectionUtils.isEmpty(scmCommits));

        scmCommits = scmAggService.getCommits(company, List.of(commitRead.getCommitSha(), commitRead1.getCommitSha()), String.valueOf(Integer.MAX_VALUE));
        Assert.assertTrue(CollectionUtils.isEmpty(scmCommits));


        DbScmPullRequest pr1 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/devops-levelops")).number("164").integrationId(gitHubIntegrationId)
                .project("levelops/devops-levelops")
                .creator("viraj-levelops").mergeSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(testScmUser)
                .title("LEV-1983").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        String prId1 = scmAggService.insert(company, pr1);
        Assert.assertNotNull(prId1);
        DbScmPullRequest prRead1 = scmAggService.getPr(company, "164", "levelops/devops-levelops", gitHubIntegrationId).get();
        validate(pr1, prRead1);
        prReviews = scmAggService.getPrReviews(company, prRead1.getId());
        Assertions.assertThat(prReviews.size()).isEqualTo(2);

        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmPrFilter.builder()
                                .repoIds(List.of("levelops/ingestion-levelops"))
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
                                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
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
                                .repoIds(List.of("levelops/does-not-exist"))
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
                                .repoIds(List.of("levelops/ingestion-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.listCommits(
                        company,
                        ScmCommitFilter.builder()
                                .repoIds(List.of("levelops/does-not-exist"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        10)
                        .getTotalCount())
                .isEqualTo(0);


        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops"))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculateCommits(
                        company, ScmCommitFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .across(ScmCommitFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops"))
                                .across(ScmPrFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                                .across(ScmPrFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrs(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .across(ScmPrFilter.DISTINCT.repo_id)
                                .build(), null).getTotalCount())
                .isEqualTo(0);

        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(2);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/integrations-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/ui-levelops"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                scmAggService.list(
                        company,
                        ScmReposFilter.builder()
                                .integrationIds(List.of(gitHubIntegrationId))
                                .repoIds(List.of("levelops/does-not-exist"))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000)
                        .getTotalCount())
                .isEqualTo(0);
                */
    }

    private String userIdOf(String cloudId) {
        return userIdentityService.getUser(company, gitHubIntegrationId, cloudId);
    }

    private void compareAggResults(List<DbScmContributorAgg> actualList, List<DbScmContributorAgg> expectedList) {
        Set<DbScmContributorAgg> expectedSet = new TreeSet<>(new Comparator<DbScmContributorAgg>() {
            @Override
            public int compare(DbScmContributorAgg o1, DbScmContributorAgg o2) {
                return o1.getName().hashCode() - o2.getName().hashCode();
            }
        });
        Set<DbScmContributorAgg> actualSet = new TreeSet<>(new Comparator<DbScmContributorAgg>() {
            @Override
            public int compare(DbScmContributorAgg o1, DbScmContributorAgg o2) {
                return o1.getName().hashCode() - o2.getName().hashCode();
            }
        });
        expectedSet.addAll(expectedList);
        actualSet.addAll(actualList);
        final Iterator<DbScmContributorAgg> expectedIter = expectedSet.iterator();
        final Iterator<DbScmContributorAgg> actualIter = actualSet.iterator();
        while (expectedIter.hasNext() && actualIter.hasNext()) {
            DbScmContributorAgg expected = expectedIter.next();
            DbScmContributorAgg actual = actualIter.next();
            Assertions.assertThat(actual.getId()).isEqualTo(expected.getId());
            Assertions.assertThat(actual.getName()).isEqualTo(expected.getName());
            Assertions.assertThat(actual.getFileTypes()).containsExactlyInAnyOrderElementsOf(expected.getFileTypes());
            Assertions.assertThat(actual.getNumRepos()).isEqualTo(expected.getNumRepos());
            Assertions.assertThat(actual.getNumCommits()).isEqualTo(expected.getNumCommits());
            Assertions.assertThat(actual.getNumAdditions()).isEqualTo(expected.getNumAdditions());
            Assertions.assertThat(actual.getNumDeletions()).isEqualTo(expected.getNumDeletions());
            Assertions.assertThat(actual.getNumPrs()).isEqualTo(expected.getNumPrs());
            Assertions.assertThat(actual.getNumChanges()).isEqualTo(expected.getNumChanges());
            Assertions.assertThat(actual.getNumJiraIssues()).isEqualTo(expected.getNumJiraIssues());
            Assertions.assertThat(actual.getNumWorkitems()).isEqualTo(expected.getNumWorkitems());
            Assertions.assertThat(actual.getRepoBreadth()).containsExactlyInAnyOrderElementsOf(expected.getRepoBreadth());
            Assertions.assertThat(actual.getTechBreadth()).containsExactlyInAnyOrderElementsOf(expected.getTechBreadth());
        }
    }

    public static void validate(DbScmPullRequest e, DbScmPullRequest a) {
        Assert.assertEquals(e.getRepoIds(), a.getRepoIds());
        Assert.assertEquals(e.getProject(), a.getProject());
        Assert.assertEquals(e.getNumber(), a.getNumber());
        Assert.assertEquals(e.getIntegrationId(), a.getIntegrationId());
        Assert.assertEquals(e.getCreator(), a.getCreator());
        Assert.assertEquals(e.getMergeSha(), a.getMergeSha());
        Assert.assertEquals(e.getTitle(), a.getTitle());
        Assert.assertEquals(e.getSourceBranch(), a.getSourceBranch());
        Assert.assertEquals(e.getState(), a.getState());
        Assert.assertEquals(e.getMerged(), a.getMerged());
        Assert.assertEquals(e.getAssignees(), a.getAssignees());
        Assert.assertEquals(e.getCommitShas(), a.getCommitShas());
        Assert.assertEquals(e.getLabels(), a.getLabels());
        Assert.assertEquals(e.getPrCreatedAt(), a.getPrCreatedAt(), 8 * 60 * 60);
        Assert.assertEquals(e.getPrUpdatedAt(), a.getPrUpdatedAt(), 8 * 60 * 60);
        Assert.assertEquals((e.getPrMergedAt() != null), (a.getPrMergedAt() != null));
        if (e.getPrMergedAt() != null) {
            Assert.assertEquals(e.getPrMergedAt(), a.getPrMergedAt(), 8 * 60 * 60);
        }
        Assert.assertEquals((e.getPrClosedAt() != null), (a.getPrClosedAt() != null));
        if (e.getPrClosedAt() != null) {
            Assert.assertEquals(e.getPrClosedAt(), a.getPrClosedAt(), 8 * 60 * 60);
        }
    }

    public static void validatePrs(List<DbScmPullRequest> e, List<DbScmPullRequest> a) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, DbScmPullRequest> actualMap = a.stream().collect(Collectors.toMap(DbScmPullRequest::getId, x -> x));
        Map<String, DbScmPullRequest> expectedMap = e.stream().collect(Collectors.toMap(DbScmPullRequest::getId, x -> x));

        for (String key : actualMap.keySet()) {
            validate(actualMap.get(key), expectedMap.get(key));
        }
    }

    @Test
    public void testScmPRAndReviews() throws SQLException {
        long now = Instant.now().getEpochSecond();
        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543339289").reviewer("viraj-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj1-levelops").originalDisplayName("viraj1-levelops").build())
                .reviewId("543339290").reviewer("viraj1-levelops")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmReview scmReview3 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .reviewId("543381675").reviewer("viraj-levelops")
                .state("CHANGES_REQUESTED").reviewedAt(now)
                .build();

        DbScmPullRequest pr = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .project("levelops/commons-levelops")
                .number("164")
                .integrationId(gitHubIntegrationId)
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call").sourceBranch("lev-1983").state("open").merged(false)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        String prId = scmAggService.insert(company, pr);
        Assert.assertNotNull(prId);
        DbScmPullRequest prRead = scmAggService.getPr(company, "164", "levelops/commons-levelops", gitHubIntegrationId).get();
        validate(pr, prRead);
        List<DbScmReview> prReviews = scmAggService.getPrReviews(company, prRead.getId());
        Assertions.assertThat(prReviews.size()).isEqualTo(2);

        DbScmPullRequest updatedPr = pr.toBuilder()
                .creator("viraj-levelops").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops").displayName("viraj-levelops").originalDisplayName("viraj-levelops").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call Updated").sourceBranch("lev-1983-u").state("merged").merged(true)
                .assignees(List.of("viraj-levelops")).commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .prCreatedAt(now).prUpdatedAt(now).prMergedAt(now).prClosedAt(now)
                .reviews(List.of(scmReview2, scmReview3))
                .build();
        String updatedPrId = scmAggService.insert(company, updatedPr);
        Assert.assertNotNull(updatedPrId);
        Assert.assertEquals(prId, updatedPrId);
        prRead = scmAggService.getPr(company, "164", "levelops/commons-levelops", gitHubIntegrationId).get();
        validate(updatedPr, prRead);
        prReviews = scmAggService.getPrReviews(company, prRead.getId());
        Assertions.assertThat(prReviews.size()).isEqualTo(2);
    }

    @Test
    public void testIssueExcludeStates() throws SQLException {
        DbListResponse<DbScmIssue> issuesList = scmAggService.list(company,
                ScmIssueFilter
                        .builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .extraCriteria(List.of())
                        .build(), Map.of(), null, 0, 10000);
        Assert.assertEquals(issuesList.getRecords().size(), 10);
        issuesList = scmAggService.list(company,
                ScmIssueFilter
                        .builder()
                        .excludeStates(List.of("open"))
                        .extraCriteria(List.of())
                        .build(), Map.of(), null, 0, 10000);
        Assert.assertEquals(issuesList.getRecords().size(), 1);
        issuesList = scmAggService.list(company,
                ScmIssueFilter
                        .builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .states(List.of("open"))
                        .excludeStates(List.of("closed"))
                        .extraCriteria(List.of())
                        .build(), Map.of(), null, 0, 10000);
        Assert.assertEquals(issuesList.getRecords().size(), 9);
        DbScmIssue closedIssue = issuesList.getRecords().stream().filter(issue -> issue.getState()
                        .equals("closed"))
                .findAny()
                .orElse(null);
        Assert.assertNull(closedIssue);
        DbListResponse<DbAggregationResult> groupByIssues = scmAggService.groupByAndCalculateIssues(company, ScmIssueFilter
                .builder()
                .across(ScmIssueFilter.DISTINCT.state)
                .excludeStates(List.of("open"))
                .extraCriteria(List.of())
                .build(), null);
        DbAggregationResult openIssue = groupByIssues.getRecords().stream().filter(issues -> issues.getKey()
                        .equals("open"))
                .findAny()
                .orElse(null);
        Assert.assertNull(openIssue);

    }

    private void validateCommits(List<DbScmCommit> e, List<DbScmCommit> a) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<String, DbScmCommit> actualMap = a.stream().collect(Collectors.toMap(DbScmCommit::getId, x -> x));
        Map<String, DbScmCommit> expectedMap = e.stream().collect(Collectors.toMap(DbScmCommit::getId, x -> x));

        for (String key : actualMap.keySet()) {
            validateCommit(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void validateCommit(DbScmCommit e, DbScmCommit a) {
        Assert.assertEquals(e.getRepoIds(), a.getRepoIds());
        Assert.assertEquals(e.getProject(), a.getProject());
        Assert.assertEquals(e.getIntegrationId(), a.getIntegrationId());
        Assert.assertEquals(e.getCommitter(), a.getCommitter());
        Assert.assertEquals(e.getCommitSha(), a.getCommitSha());
        Assert.assertEquals(e.getCommitUrl(), a.getCommitUrl());
        Assert.assertEquals(e.getAdditions(), a.getAdditions());
        Assert.assertEquals(e.getDeletions(), a.getDeletions());
        Assert.assertEquals(e.getFilesCt(), a.getFilesCt());
        Assert.assertEquals(e.getAuthor(), a.getAuthor());
        Assert.assertEquals(e.getCreatedAt(), a.getCreatedAt(), 8 * 60 * 60);
        Assert.assertEquals(e.getIngestedAt(), a.getIngestedAt(), 8 * 60 * 60);
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
                                                .projects(List.of("levelops/does-not-exist"))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmCommitWorkitemMapping() {
        List<String> commitWorkitem = scmAggService.getCommitWorkitems(company, List.of("ce694aa47bc06c7bdeeb2f17d3f340ea4ec8fd27"),
                gitHubIntegrationId);
        List<String> commitWorkitem1 = scmAggService.getCommitWorkitems(company, List.of("d091c7da5f3a9faf4b4e932c458ff4b40296314a"),
                gitHubIntegrationId);
        // FIXME
//        Assert.assertEquals("1233", commitWorkitem.stream()
//                .filter(f -> f.equals("1233")).findFirst().get());
//        Assert.assertEquals("889", commitWorkitem1.stream()
//                .filter(f -> f.equals("889")).findFirst().get());
    }

    @Test
    public void testScmPrWorkitemMapping() {
        List<String> prWorkitem = scmAggService.getPrWorkitems(company, List.of("16"), gitHubIntegrationId);
        List<String> prWorkitem1 = scmAggService.getPrWorkitems(company, List.of("14"), gitHubIntegrationId);
        // FIXME
//        Assert.assertEquals("1222", prWorkitem.stream().filter(f -> f.equals("1222")).findFirst().get());
//        Assert.assertEquals("1222", prWorkitem1.stream().filter(f -> f.equals("1222")).findFirst().get());
    }

    @Test
    public void testPrWorkItemMappingsTable() throws SQLException {
        long now = Instant.now().getEpochSecond();
        DbScmReview scmReview1 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("12345").displayName("viraj-levelops-1").originalDisplayName("viraj-levelops-1").build())
                .reviewId("543339111").reviewer("viraj-levelops-1")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmReview scmReview2 = DbScmReview.builder()
                .reviewerInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("54321").displayName("srinath-4").originalDisplayName("srinath-4").build())
                .reviewId("543339822").reviewer("srinath-4")
                .state("APPROVED").reviewedAt(now)
                .build();
        DbScmPullRequest pullRequest1 = DbScmPullRequest.builder()
                .workitemIds(List.of("LEV-1234"))
                .number("20")
                .repoIds(List.of("levelops/commons-levelops"))
                .integrationId(gitHubIntegrationId)
                .project("project-1")
                .creator("viraj-levelops-1").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops-1")
                        .displayName("viraj-levelops-1").originalDisplayName("viraj-levelops-1").build())
                .title("[LEV-1983] ADD: headers to the stellite response sent for the resp call Updated")
                .sourceBranch("lev-1983-u").state("merged").merged(true).assignees(List.of("viraj-levelops-1"))
                .commitShas(List.of("{222613ee40ceacc18ed59a787a745c49c18f71e9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops")
                        .displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now).prMergedAt(now).prClosedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        Assert.assertNotNull(scmAggService.insert(company, pullRequest1));
        DbScmPullRequest pullRequest2 = DbScmPullRequest.builder()
                .repoIds(List.of("levelops/commons-levelops1"))
                .project("project-2")
                // .workitemIds(List.of("LEV-3333"))
                .number("20")
                .integrationId(gitHubIntegrationId)
                .creator("srinath-1").mergeSha("dd4aa3fc00d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("srinath-1")
                        .displayName("srinath-1").originalDisplayName("srinath-1").build())
                .title("[LEV-1990] ADD: headers to the stellite response sent for the resp call")
                .sourceBranch("lev-1983").state("open").merged(false).assignees(List.of("srinath-1"))
                .commitShas(List.of("{934613bf40ceacc18ed59a787a745c49c18f71d9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("srinath-1")
                        .displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        Assert.assertNotNull(scmAggService.insert(company, pullRequest2));
        DbScmPullRequest pullRequest3 = DbScmPullRequest.builder()
                .issueKeys(List.of("ISSUE1"))
                .number("21")
                .repoIds(List.of("levelops/commons-levelops"))
                .integrationId(gitHubIntegrationId)
                .project("project-1")
                .creator("viraj-levelops-1").mergeSha("cd4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .creatorInfo(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops-1")
                        .displayName("viraj-levelops-1").originalDisplayName("viraj-levelops-1").build())
                .title("ADD: headers to the stellite response sent for the resp call Updated")
                .sourceBranch("lev-1983-u").state("merged").merged(true).assignees(List.of("viraj-levelops-1"))
                .commitShas(List.of("{222613ee40ceacc18ed59a787a745c49c18f71e9}")).labels(Collections.emptyList())
                .assigneesInfo(List.of(DbScmUser.builder().integrationId(gitHubIntegrationId).cloudId("viraj-levelops")
                        .displayName("viraj-levelops").originalDisplayName("viraj-levelops").build()))
                .prCreatedAt(now).prUpdatedAt(now).prMergedAt(now).prClosedAt(now)
                .reviews(List.of(scmReview1, scmReview2))
                .build();
        Assert.assertNotNull(scmAggService.insert(company, pullRequest3));
        Assert.assertEquals(1, scmAggService.list(company, ScmPrFilter.builder().projects(
                        List.of("project-1", "project-2")).hasIssueKeys("false").build(), Map.of(), null,
                0, 1000).getRecords().size());
        Assert.assertEquals(2, scmAggService.list(company, ScmPrFilter.builder().projects(
                        List.of("project-1", "project-2")).hasIssueKeys("true").build(), Map.of(), null,
                0, 1000).getRecords().size());
        Optional<DbScmPullRequest> actualPullRequest1 = scmAggService.getPr(company, "20", List.of("levelops/commons-levelops"), gitHubIntegrationId);
        String prId1 = actualPullRequest1.stream().filter(pr -> pr.getProject().equals("project-1")).findFirst().get().getNumber();
        assertThat(prId1).isEqualTo("20");
        Optional<DbScmPullRequest> actualPullRequest2 = scmAggService.getPr(company, "20", List.of("levelops/commons-levelops1"), gitHubIntegrationId);
        String prId2 = actualPullRequest2.stream().filter(pr -> pr.getProject().equals("project-2")).findFirst().get().getNumber();
        assertThat(prId2).isEqualTo("20");
        scmAggService.delete(company, "20", "levelops/commons-levelops", "project-1", gitHubIntegrationId);
        scmAggService.delete(company, "20", "levelops/commons-levelops1", "project-2", gitHubIntegrationId);
        scmAggService.delete(company, "21", "levelops/commons-levelops", "project-1", gitHubIntegrationId);
    }

    @Test
    public void testScmCommitList() throws SQLException {
        long now = Instant.now().getEpochSecond();
        DbScmCommit commit = DbScmCommit.builder()
                .repoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.GIT)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit);
        DbListResponse<DbScmCommit> dbListResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .projects(List.of("levelops/ingestion-levelops"))
                        .vcsTypes(List.of(VCS_TYPE.GIT))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assert.assertEquals(VCS_TYPE.GIT, dbListResponse.getRecords().get(0).getVcsType());
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(1);

        now = Instant.now().getEpochSecond();
        DbScmCommit commit1 = DbScmCommit.builder()
                .repoIds(List.of("levelops/devops-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/devops-levelops")
                .committer("viraj-levelops").commitSha("ad4aa3fc28d925ffc6671aefac3b412c3a0cbab2")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.TFVC)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit1);
        DbListResponse<DbScmCommit> dbListResponse1 = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .projects(List.of("levelops/devops-levelops"))
                        .vcsTypes(List.of(VCS_TYPE.TFVC))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assert.assertEquals(VCS_TYPE.TFVC, dbListResponse1.getRecords().get(0).getVcsType());
        Assertions.assertThat(dbListResponse1.getTotalCount()).isEqualTo(1);

        now = Instant.now().getEpochSecond();
        DbScmCommit commit2 = DbScmCommit.builder()
                .repoIds(List.of("levelops/api-levelops")).integrationId(gitHubIntegrationId)
                .project("levelops/ingestion-levelops")
                .committer("viraj-levelops").commitSha("80091a3200b99700b55208a225e842e6f1e4c1af")
                .committerInfo(testScmUser)
                .commitUrl("url")
                .vcsType(VCS_TYPE.PERFORCE)
                .additions(2).deletions(2).filesCt(1).changes(1).author("viraj-levelops")
                .authorInfo(testScmUser)
                .committedAt(now)
                .createdAt(now)
                .ingestedAt(now)
                .build();
        scmAggService.insertCommit(company, commit2);
        DbListResponse<DbScmCommit> dbListResponse2 = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .repoIds(List.of("levelops/api-levelops"))
                        .projects(List.of("levelops/ingestion-levelops"))
                        .vcsTypes(List.of(VCS_TYPE.PERFORCE))
                        .build(),
                Map.of(),
                null,
                0,
                10);
        Assert.assertEquals(VCS_TYPE.PERFORCE, dbListResponse2.getRecords().get(0).getVcsType());
        Assertions.assertThat(dbListResponse2.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testScmIssueList() throws SQLException {
        Assertions.assertThat(scmAggService.list(
                                company, ScmIssueFilter.builder()
                                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                        .projects(List.of("hashicorp/vault"))
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .build(), Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(scmAggService.list(
                                company, ScmIssueFilter.builder()
                                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                        .partialMatch(Map.of("project", Map.of("$ends", "/vault")))
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .build(), Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(10);
        Assertions.assertThat(scmAggService.list(
                                company, ScmIssueFilter.builder()
                                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                        .partialMatch(Map.of("project", Map.of("$ends", "/vau")))
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .build(), Map.of(), null, 0, 1000)
                        .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmFileList() throws SQLException {
        Assertions.assertThat(scmAggService.list(
                                company, ScmFilesFilter.builder()
                                        .projects(List.of("levelops/ui-levelops"))
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .commitStartTime(currentTime.toInstant().getEpochSecond() - 86400)
                                        .commitEndTime(currentTime.toInstant().getEpochSecond() + 86400)
                                        .build(), Map.of(), 0, 1000)
                        .getTotalCount())
                .isEqualTo(6);
        Assertions.assertThat(scmAggService.list(
                company, ScmFilesFilter.builder()
                        .partialMatch(Map.of("project", Map.of("$ends", "ui-levelops"),
                                "filename", Map.of("$contains", "dashboard")))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .commitStartTime(currentTime.toInstant().getEpochSecond() - 86400)
                        .commitEndTime(currentTime.toInstant().getEpochSecond() + 86400)
                        .build(), Map.of(), 0, 1000).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testScmReposList() throws SQLException {
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .projects(List.of("levelops/ui-levelops"))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .projects(List.of("levelops/does-not-exist"))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmContributorsList() {
        String user = userIdOf("piyushkantm");
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .committers(List.of(user))
                                                .includeIssues(true)
                                                .projects(List.of("levelops/ui-levelops"))
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .committers(List.of(user))
                                                .includeIssues(true)
                                                .projects(List.of("levelops/does-not-exist"))
                                                .integrationIds(List.of(gitHubIntegrationId))
                                                .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testScmFilesListModules() {
        Assertions.assertThat(scmAggService.listModules(company,
                ScmFilesFilter.builder()
                        .projects(List.of("levelops/ui-levelops"))
                        .module("wiremock")
                        .listFiles(true)
                        .commitStartTime(0L)
                        .build(),
                Map.of()).getTotalCount()).isEqualTo(0);
        Assertions.assertThat(scmAggService.listModules(company,
                ScmFilesFilter.builder()
                        .projects(List.of("levelops/does-not-exist"))
                        .module("wiremock")
                        .listFiles(true)
                        .commitStartTime(0L)
                        .build(),
                Map.of()).getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testGroupByAndCalculatePrsCount() throws SQLException {
        var results = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.project)
                        .build(), null);
        Assertions.assertThat(results.getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .projects(List.of("levelops/does-not-exist"))
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testGroupByAndCalculateCommits() {
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .vcsTypes(List.of(VCS_TYPE.GIT))
                                        .across(ScmCommitFilter.DISTINCT.vcs_type)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .vcsTypes(List.of(VCS_TYPE.TFVC))
                                        .across(ScmCommitFilter.DISTINCT.vcs_type)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .vcsTypes(List.of(VCS_TYPE.PERFORCE))
                                        .across(ScmCommitFilter.DISTINCT.vcs_type)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .across(ScmCommitFilter.DISTINCT.vcs_type)
                                        .build(), null).getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .across(ScmCommitFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(3);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateCommits(
                                company, ScmCommitFilter.builder()
                                        .integrationIds(List.of(gitHubIntegrationId))
                                        .projects(List.of("levelops/does-not-exist"))
                                        .across(ScmCommitFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(0);
    }

    @Test
    public void testGroupByAndCalculateIssues() {
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateIssues(
                                company, ScmIssueFilter.builder()
                                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                        .projects(List.of("levelops/does-not-exist"))
                                        .across(ScmIssueFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateIssues(
                                company, ScmIssueFilter.builder()
                                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                                        .across(ScmIssueFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
    }

    @Test
    public void testGroupByAndCalculateFiles() {
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateFiles(
                                company, ScmFilesFilter.builder()
                                        .projects(List.of("levelops/does-not-exist"))
                                        .across(ScmFilesFilter.DISTINCT.project)
                                        .build()).getTotalCount())
                .isEqualTo(0);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculateFiles(
                                company, ScmFilesFilter.builder()
                                        .across(ScmFilesFilter.DISTINCT.project)
                                        .build()).getTotalCount())
                .isEqualTo(1);
    }

    @Test
    public void testDeletePr() throws SQLException {
        Assert.assertTrue(
                scmAggService.delete(
                        company, "24",
                        "levelops/aggregations-levelops", "levelops/aggregations-levelops", gitHubIntegrationId)
        );
        Assert.assertTrue(
                scmAggService.delete(
                        company, "164",
                        "levelops/commons-levelops", "levelops/commons-levelops", gitHubIntegrationId)
        );
        Assert.assertFalse(
                scmAggService.delete(
                        company, "24",
                        "levelops/does-not-exist", "levelops/aggregations-levelops", gitHubIntegrationId)
        );
        Assert.assertFalse(
                scmAggService.delete(
                        company, "24",
                        "levelops/aggregations-levelops", "levelops/aggregations-levelops", "20")
        );
    }

    @Test
    public void testSortOrder() {
        String user = userIdOf("piyushkantm");
        String user2 = userIdOf("viraj-levelops");
        Validate.notNull(user, "user cannot be null.");
        Validate.notNull(user2, "user2 cannot be null.");
        DbListResponse<DbScmContributorAgg> dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of(user, user2))
                        .committers(List.of(user, user2))
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of("num_additions", SortingOrder.DESC),
                null,
                0,
                1000);
        Assert.assertEquals(dbListResponse.getRecords().get(0).getName(), "piyushkantm");
        Assert.assertEquals(dbListResponse.getRecords().get(1).getName(), "viraj-levelops");

        DbListResponse<DbScmContributorAgg> dbListResponse1 = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of(user, user2))
                        .committers(List.of(user, user2))
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of("num_additions", SortingOrder.ASC),
                null,
                0,
                1000);

        Assert.assertEquals(dbListResponse1.getRecords().get(0).getName(), "viraj-levelops");
        Assert.assertEquals(dbListResponse1.getRecords().get(1).getName(), "piyushkantm");


        DbListResponse<DbScmContributorAgg> dbListResponse2 = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of(user, user2))
                        .includeIssues(true)
                        .committers(List.of(user, user2))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of("num_deletions", SortingOrder.DESC),
                null,
                0,
                1000);

        Assert.assertEquals(dbListResponse2.getRecords().get(0).getName(), "piyushkantm");
        Assert.assertEquals(dbListResponse2.getRecords().get(1).getName(), "viraj-levelops");

        DbListResponse<DbScmContributorAgg> dbListResponse3 = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .authors(List.of(user, user2))
                        .committers(List.of(user, user2))
                        .includeIssues(true)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .dataTimeRange(ImmutablePair.of(0L, 2000000000L))
                        .build(),
                Map.of("num_deletions", SortingOrder.ASC),
                null,
                0,
                1000);

        // FIXME
//        Assert.assertEquals(dbListResponse3.getRecords().get(0).getName(), "viraj-levelops");
//        Assert.assertEquals(dbListResponse3.getRecords().get(1).getName(), "piyushkantm");

        List<CommitWithJira> commits = scmJiraZendeskService.listCommits(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                0,
                1000, Map.of(), null).getRecords();
        // FIXME
//        assertThat(commits.get(0).getCommitter()).isEqualTo("web-flow");
//        assertThat(commits.get(1).getCommitter()).isEqualTo("piyushhkantm");

        commits = scmJiraZendeskService.listCommits(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                0,
                1000, Map.of("committer", SortingOrder.ASC), null).getRecords();
        // FIXME
//        assertThat(commits.get(0).getCommitter()).isEqualTo("meghana-levelops");
//        assertThat(commits.get(1).getCommitter()).isEqualTo("web-flow");

        List<JiraWithGitZendesk> jiraIssues = scmJiraZendeskService.listJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                true,
                0,
                1000, Map.of("issue_created_at", SortingOrder.ASC), null).getRecords();
//        assertThat(jiraIssues.get(0).getIssueCreatedAt()).isLessThan(jiraIssues.get(1).getIssueCreatedAt()); // FIXME

        jiraIssues = scmJiraZendeskService.listJiraTickets(
                company,
                JiraIssuesFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                true,
                0,
                1000, Map.of("issue_created_at", SortingOrder.DESC), null).getRecords();
//        assertThat(jiraIssues.get(0).getIssueCreatedAt()).isGreaterThan(jiraIssues.get(1).getIssueCreatedAt()); // FIXME

        List<DbScmFile> escalatedFiles = scmJiraZendeskService.listEscalatedFiles(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).module("wiremock").build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                0,
                100, Map.of("additions", SortingOrder.ASC), null).getRecords();
//        assertThat(escalatedFiles.get(0).getTotalAdditions()).isEqualTo(32); // FIXME
//        assertThat(escalatedFiles.get(1).getTotalAdditions()).isEqualTo(64);

        escalatedFiles = scmJiraZendeskService.listEscalatedFiles(
                company,
                ScmFilesFilter.builder().integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId)).module("wiremock").build(),
                JiraIssuesFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(gitHubIntegrationId, jiraIntegrationId))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond()).build(),
                0,
                100, Map.of("additions", SortingOrder.DESC), null).getRecords();
//        assertThat(escalatedFiles.get(0).getTotalAdditions()).isEqualTo(64); // FIXME
//        assertThat(escalatedFiles.get(1).getTotalAdditions()).isEqualTo(32);
    }

    @Test
    public void testGetOrganizations() {
        List<String> orgs = repositoryService.getOrganizations(company, gitHubIntegrationId);
        Assert.assertEquals(orgs, List.of("hashicorp", "levelops"));
    }

    @Test
    public void testDaysCountRange() {
        String user = userIdOf("piyushkantm");
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .fileTypes(List.of("json", "jsx", "png"))
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.file_type.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.of(null, 4L))
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("jsx"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .committers(List.of(user))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.author.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.of(0L, null))
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isEqualTo(List.of("piyushkantm"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .committers(List.of(user))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.author.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.of(0L, 2L))
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isEqualTo(List.of("piyushkantm"));

    }

    @Test
    public void testCodingDaysPerWeek() {
        String user = userIdOf("piyushkantm");

        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .excludeRepoIds(List.of("levelops/ingestion-levelops", "levelops/integrations-levelops"))
                        .aggInterval(AGG_INTERVAL.week)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_days.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/ui-levelops"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .fileTypes(List.of("json", "jsx", "png"))
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.file_type.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("jsx"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .committers(List.of(user))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.author.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isEqualTo(List.of("piyushkantm"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .excludeCommitters(List.of(user))
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.committer.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isEqualTo(List.of());

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .aggInterval(AGG_INTERVAL.day)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_days.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/ui-levelops"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.day)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_days.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("CSS"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_days.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.month)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_days.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testCommitsPerCodingDays() {

        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_count.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("piyushkantm");

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.project)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.project.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/ui-levelops"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.technology.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("CSS"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .fileTypes(List.of("jsx", "png"))
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.file_type.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("jsx"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.author)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.DISTINCT.author.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isEqualTo(List.of("piyushkantm"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.vcs_type)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .sort(Map.of(ScmCommitFilter.CALCULATION.commit_count.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("GIT"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .codeChanges(List.of("small"))
                        .sort(Map.of())
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("CSS"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .codeChanges(List.of("large"))
                        .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                        .sort(Map.of())
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of());

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.repo_id)
                        .commitShas(List.of("d091c7da5f3a9faf4b4e932c458ff4b40296314a"))
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.repo_id.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/ui-levelops"));

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .aggInterval(AGG_INTERVAL.month)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.trend.toString(), SortingOrder.DESC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
//        assertThat(result.getTotalCount()).isEqualTo(1); // FIXME flaky

        result = scmAggService.groupByAndCalculateCodingDays(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .aggInterval(AGG_INTERVAL.week)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.trend.toString(), SortingOrder.ASC))
                        .daysCountRange(ImmutablePair.nullPair())
                        .build(), null);
//        assertThat(result.getTotalCount()).isEqualTo(1);// FIXME flaky
    }

    @Test
    public void testBiWeekly() throws SQLException {
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.trend.toString(), SortingOrder.ASC))
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);

        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .committedAtRange(ImmutablePair.of(1555200L, 1641600L))
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.trend.toString(), SortingOrder.ASC))
                        .build(), null);
        assertThat(result.getTotalCount()).isEqualTo(1);

        result = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.trend)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .sort(Map.of(ScmCommitFilter.DISTINCT.trend.toString(), SortingOrder.ASC))
                        .build(), null);
//        assertThat(result.getTotalCount()).isEqualTo(1); // FIXME flaky

        DbListResponse<DbAggregationResult> resultIssues = scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                        .across(ScmIssueFilter.DISTINCT.issue_created)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .build(), null);
        assertThat(resultIssues.getTotalCount()).isEqualTo(6);

        resultIssues = scmAggService.groupByAndCalculateIssues(
                company, ScmIssueFilter.builder()
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.idle))
                        .across(ScmIssueFilter.DISTINCT.issue_updated)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .build(), null);
        assertThat(resultIssues.getTotalCount()).isEqualTo(2);

        DbListResponse<DbAggregationResult> resultPrs = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.pr_updated)
                        .aggInterval(AGG_INTERVAL.biweekly)
                        .build(), null);
        assertThat(resultPrs.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testGetScmFileAndFileCommit() throws SQLException {
        Optional<DbScmFile> file = scmAggService.getFile(company, "src/assessments/components/comments/comments.component.jsx",
                "levelops/ui-levelops", "levelops/ui-levelops", gitHubIntegrationId);
        Assertions.assertThat(file.get()).isNotNull();
        Assertions.assertThat(file.get().getFilename()).isEqualTo("src/assessments/components/comments/comments.component.jsx");

        Optional<DbScmFileCommit> fileCommit = scmAggService.getFileCommit(company, "d091c7da5f3a9faf4b4e932c458ff4b40296314a", file.get().getId());
        Assertions.assertThat(fileCommit.get()).isNotNull();
        Assertions.assertThat(fileCommit.get().getAddition()).isEqualTo(4);
        Assertions.assertThat(fileCommit.get().getDeletion()).isEqualTo(4);
        Assertions.assertThat(fileCommit.get().getChange()).isEqualTo(8);

        Boolean updateFileCommitStats = scmAggService.updateFileCommitStats(company, UUID.fromString(fileCommit.get().getId()), 1, 1, 2);
        assertThat(updateFileCommitStats).isEqualTo(true);
        updateFileCommitStats = scmAggService.updateFileCommitStats(company, UUID.fromString(fileCommit.get().getId()), 4, 4, 8);
        assertThat(updateFileCommitStats).isEqualTo(true);
    }

    @Test
    public void testValues() throws SQLException {
        for (ScmPrFilter.DISTINCT distinct : ScmPrFilter.DISTINCT.values()) {
            DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                    company, ScmPrFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(distinct)
                            .build(), true, null);
            DefaultObjectMapper.prettyPrint(resultDbListResponse);
            if (resultDbListResponse.getTotalCount() != 0) {
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).containsNull();
            }
        }
        for (ScmCommitFilter.DISTINCT distinct : ScmCommitFilter.DISTINCT.values()) {
            DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                    company, ScmCommitFilter.builder()
                            .integrationIds(List.of(gitHubIntegrationId))
                            .across(distinct)
                            .build(), true, null);
            DefaultObjectMapper.prettyPrint(resultDbListResponse);
            if (resultDbListResponse.getTotalCount() != 0) {
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getP90).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMedian).collect(Collectors.toList())).containsNull();
                Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getMax).collect(Collectors.toList())).containsNull();
            }
        }
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .committedAtRange(ImmutablePair.of(0L, 333333L))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .committedAtRange(ImmutablePair.of(0L, 333333L))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_days)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testOptionalFilterJoin() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.comment_density)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("shallow");
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .calculation(ScmPrFilter.CALCULATION.author_response_time)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .calculation(ScmPrFilter.CALCULATION.reviewer_response_time)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .creators(List.of("sampple"))
                        .calculation(ScmPrFilter.CALCULATION.reviewer_response_time)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.creator)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.pr_merged)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChanges(List.of("small"))
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), true, null);
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1); // // FIXME flaky
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.file_type)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), true, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .build(), false, null);
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1); // FIXME flaky
    }

    @Test
    public void testRepoTechnicalBreadthCalc() {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count_only)
                        .build(), false, null);
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1); // FIXME flaky
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count_only)
                        .build(), false, null);
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1); // FIXME flaky
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.author)
                        .authors(List.of("sample"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count_only)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .authors(List.of("sample"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count_only)
                        .build(), false, null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testListCommiters() {
        DbListResponse<DbScmContributorAgg> dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_", SortingOrder.DESC),
                null,
                0,
                1000);
//        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0); // FIXME flaky
        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "h")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .authors(List.of("ctlo2020"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
//        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0); // FIXME flaky
//        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
//                .isEmpty(); // FIXME flaky

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "levelops")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
//        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0); // FIXME flaky
//        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
//                .isEmpty();

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.author)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("author", Map.of("$ends", "flow")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);

        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$contains", "flow")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
                .isEmpty();

        String user = userIdOf("piyushkantm");
        dbListResponse = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .across(ScmContributorsFilter.DISTINCT.committer)
                        .includeIssues(false)
                        .authors(List.of(user))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .partialMatch(Map.of("committer", Map.of("$contains", "flow")))
                        .build(),
                Map.of("num_repos", SortingOrder.DESC),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(dbListResponse.getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
                .isEmpty();
    }

    @Test
    public void testTechnologyAcrossAndFilters() {

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .build(), null);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("CSS");
        DbListResponse<DbScmCommit> dbScmCommitDbListResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .technologies(List.of("CSS"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .contains(dbScmCommitDbListResponse.getTotalCount().longValue());

        dbAggregationResultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.technology)
                        .excludeTechnologies(List.of("CSS"))
                        .build(), null);
        Assertions.assertThat(dbAggregationResultDbListResponse.getRecords()).isEmpty();
        dbScmCommitDbListResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .excludeTechnologies(List.of("CSS"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(dbScmCommitDbListResponse.getRecords()).isEmpty();
    }

    @Test
    public void testCodeChangeAcrossAndDrilldownCounts() {
        DbListResponse<DbAggregationResult> aggregateResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.code_change)
                        .build(), null);
        Assertions.assertThat(aggregateResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(aggregateResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("small");
        DbListResponse<DbScmCommit> listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("small"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("small"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .contains(listResponse.getTotalCount().longValue());

        listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("medium"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("medium"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEmpty();

        listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("large"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("large"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEmpty();

        aggregateResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                        .across(ScmCommitFilter.DISTINCT.code_change)
                        .build(), null);
        Assertions.assertThat(aggregateResponse.getRecords()).isNotEmpty();
        Assertions.assertThat(aggregateResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("small");

        listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("large"))
                        .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("large"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEmpty();

        listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("small"))
                        .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("small"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .contains(listResponse.getTotalCount().longValue());


        listResponse = scmAggService.listCommits(
                company,
                ScmCommitFilter.builder()
                        .codeChanges(List.of("medium"))
                        .codeChangeSizeConfig(Map.of("small", "5", "medium", "10"))
                        .integrationIds(List.of(gitHubIntegrationId))
                        .build(),
                Map.of(),
                null,
                0,
                10000);
        Assertions.assertThat(aggregateResponse.getRecords().stream().filter(x -> x.getKey().equals("medium"))
                        .map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .isEmpty();
    }

    @Test
    public void testFirstReviewAggregateAndDrillDown() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .sort(Map.of("pr_created", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        DbListResponse<DbScmPullRequest> list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .hasComments(true)
                .prCreatedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_updated)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .sort(Map.of("pr_updated", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .hasComments(true)
                .prUpdatedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_merged)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .sort(Map.of("pr_merged", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .hasComments(true)
                .codeChangeSizeConfig(Map.of())
                .prUpdatedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .missingFields(Map.of("pr_merged", false))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);

        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_closed)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .sort(Map.of("pr_closed", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .hasComments(true)
                .codeChangeSizeConfig(Map.of())
                .prClosedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .missingFields(Map.of("pr_closed", false))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testFirstReviewToMergeAggregateAndDrillDown() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_created)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                        .sort(Map.of("pr_created", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        DbListResponse<DbScmPullRequest> list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .hasComments(true)
                .missingFields(Map.of("pr_merged", false))
                .prCreatedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_updated)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                        .sort(Map.of("pr_updated", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .hasComments(true)
                .missingFields(Map.of("pr_merged", false))
                .prCreatedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);
        resultDbListResponse = scmAggService.groupByAndCalculatePrsDuration(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.pr_closed)
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                        .sort(Map.of("pr_closed", SortingOrder.ASC))
                        .build(), null);
        Assertions.assertThat(resultDbListResponse).isNotNull();
        list = scmAggService.list(company, ScmPrFilter.builder()
                .codeChangeUnit("lines")
                .codeChangeSizeConfig(Map.of())
                .hasComments(true)
                .prClosedRange(ImmutablePair.of(1588291200L, 1590969593L))
                .missingFields(Map.of("pr_merged", false, "pr_closed", false))
                .commentDensitySizeConfig(Map.of())
                .build(), Map.of(), null, 0, 10000);
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(0);
    }

    @Test
    public void testPagination() throws SQLException {

        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.groupByAndCalculatePrs(
                company, ScmPrFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChanges(List.of("small"))
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), false, null, 0, 1);
        Assertions.assertThat(resultDbListResponse.getCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("NONE");
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .across(ScmCommitFilter.DISTINCT.committer)
                        .calculation(ScmCommitFilter.CALCULATION.commit_count)
                        .build(), false, null, 0, 2);
//        Assertions.assertThat(resultDbListResponse.getCount()).isEqualTo(1); // FIXME flaky
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1); // FIXME flaky
        resultDbListResponse = scmAggService.groupByAndCalculateCommits(
                company, ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.author)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), false, null, 0, 2);
//        Assertions.assertThat(resultDbListResponse.getCount()).isEqualTo(1); // FIXME flaky
//        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        resultDbListResponse = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null, 0, 1);
        Assert.assertNotNull(resultDbListResponse);
        Assertions.assertThat(resultDbListResponse.getCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(8);
        resultDbListResponse = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_updated"))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .sort(Map.of("issue_updated", SortingOrder.ASC))
                        .build(), null, 0, 3);
        Assert.assertNotNull(resultDbListResponse);
        Assertions.assertThat(resultDbListResponse.getCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(resultDbListResponse.getRecords().stream().map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("1594598400", "1594425600", "1594512000");
    }

    @Test
    public void testTimeDifferenceInDays() {
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687824000L, 1687910400L))).isEqualTo(1L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687823999L, 1687910400L))).isEqualTo(2L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687824000L, 1687910399L))).isEqualTo(1L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687824001L, 1687910400L))).isEqualTo(1L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687824000L, 1687910401L))).isEqualTo(2L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687823999L, 1687910399L))).isEqualTo(1L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1687824001L, 1687910401L))).isEqualTo(1L);
        Assertions.assertThat(ScmAggService.getTimeDifferenceInDays(ImmutablePair.of(1682380800L, 1682965799L))).isEqualTo(7L);
    }

    @Test
    public void testScmTag() throws SQLException {
        List<DbScmTag> expected = testInsertsScmTag(5);
        testAllListScmTag(expected);
        expected = testUpdatesScmTag(expected);
        testDeleteScmTag(expected);
    }

    private List<DbScmTag> testInsertsScmTag(int n) throws SQLException {
        List<DbScmTag> expected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            DbScmTag current = testInsertScmTag(i);
            testGetScmTag(current);
            expected.add(current);
        }
        return expected;
    }

    private DbScmTag testInsertScmTag(int i) throws SQLException {
        DbScmTag expected = createObjectScmTag(i);
        UUID id = scmAggService.insertTag(company, expected);
        Assert.assertNotNull(id);
        expected = expected.toBuilder().id(id).build();
        return expected;
    }

    private void testGetScmTag(DbScmTag allExpected) throws SQLException {
        Optional<DbScmTag> result = scmAggService.getTag(company, allExpected.getId().toString());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        verifyRecordScmTag(result.get(), allExpected);
    }

    private void verifyRecordScmTag(DbScmTag a, DbScmTag e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getIntegrationId(), e.getIntegrationId());
        Assert.assertEquals(a.getProject(), e.getProject());
        Assert.assertEquals(a.getRepo(), e.getRepo());
        Assert.assertEquals(a.getTag(), e.getTag());
        Assert.assertEquals(a.getCommitSha(), e.getCommitSha());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private DbScmTag createObjectScmTag(int i) {
        String namePrefix = (i % 2 == 0) ? "EvenPrefix " : "OddPrefix ";
        String nameSuffix = (i % 2 == 0) ? " EvenSuffix" : " OddSuffix";
        return DbScmTag.builder()
                .integrationId("1")
                .tag(namePrefix + i + nameSuffix)
                .project("Project")
                .repo("repo1")
                .commitSha("ac")
                .build();
    }

    private void testListByFiltersIntegrationIdScmTag(List<DbScmTag> allExpected) {
        Map<String, List<DbScmTag>> map = allExpected.stream().collect(Collectors.groupingBy(DbScmTag::getIntegrationId));
        for (String integrationId : map.keySet()) {
            List<DbScmTag> expected = map.get(integrationId);
            DbListResponse<DbScmTag> result = scmAggService.listByFilterTag(company, 0, 100, List.of(integrationId), null, null, null, null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecordsScmTag(result.getRecords(), expected);
        }
        List<String> allIntegrationIds = allExpected.stream().map(DbScmTag::getIntegrationId).collect(Collectors.toList());
        DbListResponse<DbScmTag> result = scmAggService.listByFilterTag(company, 0, 100, allIntegrationIds, null, null, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecordsScmTag(result.getRecords(), allExpected);
    }

    private void testListByFiltersIdsScmTag(List<DbScmTag> allExpected) {
        List<UUID> ids = new ArrayList<>();
        List<DbScmTag> expected = new ArrayList<>();
        for (int i = 0; i < allExpected.size(); i++) {
            if (i % 2 == 0) {
                continue;
            }
            ids.add(allExpected.get(i).getId());
            expected.add(allExpected.get(i));
        }
        DbListResponse<DbScmTag> result = scmAggService.listByFilterTag(company, 0, 100, null, ids, null, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecordsScmTag(result.getRecords(), expected);
    }

    private void testListScmTag(List<DbScmTag> expected) throws SQLException {
        DbListResponse<DbScmTag> result = scmAggService.listTag(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        List<DbScmTag> actual = result.getRecords();
        verifyRecordsScmTag(actual, expected);

        result = scmAggService.listTag(company, 0, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());
        actual = result.getRecords();
        verifyRecordsScmTag(actual, expected.subList(expected.size() - 2, expected.size()));
    }

    private void testAllListScmTag(List<DbScmTag> allExpected) throws SQLException {
        testListScmTag(allExpected);
        testListByFiltersIdsScmTag(allExpected);
        testListByFiltersIntegrationIdScmTag(allExpected);
    }

    private void verifyRecordsScmTag(List<DbScmTag> a, List<DbScmTag> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, DbScmTag> actualMap = a.stream().collect(Collectors.toMap(DbScmTag::getId, x -> x));
        Map<UUID, DbScmTag> expectedMap = e.stream().collect(Collectors.toMap(DbScmTag::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecordScmTag(actualMap.get(key), expectedMap.get(key));
        }
    }

    private List<DbScmTag> testUpdatesScmTag(List<DbScmTag> allExpected) throws SQLException {
        List<DbScmTag> allUpdated = new ArrayList<>();
        for (int i = 0; i < allExpected.size(); i++) {
            DbScmTag updated = testUpdateScmTag(allExpected.get(i), i);
            allUpdated.add(updated);
        }
        testListScmTag(allUpdated);
        return allUpdated;
    }

    private DbScmTag testUpdateScmTag(DbScmTag actual, int i) throws SQLException {
        String prefix = ((100 + i) % 2 == 0) ? "EvenPrefix " : "OddPrefix ";
        String suffix = ((100 + i) % 2 == 0) ? " EvenSuffix" : " OddSuffix";

        DbScmTag updated = DbScmTag.builder()
                .id(actual.getId())
                .integrationId(actual.getIntegrationId())
                .tag(prefix + (100 + i) + suffix)
                .project(actual.getProject())
                .repo(actual.getRepo())
                .commitSha(actual.getCommitSha())
                .build();

        scmAggService.updateTag(company, updated);
        testGetScmTag(updated);
        return updated;
    }

    private void testDeleteScmTag(List<DbScmTag> allExpected) throws SQLException {
        for (int i = 0; i < allExpected.size(); i++) {
            DbScmTag current = allExpected.get(0);
            Boolean success = scmAggService.deleteTag(company, current.getId().toString());
            Assert.assertTrue(success);
            allExpected.remove(0);
            testListScmTag(allExpected);
        }
        testListScmTag(allExpected);
    }
}