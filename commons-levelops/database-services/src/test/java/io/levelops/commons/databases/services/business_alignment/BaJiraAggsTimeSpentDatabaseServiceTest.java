package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.util.TriFunction;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class BaJiraAggsTimeSpentDatabaseServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final String COMPANY = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final long INGESTED_AT = 1634768550;
    private static final long INGESTED_AT_PREV = 1633768550;

    DataSource dataSource;

    BaJiraAggsDatabaseService baJiraAggsDatabaseService;
    JiraIssueService jiraIssueService;
    String integrationId;
    JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;
    UserIdentityService userIdentityService;

    JiraIssuesFilter filter = JiraIssuesFilter.builder()
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
            .projects(List.of("LEV"))
            .build();

    private Optional<String> userIdOf(String displayName) {
        return userIdentityService.getUserByDisplayName(COMPANY, "1", displayName);
    }

    @Before
    public void setUp() throws Exception {
        dataSource = DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        BaJiraAggsQueryBuilder baJiraAggsQueryBuilder = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder = new BaJiraAggsActiveWorkQueryBuilder(baJiraAggsQueryBuilder);

        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());

        baJiraAggsDatabaseService = new BaJiraAggsDatabaseService(dataSource, baJiraAggsQueryBuilder, baJiraAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);

        integrationId = DatabaseTestUtils.createIntegrationId(jiraTestDbs.getIntegrationService(), COMPANY, "jira");
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        jiraStatusMetadataDatabaseService = jiraTestDbs.getJiraStatusMetadataDatabaseService();

        jiraStatusMetadataDatabaseService.insert(COMPANY,
                DbJiraStatusMetadata.builder()
                        .integrationId(integrationId)
                        .status("IN PROGRESS")
                        .statusCategory("IN PROGRESS")
                        .statusId("1")
                        .build());
        jiraStatusMetadataDatabaseService.insert(COMPANY,
                DbJiraStatusMetadata.builder()
                        .integrationId(integrationId)
                        .status("IN QA")
                        .statusCategory("IN PROGRESS")
                        .statusId("2")
                        .build());
        jiraStatusMetadataDatabaseService.insert(COMPANY,
                DbJiraStatusMetadata.builder()
                        .integrationId(integrationId)
                        .status("DONE")
                        .statusCategory("DONE")
                        .statusId("3")
                        .build());

        userIdentityService = jiraTestDbs.getUserIdentityService();
        userIdentityService.ensureTableExistence(COMPANY);

        String input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(user -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(user, "1");
            jiraIssueService.insertJiraUser(COMPANY, tmp);
            if (user.getDisplayName() != null) {
                try {
                    userIdentityService.batchUpsert(COMPANY,
                            List.of(DbScmUser.builder()
                                    .integrationId("1")
                                    .cloudId(user.getAccountId())
                                    .displayName(user.getDisplayName())
                                    .originalDisplayName(user.getDisplayName())
                                    .build()));
                } catch (SQLException throwables) {
                    System.out.println("Failed to insert into integration_users with display name: " + user.getDisplayName() + " , company: " + COMPANY + ", integration id:" + "1");
                }
            }
        });
    }

    public DbJiraIssue buildBaseJiraIssue(String integrationId) {
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
                .assigneeId(userIdOf("gandalf").isPresent() ? userIdOf("gandalf").get() : "")
                .assignee("frodo")
                .assigneeId(userIdOf("frodo").isPresent() ? userIdOf("frodo").get() : "")
                .build();
    }

    private void insertMockData() throws SQLException {
        TriFunction<String, String, Long, DbJiraStatus> statusBuilder = (String key, String status, Long duration) -> DbJiraStatus.builder().integrationId(integrationId).issueKey(key).status(status)
                .startTime(10L).endTime(10L + duration).build();

        // ---- ingested_at = INGESTED_AT
        DbJiraIssue issue1 = buildBaseJiraIssue(integrationId).toBuilder().ingestedAt(INGESTED_AT).build();
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-1").issueType("BUG").storyPoints(20).issueResolvedAt(1600000000L)
                .statuses(List.of(statusBuilder.apply("LEV-1", "IN PROGRESS", 10L), statusBuilder.apply("LEV-1", "DONE", 1000L))).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-2").issueType("TASK").storyPoints(5).issueResolvedAt(1600000000L)
                .statuses(List.of(statusBuilder.apply("LEV-2", "IN PROGRESS", 10L), statusBuilder.apply("LEV-2", "IN QA", 50L), statusBuilder.apply("LEV-2", "DONE", 1000L))).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-3").issueType("TASK").statusCategory("IN PROGRESS").build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-4").issueType("EPIC").issueResolvedAt(1600300000L)
                .statuses(List.of(statusBuilder.apply("LEV-4", "IN PROGRESS", 30L), statusBuilder.apply("LEV-4", "DONE", 1000L))).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-5").issueType("TASK").issueResolvedAt(1600400000L).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LEV-6").issueType("BUG").assignee("aragorn").storyPoints(100).issueResolvedAt(1600400000L)
                .statuses(List.of(statusBuilder.apply("LEV-6", "IN PROGRESS", 10L), statusBuilder.apply("LEV-6", "IN QA", 70L), statusBuilder.apply("LEV-6", "DONE", 1000L))).build());
        jiraIssueService.insert(COMPANY, issue1.toBuilder().key("LFE-1").issueType("TASK").assignee("aragorn").project("LFE").issueResolvedAt(1600600000L)
                .statuses(List.of(statusBuilder.apply("LFE-1", "IN PROGRESS", 20L))).build());
        /*
         frodo:
         - cat1: LEV-1 (10) / 100 = 0.1
         - cat2: LEV-2 (10+50=60) / 100 = .6
         - other: LEV-4 (30) / 100 = .3
         - total: 10+60+30=100
         aragorn:
         - cat1: LEV-6 (80) / 100 = .8
         - cat2: LFE-1 (20) / 100 = .2 -- filtered out
         - total: 100
         ---
         cat1: .1+.8 = .9
         cat2: .6
         other: .3
         */

    }

    @Test
    public void testTicketTimeSpentAcrossCat() throws SQLException, BadRequestException {
        insertMockData();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(0.9f).total(200L).effort(90L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.6f).total(100L).effort(60L).build(),
                DbAggregationResult.builder().key("Other").fte(0.3f).total(100L).effort(30L).build()
        );
    }

    @Test
    public void testTicketTimeSpentAcrossAssignee() throws SQLException, BadRequestException {
        insertMockData();
        // -- cat 1
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.ASSIGNEE, filter.toBuilder()
                .ticketCategories(List.of("cat1"))
                .build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("aragorn").fte(0.8f).total(100L).effort(80L).build(),
                DbAggregationResult.builder().key("frodo").fte(0.1f).total(100L).effort(10L).build()
        );
        // -- cat 2
        response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.ASSIGNEE, filter.toBuilder()
                .ticketCategories(List.of("cat2"))
                .build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.6f).total(100L).effort(60L).build()
        );
        // -- other
        response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.ASSIGNEE, filter.toBuilder()
                .ticketCategories(List.of("Other"))
                .build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("frodo").fte(0.3f).total(100L).effort(30L).build()
        );
    }

    @Test
    public void testTicketTimeSpentAcrossResolvedAt() throws SQLException, BadRequestException {
        insertMockData();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.ISSUE_RESOLVED_AT, filter.toBuilder()
                .ticketCategories(List.of("cat1", "Other"))
                .assignees(List.of(userIdOf("frodo").isPresent() ? userIdOf("frodo").get() : ""))
                .build(), null, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("1600239600").additionalKey("16-9-2020").fte(1.0f).total(30L).effort(30L).build(),
                DbAggregationResult.builder().key("1600326000").additionalKey("17-9-2020").fte(1.0f).total(80L).effort(80L).build(),
                DbAggregationResult.builder().key("1599980400").additionalKey("13-9-2020").fte(0.14285715f).total(70L).effort(10L).build()
        );
    }

    @Test
    public void testTicketTimeSpentBaOptions() throws SQLException, BadRequestException {
        insertMockData();
        DbListResponse<DbAggregationResult> response = baJiraAggsDatabaseService.calculateTicketTimeSpentFTE(COMPANY, JiraAcross.TICKET_CATEGORY, filter, BaJiraOptions.builder()
                        .inProgressStatuses(List.of("IN QA", "DONE"))
                .build(), null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("cat1").fte(1.3278688f).total(4120L).effort(2070L).build(),
                DbAggregationResult.builder().key("cat2").fte(0.3442623f).total(3050L).effort(1050L).build(),
                DbAggregationResult.builder().key("Other").fte(0.32786885f).total(3050L).effort(1000L).build()
        );
    }

}