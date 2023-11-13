package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

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

@Log4j2
public class ZendeskQueryServiceTest {

    private static final String COMPANY = "test";
    private static final String INTEGRATION_ID = "1";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ZendeskTicketService zendeskTicketService;
    private static Date currentTime;
    private static ZendeskQueryService zendeskQueryService;
    private static ZendeskFieldService zendeskFieldService;
    private static IntegrationService integrationService;
    private static TagsService tagService;
    private static TagItemDBService tagItemDBService;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static JiraIssueService jiraIssueService;
    private static ScmJiraZendeskService scmJiraZendeskService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        long currentTimeMillis = System.currentTimeMillis();
        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        integrationService = jiraTestDbs.getIntegrationService();
        zendeskFieldService = new ZendeskFieldService(dataSource);
        tagService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, DefaultObjectMapper.get());
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);
        zendeskQueryService = new ZendeskQueryService(integrationService, zendeskTicketService, scmJiraZendeskService);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);

        tagService.ensureTableExistence(COMPANY);
        tagItemDBService.ensureTableExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("zendesk")
                .id(INTEGRATION_ID)
                .name("zendesk_test")
                .status("enabled")
                .tags(List.of("tag1"))
                .url("https://test.zendesk.com/")
                .build());
        integrationService.insert(COMPANY, Integration.builder()
                .application("jira")
                .id("2")
                .name("jira test")
                .status("enabled")
                .url("https://test.jira.com/")
                .build());
        integrationService.insert(COMPANY, Integration.builder()
                .application("github")
                .id("3")
                .url("https://api.github.com/")
                .name("github test")
                .status("enabled")
                .build());
        assertThat(integrationService.get(COMPANY, INTEGRATION_ID)).isNotEmpty();
        userIdentityService.ensureTableExistence(COMPANY);
        scmAggService.ensureTableExistence(COMPANY);
        jiraIssueService.ensureTableExistence(COMPANY);
        teamMembersDatabaseService.ensureTableExistence(COMPANY);
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
        String input = ResourceUtils.getResourceAsString("json/databases/zendesk-tickets.json");
        PaginatedResponse<Ticket> zendeskTickets = OBJECT_MAPPER.readValue(input, OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, Ticket.class));
        List<DbZendeskTicket> tickets = zendeskTickets.getResponse().getRecords().stream()
                .map(ticket -> DbZendeskTicket
                        .fromTicket(ticket, INTEGRATION_ID, currentTime, Collections.emptyList(), Collections.emptyList()))
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
        String inputGithub = ResourceUtils.getResourceAsString("json/databases/githubprs.json");
        PaginatedResponse<GithubRepository> prs = OBJECT_MAPPER.readValue(inputGithub,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, GithubRepository.class));
        List<DbRepository> repos = new ArrayList<>();
        prs.getResponse().getRecords().forEach(repo -> {
            repos.add(DbRepository.fromGithubRepository(repo, "3"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), "3", null);
                            scmAggService.insert(COMPANY, tmp);
                        } catch (SQLException throwable) {
                            throwable.printStackTrace();
                        }
                    });
        });

        inputGithub = ResourceUtils.getResourceAsString("json/databases/github_issues.json");
        PaginatedResponse<GithubRepository> issues = OBJECT_MAPPER.readValue(inputGithub,
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

        inputGithub = ResourceUtils.getResourceAsString("json/databases/githubcommits.json");
        PaginatedResponse<GithubRepository> commits = OBJECT_MAPPER.readValue(inputGithub,
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

    @Test
    public void zendeskListTest() throws SQLException {
        List<DbZendeskTicket> zendeskTickets = zendeskQueryService.list(COMPANY,
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(INTEGRATION_ID)).build(), Map.of(), 0, 100).getRecords();
        DbZendeskTicket zendeskTicket = zendeskTickets.stream().findFirst().orElseThrow();
        assertThat(zendeskTicket.getTicketUrl()).isEqualTo("https://test.zendesk.com/agent/tickets/" + zendeskTicket.getTicketId());
    }

    @Test
    public void zendeskJiraListTest() {
        List<ZendeskWithJira> zendeskWithJiras = zendeskQueryService.listZendeskTickets(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(INTEGRATION_ID)).build(), 0, 100, Map.of(), null).getRecords();
        ZendeskWithJira zendeskWithJira = zendeskWithJiras.stream().findFirst().orElseThrow();
        assertThat(zendeskWithJira.getTicketUrl()).isEqualTo("https://test.zendesk.com/agent/tickets/" + zendeskWithJira.getTicketId());
    }

    //     @Test
    public void zendeskJiraWithEscalationTimeReportTest() {
        List<ZendeskWithJira> zendeskWithJiras = zendeskQueryService.listZendeskTicketsWithEscalationTime(COMPANY,
                JiraIssuesFilter.builder()
                        .integrationIds(List.of("2"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(INTEGRATION_ID)).build(), 0, 100, Map.of(), null).getRecords();
        ZendeskWithJira zendeskWithJira = zendeskWithJiras.stream().findFirst().orElseThrow();
        assertThat(zendeskWithJira.getTicketUrl()).isEqualTo("https://test.zendesk.com/agent/tickets/" + zendeskWithJira.getTicketId());
    }

    @Test
    public void zendeskJiraWithEscalationFileReport() {
        List<DbScmFile> dbScmFiles = zendeskQueryService.listEscalatedFiles(COMPANY,
                ScmFilesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID, "2", "3")).build(),
                JiraIssuesFilter.builder()
                        .integrationIds(List.of(INTEGRATION_ID, "2", "3"))
                        .ingestedAt(currentTime.toInstant().getEpochSecond())
                        .hygieneCriteriaSpecs(Map.of())
                        .build(),
                ZendeskTicketsFilter.builder().ingestedAt(currentTime.toInstant().getEpochSecond())
                        .integrationIds(List.of(INTEGRATION_ID, "2", "3")).build(), 0, 100, Map.of(), null).getRecords();
        // FIXME
//        DbScmFile dbScmFile = dbScmFiles.stream().findFirst().orElseThrow();
//        dbScmFile.getZendeskTicketIds().forEach(ticketId -> assertThat(dbScmFile.getZendeskTicketUrls()).contains("https://test.zendesk.com/agent/tickets/" + ticketId));
    }
}
