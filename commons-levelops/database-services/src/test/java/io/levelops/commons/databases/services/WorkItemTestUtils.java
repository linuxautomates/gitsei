package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkItemTestUtils {

    @Value
    public static class TestDbs {
        WorkItemsReportService workItemsReportService;
        WorkItemsAgeReportService workItemsAgeReportService;
        WorkItemsStageTimesReportService workItemsStageTimesReportService;
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
        IntegrationService integrationService;
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
        WorkItemsPrioritySLAService workItemsPrioritySLAService;
        WorkItemsService workItemsService;
    }

    public static TestDbs initDbServices(DataSource dataSource, String company) throws SQLException {
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsReportService workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsStageTimesReportService workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsAgeReportService workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsService workItemsService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);

        workItemsService.ensureTableExistence(company);

        return new TestDbs(workItemsReportService, workItemsAgeReportService, workItemsStageTimesReportService, workItemsResolutionTimeReportService, integrationService, workItemsResponseTimeReportService, workItemsPrioritySLAService, workItemsService);
    }

    public static WorkItem createWorkItem(WorkItemDBService workItemDBService, String company, int i, String productId, State state, String userId) throws SQLException {
        WorkItem.WorkItemBuilder bldr = WorkItem.builder()
                .reason("asd" + i)
                .title("title" + i)
                //.integrationId(integrationId)
                .type(WorkItem.ItemType.AUTOMATED)
                .notify(Boolean.FALSE)
                .title("title" + i)
                .description("description" + i)
                .reporter("abc" + i + "@test.com")
                .productId(productId)
                .stateId(state.getId())
                .status(state.getName())
                .ticketType(WorkItem.TicketType.WORK_ITEM)
                .dueAt(System.currentTimeMillis())
                .artifactTitle("artifactTitle" + i)
                .cloudOwner("cloudOwner" + i)
                .artifact("asd" + i)
                .assignee(WorkItem.Assignee.builder().userId(userId).build());

        WorkItem wi = bldr.build();
        String workItemId = workItemDBService.insert(company, wi);
        Assert.assertNotNull(workItemId);
        return wi.toBuilder().id(workItemId).build();
    }

    public static List<WorkItem> createWorkItems(WorkItemDBService workItemDBService, String company, int n, String productId, State state, String userId) throws SQLException {
        List<WorkItem> workItems = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            workItems.add(createWorkItem(workItemDBService, company, i, productId, state, userId));
        }
        return workItems;
    }

    public static void compareAggResults(String testDesc, List<DbAggregationResult> expectedList,
                                         List<DbAggregationResult> actualList) {
        log.debug("compareAggResultsForAcross: Comparing {} actual results against" +
                " {} expected results for {}", actualList.size(), expectedList.size(), testDesc);
        final Iterator<DbAggregationResult> expectedIter = expectedList.iterator();
        final Iterator<DbAggregationResult> actualIter = actualList.iterator();
        while (expectedIter.hasNext() && actualIter.hasNext()) {
            DbAggregationResult expected = expectedIter.next();
            DbAggregationResult actual = actualIter.next();
            log.info("compareAggResultsForAcross: Comparing actual value {} " +
                    "with expected value {} for fields {}", actual, expected, testDesc);
            assertThat(actual.getKey()).as("key for " + testDesc).isEqualTo(expected.getKey());
            assertThat(actual.getCount()).as("count for " + testDesc+ " for key=" + actual.getKey()).isEqualTo(expected.getCount());
            assertThat(actual.getAdditionalKey()).as("additional key " + testDesc+ " for key=" + actual.getKey()).isEqualTo(expected.getAdditionalKey());
            assertThat(actual.getTotalTickets()).as("total tickets " + testDesc + " for key=" + actual.getKey() + " (additionalKey=" + actual.getAdditionalKey() + ")").isEqualTo(expected.getTotalTickets());
        }
        assertThat((actualIter.hasNext() || expectedIter.hasNext())).as("total count for " + testDesc).isFalse();
    }
}
