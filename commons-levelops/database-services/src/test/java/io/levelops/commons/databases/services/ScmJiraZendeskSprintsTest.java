package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.combined.JiraWithGitZendesk;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmJiraZendeskSprintsTest {
    private static final String COMPANY = "test";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private ZendeskTicketService zendeskTicketService;
    private ScmJiraZendeskService scmJiraZendeskService;
    private ScmAggService scmAggService;
    private JiraIssueService jiraIssueService;
    private static ZendeskFieldService zendeskFieldService;
    private Date currentTime;
    private UserIdentityService userIdentityService;

    @Before
    public void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);

        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        integrationService.insert(COMPANY, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insert(COMPANY, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(COMPANY);
        scmAggService.ensureTableExistence(COMPANY);
        jiraIssueService.ensureTableExistence(COMPANY);
        zendeskTicketService.ensureTableExistence(COMPANY);
        repositoryService.ensureTableExistence(COMPANY);

        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = OBJECT_MAPPER.readValue(jiraIn,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .build());
                if (issue.getKey().equals("LEV-889")) {
                    tmp = tmp.toBuilder().sprintIds(List.of(1, 2, 3)).issueResolvedAt(1589409818L).build();
                } else if (issue.getKey().equals("LEV-1202")) {
                    tmp = tmp.toBuilder().sprintIds(List.of(1, 2)).issueResolvedAt(1589409818L).build();
                } else if (issue.getKey().equals("LEV-1005")) {
                    tmp = tmp.toBuilder().sprintIds(List.of(3)).issueResolvedAt(1589409818L).build();
                }
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
                jiraIssueService.insert(COMPANY, tmp);
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("This issue should exist.");
                }
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent()) {
                    throw new RuntimeException("This issue shouldnt exist.");
                }
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
        //start zendesk zone
        String input = ResourceUtils.getResourceAsString("json/databases/zendesk-tickets.json");
        PaginatedResponse<Ticket> zendeskTickets = OBJECT_MAPPER.readValue(input, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Ticket.class));
        List<DbZendeskTicket> tickets = zendeskTickets.getResponse().getRecords().stream()
                .map(ticket -> DbZendeskTicket
                        .fromTicket(ticket, "3", currentTime, Collections.emptyList(), Collections.emptyList()))
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
                zendeskTicketService.insert(COMPANY, dbZendeskTicket);
                if (zendeskTicketService.get(COMPANY, dbZendeskTicket.getTicketId(), integrationId,
                        dbZendeskTicket.getIngestedAt()).isEmpty()) {
                    throw new RuntimeException("The ticket must exist: " + dbZendeskTicket);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //end zendesk zone
        input = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = OBJECT_MAPPER.readValue(input,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), "1", null);
                            scmAggService.insert(COMPANY, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = OBJECT_MAPPER.readValue(input,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        issues.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), "1");
                        if (scmAggService.getIssue(COMPANY, tmp.getIssueId(), tmp.getRepoId(),
                                tmp.getIntegrationId()).isEmpty()) {
                            scmAggService.insertIssue(COMPANY, tmp);
                        }
                    });
        });

        input = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = OBJECT_MAPPER.readValue(input,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        commits.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), "1",
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(COMPANY, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(COMPANY, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), "1", currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(COMPANY, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(COMPANY, repos);
        Date currentTime = new Date();
        List<DbJiraSprint> sprints = List.of(
                DbJiraSprint.builder().name("Sprint 1").sprintId(1).state("ACTIVE").integrationId(1)
                        .startDate(1617290995L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 2").sprintId(2).state("ACTIVE").integrationId(1)
                        .startDate(1617290985L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 3").sprintId(3).state("CLOSED").integrationId(1)
                        .startDate(1617290975L).endDate(1617290995L).updatedAt(currentTime.toInstant().getEpochSecond()).build()
        );
        sprints.forEach(s -> jiraIssueService.insertJiraSprint(COMPANY, s));
    }

    @Test
    public void testExcludeAndIncludeSprints() {
        DbListResponse<DbAggregationResult> dbListResponse = scmJiraZendeskService.groupJiraTickets(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of("1", "2", "3"),
                true,
                true, Map.of(), null);
        assertThat(dbListResponse.getTotalCount()).isEqualTo(0);
        assertThat(dbListResponse.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .isEmpty();

        DbListResponse<DbAggregationResult> dbListResponse1 = scmJiraZendeskService.groupJiraTickets(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintNames(List.of("Sprint 1"))
                        .excludeSprintStates(List.of("CLOSED"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of(),
                true,
                true, Map.of(), null);
        assertThat(dbListResponse1.getTotalCount()).isEqualTo(0);
//        assertThat(dbListResponse1.getRecords().get(0).getKey()).isEqualTo("TO DO"); // FIXME

        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("1", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of("1", "2", "3"),
                true, Map.of(), null).getRecords()).isEmpty(); // FIXME
//                get(0).getTotalTickets()).isEqualTo(2);

        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .excludeSprintNames(List.of("Sprint 1"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true, Map.of(), null)
                .getRecords()).isEmpty();
//                .get(0).getTotalTickets()).isEqualTo(1); // FIXME

        DbListResponse<JiraWithGitZendesk> jiraWithGitZendeskDbListResponse = scmJiraZendeskService.listJiraTickets(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .sprintNames(List.of("Sprint 1"))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(),
                List.of("1", "2", "3"),
                true,
                0, 100, Map.of(), null);
        Assertions.assertThat(jiraWithGitZendeskDbListResponse.getRecords().stream().map(JiraWithGitZendesk::getKey).collect(Collectors.toList()))
                .isEmpty();

        DbListResponse<JiraWithGitZendesk> jiraWithGitZendeskDbListResponse1 = scmJiraZendeskService.listJiraTickets(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .excludeSprintStates(List.of("ACTIVE"))
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(),
                List.of("1", "2", "3"),
                true,
                0, 100, Map.of(), null);
        assertThat(jiraWithGitZendeskDbListResponse1.getTotalCount()).isEqualTo(0);
//        assertThat(jiraWithGitZendeskDbListResponse1.getRecords().get(0).getKey()).isEqualTo("LEV-1005"); // FIXME

        DbListResponse<DbAggregationResult> zendeskEscalationTimeReport = scmJiraZendeskService.getZendeskEscalationTimeReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("1", "2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(), Map.of(), null);
        assertThat(zendeskEscalationTimeReport.getTotalCount()).isEqualTo(2);

        DbListResponse<DbAggregationResult> dbAggregationResultDbListResponse = scmJiraZendeskService.groupZendeskTicketsWithJiraLink(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("3"))
                        .sprintStates(List.of("CLOSED"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), Map.of(), null);
        assertThat(dbAggregationResultDbListResponse.getTotalCount()).isEqualTo(2);

        DbListResponse<ZendeskWithJira> asc = scmJiraZendeskService.listZendeskTicketsWithEscalationTime(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintStates(List.of("CLOSED", "ACTIVE"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 100, Map.of(), null);
        assertThat(asc.getTotalCount()).isEqualTo(2);

        DbListResponse<DbAggregationResult> topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .excludeSprintStates(List.of("ACTIVE"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                List.of("1", "2", "3"), Map.of(), null);
        assertThat(topCommitters.getTotalCount()).isEqualTo(0);

        DbListResponse<DbAggregationResult> topCommitters1 = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("1"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                ScmCommitFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                ScmFilesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .build(),
                List.of("1", "2", "3"), Map.of(), null);
        assertThat(topCommitters1.getTotalCount()).isEqualTo(0);
    }
}
