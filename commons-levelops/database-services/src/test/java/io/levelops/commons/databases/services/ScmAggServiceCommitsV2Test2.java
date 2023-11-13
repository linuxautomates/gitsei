package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

public class ScmAggServiceCommitsV2Test2 {
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
        scmAggService = new ScmAggService(dataSource, userIdentityService, 10);
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
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").toEpochMilli()), Calendar.DATE);
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
    public void test() throws IOException, SQLException {
        List<IntegrationConfig.RepoConfigEntry> configEntries = List.of(IntegrationConfig.RepoConfigEntry.builder()
                .repoId("bcopa").pathPrefix("//bcopa/").build());

        String input = ResourceUtils.getResourceAsString("json/databases/broadcom_helix_commits_2.json");
        PaginatedResponse<HelixCoreChangeList> helixChangelist = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, HelixCoreChangeList.class));
        helixChangelist.getResponse().getRecords().forEach(changeList -> {
            List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, "1", configEntries);
            Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
            DbScmCommit helixCoreCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, "1");
            if (CollectionUtils.isNotEmpty(repoIds) && scmAggService.getCommit(company, helixCoreCommit.getCommitSha(),
                    helixCoreCommit.getRepoIds(), "1").isEmpty() && dbScmFiles.size() > 0) {
                try {
                    String id = scmAggService.insertV2(company, helixCoreCommit, dbScmFiles);
                    if (StringUtils.isNotEmpty(id)) {
                        DbScmCommit latestCreatedAtScmCommit = scmAggService.getLatestCreatedAtScmCommit(company).get();
                        //ToDo: VA Fix Assertion
                        //Assert.assertEquals(latestCreatedAtScmCommit.getCommitSha(), helixCoreCommit.getCommitSha());
                        //Assert.assertEquals(latestCreatedAtScmCommit.getId(), id);
                        //Assert.assertEquals(latestCreatedAtScmCommit.getCommittedAt().longValue(), helixCoreCommit.getCommittedAt().longValue());
                        //Thread.sleep(TimeUnit.SECONDS.toSeconds(1));
                    }
                    scmAggService.insertFilesV2(company, helixCoreCommit.getIntegrationId(), helixCoreCommit.getCommitSha(), dbScmFiles);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
