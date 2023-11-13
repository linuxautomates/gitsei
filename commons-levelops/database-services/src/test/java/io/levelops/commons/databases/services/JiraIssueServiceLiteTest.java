package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraPriority;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraPriority;
import io.levelops.integrations.jira.models.JiraUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE: THIS CLASS CONTAINS FAST TESTS WITH VERY LITTLE DATA FOR FASTER DEV TESTING.
 */
public class JiraIssueServiceLiteTest {
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static IntegrationService integrationService;
    private static JiraProjectService jiraProjectService;
    private static JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    private static IntegrationTrackingService integrationTrackingService;
    private static String integrationId;
    private static String integrationId2;
    private static UserIdentityService userIdentityService;
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        jiraProjectService = jiraTestDbs.getJiraProjectService();
        integrationService = jiraTestDbs.getIntegrationService();

        sprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new TagsService(dataSource).ensureTableExistence(company);
        new TagItemDBService(dataSource).ensureTableExistence(company);
        integrationTrackingService = new IntegrationTrackingService(dataSource);
        sprintMappingDatabaseService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationId2 = integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);
        jiraFieldService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(company);
        integrationTrackingService.ensureTableExistence(company);
        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(user -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(user, "1");
            jiraIssueService.insertJiraUser(company, tmp);
            if (user.getDisplayName() != null) {
                try {
                    userIdentityService.batchUpsert(company,
                            List.of(DbScmUser.builder()
                                    .integrationId("1")
                                    .cloudId(user.getAccountId())
                                    .displayName(user.getDisplayName())
                                    .originalDisplayName(user.getDisplayName())
                                    .build()));
                } catch (SQLException throwables) {
                    System.out.println("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + company + ", integration id:" + "1");
                }
            }
        });
    }

    //
    @Before
    public void setUp() throws Exception {
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_issues;").execute();
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_issue_statuses;").execute();
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_issue_sprints;").execute();
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_issue_sprint_mappings;").execute();
        dataSource.getConnection().prepareStatement("delete from " + company + ".integration_tracker;").execute();
        dataSource.getConnection().prepareStatement("delete from " + company + ".jira_priorities;").execute();
    }

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(company, "1", displayName);
    }

    private DbJiraIssue buildBaseIssue() {
        return DbJiraIssue.builder()
                .key("LEV-123")
                .integrationId(integrationId)
                .project("def")
                .summary("sum")
                .descSize(3)
                .priority("high")
                .status("won't do")
                .issueType("bug")
                .hops(2)
                .bounces(4)
                .numAttachments(0)
                .issueCreatedAt(1L)
                .issueUpdatedAt(2L)
                .ingestedAt(0L)
                .reporter("gandalf")
                .assignee("frodo")
                .assigneeId(userIdOf("frodo").isPresent() ? userIdOf("frodo").get() : "")
                .reporterId(userIdOf("gandalf").isPresent() ? userIdOf("gandalf").get() : "")
                .build();
    }

    @Test
    public void testNullIngestedAt() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .ingestedAt(1L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .ingestedAt(2L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .ingestedAt(3L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .ingestedAt(3L)
                .build());


        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().build(), null, null, 0, 10).getTotalCount()).isEqualTo(4);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().ingestedAt(2L).build(), null, null, 0, 10).getTotalCount()).isEqualTo(1);
        assertThat(jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .integrationIds(List.of(integrationId))
                .keys(List.of("LEV-123"))
                .build(), null, null, 0, 10).getTotalCount()).isEqualTo(3);
    }

    @Test
    public void testTicketCategory() throws SQLException {

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .status("todo")
                .storyPoints(2)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .status("done")
                .project("SUPPORT")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .project("SUPPORT")
                .status("todo")
                .storyPoints(5)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("e")
                .project("SUPPORT")
                .status("won't do")
                .storyPoints(7)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .status("done")
                .storyPoints(11)
                .build());

        // -- agg

        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("Completed tickets")
                        .index(20)
                        .filter(JiraIssuesFilter.builder()
                                .statuses(List.of("done"))
                                .build())
                        .build(),
                JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("Support Tickets")
                        .index(10)
                        .filter(JiraIssuesFilter.builder()
                                .projects(List.of("SUPPORT"))
                                .statuses(List.of("todo", "done"))
                                .build())
                        .build());

        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
//                        .projects(List.of("def"))
                        .priorities(List.of("high"))
//                        .statuses(List.of("won't do"))
                        .reporters(List.of(userIdOf("gandalf").isPresent() ? userIdOf("gandalf").get() : ""))
