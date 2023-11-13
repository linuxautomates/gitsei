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
import org.junit.BeforeClass;
import org.junit.ClassRule;

import javax.sql.DataSource;
import java.io.IOException;
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

public class ScmJiraZendeskServiceTest {

    private static final String COMPANY = "test";

    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ZendeskTicketService zendeskTicketService;
    private static ScmJiraZendeskService scmJiraZendeskService;
    private static ScmAggService scmAggService;
    private static JiraIssueService jiraIssueService;
    private static ZendeskFieldService zendeskFieldService;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
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
    }

    //     @Test
    public void test() {
        assertThat(scmJiraZendeskService.groupJiraTickets(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true,
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(scmJiraZendeskService.groupJiraTickets(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of(),
                        true,
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(2);
//        assertThat(scmJiraZendeskService.groupJiraTickets(COMPANY,
//                JiraIssuesFilter.builder()
//                        .integrationIds(List.of("1", "2", "3"))
//                        .ingestedAt(currentTime.toInstant().getEpochSecond())
//                        .hygieneCriteriaSpecs(Map.of())
//                        .build(),
//                ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
//                        .ingestedAt(currentTime.toInstant().getEpochSecond())
//                        .build(),
//                List.of("1", "2", "3"),
//                true,
//                false)
//                .getTotalCount()).isEqualTo(1);
        DbListResponse<DbAggregationResult> topCommitters = scmJiraZendeskService.getTopCommitters(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("1", "2", "3"))
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
        assertThat(topCommitters.getTotalCount()).isEqualTo(29);
        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        false, Map.of(), null)
                .getTotalCount()).isEqualTo(1);
        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("4"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        true, Map.of(), null)
                .getTotalCount()).isEqualTo(0);
        assertThat(scmJiraZendeskService.resolvedTicketsTrendReport(COMPANY,
                        JiraIssuesFilter.builder()
                                .integrationIds(List.of("1", "2", "3"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .hygieneCriteriaSpecs(Map.of())
                                .build(),
                        ZendeskTicketsFilter.builder().integrationIds(List.of("4"))
                                .ingestedAt(currentTime.toInstant().getEpochSecond())
                                .build(),
                        List.of("1", "2", "3"),
                        false, Map.of(), null)
                .getTotalCount()).isEqualTo(0);
    }
}
