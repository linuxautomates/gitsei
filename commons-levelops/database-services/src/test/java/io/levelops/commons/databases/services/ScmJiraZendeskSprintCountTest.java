package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
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
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmJiraZendeskSprintCountTest {

    private static final String COMPANY = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static ScmJiraZendeskService scmJiraZendeskService;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        ScmAggService scmAggService = new ScmAggService(dataSource, userIdentityService);
        ZendeskFieldService zendeskFieldService = new ZendeskFieldService(dataSource);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, OBJECT_MAPPER);
        JiraIssueService jiraIssueService = jiraTestDbs.getJiraIssueService();
        ZendeskTicketService zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
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
        teamMembersDatabaseService.ensureTableExistence(COMPANY);
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

                switch (issue.getKey()) {
                    case "LEV-888":
                        tmp = tmp.toBuilder().sprintIds(List.of(1, 2)).issueResolvedAt(1589409818L).build();
                        break;
                    case "LEV-1202":
                        tmp = tmp.toBuilder().sprintIds(List.of(2, 3)).issueResolvedAt(1589409818L).build();
                        break;
                    case "LEV-1005":
                        tmp = tmp.toBuilder().sprintIds(List.of(3)).issueResolvedAt(1589409818L).build();
                        break;
                    case "LEV-889":
                        tmp = tmp.toBuilder().sprintIds(List.of(4)).issueResolvedAt(1589409818L).build();
                        break;
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
                                throw new RuntimeStreamException(e);
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), "1", currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(COMPANY, scmFile));
                        }
                    });
        });
        repositoryService.batchUpsert(COMPANY, repos);

        List<DbJiraSprint> sprints = List.of(
                DbJiraSprint.builder().name("Sprint 1").sprintId(1).state("CLOSED").integrationId(1).startDate(1617290195L).endDate(1617290295L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 2").sprintId(2).state("CLOSED").integrationId(1).startDate(1617290285L).endDate(1617290395L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Sprint 3").sprintId(3).state("CLOSED").integrationId(1).startDate(1617290375L).endDate(1617290495L).updatedAt(currentTime.toInstant().getEpochSecond()).build(),
                DbJiraSprint.builder().name("Current Sprint").sprintId(4).state("ACTIVE").integrationId(1).startDate(1617290375L).updatedAt(currentTime.toInstant().getEpochSecond()).build()
        );
        sprints.forEach(s -> jiraIssueService.insertJiraSprint(COMPANY, s));
    }

    /**
     * setup:
     * <p>
     * LEV-1005 - {3}
     * LEV-889 - {4}
     * LEV-1202 - {2, 3}
     * LEV-888 - {1, 2}
     */
    @Test
    public void test() {
        List<DbAggregationResult> records = scmJiraZendeskService.groupJiraTickets(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of("1", "2", "3"),
                true,
                true, Map.of(), null).getRecords();

        assertThat(records.size()).isEqualTo(0);
        // FIXME
//        assertThat(records.get(0).getKey()).isEqualTo("TO DO");
//        assertThat(records.get(0).getTotalTickets()).isEqualTo(2);

        records = scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(),
                List.of("1", "2", "3"),
                true, Map.of(), null).getRecords();

        assertThat(records.size()).isEqualTo(0);
