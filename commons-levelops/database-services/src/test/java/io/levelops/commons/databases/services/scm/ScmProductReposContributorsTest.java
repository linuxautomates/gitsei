package io.levelops.commons.databases.services.scm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IssueMgmtTestUtil;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsAgeReportService;
import io.levelops.commons.databases.services.WorkItemsFirstAssigneeReportService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsReportService;
import io.levelops.commons.databases.services.WorkItemsResolutionTimeReportService;
import io.levelops.commons.databases.services.WorkItemsResponseTimeReportService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkItemsStageTimesReportService;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.organization.TeamMembersDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.zendesk.models.Ticket;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unused")
public class ScmProductReposContributorsTest {

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
    private static ZendeskFieldService zendeskFieldService;
    private static ProductsDatabaseService productsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static TeamMembersDatabaseService teamMembersDatabaseService;
    private static WorkItemsService workItemService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static Date currentTime;
    private static Long ingestedAt;

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
        productsDatabaseService = new ProductsDatabaseService(dataSource, m);
        tagService = new TagsService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        zendeskTicketService = new ZendeskTicketService(dataSource, integrationService, zendeskFieldService);
        scmJiraZendeskService = new ScmJiraZendeskService(dataSource, scmAggService, jiraTestDbs.getJiraConditionsBuilder(), zendeskTicketService);
        teamMembersDatabaseService = new TeamMembersDatabaseService(dataSource, m);
        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);

        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                null, null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);

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
        integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("azure_devops test")
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
        workItemService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(company);
        String jiraIn = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> jissues = m.readValue(jiraIn,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);
        jissues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "2", currentTime,
                        JiraIssueParser.JiraParserConfig.builder()
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

        //workitem zone
        input = ResourceUtils.getResourceAsString("json/databases/azure_devops_work_item_fields.json");
        List<WorkItemField> workItemFields = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, WorkItemField.class));
        List<DbWorkItemField> customFieldProperties = workItemFields.stream()
                .map(field -> DbWorkItemField.fromAzureDevopsWorkItemField("4", field))
                .filter(dbWorkItemField -> BooleanUtils.isTrue(dbWorkItemField.getCustom()))
                .collect(Collectors.toList());
        List<IntegrationConfig.ConfigEntry> customFieldConfig = List.of(IntegrationConfig.ConfigEntry.builder()
                        .key("Custom.TestCustomField1")
                        .build(),
                IntegrationConfig.ConfigEntry.builder()
                        .key("Custom.TestCustomField2")
                        .build());

        currentTime = new Date();
        ingestedAt = io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE);
        String workitemsResourcePath = "json/databases/azure_devops_work_items.json";
        IssueMgmtTestUtil.setupWorkItems(company, "4", workItemService,
                workItemTimelineService, null, null, currentTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        Date previousTime = org.apache.commons.lang3.time.DateUtils.addDays(currentTime, -1);
        IssueMgmtTestUtil.setupWorkItems(company, "4", workItemService,
                workItemTimelineService, null, null, previousTime,
                workitemsResourcePath, customFieldConfig, customFieldProperties, List.of("date", "datetime"), userIdentityService);
        //workitem zone end

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
    public void testScmReposList() throws SQLException {
        //Product with No integ and Filters
        DBOrgProduct productOne = ScmProductServiceUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .projects(List.of("levelops/ui-levelops"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        //Product with Integ no filters
        DBOrgProduct productWithInteg = ScmProductServiceUtils.getProductWithInteg();
        uuid = productsDatabaseService.insert(company, productWithInteg);

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(2);

        // Product with integ and filter
        DBOrgProduct dbOrgProduct = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, dbOrgProduct);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
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
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .isEqualTo(List.of());
        //Product with integ and two filters
        DBOrgProduct product = ScmProductServiceUtils.getProductWithIntegAndTwoFilters();
        uuid = productsDatabaseService.insert(company, product);

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .projects(List.of("levelops/ui-levelops"))
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);

        //2 products with one integ and one filter
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilter();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
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
                                                .integrationIds(List.of("1"))
                                                .projects(List.of("levelops/does-not-exist"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/ui-levelops", "levelops/aggregations-levelops"));
        //2 products with one integ and 2 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .projects(List.of("levelops/ui-levelops"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
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
                                                .integrationIds(List.of("1"))
                                                .projects(List.of("levelops/does-not-exist"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList()))
                .isEqualTo(List.of("levelops/aggregations-levelops"));
        //2 Products with 2 integs and 0 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndNoFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }

        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmReposFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(2);
        uuid = productsDatabaseService.insert(company, getProductWithRepoProjects());
        Assertions.assertThat(scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of("1"))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of("1"))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getRecords().stream().map(DbScmRepoAgg::getName).collect(Collectors.toList())).contains("levelops/aggregations-levelops");

        Assertions.assertThat(scmAggService.list(
                                company,
                                ScmReposFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                        .build(),
                                Map.of(),
                                null,
                                0,
                                1000)
                        .getRecords().stream().map(DbScmRepoAgg::getNumCommits).collect(Collectors.toList()))
                .isEqualTo(List.of(3, 0));

        Assertions.assertThat(scmAggService.list(
                                company,
                                ScmReposFilter.builder()
                                        .integrationIds(List.of("1"))
                                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                        .build(),
                                Map.of(),
                                null,
                                0,
                                1000)
                        .getRecords().stream().map(DbScmRepoAgg::getNumPrs).collect(Collectors.toList()))
                .isEqualTo(List.of(0, 10));


        DbListResponse<DbScmRepoAgg> actual = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of("1"))
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        DbListResponse<DbScmRepoAgg> expected = DbListResponse.of(List.of(
                        DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3).numPrs(0)
                                .numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build(),
                        DbScmRepoAgg.builder().id("levelops/aggregations-levelops").name("levelops/aggregations-levelops").numCommits(0).numPrs(10)
                                .numAdditions(0).numDeletions(0).numChanges(0).numJiraIssues(1).numWorkitems(1).build()),
                2);
        assertThat(actual.getRecords()).containsExactlyInAnyOrderElementsOf(expected.getRecords());

        actual = scmAggService.list(
                company,
                ScmReposFilter.builder()
                        .integrationIds(List.of("1"))
                        .projects(List.of("levelops/ui-levelops"))
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0))))
                        .build(),
                Map.of(),
                null,
                0,
                1000);
        expected = DbListResponse.of(List.of(
                        DbScmRepoAgg.builder().id("levelops/ui-levelops").name("levelops/ui-levelops").numCommits(3).numPrs(0)
                                .numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()),
                1);
        assertThat(actual.getRecords()).containsExactlyInAnyOrderElementsOf(expected.getRecords());
    }

    @Test
    public void testScmContributorsList() throws SQLException {
        //Product with No integ and Filters
        DBOrgProduct productOne = ScmProductServiceUtils.getProductWithNoIntegAndFilter();
        String uuid = productsDatabaseService.insert(company, productOne);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .integrationIds(List.of("1"))
                                                .includeIssues(true)
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(3);
        //Product with Integ no filters
        DBOrgProduct productWithInteg = ScmProductServiceUtils.getProductWithInteg();
        uuid = productsDatabaseService.insert(company, productWithInteg);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .committers(List.of("piyushkantm"))
                                                .projects(List.of("levelops/ui-levelops"))
                                                .includeIssues(true)
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);
        // Product with integ and filter
        DBOrgProduct dbOrgProduct = ScmProductServiceUtils.getProductWithIntegAndFilter();
        uuid = productsDatabaseService.insert(company, dbOrgProduct);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .includeIssues(true)
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);
        //Product with integ and two filters
        DBOrgProduct product = ScmProductServiceUtils.getProductWithIntegAndTwoFilters();
        uuid = productsDatabaseService.insert(company, product);
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                                .includeIssues(true)
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(0);
        //2 products with one integ and one filter
        List<String> uuidsList = new ArrayList<>();
        List<DBOrgProduct> orgProductList = ScmProductServiceUtils.getTwoProductWithIntegAndFilter();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .includeIssues(true)
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
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
                                                .committers(List.of("piyushkantm"))
                                                .includeIssues(true)
                                                .projects(List.of("levelops/does-not-exist"))
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),

                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
                .isEqualTo(List.of("piyushkantm"));

        //2 products with one integ and 2 filters
        uuidsList.clear();
        orgProductList = getProductWithIntegAndTwoFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .committers(List.of("piyushkantm"))
                                                .includeIssues(true)
                                                .projects(List.of("levelops/ui-levelops"))
                                                .integrationIds(List.of("1"))
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getTotalCount())
                .isEqualTo(1);
        //2 Products with 2 integs and 0 filters
        uuidsList.clear();
        orgProductList = ScmProductServiceUtils.getTwoProductWithTwoIntegAndNoFilters();
        for (DBOrgProduct prod : orgProductList) {
            uuidsList.add(productsDatabaseService.insert(company, prod));
        }
        Assertions.assertThat(
                        scmAggService.list(
                                        company,
                                        ScmContributorsFilter.builder()
                                                .integrationIds(List.of("1"))
                                                .includeIssues(true)
                                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                                .orgProductIds(Set.of(UUID.fromString(uuidsList.get(0)), UUID.fromString(uuidsList.get(1))))
                                                .build(),
                                        Map.of(),
                                        null,
                                        0,
                                        1000)
                                .getRecords().stream().map(DbScmContributorAgg::getName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("piyushkantm", "viraj-levelops", "ivan-levelops");
        uuid = productsDatabaseService.insert(company, getProductWithContributorProjects());
        Assertions.assertThat(scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .integrationIds(List.of("1"))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .includeIssues(true)
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .build(),
                Map.of(),
                null,
                0,
                1000).getTotalCount()).isEqualTo(1);
        Assertions.assertThat(scmAggService.list(
                        company,
                        ScmContributorsFilter.builder()
                                .integrationIds(List.of("1"))
                                .orgProductIds(Set.of(UUID.fromString(uuid)))
                                .includeIssues(true)
                                .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                                .build(),
                        Map.of(),
                        null,
                        0,
                        1000).getRecords().stream().map(DbScmContributorAgg::getId).collect(Collectors.toList()))
                .contains(userIdOf("piyushkantm"));

        DbListResponse<DbScmContributorAgg> actual = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .integrationIds(List.of("1"))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .includeIssues(true)
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .build(),
                Map.of("num_commits", SortingOrder.DESC),
                null,
                0,
                1000);

        DbListResponse<DbScmContributorAgg> expected = DbListResponse.of(List.of(
                        DbScmContributorAgg.builder()
                                .id(userIdOf("piyushkantm"))
                                .name("piyushkantm")
                                .fileTypes(List.of("jsx", "js"))
                                .techBreadth(List.of("Javascript/Typescript"))
                                .repoBreadth(List.of("levelops/ui-levelops"))
                                .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()),
                1);
        DefaultObjectMapper.prettyPrint(actual.getRecords());
        compareAggResults(actual.getRecords(), expected.getRecords());

        actual = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .projects(List.of("levelops/aggregations-levelops"))
                        .integrationIds(List.of("1"))
                        .includeIssues(true)
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .build(),
                Map.of("num_commits", SortingOrder.DESC),
                null,
                0,
                1000);
        expected = DbListResponse.of(List.of(
                        DbScmContributorAgg.builder()
                                .id(userIdOf("piyushkantm"))
                                .name("piyushkantm")
                                .fileTypes(List.of("jsx", "js"))
                                .techBreadth(List.of("Javascript/Typescript"))
                                .repoBreadth(List.of("levelops/ui-levelops"))
                                .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()
                ),
                1);
        DefaultObjectMapper.prettyPrint(actual.getRecords());
        compareAggResults(actual.getRecords(), expected.getRecords());

        actual = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .integrationIds(List.of("1"))
                        .includeIssues(true)
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .build(),
                Map.of("num_additions", SortingOrder.DESC),
                null,
                0,
                1000);
        expected = DbListResponse.of(List.of(
                        DbScmContributorAgg.builder()
                                .id(userIdOf("piyushkantm"))
                                .name("piyushkantm")
                                .fileTypes(List.of("jsx", "js"))
                                .techBreadth(List.of("Javascript/Typescript"))
                                .repoBreadth(List.of("levelops/ui-levelops"))
                                .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()
                ),
                4);
        compareAggResults(actual.getRecords(), expected.getRecords());

        actual = scmAggService.list(
                company,
                ScmContributorsFilter.builder()
                        .integrationIds(List.of("1"))
                        .includeIssues(true)
                        .dataTimeRange(ImmutablePair.of(0L, System.currentTimeMillis()))
                        .orgProductIds(Set.of(UUID.fromString(uuid)))
                        .build(),
                Map.of("num_deletions", SortingOrder.ASC),
                null,
                0,
                1000);
        expected = DbListResponse.of(List.of(
                        DbScmContributorAgg.builder()
                                .id(userIdOf("piyushkantm"))
                                .name("piyushkantm")
                                .fileTypes(List.of("jsx", "js"))
                                .techBreadth(List.of("Javascript/Typescript"))
                                .repoBreadth(List.of("levelops/ui-levelops"))
                                .numRepos(1).numCommits(3).numPrs(0).numAdditions(219).numDeletions(177).numChanges(396).numJiraIssues(1).numWorkitems(1).build()),
                4);
        compareAggResults(actual.getRecords(), expected.getRecords());
    }

    public static List<DBOrgProduct> getProductWithIntegAndTwoFilters() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 4")
                        .description("This is a sample product")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("modules", List.of("src")))
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 6")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test 2")
                                        .type("github")
                                        .build()
                        ))
                        .build());
    }

    public static DBOrgProduct getProductWithRepoProjects() {
        return DBOrgProduct.builder()
                .name("Product Id 2")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of("projects", List.of("levelops/aggregations-levelops")))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithContributorProjects() {
        return DBOrgProduct.builder()
                .name("Product Id")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of("projects", List.of("levelops/ui-levelops")))
                                .build()
                ))
                .build();
    }

    private String userIdOf(String cloudId) {
        return userIdentityService.getUser(company, gitHubIntegrationId, cloudId);
    }

    private void compareAggResults(List<DbScmContributorAgg> actualList, List<DbScmContributorAgg> expectedList) {
        final Iterator<DbScmContributorAgg> expectedIter = expectedList.iterator();
        final Iterator<DbScmContributorAgg> actualIter = actualList.iterator();
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
}
