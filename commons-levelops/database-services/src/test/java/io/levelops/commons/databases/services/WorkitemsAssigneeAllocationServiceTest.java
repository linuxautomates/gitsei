package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@SuppressWarnings("unused")
public class WorkitemsAssigneeAllocationServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static IssueMgmtSprintMappingDatabaseService sprintMappingService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static WorkItemTimelineService workItemsTimelineService;
    private static WorkItemsService workItemService;
    private static String integrationId;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        sprintMappingService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        workItemsTimelineService = new WorkItemTimelineService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsAssigneeAllocationReportService workItemsAssigneeAllocationReportService
                = new WorkItemsAssigneeAllocationReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, workItemsAssigneeAllocationReportService,
                null, null, null, workItemsFirstAssigneeReportService,
                workItemFieldsMetaService, null);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("Azure-Devops-test")
                .status("enabled")
                .build());
        workItemService.ensureTableExistence(company);
        sprintMappingService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        workItemsTimelineService.ensureTableExistence(company);
        currentTime = new Date();
    }

    @Test
    public void test() throws SQLException, BadRequestException {
        int page = 0;
        int pageSize = 25;
        Instant month1A = Instant.parse("2020-01-01T01:02:03Z");
        Instant month1B = Instant.parse("2020-01-10T01:02:03Z");
        Instant month1C = Instant.parse("2020-01-15T01:02:03Z");
        Instant month1D = Instant.parse("2020-01-20T01:02:03Z");
        Instant month2 = Instant.parse("2020-02-01T01:02:03Z");
        Instant month3 = Instant.parse("2020-03-01T01:02:03Z");

        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("a")
                .integrationId("1")
                .epic("E1")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("b")
                .integrationId("1")
                .epic("E1")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("c")
                .integrationId("1")
                .epic("E2")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("d")
                .integrationId("1")
                .build());
        workItemService.insert(company, DbWorkItem.builder()
                .workItemId("e")
                .integrationId("1")
                .epic("E2")
                .build());

        ArrayList<String> statementToExecute = new ArrayList<>();
        String timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("a")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u1")
                .startDate(Timestamp.from(month1A))
                .endDate(Timestamp.from(month1B))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1B)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("a")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u2")
                .startDate(Timestamp.from(month1B))
                .endDate(Timestamp.from(month1C))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1C)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("a")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u3")
                .startDate(Timestamp.from(month1C))
                .endDate(Timestamp.from(month1D))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1D)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("a")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("UNASSIGNED")
                .startDate(Timestamp.from(month1A))
                .endDate(Timestamp.from(month1B))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1B)));

        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("b")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u2")
                .startDate(Timestamp.from(month1A))
                .endDate(Timestamp.from(month1B))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1B)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("b")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u3")
                .startDate(Timestamp.from(month1B))
                .endDate(Timestamp.from(month1C))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1C)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("b")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u4")
                .startDate(Timestamp.from(month1C))
                .endDate(Timestamp.from(month2))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month2)));

        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("c")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u1")
                .startDate(Timestamp.from(month1A))
                .endDate(Timestamp.from(month1B))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1B)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("c")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u2")
                .startDate(Timestamp.from(month1B))
                .endDate(Timestamp.from(month1C))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1C)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("c")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u3")
                .startDate(Timestamp.from(month1C))
                .endDate(Timestamp.from(month3))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month3)));

        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("d")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u6")
                .startDate(Timestamp.from(month1A))
                .endDate(Timestamp.from(month1B))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month1B)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("d")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u7")
                .startDate(Timestamp.from(month1B))
                .endDate(Timestamp.from(month2))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month2)));
        timeLineId = workItemsTimelineService.insert(company, DbWorkItemHistory.builder()
                .workItemId("d")
                .integrationId("1")
                .fieldType("assignee")
                .fieldValue("u8")
                .startDate(Timestamp.from(month2))
                .endDate(Timestamp.from(month3))
                .build());
        statementToExecute.add(updateEndDate(timeLineId, DateUtils.toEpochSecond(month3)));
        dataSource.getConnection().prepareStatement(String.join(" ", statementToExecute)).execute();

        DbListResponse<DbAggregationResult> result = workItemService.getAssigneeAllocationReport(company,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.epic)
                        .calculation(WorkItemsFilter.CALCULATION.assignees)
                        .build(),
                null, false, null, page, pageSize);
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().key("E1").total(4L).assignees(List.of("u1", "u2", "u3", "u4")).build(),
                DbAggregationResult.builder().key("E2").total(3L).assignees(List.of("u1", "u2", "u3")).build()
        );

        result = workItemService.getAssigneeAllocationReport(company,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.none)
                        .calculation(WorkItemsFilter.CALCULATION.assignees)
                        .build(),
                null, false, null, page, pageSize);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().total(7L).assignees(List.of("u1", "u2", "u3", "u4", "u6", "u7", "u8")).build()
        );

        testAssigneesFilters(month1A, month1B, "u1", "u2", "u6");
        testAssigneesFilters(month1B, month1C, "u2", "u3", "u7");
        testAssigneesFilters(month1C, month2, "u3", "u4", "u7");
        testAssigneesFilters(month2, month3, "u3", "u8");
        testAssigneesFilters(month1B, month2, "u2", "u3", "u4", "u7");
        testAssigneesFilters(month1C, month3, "u3", "u4", "u7", "u8");
    }

    private void testAssigneesFilters(Instant from, Instant to, String... users) throws SQLException, BadRequestException {
        DbListResponse<DbAggregationResult> result = workItemService.getAssigneeAllocationReport(company,
                WorkItemsFilter.builder()
                        .across(WorkItemsFilter.DISTINCT.none)
                        .calculation(WorkItemsFilter.CALCULATION.assignees)
                        .assigneesDateRange(ImmutablePair.of(DateUtils.toEpochSecond(from), DateUtils.toEpochSecond(to)))
                        .build(),
                null, false, null, 0, 25);
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRecords()).containsExactlyInAnyOrder(
                DbAggregationResult.builder().total((long) users.length).assignees(Arrays.asList(users)).build()
        );
    }

    public String updateEndDate(String timelineId, long time) throws SQLException {
        return "update " + company + ".issue_mgmt_workitems_timeline " +
                "set end_date = to_timestamp(" + time + ") where id = '" + timelineId + "';";
    }
}
