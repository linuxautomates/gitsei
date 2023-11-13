package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTestUtils;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.web.exceptions.NotFoundException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BaWorkItemsScmAggsQueryBuilderTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;
    private static final WorkItemsFilter BASE_WORKITEMS_FILTER = WorkItemsFilter.builder()
            .ingestedAt(INGESTED_AT)
            .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
            .ticketCategorizationFilters(List.of(
                    WorkItemsFilter.TicketCategorizationFilter.builder()
                            .index(0)
                            .name("cat1")
                            .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                            .build(),
                    WorkItemsFilter.TicketCategorizationFilter.builder()
                            .index(1)
                            .name("cat2")
                            .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                            .build()
            ))
            .build();

    DataSource dataSource;
    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    BaWorkItemsAggsDatabaseService baWorkItemsAggsDatabaseService;
    WorkItemsService workItemsService;
    ScmAggService scmAggService;
    String integrationIdWorkItems;
    String integrationIdScm;
    UserIdentityService userIdentityService;
    WorkItemFieldsMetaService workItemFieldsMetaService;
    WorkItemTimelineService workItemTimelineService;
    IssuesMilestoneService issuesMilestoneService;
    IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    WorkItemsPrioritySLAService workItemsPrioritySLAService;

    @Before
    public void setUp() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        WorkItemTestUtils.TestDbs testDbs = WorkItemTestUtils.initDbServices(dataSource, COMPANY);

        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        userIdentityService.ensureTableExistence(COMPANY);
        scmAggService.ensureTableExistence(COMPANY);
        dataSource.getConnection().prepareStatement(arrayUniq).execute();

        integrationIdWorkItems = testDbs.getIntegrationService().insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("azure_devops_test")
                .status("enabled")
                .build());
        integrationIdScm = testDbs.getIntegrationService().insert(COMPANY, Integration.builder()
                .application("github")
                .name("github_test")
                .status("enabled")
                .build());

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemsService = testDbs.getWorkItemsService();
        workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issuesMilestoneService.ensureTableExistence(COMPANY);
        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService = testDbs.getWorkItemsPrioritySLAService();
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder = new BaWorkItemsAggsQueryBuilder(workItemFieldsMetaService);
        BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder = new BaWorkItemsAggsActiveWorkQueryBuilder(baWorkItemsAggsQueryBuilder);
        BaWorkItemsScmAggsQueryBuilder baWorkItemsScmAggsQueryBuilder = new BaWorkItemsScmAggsQueryBuilder(baWorkItemsAggsQueryBuilder, scmAggService);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        baWorkItemsAggsDatabaseService = new BaWorkItemsAggsDatabaseService(dataSource, baWorkItemsAggsQueryBuilder,
                baWorkItemsAggsActiveWorkQueryBuilder, baWorkItemsScmAggsQueryBuilder, ticketCategorizationSchemeDatabaseService);

        ticketCategorizationSchemeDatabaseService = Mockito.mock(TicketCategorizationSchemeDatabaseService.class);
        when(ticketCategorizationSchemeDatabaseService.get(eq(COMPANY), anyString())).thenReturn(Optional.of(TicketCategorizationScheme.builder()
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .categories(Map.of(
                                "cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("cat1")
                                        .index(0)
                                        .build(),
                                "cat2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("cat2")
                                        .index(1)
                                        .build()))
                        .build())
                .build()));
        baWorkItemsAggsDatabaseService = new BaWorkItemsAggsDatabaseService(dataSource, baWorkItemsAggsQueryBuilder,
                baWorkItemsAggsActiveWorkQueryBuilder, baWorkItemsScmAggsQueryBuilder, ticketCategorizationSchemeDatabaseService);
    }

    public static DbWorkItem buildBaseWorkItem(String integrationId) {
        return DbWorkItem.builder()
                .workItemId("123")
                .integrationId(integrationId)
                .project("Agile-Project")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("Done")
                .statusCategory("Done")
                .workItemType("Bug")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .workItemCreatedAt(DateUtils.toTimestamp(Instant.now()))
                .workItemUpdatedAt(DateUtils.toTimestamp(Instant.now()))
                .ingestedAt(INGESTED_AT)
                .reporter("gandalf")
                .assignee("frodo")
                .build();
    }

    private void insertMockData() throws SQLException {
        // -- users
        DbScmUser frodo = DbScmUser.builder().integrationId(integrationIdScm).cloudId("156e0354-2afd-42e5-ac80-8097d3eef5f4").displayName("frodo").originalDisplayName("frodo").build();
        DbScmUser legolas = DbScmUser.builder().integrationId(integrationIdScm).cloudId("30a20e36-5917-453a-9662-9a8b2905ac01").displayName("legolas").originalDisplayName("legolas").build();
        DbScmUser aragorn = DbScmUser.builder().integrationId(integrationIdScm).cloudId("ff7afc73-a5a1-4e57-b715-59533a065b87").displayName("aragorn").originalDisplayName("aragorn").build();
        DbScmUser maxime = DbScmUser.builder().integrationId(integrationIdScm).cloudId("5cefa2f2-0928-489c-a233-bed5ab4c608e").displayName("maxime").originalDisplayName("maxime").build();

        issuesMilestoneService.insert(COMPANY, DbIssuesMilestone.builder()
                .integrationId(NumberUtils.toInteger(integrationIdWorkItems))
                .name("sprint42")
                .fieldType("sprint")
                .fieldValue("9e01d08b-29c2-482e-9ff2-2256dc312911")
                .state("current")
                .parentFieldValue("Agile-Project")
                .projectId("79e659ee-dcfc-42f7-9f90-7302057683be")
                .attributes(Map.of("project", "project-test-2"))
                .startDate(DateUtils.toTimestamp(Instant.now()))
                .build());

        issueMgmtSprintMappingDatabaseService.upsert(COMPANY, DbIssueMgmtSprintMapping.builder()
                .integrationId(integrationIdWorkItems)
                .workitemId("3")
                .sprintId("Agile-Project\\sprint42")
                .addedAt(1628768623L)
                .planned(false)
                .delivered(false)
                .outsideOfSprint(true)
                .ignorableWorkitemType(false)
                .storyPointsPlanned((float) 0)
                .storyPointsDelivered((float) 0)
                .build());

        // ---- ingested_at = INGESTED_AT
        DbWorkItem workItem1 = buildBaseWorkItem(integrationIdWorkItems).toBuilder().ingestedAt(INGESTED_AT).build();
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("1").workItemType("Bug").storyPoint(20F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600000000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("2").workItemType("Task").storyPoint(5F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600200000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("3").workItemType("Task")
                .statusCategory("InProgress").build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("4").workItemType("Epic")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600300000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("5").workItemType("Task")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("6").workItemType("Bug").assignee("aragorn").storyPoint(100F)
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600400000L))).build());
        workItemsService.insert(COMPANY, workItem1.toBuilder().workItemId("111").workItemType("Task").project("Basic-Project")
                .workItemResolvedAt(DateUtils.toTimestamp(Instant.ofEpochSecond(1600600000L))).build());
        // -- ticket count:
        // frodo: total_effort = 3 tasks (ignoring 1 in progress) + 1 bug + 1 epic = 5
        // aragorn: total_effort = 1 bug

        // -- story points:
        // frodo: total_effort = 20 (bug) + 5 (task) = 25
        // aragorn: total_effort = 100 (bug)

        // ---- ingested_at = INGESTED_AT_PREV
        DbWorkItem workItem2 = buildBaseWorkItem(integrationIdWorkItems).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("1").workItemType("Bug").storyPoint(20F).build());
        workItemsService.insert(COMPANY, workItem2.toBuilder().workItemId("2").workItemType("Task").storyPoint(5F).build());

        // ----- COMMITS
        DbScmCommit baseCommit = DbScmCommit.builder()
                .repoIds(List.of("org/repo1"))
                .author("frodo")
                .authorInfo(frodo)
                .committer("maxime")
                .committerInfo(maxime)
                .integrationId(integrationIdScm)
                .commitSha("111")
                .filesCt(0)
                .additions(0)
                .deletions(0)
                .changes(0)
                .committedAt(1000L)
                .vcsType(VCS_TYPE.GIT)
                .project("0")
                .ingestedAt(1000L)
                .workitemIds(List.of("1", "2")) // --> cat1 (higher precedence)
                .build();
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("111").committedAt(1633899877L)
                .build());
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("222").committedAt(1633899877L)
                .workitemIds(List.of("4")).build());
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("333").committedAt(1633899877L)
                .workitemIds(List.of("6")).build());
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("444").committedAt(1636578277L)
                .author("legolas").authorInfo(legolas).workitemIds(List.of("111")).build());
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("555").committedAt(1636578277L)
                .author("aragorn").authorInfo(aragorn).workitemIds(List.of()).build());

        System.out.println(">>> commit count: " +
                scmAggService.listCommits(COMPANY, ScmCommitFilter.builder().build(), Map.of(), null, 0, 1).getTotalCount());
        System.out.println(">>> workitem count: " +
                workItemsService.list(COMPANY, 0, 1).getTotalCount());
        System.out.println(">>> scm workitem mappings: " + new JdbcTemplate(dataSource).queryForObject("select count(*) from test.scm_commit_workitem_mappings", Integer.class));
        System.out.println(">>> users: " + new JdbcTemplate(dataSource).queryForObject("select count(*) from test.integration_users", Integer.class));
        userIdentityService.list(COMPANY, 0, 100).getRecords().forEach(System.out::println);
    }

    @Test
    public void testAcrossTicketCat() throws NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baWorkItemsAggsDatabaseService.calculateScmCommitCountFTE(COMPANY,
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.TICKET_CATEGORY, scmCommitFilter, BASE_WORKITEMS_FILTER, WorkItemsMilestoneFilter.builder().build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.6666667f).total(3L).effort(2L).build(),
                DbAggregationResult.builder().key("cat2").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("Other").fte(1.3333334f).total(4L).effort(2L).build()
        );
    }

    @Test
    public void testAcrossAuthor() throws NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baWorkItemsAggsDatabaseService.calculateScmCommitCountFTE(COMPANY,
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.AUTHOR, scmCommitFilter,
                BASE_WORKITEMS_FILTER.toBuilder()
                        .ticketCategories(List.of("Other"))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null);
        DefaultObjectMapper.prettyPrint(output);
        String aragorn = userIdentityService.getUser(COMPANY, integrationIdScm, "ff7afc73-a5a1-4e57-b715-59533a065b87");
        String frodo = userIdentityService.getUser(COMPANY, integrationIdScm, "156e0354-2afd-42e5-ac80-8097d3eef5f4");
        String legolas = userIdentityService.getUser(COMPANY, integrationIdScm, "30a20e36-5917-453a-9662-9a8b2905ac01");
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").additionalKey(aragorn).fte(1f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("frodo").additionalKey(frodo).fte(1f).total(3L).effort(3L).build(),
                DbAggregationResult.builder().key("legolas").additionalKey(legolas).fte(1f).total(1L).effort(1L).build()
        );

        output = baWorkItemsAggsDatabaseService.calculateScmCommitCountFTE(COMPANY,
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.AUTHOR, scmCommitFilter,
                BASE_WORKITEMS_FILTER.toBuilder()
                        .ticketCategories(List.of("cat1", "cat2"))
                        .build(), WorkItemsMilestoneFilter.builder().build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("legolas").additionalKey(legolas).fte(1f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("frodo").additionalKey(frodo).fte(0.6666667f).total(3L).effort(2L).build()
        );
    }

    @Test
    public void testAcrossCommittedAt() throws NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baWorkItemsAggsDatabaseService.calculateScmCommitCountFTE(COMPANY,
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.COMMITTED_AT,
                scmCommitFilter,
                BASE_WORKITEMS_FILTER.toBuilder().aggInterval("week").ticketCategories(List.of("cat1", "cat2")).build(),
                WorkItemsMilestoneFilter.builder().build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1636502400").additionalKey("10-11-2021").fte(1f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1633824000").additionalKey("10-10-2021").fte(0.6666667f).total(3L).effort(2L).build()
        );
    }

    @Test
    public void testFilters() throws NotFoundException, SQLException {
        insertMockData();
        String userId1 = userIdentityService.getUser(COMPANY, integrationIdScm, "156e0354-2afd-42e5-ac80-8097d3eef5f4"); // frodo
        String userId2 = userIdentityService.getUser(COMPANY, integrationIdScm, "5cefa2f2-0928-489c-a233-bed5ab4c608e"); // maxime

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baWorkItemsAggsDatabaseService.calculateScmCommitCountFTE(COMPANY,
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.TICKET_CATEGORY,
                scmCommitFilter.toBuilder()
                        .integrationIds(List.of(integrationIdScm))
                        .repoIds(List.of("org/repo1"))
                        .authors(List.of(userId1))
                        .committers(List.of(userId2))
                        .projects(List.of("0"))
                        .vcsTypes(List.of(VCS_TYPE.GIT))
                        .committedAtRange(ImmutablePair.of(900L, 1700000000L))
                        .build(), BASE_WORKITEMS_FILTER, WorkItemsMilestoneFilter.builder().build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.6666667f).total(3L).effort(2L).build(),
                DbAggregationResult.builder().key("Other").fte(0.33333334f).total(3L).effort(1L).build()
        );
    }
}
