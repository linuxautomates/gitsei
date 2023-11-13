package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class ScmAggServiceDateTest {

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
    private static Date currentTime;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("viraj-levelops")
            .displayName("viraj-levelops")
            .originalDisplayName("viraj-levelops")
            .build();
    private static List<DbScmIssue> issueList;
    private static List<DbScmCommit> commitList;
    private static List<DbScmPullRequest> prsList;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        final JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        final JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);

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
        currentTime = new Date();
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
        prsList = new ArrayList<>();
        input = ResourceUtils.getResourceAsString("json/databases/github_prs_2021.json");
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
                            prsList.add(tmp);
                            scmAggService.insert(company, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issueList = new ArrayList<>();
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, gitHubIntegrationId));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), gitHubIntegrationId);
                        issueList.add(tmp);
                        if (scmAggService.getIssue(company, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(company, tmp);
                        }
                    });
        });
        commitList = new ArrayList<>();
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
                        commitList.add(tmp);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                throw new RuntimeStreamException(e);
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

    @Test
    public void testCommits() {
        List<Long> listOfInputDates = commitList.stream().map(DbScmCommit::getCommittedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        result = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        Assert.assertNotNull(actualList);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculateCommits(company,
                ScmCommitFilter.builder()
                        .across(ScmCommitFilter.DISTINCT.fromString("trend"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmCommitFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testPrs() throws SQLException {
        List<Long> listOfInputDates = prsList.stream().map(DbScmPullRequest::getPrCreatedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_created"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_created"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_created"))
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_created"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_created"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testPrsByUpdatedAt() throws SQLException {
        List<Long> listOfInputDates = prsList.stream().map(DbScmPullRequest::getPrUpdatedAt).collect(Collectors.toList());
        List<String> expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_updated"))
                        .aggInterval(AGG_INTERVAL.day)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        List<String> actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_updated"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_updated"))
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_updated"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_updated"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testPrsByMergedAt() throws SQLException {
        List<Long> listOfInputDates = prsList.stream().map(DbScmPullRequest::getPrMergedAt).collect(Collectors.toList());
        List<String> expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_merged"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        List<String> actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)),
                        Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_merged"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_merged"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testIssues() {
        List<Long> listOfInputDates = issueList.stream().map(DbScmIssue::getIssueCreatedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testPrsCountGroupBy() throws SQLException {
        List<Long> listOfInputDates = prsList.stream().map(DbScmPullRequest::getPrClosedAt).collect(Collectors.toList());
        List<String> expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.day)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        List<String> actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrs(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmPrFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.day)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.DAY_OF_MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.month)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.week)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.WEEK_OF_YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();

        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        result = scmAggService.groupByAndCalculatePrsDuration(company,
                ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.fromString("pr_closed"))
                        .aggInterval(AGG_INTERVAL.quarter)
                        .calculation(ScmPrFilter.CALCULATION.first_review_time)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .map(key -> new Date(TimeUnit.MILLISECONDS.toMicros(Long.parseLong(key))))
                .map(date -> extractDataComponentForDbResults(date, Calendar.MONTH, true))
                .map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
    }

    @Test
    public void testIssueUpdatedAtFilters() throws SQLException {
        List<Long> listOfInputDates = issueList.stream().map(DbScmIssue::getIssueUpdatedAt).collect(Collectors.toList());
        List<String> actualList;
        List<String> expectedList;
        expectedList = listOfInputDates.stream()
                .filter(Objects::nonNull)
                .map(instant -> extractDataComponentForDbResults(new Date(TimeUnit.MILLISECONDS.toMicros(instant)), Calendar.YEAR, false))
                .map(String::valueOf)
                .collect(Collectors.toList());
        DbListResponse<DbAggregationResult> result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_updated"))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        actualList = result.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList());
        Assert.assertNotNull(result);
        assertThat(expectedList.containsAll(actualList)).isEqualTo(true);
        assertThat(result.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .isNotNull();
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .issueUpdatedRange(ImmutablePair.of(0L, 2231884799L))
                        .aggInterval(AGG_INTERVAL.year)
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        Assert.assertNotNull(result);
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_updated"))
                        .aggInterval(AGG_INTERVAL.year)
                        .issueUpdatedRange(ImmutablePair.of(1577836800L, 1609459199L))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        Assert.assertNotNull(result);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(10L);
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_updated"))
                        .aggInterval(AGG_INTERVAL.month)
                        .issueUpdatedRange(ImmutablePair.of(1593561600L, 1596239999L))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        Assert.assertNotNull(result);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(10L);
        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("first_comment"))
                        .aggInterval(AGG_INTERVAL.month)
                        .firstCommentAtRange(ImmutablePair.of(1567296000L, 1569801600L))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        Assert.assertNotNull(result);
        Assertions.assertThat(result.getRecords().stream().map(DbAggregationResult::getCount).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L);

        result = scmAggService.groupByAndCalculateIssues(company,
                ScmIssueFilter.builder()
                        .extraCriteria(List.of())
                        .across(ScmIssueFilter.DISTINCT.fromString("issue_created"))
                        .aggInterval(AGG_INTERVAL.month)
                        .firstCommentAtRange(ImmutablePair.of(1567296000L, 1569801600L))
                        .calculation(ScmIssueFilter.CALCULATION.count)
                        .build(), null);
        Assert.assertNotNull(result);
        Assertions.assertThat(result.getTotalCount()).isEqualTo(2);
        DbListResponse<DbScmIssue> dbListResponse = scmAggService.list(
                company,
                ScmIssueFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .firstCommentAtRange(ImmutablePair.of(1567296000L, 1569801600L))
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse).isNotNull();
        Assertions.assertThat(dbListResponse.getCount()).isEqualTo(2);
        dbListResponse = scmAggService.list(
                company,
                ScmIssueFilter.builder()
                        .integrationIds(List.of(gitHubIntegrationId))
                        .firstCommentAtRange(ImmutablePair.of(1567296000L, 1569801600L))
                        .issueUpdatedRange(ImmutablePair.of(1567296000L, 1569801600L))
                        .extraCriteria(List.of(ScmIssueFilter.EXTRA_CRITERIA.missed_response_time))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        Assertions.assertThat(dbListResponse.getCount()).isEqualTo(0);
    }

    private int extractDataComponentForDbResults(Date date, int dateComponent, boolean isIntervalQuarter) {
        Calendar calendar = getPGCompatibleCalendar();
        calendar.setTime(date);
        if (isIntervalQuarter) {
            return (calendar.get(dateComponent) / 3) + 1;
        }
        return calendar.get(dateComponent);
    }

    /**
     * By definition, ISO weeks start on Mondays and the first week of a year contains January 4 of that year.
     * In other words, the first Thursday of a year is in week 1 of that year.
     * {@see https://tapoueh.org/blog/2017/06/postgresql-and-the-calendar/}
     */
    @NotNull
    private Calendar getPGCompatibleCalendar() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        return calendar;
    }

}
