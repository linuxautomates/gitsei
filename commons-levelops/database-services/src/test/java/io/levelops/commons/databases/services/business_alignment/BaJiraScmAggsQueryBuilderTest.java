package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.business_alignment.BaJiraScmAggsQueryBuilder.ScmAcross;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.IN_PROGRESS_STATUS_CATEGORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BaJiraScmAggsQueryBuilderTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;
    private static final JiraIssuesFilter BASE_JIRA_FILTER = JiraIssuesFilter.builder()
            .ingestedAt(INGESTED_AT)
            .ticketCategorizationSchemeId("b530ea0b-1c76-4b3c-8646-e2c4c0fe402c")
            .ticketCategorizationFilters(List.of(
                    JiraIssuesFilter.TicketCategorizationFilter.builder()
                            .index(0)
                            .name("cat1")
                            .filter(JiraIssuesFilter.builder().issueTypes(List.of("BUG")).build())
                            .build(),
                    JiraIssuesFilter.TicketCategorizationFilter.builder()
                            .index(1)
                            .name("cat2")
                            .filter(JiraIssuesFilter.builder().issueTypes(List.of("TASK")).build())
                            .build()
            ))
            .build();

    DataSource dataSource;

    TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    BaJiraAggsDatabaseService baJiraAggsDatabaseService;
    JiraIssueService jiraIssueService;
    ScmAggService scmAggService;
    String integrationIdJira;
    String integrationIdScm;
    UserIdentityService userIdentityService;


    @Before
    public void setUp() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        BaJiraAggsQueryBuilder baJiraAggsQueryBuilder = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder = new BaJiraAggsActiveWorkQueryBuilder(baJiraAggsQueryBuilder);

        userIdentityService = jiraTestDbs.getUserIdentityService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        userIdentityService.ensureTableExistence(COMPANY);
        scmAggService.ensureTableExistence(COMPANY);
        dataSource.getConnection().prepareStatement(arrayUniq).execute();

        BaJiraScmAggsQueryBuilder baJiraScmAggsQueryBuilder = new BaJiraScmAggsQueryBuilder(baJiraAggsQueryBuilder, scmAggService);

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

        baJiraAggsDatabaseService = new BaJiraAggsDatabaseService(dataSource, baJiraAggsQueryBuilder, baJiraAggsActiveWorkQueryBuilder, baJiraScmAggsQueryBuilder, ticketCategorizationSchemeDatabaseService);

        integrationIdJira = DatabaseTestUtils.createIntegrationId(jiraTestDbs.getIntegrationService(), COMPANY, "jira");
        integrationIdScm = DatabaseTestUtils.createIntegrationId(jiraTestDbs.getIntegrationService(), COMPANY, "github");
        jiraIssueService = jiraTestDbs.getJiraIssueService();

    }

    public static DbJiraIssue buildBaseJiraIssue(String integrationId) {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("LEV")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .reporter("max")
                .status("Done")
                .statusCategory("Done")
                .issueType("BUG")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .issueCreatedAt(1L)
                .issueUpdatedAt(2L)
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

        // -- sprint
        jiraIssueService.insertJiraSprint(COMPANY, DbJiraSprint.builder()
                .integrationId(Integer.valueOf(integrationIdJira))
                .sprintId(42)
                .name("sprint 42")
                .state("ACTIVE")
                .updatedAt(10L)
                .build());

        // ---- ingested_at = INGESTED_AT
        DbJiraIssue issue1 = buildBaseJiraIssue(integrationIdJira).toBuilder().ingestedAt(INGESTED_AT).build();
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).issueResolvedAt(1600000000L).build()); // cat1
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).issueResolvedAt(1600200000L).build()); // cat2
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-3").issueType("TASK").statusCategory(IN_PROGRESS_STATUS_CATEGORY).sprintIds(List.of(42)).build()); // cat2
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-4").issueType("EPIC").issueResolvedAt(1600300000L).build()); // Other
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-5").issueType("TASK").issueResolvedAt(1600400000L).build()); // cat2
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-6").issueType("BUG").assignee("aragorn").storyPoints(100).issueResolvedAt(1600400000L).build()); // cat1
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LFE-1").issueType("TASK").project("LFE").issueResolvedAt(1600600000L).build()); // cat2

        // ---- ingested_at = INGESTED_AT_PREV
        DbJiraIssue issue2 = buildBaseJiraIssue(integrationIdJira).toBuilder().ingestedAt(INGESTED_AT_PREV).build();
        jiraIssueService.insert(COMPANY, issue2.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).build());
        jiraIssueService.insert(COMPANY, issue2.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).build());

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
                .issueKeys(List.of("LEV-1", "LEV-2")) // --> cat1 (higher precedence)
                .build();
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("111").committedAt(1633899877L).build()); // cat1
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("222").committedAt(1633899877L).issueKeys(List.of("LEV-4")).build()); // Other
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("333").committedAt(1633899877L).issueKeys(List.of("LEV-6")).build()); // cat1
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("444").committedAt(1636578277L).issueKeys(List.of("LFE-1")).author("legolas").authorInfo(legolas).build()); // cat2
        scmAggService.insertCommit(COMPANY, baseCommit.toBuilder().commitSha("555").committedAt(1636578277L).issueKeys(List.of()).author("aragorn").authorInfo(aragorn).build()); // Other

        // authors:
        // - frodo: cat1: 111 + 333, Other: 222 -> cat1: 2/3, Other 1/3
        // - legolas: cat2: 444 -> cat2: 1/1
        // - aragorn: Other: 555 -> Other: 1/1
        // categories:
        // - cat1: 0.6666...
        // - cat2: 1
        // - Other: 1.3333...

        DbListResponse<DbScmCommit> commits = scmAggService.listCommits(COMPANY, ScmCommitFilter.builder().build(), Map.of(), null, 0, 20);