//                        .ticketCategories(List.of("Test", "Other"))
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Support Tickets").totalTickets(2L).meanStoryPoints(2.5).totalStoryPoints(5L).build(),
                DbAggregationResult.builder().key("Completed tickets").totalTickets(1L).totalStoryPoints(11L).meanStoryPoints(11.0).build(),
                DbAggregationResult.builder().key("Other").totalTickets(2L).totalStoryPoints(9L).meanStoryPoints(4.5).build()
        );

        // story points
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.story_points)
                        .priorities(List.of("high"))
                        .reporters(List.of(userIdOf("gandalf").isPresent() ? userIdOf("gandalf").get() : ""))
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Support Tickets").totalTickets(2L).totalStoryPoints(5L).totalUnestimatedTickets(1L).build(),
                DbAggregationResult.builder().key("Completed tickets").totalTickets(1L).totalStoryPoints(11L).totalUnestimatedTickets(0L).build(),
                DbAggregationResult.builder().key("Other").totalTickets(2L).totalStoryPoints(9L).totalUnestimatedTickets(0L).build()
        );

        // -- list

        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .ticketCategories(List.of("Support Tickets"))
                .ticketCategorizationFilters(ticketCategorizationFilters).build(), null, null, 0, 10);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("b", "c");

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .ticketCategories(List.of("Completed tickets"))
                .ticketCategorizationFilters(ticketCategorizationFilters).build(), null, null, 0, 10);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("d");

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .ticketCategories(List.of("Other"))
                .ticketCategorizationFilters(ticketCategorizationFilters).build(), null, null, 0, 10);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("a", "e");

        ticketCategorizationFilters = List.of(JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("Done tickets")
                        .index(10)
                        .filter(JiraIssuesFilter.builder()
                                .statuses(List.of("done"))
                                .build())
                        .build(),
                JiraIssuesFilter.TicketCategorizationFilter.builder()
                        .name("Todo Tickets")
                        .index(20)
                        .filter(JiraIssuesFilter.builder()
                                .projects(List.of("SUPPORT"))
                                .statuses(List.of("todo"))
                                .build())
                        .build());

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .ticketCategories(List.of("Done tickets"))
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Done tickets").totalTickets(2L).meanStoryPoints(5.5).totalStoryPoints(11L).build()
        );

        list = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .ticketCategories(List.of("Done tickets"))
                .ticketCategorizationFilters(ticketCategorizationFilters).build(), null, null, 0, 10);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("b", "d");
    }

    @Test
    public void testTicketCategoryEmpty() throws SQLException {
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(JiraIssuesFilter.TicketCategorizationFilter.builder()
                .name("A")
                .index(20)
                .filter(JiraIssuesFilter.builder()
                        .build())
                .build());
        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(), false, null, null, Map.of());
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void testTicketCategoryWithCustomFields() throws SQLException {

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .customFields(Map.of("customfield_1234", "true"))
                .build());

        // -- agg

        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = List.of(JiraIssuesFilter.TicketCategorizationFilter.builder()
                .name("A")
                .index(20)
                .filter(JiraIssuesFilter.builder()
                        .customFields(Map.of("customfield_1234", List.of("true")))
                        .build())
                .build());

        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .ticketCategorizationFilters(ticketCategorizationFilters)
                        .build(), false, null, null, Map.of());
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("A").totalTickets(1L).meanStoryPoints(0.0).totalStoryPoints(0L).build(),
                DbAggregationResult.builder().key("Other").totalTickets(1L).meanStoryPoints(0.0).totalStoryPoints(0L).build()
        );
    }

    @Test
    public void testStoryPointsCalc() throws SQLException {

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .status("todo")
                .storyPoints(null)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .status("done")
                .project("SUPPORT")
                .storyPoints(2)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .project("SUPPORT")
                .status("todo")
                .storyPoints(3)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("e")
                .project("SUPPORT")
                .status("won't do")
                .storyPoints(5)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .status("done")
                .storyPoints(7)
                .build());

        // -- agg

        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.ticket_category)
                        .calculation(JiraIssuesFilter.CALCULATION.story_points)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("Other").totalTickets(5L).totalStoryPoints(17L).totalUnestimatedTickets(1L).build()
        );

    }

    @Test
    public void testSprints() {
        Date currentTime = new Date();
        DbJiraSprint sprint1 = DbJiraSprint.builder()
                .name("Name One")
                .integrationId(1)
                .sprintId(1)
                .state("Active")
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build();
        DbJiraSprint sprint2 = DbJiraSprint.builder()
                .name("Name Two")
                .integrationId(2)
                .sprintId(2)
                .state("Closed")
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build();
        String id1 = jiraIssueService.insertJiraSprint(company, sprint1).orElseThrow();
        String id2 = jiraIssueService.insertJiraSprint(company, sprint2).orElseThrow();

        // -- state filter
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .state("cLoSeD")
                .build()).map(DbJiraSprint::getId)).containsExactly(id2);

        // -- name filters
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .names(List.of("nAmE"))
                .build())).hasSize(0);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .names(List.of("nAmE ONE"))
                .build()).map(DbJiraSprint::getId)).containsExactly(id1);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .names(List.of("nAmE ONE", "name two"))
                .build()).map(DbJiraSprint::getId)).containsExactly(id1, id2);

        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .nameStartsWith("nAmE")
                .build()).map(DbJiraSprint::getId)).containsExactly(id1, id2);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .nameContains("AmE")
                .build()).map(DbJiraSprint::getId)).containsExactly(id1, id2);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .nameEndsWith("One")
                .build()).map(DbJiraSprint::getId)).containsExactly(id1);

        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .excludeNames(List.of("abc"))
                .build()).map(DbJiraSprint::getId)).containsExactly(id1, id2);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .excludeNames(List.of("nAmE ONE"))
                .build()).map(DbJiraSprint::getId)).containsExactly(id2);
        assertThat(jiraIssueService.streamSprints(company, JiraSprintFilter.builder()
                .excludeNames(List.of("nAmE ONE", "name two"))
                .build()).map(DbJiraSprint::getId)).containsExactly();

    }

    @Test
    public void testAssigneesCalc() throws SQLException {
        DbJiraAssignee.DbJiraAssigneeBuilder builder = DbJiraAssignee.builder().integrationId(integrationId);

        Long month1A = DateUtils.toEpochSecond(Instant.parse("2020-01-01T01:02:03Z"));
        Long month1B = DateUtils.toEpochSecond(Instant.parse("2020-01-10T01:02:03Z"));
        Long month1C = DateUtils.toEpochSecond(Instant.parse("2020-01-15T01:02:03Z"));
        Long month1D = DateUtils.toEpochSecond(Instant.parse("2020-01-20T01:02:03Z"));
        Long month2 = DateUtils.toEpochSecond(Instant.parse("2020-02-01T01:02:03Z"));
        Long month3 = DateUtils.toEpochSecond(Instant.parse("2020-03-01T01:02:03Z"));

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .epic("E1")
                .ingestedAt(month1A)
                .assigneeList(List.of(
                        builder.assignee("u1").issueKey("a").startTime(month1A).endTime(month1B).build(),
                        builder.assignee("u2").issueKey("a").startTime(month1B).endTime(month1C).build(),
                        builder.assignee("u3").issueKey("a").startTime(month1C).endTime(month1D).build(),
                        builder.assignee("_UNASSIGNED_").issueKey("a").startTime(month1C).endTime(month1D).build()
                ))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .epic("E1")
                .ingestedAt(month2)
                .assigneeList(List.of(
                        builder.assignee("u2").issueKey("b").startTime(month1A).endTime(month1B).build(),
                        builder.assignee("u3").issueKey("b").startTime(month1B).endTime(month1C).build(),
                        builder.assignee("u4").issueKey("b").startTime(month1C).endTime(month2).build()
                ))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .epic("E2")
                .ingestedAt(month3)
                .assigneeList(List.of(
                        builder.assignee("u1").issueKey("c").startTime(month1A).endTime(month1B).build(),
                        builder.assignee("u2").issueKey("c").startTime(month1B).endTime(month1C).build(),
                        builder.assignee("u3").issueKey("c").startTime(month1C).endTime(month3).build()
                ))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("e")
                .epic("E2")
                .ingestedAt(month3)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .ingestedAt(month3)
                .assigneeList(List.of(
                        builder.assignee("u6").issueKey("d").startTime(month1A).endTime(month1B).build(),
                        builder.assignee("u7").issueKey("d").startTime(month1B).endTime(month2).build(),
                        builder.assignee("u8").issueKey("d").startTime(month2).endTime(month3).build()
                ))
                .build());

        // -- agg

        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.epic)
                        .calculation(JiraIssuesFilter.CALCULATION.assignees)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("E1").total(4L).assignees(List.of("u1", "u2", "u3", "u4")).build(),
                DbAggregationResult.builder().key("E2").total(3L).assignees(List.of("u1", "u2", "u3")).build()
        );

        // -- none
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.assignees)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().total(7L).assignees(List.of("u1", "u2", "u3", "u4", "u6", "u7", "u8")).build()
        );

        // -- filters
        testAssigneesFilters(0L, 99999999999L, "u1", "u2", "u3", "u4", "u6", "u7", "u8");
        testAssigneesFilters(month1A, month1B, "u1", "u2", "u6");
        testAssigneesFilters(month1B, month1C, "u2", "u3", "u7");
        testAssigneesFilters(month1C, month2, "u3", "u4", "u7");
        testAssigneesFilters(month2, month3, "u3", "u8");
        testAssigneesFilters(month1B, month2, "u2", "u3", "u4", "u7");
        testAssigneesFilters(month1C, month3, "u3", "u4", "u7", "u8");
    }

    private void testAssigneesFilters(Long from, Long to, String... users) throws SQLException {
        var result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.assignees)
                        .assigneesDateRange(ImmutablePair.of(from, to))
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().total((long) users.length).assignees(Arrays.asList(users)).build()
        );
    }

    @Test
    public void testSprintMappings() throws SQLException {

        Date currentTime = new Date();

        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(1)
                .integrationId(Integer.valueOf(integrationId))
                .name("LO-1")
                .completedDate(12L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(2)
                .integrationId(Integer.valueOf(integrationId))
                .name("LO-2")
                .completedDate(42L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());
        jiraIssueService.insertJiraSprint(company, DbJiraSprint.builder()
                .sprintId(1)
                .integrationId(Integer.valueOf(integrationId2))
                .name("LFE-1")
                .completedDate(108L)
                .updatedAt(currentTime.toInstant().getEpochSecond())
                .build());

        // integration=1, sprint=1: a, b
        // integration=1, sprint=2: b, c(ignorable), d
        // integration=2, sprint=1: a
        DbJiraIssueSprintMapping mapping1 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("a")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id1 = sprintMappingDatabaseService.insert("test", mapping1);
        mapping1 = mapping1.toBuilder().id(id1).build();

        DbJiraIssueSprintMapping mapping2 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("b")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id2 = sprintMappingDatabaseService.insert("test", mapping2);
        mapping2 = mapping2.toBuilder().id(id2).build();

        DbJiraIssueSprintMapping mapping3 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("c")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id3 = sprintMappingDatabaseService.insert("test", mapping3);
        mapping3 = mapping3.toBuilder().id(id3).build();

        DbJiraIssueSprintMapping mapping4 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("d")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id4 = sprintMappingDatabaseService.insert("test", mapping4);
        mapping4 = mapping4.toBuilder().id(id4).build();

        DbJiraIssueSprintMapping mapping5 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId2)
                .issueKey("a")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id5 = sprintMappingDatabaseService.insert("test", mapping5);
        mapping5 = mapping5.toBuilder().id(id5).build();

        DbJiraIssueSprintMapping mapping6 = DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("b")
                .sprintId("2")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(false)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build();
        String id6 = sprintMappingDatabaseService.insert("test", mapping6);
        mapping6 = mapping6.toBuilder().id(id6).build();

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .status("todo")
                .storyPoints(null)
                .issueType("task")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .key("a")
                .status("todo")
                .storyPoints(null)
                .issueType("bug")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .status("done")
                .project("SUPPORT")
                .storyPoints(2)
                .issueType("story")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .project("SUPPORT")
                .status("todo")
                .storyPoints(3)
                .issueType("subtask")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .status("done")
                .storyPoints(7)
                .issueType("epic")
                .build());

        JiraIssueSprintMappingAggResult aggMapping1 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping1)
                .issueType("TASK")
                .build();
        JiraIssueSprintMappingAggResult aggMapping2 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping2)
                .issueType("STORY")
                .build();
        JiraIssueSprintMappingAggResult aggMapping3 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping3)
                .issueType("SUBTASK")
                .build();
        JiraIssueSprintMappingAggResult aggMapping4 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping4)
                .issueType("EPIC")
                .build();
        JiraIssueSprintMappingAggResult aggMapping5 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping5) // 2 a
                .issueType("BUG")
                .build();
        JiraIssueSprintMappingAggResult aggMapping6 = JiraIssueSprintMappingAggResult.builder()
                .sprintMapping(mapping6)
                .issueType("STORY")
                .build();
        // -- agg

        DbListResponse<DbAggregationResult> result;

        // no filters
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 3, 3);
        assertThat(result.getRecords().get(2).getSprintName()).isEqualTo("LO-1");
        assertThat(result.getRecords().get(2).getSprintCompletedAt()).isEqualTo(12L);
        assertThat(result.getRecords().get(1).getSprintName()).isEqualTo("LO-2");
        assertThat(result.getRecords().get(1).getSprintCompletedAt()).isEqualTo(42L);
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LFE-1");
        assertThat(result.getRecords().get(0).getSprintCompletedAt()).isEqualTo(108L);
        verifySprintMappings(result, 2, "1", "1", aggMapping1, aggMapping2);
        verifySprintMappings(result, 1, "1", "2", aggMapping3, aggMapping4, aggMapping6);
        verifySprintMappings(result, 0, "2", "1", aggMapping5);

        // sprint mapping filter
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintMappingIgnorableIssueType(false)
                        .build(),
                false, null, null, Map.of());
        verifyCounts(result, 3, 3);
        verifySprintMappings(result, 2, "1", "1", aggMapping1, aggMapping2);
        verifySprintMappings(result, 1, "1", "2", aggMapping4, aggMapping6);
        verifySprintMappings(result, 0, "2", "1", aggMapping5);

        // issue filter
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintMappingIgnorableIssueType(false)
                        .statuses(List.of("done"))
                        .build(),
                false, null, null, Map.of());
        verifyCounts(result, 2, 2);
        verifySprintMappings(result, 1, "1", "1", aggMapping2);
        verifySprintMappings(result, 0, "1", "2", aggMapping6, aggMapping4);

        // sprints filters
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintMappingSprintIds(List.of("2"))
                        .build(),
                false, null, null, Map.of());
        verifyCounts(result, 1, 1);
        verifySprintMappings(result, 0, "1", "2", aggMapping3, aggMapping4, aggMapping6);

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintMappingIgnorableIssueType(false)
                        .sprintMappingSprintNameStartsWith("LO")
                        .build(),
                false, null, null, Map.of());
        verifyCounts(result, 2, 2);
        verifySprintMappings(result, 1, "1", "1", aggMapping1, aggMapping2);
        verifySprintMappings(result, 0, "1", "2", aggMapping4, aggMapping6);

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .sprintMappingIgnorableIssueType(false)
                        .sprintMappingSprintNameEndsWith("2")
                        .build(),
                false, null, null, Map.of());
        verifyCounts(result, 1, 1);
        verifySprintMappings(result, 0, "1", "2", aggMapping4, aggMapping6);

        // pagination
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .pageSize(2)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 2, 2);
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LFE-1");
        assertThat(result.getRecords().get(1).getSprintName()).isEqualTo("LO-2");

        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.sprint_mapping)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping)
                        .pageSize(2)
                        .page(1)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 1, 1);
        assertThat(result.getRecords().get(0).getSprintName()).isEqualTo("LO-1");

        // --count
        result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.sprint_mapping_count)
                        .build(),
                false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(result);
        verifyCounts(result, 1, 1);
        assertThat(result.getRecords().get(0).getTotal()).isEqualTo(3);
    }

    private void verifyCounts(DbListResponse<DbAggregationResult> result, int total, int count) {
        assertThat(result.getTotalCount()).isEqualTo(total);
        assertThat(result.getCount()).isEqualTo(count);
        assertThat(result.getRecords()).hasSize(count);
    }

    private void verifySprintMappings(DbListResponse<DbAggregationResult> result, int index, String integration, String sprintId, JiraIssueSprintMappingAggResult... mappings) {
        assertThat(result.getRecords().get(index).getIntegrationId()).isEqualTo(integration);
        assertThat(result.getRecords().get(index).getSprintId()).isEqualTo(sprintId);
        List<JiraIssueSprintMappingAggResult> sanitized = result.getRecords().get(index).getSprintMappingAggs().stream()
                .map(o -> o.toBuilder()
                        .sprintMapping(o.getSprintMapping().toBuilder()
                                .createdAt(null)
                                .build())
                        .build())
                .collect(Collectors.toList());
        assertThat(sanitized).containsExactlyInAnyOrder(mappings);
    }

    @Test
    public void testLatestIngestedAt() throws SQLException {
        integrationTrackingService.upsert(company, IntegrationTracker.builder()
                .integrationId(integrationId)
                .latestIngestedAt(10L)
                .build());
        integrationTrackingService.upsert(company, IntegrationTracker.builder()
                .integrationId(integrationId2)
                .latestIngestedAt(20L)
                .build());

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .key("d")
                .status("todo")
                .ingestedAt(15L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .key("d")
                .status("done")
                .ingestedAt(20L)
                .build());

        verifyIngestedAtCount(123456L, null, 0L); // invalid
        verifyIngestedAtCount(10L, null, 3L);
        verifyIngestedAtCount(20L, null, 1L);
        verifyIngestedAtCount(0L, Map.of(integrationId, 10L, integrationId2, 20L), 4L);
        verifyIngestedAtCount(0L, Map.of(integrationId, 10L, integrationId2, 15L), 4L);
        verifyIngestedAtCount(0L, Map.of(integrationId, 20L, integrationId2, 20L), 1L);

        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .ingestedAt(-1L)
                .ingestedAtByIntegrationId(Map.of(integrationId, 10L, integrationId2, 20L))
                .build(), null, null, 0, 100);
        assertThat(list.getRecords()).hasSize(4);
        assertThat(list.getRecords().get(3).getStatus()).isEqualTo("done");

        DbListResponse<JiraStatusTime> statusTime = jiraIssueService.listIssueStatusesByTime(company, JiraIssuesFilter.builder()
                .ingestedAt(-1L)
                .ingestedAtByIntegrationId(Map.of(integrationId, 10L, integrationId2, 20L))
                .build(), null, 0, 100);
        assertThat(statusTime.getRecords()).hasSize(0);

        DbListResponse<JiraAssigneeTime> assigneeTime = jiraIssueService.listIssueAssigneesByTime(company, JiraIssuesFilter.builder()
                .ingestedAt(-1L)
                .ingestedAtByIntegrationId(Map.of(integrationId, 10L, integrationId2, 20L))
                .build(), null, 0, 100);
        assertThat(assigneeTime.getRecords()).hasSize(0);
    }

    private void verifyIngestedAtCount(Long ingestedAt, Map<String, Long> ingestedAtByIntegrationId, long expectedTotal) throws SQLException {
        DbListResponse<DbAggregationResult> result = jiraIssueService.groupByAndCalculate(company,
                JiraIssuesFilter.builder()
                        .across(JiraIssuesFilter.DISTINCT.none)
                        .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                        .ingestedAt(ingestedAt)
                        .ingestedAtByIntegrationId(ingestedAtByIntegrationId)
                        .build(),
                false, null, null, Map.of());
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getTotalTickets()).isEqualTo(expectedTotal);
    }

    @Test
    public void testEpicPriority() throws SQLException {
        jiraProjectService.batchUpsertPriorities(company, List.of(DbJiraProject.builder()
                        .integrationId(integrationId)
                        .defaultPriorities(List.of(
                                JiraPriority.builder().priorityOrder(10).name("highest").build(),
                                JiraPriority.builder().priorityOrder(20).name("high").build(),
                                JiraPriority.builder().priorityOrder(30).name("low").build()))
                        .build(),
                DbJiraProject.builder()
                        .integrationId(integrationId2)
                        .defaultPriorities(List.of(
                                JiraPriority.builder().priorityOrder(15).name("highest").build(),
                                JiraPriority.builder().priorityOrder(25).name("high").build(),
                                JiraPriority.builder().priorityOrder(35).name("low").build()))
                        .build()));

        List<DbJiraPriority> priorities = jiraProjectService.getPriorities(company, List.of(integrationId), 0, 100);
        DefaultObjectMapper.prettyPrint(priorities);

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .issueType("epic")
                .priority("low")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .issueType("epic")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .issueType("task")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .integrationId(integrationId2)
                .key("d")
                .issueType("epic")
                .priority("highest")
                .ingestedAt(10L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .issueType("epic")
                .priority("high")
                .ingestedAt(20L)
                .build());

        DbListResponse<DbJiraIssue> issues = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                .issueTypes(List.of("EPIC"))
                .ingestedAt(10L)
                .integrationIds(List.of(integrationId, integrationId2))
                .build(), null, Map.of("priority", SortingOrder.DESC), 0, 10);
        DefaultObjectMapper.prettyPrint(issues);
        assertThat(issues.getCount()).isEqualTo(3);

        DbJiraIssue issue = issues.getRecords().get(0);
        assertThat(issue.getKey()).isEqualTo("d");
        assertThat(issue.getPriority()).isEqualTo("highest");
        assertThat(issue.getPriorityOrder()).isEqualTo(15);

        issue = issues.getRecords().get(1);
        assertThat(issue.getKey()).isEqualTo("b");
        assertThat(issue.getPriority()).isEqualTo("high");
        assertThat(issue.getPriorityOrder()).isEqualTo(20);

        issue = issues.getRecords().get(2);
        assertThat(issue.getKey()).isEqualTo("a");
        assertThat(issue.getPriority()).isEqualTo("low");
        assertThat(issue.getPriorityOrder()).isEqualTo(30);

    }

    @Test
    public void testEpicPriorityTrend() throws SQLException {
        jiraProjectService.batchUpsertPriorities(company, List.of(DbJiraProject.builder()
                        .integrationId(integrationId)
                        .defaultPriorities(List.of(
                                JiraPriority.builder().priorityOrder(10).name("highest").build(),
                                JiraPriority.builder().priorityOrder(20).name("high").build(),
                                JiraPriority.builder().priorityOrder(30).name("low").build()))
                        .build(),
                DbJiraProject.builder()
                        .integrationId(integrationId2)
                        .defaultPriorities(List.of(
                                JiraPriority.builder().priorityOrder(15).name("highest").build(),
                                JiraPriority.builder().priorityOrder(25).name("high").build(),
                                JiraPriority.builder().priorityOrder(35).name("low").build()))
                        .build()));

        List<DbJiraPriority> priorities = jiraProjectService.getPriorities(company, List.of(integrationId), 0, 100);
        DefaultObjectMapper.prettyPrint(priorities);

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .issueType("epic")
                .priority("low")
                .ingestedAt(1616301772L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .issueType("epic")
                .priority("highest")
                .ingestedAt(1618980172L)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .issueType("epic")
                .priority("high")
                .ingestedAt(1621572172L)
                .build());

        DbListResponse<DbAggregationResult> agg = jiraIssueService.groupByAndCalculate(company, JiraIssuesFilter.builder()
                .across(JiraIssuesFilter.DISTINCT.trend)
                .calculation(JiraIssuesFilter.CALCULATION.priority)
                .ingestedAt(10L)
                .keys(List.of("a"))
                .integrationIds(List.of(integrationId, integrationId2))
                .build(), false, null, null, Map.of());
        DefaultObjectMapper.prettyPrint(agg);
        assertThat(agg.getCount()).isEqualTo(3);
        assertThat(agg.getTotalCount()).isEqualTo(3);
        assertThat(agg.getRecords()).containsExactly(
                DbAggregationResult.builder().key("1621494000").additionalKey("20-5-2021").priority("high").priorityOrder(20).build(),
                DbAggregationResult.builder().key("1618902000").additionalKey("20-4-2021").priority("highest").priorityOrder(10).build(),
                DbAggregationResult.builder().key("1616223600").additionalKey("20-3-2021").priority("low").priorityOrder(30).build()
        );

    }

    @Test
    public void testGetHistoricalStatusForIssues() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .statuses(List.of(
                        DbJiraStatus.builder()
                                .startTime(0L)
                                .endTime(10L)
                                .integrationId(integrationId)
                                .issueKey("a")
                                .status("todo")
                                .statusId("1")
                                .build(),
                        DbJiraStatus.builder()
                                .startTime(10L)
                                .endTime(20L)
                                .integrationId(integrationId)
                                .issueKey("a")
                                .status("done")
                                .statusId("2")
                                .build(),
                        DbJiraStatus.builder()
                                .startTime(10L)
                                .endTime(20L)
                                .integrationId(integrationId)
                                .issueKey("b")
                                .status("in review")
                                .statusId("2")
                                .build()))
                .build());
        Map<String, DbJiraStatus> statuses = jiraIssueService.getHistoricalStatusForIssues(company, integrationId, List.of("a", "b"), 5L);
        assertThat(statuses).containsOnlyKeys("a");
        assertThat(statuses.get("a").getStatus()).isEqualTo("todo");
        assertThat(statuses.get("a").getStartTime()).isEqualTo(0L);
        assertThat(statuses.get("a").getEndTime()).isEqualTo(10L);

        statuses = jiraIssueService.getHistoricalStatusForIssues(company, integrationId, List.of("a", "b", "c"), 15L);
        assertThat(statuses).containsOnlyKeys("a", "b");
        assertThat(statuses.get("a").getStatus()).isEqualTo("done");
        assertThat(statuses.get("b").getStatus()).isEqualTo("in review");

        assertThat(jiraIssueService.getHistoricalStatusForIssues(company, integrationId, List.of("c"), 15L)).isEmpty();
        assertThat(jiraIssueService.getHistoricalStatusForIssues(company, integrationId2, List.of("b"), 15L)).isEmpty();
    }

    private void setupBulkUpdateStoryPointsData() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-1")
                .issueType("EPIC")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-2")
                .issueType("EPIC")
                .storyPoints(50)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-3")
                .issueType("EPIC")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-4")
                .issueType("EPIC")
                .storyPoints(1)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("a")
                .epic("EPIC-1")
                .storyPoints(10)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("b")
                .epic("EPIC-1")
                .storyPoints(20)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("c")
                .epic("EPIC-2")
                .storyPoints(30)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("d")
                .epic("EPIC-2")
                .storyPoints(20)
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("3")
                .epic("EPIC-4")
                .storyPoints(2)
                .build());
    }

    @Test
    public void bulkUpdateStoryPointsForParents() throws SQLException {
        setupBulkUpdateStoryPointsData();

        jiraIssueService.bulkUpdateEpicStoryPoints(company, integrationId, 0L);
        jiraIssueService.bulkUpdateEpicStoryPoints(company, integrationId, 0L);

        verifyIssueStoryPoints("EPIC-1", 0L, 30);
        verifyIssueStoryPoints("EPIC-2", 0L, 50);
        verifyIssueStoryPoints("EPIC-3", 0L, null);
        verifyIssueStoryPoints("EPIC-4", 0L, 2);
    }

    @Test
    public void updateEpicStoryPoints() throws SQLException {
        setupBulkUpdateStoryPointsData();

        boolean hasMore = jiraIssueService.bulkUpdateEpicStoryPointsSinglePage(company, integrationId, 0L, 2, 0, 1);
        assertThat(hasMore).isTrue();
        verifyIssueStoryPoints("EPIC-1", 0L, 30);
        verifyIssueStoryPoints("EPIC-2", 0L, 50);
        verifyIssueStoryPoints("EPIC-3", 0L, null);
        verifyIssueStoryPoints("EPIC-4", 0L, 1); // not yet in the page

        hasMore = jiraIssueService.bulkUpdateEpicStoryPointsSinglePage(company, integrationId, 0L, 2, 2, 2);
        assertThat(hasMore).isTrue();
        verifyIssueStoryPoints("EPIC-4", 0L, 2);

        hasMore = jiraIssueService.bulkUpdateEpicStoryPointsSinglePage(company, integrationId, 0L, 2, 4, 2);
        assertThat(hasMore).isFalse();
    }

    private void verifyIssueStoryPoints(String key, Long ingestedAt, Integer expectedStoryPoints) throws SQLException {
        DbJiraIssue output = jiraIssueService.get(company, key, integrationId, ingestedAt).orElseThrow();
        assertThat(output.getStoryPoints()).isEqualTo(expectedStoryPoints);
    }

    @Test
    public void testParentIssueType() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-1")
                .issueType("EPIC")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("SAGA-1")
                .issueType("SAGA")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-1")
                .issueType("TASK")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-2")
                .issueType("TASK")
                .parentKey("EPIC-1")
                .parentIssueType("EPIC")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-3")
                .issueType("TASK")
                .parentKey("SAGA-1")
                .parentIssueType("SAGA")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-4")
                .issueType("TASK")
                .parentKey("SAGA-1")
                .parentIssueType("SAGA")
                .build());

        // -- no filter
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .build();
        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, filter, null, null, null, null, null, 0, 100);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("EPIC-1", "SAGA-1", "TASK-1", "TASK-2", "TASK-3", "TASK-4");

        // -- parent = EPIC
        filter = JiraIssuesFilter.builder()
                .parentIssueTypes(List.of("EPIC"))
                .build();
        list = jiraIssueService.list(company, filter, null, null, null, null, null, 0, 100);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("TASK-2");

        filter = filter.toBuilder().across(JiraIssuesFilter.DISTINCT.issue_type).build();
        DbListResponse<DbAggregationResult> output = jiraIssueService.groupByAndCalculate(company, filter, false, null, null, null);
        assertThat(output.getRecords()).hasSize(1);
        assertThat(output.getRecords().get(0).getKey()).isEqualTo("TASK");
        assertThat(output.getRecords().get(0).getTotalTickets()).isEqualTo(1);

        // -- parent = SAGA
        filter = JiraIssuesFilter.builder()
                .parentIssueTypes(List.of("SAGA"))
                .build();
        list = jiraIssueService.list(company, filter, null, null, null, null, null, 0, 100);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("TASK-3", "TASK-4");

        filter = filter.toBuilder().across(JiraIssuesFilter.DISTINCT.issue_type).build();
        output = jiraIssueService.groupByAndCalculate(company, filter, false, null, null, null);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output.getRecords()).hasSize(1);
        assertThat(output.getRecords().get(0).getKey()).isEqualTo("TASK");
        assertThat(output.getRecords().get(0).getTotalTickets()).isEqualTo(2);

    }

    @Test
    public void testParentLabels() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("EPIC-1")
                .labels(List.of("A", "B"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("SAGA-1")
                .labels(List.of("C", "D"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-1")
                .issueType("TASK")
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-2")
                .issueType("TASK")
                .parentKey("EPIC-1")
                .parentLabels(List.of("A", "B"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-3")
                .issueType("TASK")
                .parentKey("SAGA-1")
                .parentLabels(List.of("C", "D"))
                .build());
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("TASK-4")
                .issueType("TASK")
                .parentKey("SAGA-1")
                .parentLabels(List.of("C", "D"))
                .build());

        // -- no filter
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .build();
        DbListResponse<DbJiraIssue> list = jiraIssueService.list(company, filter, null, null, null, null, null, 0, 100);
        assertThat(list.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrder("EPIC-1", "SAGA-1", "TASK-1", "TASK-2", "TASK-3", "TASK-4");

        // -- with filter
        BiConsumer<List<String>, List<String>> verifier = (filterByParentLabels, expectedTasks) -> {
            System.out.println(">>> filter by parent_labels=" + filterByParentLabels + " - expectedTasks=" + expectedTasks);
            try {
                JiraIssuesFilter verifierFilter = JiraIssuesFilter.builder()
                        .parentLabels(filterByParentLabels)
                        .build();
                DbListResponse<DbJiraIssue> outputList = jiraIssueService.list(company, verifierFilter, null, null, null, null, null, 0, 100);
                assertThat(outputList.getRecords().stream().map(DbJiraIssue::getKey)).containsExactlyInAnyOrderElementsOf(expectedTasks);

                verifierFilter = verifierFilter.toBuilder().across(JiraIssuesFilter.DISTINCT.issue_type).build();
                DbListResponse<DbAggregationResult> output = jiraIssueService.groupByAndCalculate(company, verifierFilter, false, null, null, null);
                assertThat(output.getRecords()).hasSize(1);
                assertThat(output.getRecords().get(0).getKey()).isEqualTo("TASK");
                assertThat(output.getRecords().get(0).getTotalTickets()).isEqualTo(expectedTasks.size());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // -- parent_label = A
        verifier.accept(List.of("A"), List.of("TASK-2"));
        verifier.accept(List.of("B"), List.of("TASK-2"));
        verifier.accept(List.of("A", "B"), List.of("TASK-2"));
        verifier.accept(List.of("C"), List.of("TASK-3", "TASK-4"));
        verifier.accept(List.of("D"), List.of("TASK-3", "TASK-4"));
        verifier.accept(List.of("C", "D"), List.of("TASK-3", "TASK-4"));
        verifier.accept(List.of("A", "C"), List.of("TASK-2", "TASK-3", "TASK-4"));
    }

    @Test
    public void testConfigVersion() throws SQLException {
        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("proj-1")
                        .configVersion(123L)
                .build());
        DbJiraIssue output = jiraIssueService.get(company, "proj-1", integrationId, 0L).orElseThrow();
        assertThat(output.getConfigVersion()).isEqualTo(123L);

        jiraIssueService.insert(company, buildBaseIssue().toBuilder()
                .key("proj-1")
                .configVersion(456L)
                .build());
        output = jiraIssueService.get(company, "proj-1", integrationId, 0L).orElseThrow();
        assertThat(output.getConfigVersion()).isEqualTo(456L);
    }
}