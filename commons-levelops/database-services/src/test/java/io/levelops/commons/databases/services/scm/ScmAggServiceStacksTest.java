package io.levelops.commons.databases.services.scm;

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
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmIssueMgmtService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.databases.services.ZendeskTicketService;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.Assertions;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmAggServiceStacksTest {

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

        String arrayCatAgg = "CREATE AGGREGATE array_cat_agg(anyarray) (\n" +
                "  SFUNC=array_cat,\n" +
                "  STYPE=anyarray\n" +
                ");";
        dataSource.getConnection().prepareStatement(arrayCatAgg)
                .execute();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();

    }

    @Test
    public void testPrsStackedGroupBy() throws SQLException {
        DbListResponse<DbAggregationResult> resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeUnit("files")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.state), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("closed"));

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeUnit("files")
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.state), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("closed"));


        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.reviewer_count), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(2);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("0", "1"));

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.approver_count), null, true);

        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("0"));
        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.assignee)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.approval_status), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("viraj-levelops", "NONE");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("not reviewed");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.reviewer)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.approval_status), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("NONE", "viraj-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NONE", "not reviewed", "peer approved");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.code_change), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).isEqualTo(List.of("small"));

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.creator), null, true);

        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(2);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.state)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.reviewer), null, true);

        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(2);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("closed"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())
        ).containsAnyOf("viraj-levelops", "NONE");
        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.branch)
                        .sourceBranches(List.of("jenkins"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.approver), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
        Assertions.assertThat(resultDbListResponse.getRecords()).isEmpty();
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .size()).isEqualTo(2);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEmpty();
//        assertThat(resultDbListResponse // FIXME
//                .getRecords()
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .stream().filter(Objects::nonNull).map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())
//        ).containsExactlyInAnyOrder("ivan-levelops", "NONE");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approval_status)
                        .sourceBranches(List.of("jenkins"))
                        .approvalStatuses(List.of("peer approved"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.approver), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
//        Assertions.assertThat(resultDbListResponse.getRecords()
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .size()).isEqualTo(1); // FIXME
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEmpty();
//        assertThat(resultDbListResponse
//                .getRecords()
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .stream().filter(Objects::nonNull).map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())
//        ).containsExactlyInAnyOrder("ivan-levelops");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.creator)
                        .sourceBranches(List.of("jenkins"))
                        .approvalStatuses(List.of("peer approved"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.project), null, true);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(0);
//        Assertions.assertThat(resultDbListResponse.getRecords()
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .size()).isEqualTo(1); // FIXME
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).isEmpty();
//        assertThat(resultDbListResponse // FIXME
//                .getRecords()
//                .stream()
//                .findFirst()
//                .orElseThrow()
//                .getStacks()
//                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
//        ).containsExactlyInAnyOrder("levelops/aggregations-levelops");

        // FIXME
        /*
        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .approvers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                        .codeChangeSizeConfig(Map.of())
                        .aggInterval(AGG_INTERVAL.year)
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.pr_closed), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())
        ).containsExactlyInAnyOrder("2020");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .approvers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "ivan-levelops")))
                        .aggInterval(AGG_INTERVAL.year)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.pr_closed), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList())
        ).containsAnyOf("2020");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.review_type), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NOT_REVIEWED", "PEER_REVIEWED", "SELF_REVIEWED");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .reviewTypes(List.of("not_reviewed"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.review_type), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        Assertions.assertThat(resultDbListResponse.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NOT_REVIEWED");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .codeChangeSizeConfig(Map.of())
                        .excludeReviewTypes(List.of("SElF_REvIEwED"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.review_type), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(1);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).isEqualTo(List.of("levelops/aggregations-levelops"));
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NOT_REVIEWED", "PEER_REVIEWED");

        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver)
                        .codeChangeSizeConfig(Map.of())
                        .excludeReviewTypes(List.of("self_reviewed"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.review_type), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(4);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("NONE", "ivan-levelops", "maxime-levelops", "viraj-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findAny()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NOT_REVIEWED", "PEER_REVIEWED");

        resultDbListResponse =  scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.approver)
                        .excludeApprovers(List.of(userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops")))
                        .codeChangeSizeConfig(Map.of())
                        .excludeReviewTypes(List.of("self_reviewed"))
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.review_type), null);
        Assertions.assertThat(resultDbListResponse.getTotalCount()).isEqualTo(2);
        assertThat(resultDbListResponse.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("NONE", "ivan-levelops");
        assertThat(resultDbListResponse
                .getRecords()
                .stream()
                .findAny()
                .orElseThrow()
                .getStacks()
                .stream().filter(Objects::nonNull).map(DbAggregationResult::getKey).collect(Collectors.toList())
        ).containsAnyOf("NOT_REVIEWED", "PEER_REVIEWED");
        resultDbListResponse = scmAggService.stackedPrsGroupBy(
                company, ScmPrFilter.builder()
                        .across(ScmPrFilter.DISTINCT.project)
                        .sourceBranches(List.of("jenkins"))
                        .approvalStatuses(List.of("peer approved"))
                        .codeChangeSizeConfig(Map.of())
                        .commentDensitySizeConfig(Map.of())
                        .sort(Map.of())
                        .build(), List.of(ScmPrFilter.DISTINCT.assignee), null);

         */
    }

    @Test
    public void testCommitsStackedGroupBy() throws SQLException {
        var res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.project).build(),
                List.of(ScmCommitFilter.DISTINCT.committer), null);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getAdditionalKey()).isEqualTo("piyushkantm");

        res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.project).build(),
                List.of(ScmCommitFilter.DISTINCT.code_category), null);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("new_lines");

        res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.committer).build(),
                List.of(ScmCommitFilter.DISTINCT.code_category), null);
        Assertions.assertThat(res.getRecords().get(0).getAdditionalKey()).isEqualTo("piyushkantm");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("new_lines");

        res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.project).build(),
                List.of(ScmCommitFilter.DISTINCT.repo_id), null);
        Assertions.assertThat(res.getRecords().get(0).getKey()).isEqualTo("levelops/ui-levelops");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("levelops/ui-levelops");

        res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.committer).build(),
                List.of(ScmCommitFilter.DISTINCT.vcs_type), null);
        Assertions.assertThat(res.getRecords().get(0).getAdditionalKey()).isEqualTo("piyushkantm");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("GIT");

        res = scmAggService.stackedCommitsGroupBy(company,
                ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.committer).build(),
                List.of(ScmCommitFilter.DISTINCT.project), null);
        Assertions.assertThat(res.getRecords().get(0).getAdditionalKey()).isEqualTo("piyushkantm");
        Assertions.assertThat(res.getRecords().get(0).getStacks().get(0).getKey()).isEqualTo("levelops/ui-levelops");


    }
}