//        assertThat(records.get(0).getTotalTickets()).isEqualTo(2); // FIXME

        List<JiraWithGitZendesk> jiraTickets = scmJiraZendeskService.listJiraTickets(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .sprintCount(3)
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(),
                List.of("1", "2", "3"),
                true,
                0, 100, Map.of(), null).getRecords();

        assertThat(jiraTickets.size()).isEqualTo(0);

        Set<String> jiraKeys = jiraTickets.stream().map(JiraWithGitZendesk::getKey).collect(Collectors.toSet());
        assertThat(jiraKeys).isEmpty();

        List<DbAggregationResult> zendeskEscalationReport = scmJiraZendeskService.getZendeskEscalationTimeReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(), Map.of(), null).getRecords();

        assertThat(zendeskEscalationReport.size()).isEqualTo(2);

        List<DbAggregationResult> zendeskTicketsWithJira = scmJiraZendeskService.groupZendeskTicketsWithJiraLink(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .sprintStates(List.of("CLOSED"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), Map.of(), null).getRecords();

        assertThat(zendeskTicketsWithJira.size()).isEqualTo(2);

        List<ZendeskWithJira> zendeskTicketWithEscalationTime = scmJiraZendeskService.listZendeskTicketsWithEscalationTime(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 100, Map.of(), null).getRecords();

        assertThat(zendeskTicketWithEscalationTime.size()).isEqualTo(2);

        List<DbAggregationResult> topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("1"))
                        .sprintCount(1)
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
                List.of("1", "2", "3"), Map.of(), null).getRecords();

        assertThat(topCommitters.size()).isEqualTo(0);

        topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(1)
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
                List.of("1", "2", "3"), Map.of(), null).getRecords();

        assertThat(topCommitters.size()).isEqualTo(0);

        topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("2"))
                        .sprintCount(3)
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
                List.of("1", "2", "3"), Map.of(), null).getRecords();

        assertThat(topCommitters.size()).isEqualTo(0);
    }

    @Test
    public void testSortingOrder() {

        List<DbAggregationResult> topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("2"))
                        .sprintCount(3)
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
                List.of("1", "2", "3"), Map.of("filename", SortingOrder.ASC), null).getRecords();

        // FIXME
//        assertThat(topCommitters.get(0).getKey()).isEqualTo("src/configurations/pages/configuration-page.component.jsx");
//        assertThat(topCommitters.get(10).getKey()).isEqualTo("src/templates/pages/template-menu-page.component.jsx");

        topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintIds(List.of("2"))
                        .sprintCount(3)
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
                List.of("1", "2", "3"), Map.of("filename", SortingOrder.DESC), null).getRecords();

        // FIXME
//        assertThat(topCommitters.get(0).getKey()).isEqualTo("src/templates/pages/template-menu-page.component.jsx");
//        assertThat(topCommitters.get(10).getKey()).isEqualTo("src/configurations/pages/configuration-page.component.jsx");

        List<ZendeskWithJira> zendeskTicketWithEscalationTime = scmJiraZendeskService.listZendeskTicketsWithEscalationTime(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 100, Map.of("escalation_time", SortingOrder.ASC), null).getRecords();
        assertThat(zendeskTicketWithEscalationTime.get(0).getEscalationTime())
                .isLessThan(zendeskTicketWithEscalationTime.get(1).getEscalationTime());

        zendeskTicketWithEscalationTime = scmJiraZendeskService.listZendeskTicketsWithEscalationTime(COMPANY, JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .build(), 0, 100, Map.of("escalation_time", SortingOrder.DESC), null).getRecords();
        assertThat(zendeskTicketWithEscalationTime.get(0).getEscalationTime())
                .isGreaterThan(zendeskTicketWithEscalationTime.get(1).getEscalationTime());

        List<DbAggregationResult> zendeskEscalationReport = scmJiraZendeskService.getZendeskEscalationTimeReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(), Map.of("status", SortingOrder.ASC), null).getRecords();

        assertThat(zendeskEscalationReport.get(0).getKey()).isEqualTo("NEW");
        assertThat(zendeskEscalationReport.get(1).getKey()).isEqualTo("OPEN");

        zendeskEscalationReport = scmJiraZendeskService.getZendeskEscalationTimeReport(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .sprintCount(3)
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.status)
                        .build(), Map.of("status", SortingOrder.DESC), null).getRecords();

        assertThat(zendeskEscalationReport.get(0).getKey()).isEqualTo("OPEN");
        assertThat(zendeskEscalationReport.get(1).getKey()).isEqualTo("NEW");
    }
}