//        System.out.println(">>> commits: " + DefaultObjectMapper.writeAsPrettyJson(commits.getRecords()));
        System.out.println(">>> commit count: " + commits.getTotalCount());
        DbListResponse<DbJiraIssue> issues = jiraIssueService.list(COMPANY, 0, 20);
//        System.out.println(">>> issues: " + DefaultObjectMapper.writeAsPrettyJson(issues.getRecords()));
        System.out.println(">>> issue count: " + issues.getTotalCount());
        System.out.println(">>> scm jira mappings: " + new JdbcTemplate(dataSource).queryForObject("select count(*) from test.scm_commit_jira_mappings", Integer.class));
        System.out.println(">>> users: " + new JdbcTemplate(dataSource).queryForObject("select count(*) from test.integration_users", Integer.class));
        userIdentityService.list(COMPANY, 0, 100).getRecords().forEach(System.out::println);
    }

    @Test
    public void testAcrossTicketCat() throws BadRequestException, NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baJiraAggsDatabaseService.calculateScmCommitCountFTE(COMPANY, ScmAcross.TICKET_CATEGORY, scmCommitFilter, BASE_JIRA_FILTER, null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.6666667f).total(3L).effort(2L).build(),
                DbAggregationResult.builder().key("cat2").fte(1.0f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("Other").fte(1.3333334f).total(4L).effort(2L).build()
        );
    }

    @Test
    public void testAcrossAuthor() throws BadRequestException, NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baJiraAggsDatabaseService.calculateScmCommitCountFTE(COMPANY, ScmAcross.AUTHOR, scmCommitFilter, BASE_JIRA_FILTER.toBuilder()
                .ticketCategories(List.of("Other"))
                .build(), null);
        DefaultObjectMapper.prettyPrint(output);
        String aragorn = userIdentityService.getUser(COMPANY, integrationIdScm, "ff7afc73-a5a1-4e57-b715-59533a065b87");
        String frodo = userIdentityService.getUser(COMPANY, integrationIdScm, "156e0354-2afd-42e5-ac80-8097d3eef5f4");
        String legolas = userIdentityService.getUser(COMPANY, integrationIdScm, "30a20e36-5917-453a-9662-9a8b2905ac01");
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").additionalKey(aragorn).total(1L).fte(1f).effort(1L).build(),
                DbAggregationResult.builder().key("frodo").additionalKey(frodo).total(3L).fte(0.33333334f).effort(1L).build()
        );

        output = baJiraAggsDatabaseService.calculateScmCommitCountFTE(COMPANY, ScmAcross.AUTHOR, scmCommitFilter, BASE_JIRA_FILTER.toBuilder()
                .ticketCategories(List.of("cat1", "cat2"))
                .build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("legolas").additionalKey(legolas).total(1L).fte(1f).effort(1L).build(),
                DbAggregationResult.builder().key("frodo").additionalKey(frodo).total(3L).fte(0.6666667f).effort(2L).build()
        );
    }

    @Test
    public void testAcrossCommittedAt() throws BadRequestException, NotFoundException, SQLException {
        insertMockData();

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        DbListResponse<DbAggregationResult> output = baJiraAggsDatabaseService.calculateScmCommitCountFTE(COMPANY, ScmAcross.COMMITTED_AT, scmCommitFilter, BASE_JIRA_FILTER.toBuilder()
                .aggInterval("week")
                .ticketCategories(List.of("cat1", "cat2"))
                .build(), null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1636502400").additionalKey("10-11-2021").fte(1f).total(1L).effort(1L).build(),
                DbAggregationResult.builder().key("1633824000").additionalKey("10-10-2021").fte(0.6666667f).total(3L).effort(2L).build()
        );
    }

    @Test
    public void testFilters() throws BadRequestException, NotFoundException, SQLException {
        insertMockData();
        String userId1 = userIdentityService.getUser(COMPANY, integrationIdScm, "156e0354-2afd-42e5-ac80-8097d3eef5f4"); // frodo
        String userId2 = userIdentityService.getUser(COMPANY, integrationIdScm, "5cefa2f2-0928-489c-a233-bed5ab4c608e"); // maxime

        ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder().build();

        // TODO test more filters
        /*
    List<String> fileTypes; // not working
    List<String> codeCategory;
    String codeChangeUnit;
    Map<String, String> codeChangeSizeConfig;
    Map<String, String> codeChangeSize;

    List<String> codeChanges;
    Long legacyCodeConfig;
         */
        DbListResponse<DbAggregationResult> output = baJiraAggsDatabaseService.calculateScmCommitCountFTE(COMPANY, ScmAcross.TICKET_CATEGORY, scmCommitFilter.toBuilder()
                        .integrationIds(List.of(integrationIdScm))
                        .repoIds(List.of("org/repo1"))
                .authors(List.of(userId1))
                .committers(List.of(userId2))
                .projects(List.of("0"))
                .vcsTypes(List.of(VCS_TYPE.GIT))
                .committedAtRange(ImmutablePair.of(900L, 1700000000L))
//                .fileTypes(List.of(".java")) // FIXME
//                .codeCategory(List.of("cat")) // FIXME
                .build(), BASE_JIRA_FILTER, null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.6666667f).total(3L).effort(2L).build(),
                DbAggregationResult.builder().key("Other").fte(0.33333334f).total(3L).effort(1L).build()
        );
    }
}