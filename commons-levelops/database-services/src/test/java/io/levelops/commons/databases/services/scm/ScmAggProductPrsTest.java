package io.levelops.commons.databases.services.scm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;

@Log4j2
@SuppressWarnings("unused")
public class ScmAggProductPrsTest {

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
    private static TagsService tagService;
    private static TagItemDBService tagItemDBService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static ZendeskFieldService zendeskFieldService;
    private static ProductsDatabaseService productsDatabaseService;
    private static Date currentTime;

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
        productsDatabaseService = new ProductsDatabaseService(dataSource, m);
        tagService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insert(company, Integration.builder()
                .application("zendesk")
                .name("zendesk_test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
        zendeskTicketService.ensureTableExistence(company);
        teamMembersDatabaseService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        productsDatabaseService.ensureTableExistence(company);
        tagService.ensureTableExistence(company);
        tagItemDBService.ensureTableExistence(company);
        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "2", currentTime, JiraIssueParser.JiraParserConfig.builder()
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
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getPullRequests()
                    .forEach(review -> {
                        try {
                            DbScmPullRequest tmp = DbScmPullRequest
                                    .fromGithubPullRequest(review, repo.getId(), "1", null);
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
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getIssues()
                    .forEach(issue -> {
                        DbScmIssue tmp = DbScmIssue
                                .fromGithubIssue(issue, repo.getId(), "1");
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
            repos.add(DbRepository.fromGithubRepository(repo, "1"));
            repo.getEvents().stream()
                    .filter(ev -> "PushEvent".equals(ev.getType()))
                    .flatMap(ev -> ev.getCommits().stream())
                    .forEach(commit -> {
                        DbScmCommit tmp = DbScmCommit
                                .fromGithubCommit(commit, repo.getId(), "1",
                                        currentTime.toInstant().getEpochSecond(), 0L);
                        if (scmAggService.getCommit(company, tmp.getCommitSha(), tmp.getRepoIds(),
                                tmp.getIntegrationId()).isEmpty()) {
                            try {
                                scmAggService.insertCommit(company, tmp);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            DbScmFile.fromGithubCommit(
                                            commit, repo.getId(), "1", currentTime.toInstant().getEpochSecond())
                                    .forEach(scmFile -> scmAggService.insertFile(company, scmFile));
                        }
                    });
        });
        productsDatabaseService.ensureTableExistence(company);
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
    }


    @Test
    public void testGroupByAndCalculatePrs() throws SQLException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        Assert.assertNotNull(userId1);
        //Product with No integ and Filters
        DBOrgProduct productOne = ScmProductServiceUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        //Product with Integ no filters
        DBOrgProduct productWithInteg = ScmProductServiceUtils.getProductWithInteg();
        uuid = productsDatabaseService.insert(company, productWithInteg);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        // Product with integ and filter
        DBOrgProduct dbOrgProduct = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, dbOrgProduct);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);

        //Product with integ and two filters
        DBOrgProduct product = ScmProductServiceUtils.getProductWithIntegAndTwoFilters(List.of(userId1), Collections.emptyList());
        uuid = productsDatabaseService.insert(company, product);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .build(), null).getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("levelops/aggregations-levelops");
        //2 products with one integ and one filter
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilter(List.of(userId1));
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .projects(List.of("levelops/does-not-exist"))
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .build(), null).getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("levelops/aggregations-levelops");
        //2 products with one integ and 2 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilters(List.of(userId1), Collections.emptyList());
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .build(), null).getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .contains("levelops/aggregations-levelops");
        Assertions.assertThat(
                scmAggService.groupByAndCalculatePrsDuration(
                        company, ScmPrFilter.builder()
                                .integrationIds(List.of("1"))
                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                                .across(ScmPrFilter.DISTINCT.pr_merged)
                                .build(), null).getTotalCount()).isEqualTo(1);
        //2 Products with 2 integs and 0 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndNoFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                        .build(), null).getTotalCount())
                .isEqualTo(1);

        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndOneFilter();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.groupByAndCalculatePrs(
                                company, ScmPrFilter.builder()
                                        .across(ScmPrFilter.DISTINCT.project)
                                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                        .build(), null).getTotalCount())
                .isEqualTo(0);

        uuidsList.clear();

        DBOrgProduct productWithIntegAndFilter = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, productWithIntegAndFilter);
        Assertions.assertThat((int) scmAggService.groupByAndCalculatePrs(company,
                        ScmPrFilter.builder()
                                .across(ScmPrFilter.DISTINCT.pr_merged)
                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getKey).count()).isEqualTo(3);
        Assertions.assertThat((int) scmAggService.groupByAndCalculatePrs(company,
                        ScmPrFilter.builder()
                                .across(ScmPrFilter.DISTINCT.pr_created)
                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getKey).count()).isEqualTo(4);
        Assertions.assertThat((int) scmAggService.groupByAndCalculatePrs(company,
                        ScmPrFilter.builder()
                                .across(ScmPrFilter.DISTINCT.pr_updated)
                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                .build(), null).getRecords().stream()
                .map(DbAggregationResult::getKey).count()).isEqualTo(3);
    }

    @Test
    public void testScmPrList() throws SQLException {
        String userId1 = userIdentityService.getUser(company, gitHubIntegrationId, "viraj-levelops");
        Assert.assertNotNull(userId1);
        //Product with No integ and Filters
        DBOrgProduct productOne = ScmProductServiceUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(10);
        //Product with Integ no filters
        DBOrgProduct productWithInteg = ScmProductServiceUtils.getProductWithInteg();
        uuid = productsDatabaseService.insert(company, productWithInteg);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(10);
        // Product with integ and filter
        DBOrgProduct dbOrgProduct = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, dbOrgProduct);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
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
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getRecords().stream().map(DbScmPullRequest::getIntegrationId).findFirst().get())
                .isEqualTo("1");
        //Product with integ and two filters
        DBOrgProduct product = ScmProductServiceUtils.getProductWithIntegAndTwoFilters(List.of(userId1), Collections.emptyList());
        uuid = productsDatabaseService.insert(company, product);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
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
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getRecords().stream().map(DbScmPullRequest::getIntegrationId).findFirst().get())
                .isEqualTo("1");

        //2 products with one integ and one filter
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilter(List.of(userId1));
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
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
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getRecords().stream().map(DbScmPullRequest::getAssignees).collect(Collectors.toList()))
                .contains(List.of("viraj-levelops"));

        //2 products with one integ and 2 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilters(List.of(userId1), Collections.emptyList());
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
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
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getRecords().stream().map(DbScmPullRequest::getIntegrationId).findFirst().get())
                .isEqualTo("1");
        //2 Products with 2 integs and 0 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndNoFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(10);
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndOneFilter();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmPrFilter.builder()
                                                .projects(List.of("levelops/aggregations-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        10)
                                .getTotalCount())
                .isEqualTo(0);
    }
}
