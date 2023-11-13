package io.levelops.commons.service.dora;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsAgeReportService;
import io.levelops.commons.databases.services.WorkItemsBouncesReportService;
import io.levelops.commons.databases.services.WorkItemsFirstAssigneeReportService;
import io.levelops.commons.databases.services.WorkItemsHopsReportService;
import io.levelops.commons.databases.services.WorkItemsMetadataService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsReportService;
import io.levelops.commons.databases.services.WorkItemsResolutionTimeReportService;
import io.levelops.commons.databases.services.WorkItemsResponseTimeReportService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkItemsStageTimesReportService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class ADODoraServiceTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static WorkItemsReportService workItemsReportService;
    private static ADODoraService adoDoraService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static UserIdentityService userIdentityService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        adoDoraService = new ADODoraService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        integrationService = new IntegrationService(dataSource);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsMetadataService workItemsMetadataService = new WorkItemsMetadataService(dataSource);
        WorkItemsBouncesReportService workItemsBouncesReportService = new WorkItemsBouncesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsHopsReportService workItemsHopsReportService = new WorkItemsHopsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService,
                workItemsBouncesReportService, workItemsHopsReportService, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        workItemService.ensureTableExistence(COMPANY);
        workItemsMetadataService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);

        integrationService.insert(COMPANY, Integration.builder()
                .application("azure_devops")
                .name("issue mgmt test1")
                .status("enabled")
                .build());

        String workitemsResourcePath = "json/dora/workitems_data.json";
        String workitemsResolvedtimeResourcePath = "json/dora/workitems_resolved_time.json";
        String workItemsString = ResourceUtils.getResourceAsString(workitemsResourcePath);
        String workItemsResolvedTimeString = ResourceUtils.getResourceAsString(workitemsResolvedtimeResourcePath);
        List<DbWorkItem> workItems = mapper.readValue(workItemsString,
                mapper.getTypeFactory().constructCollectionLikeType(List.class, DbWorkItem.class));
        List<Map<String, String>> workItemResolvedTimes = mapper.readValue(workItemsResolvedTimeString,
                mapper.getTypeFactory().constructCollectionLikeType(List.class, Map.class));
        int i = 0;
        for (DbWorkItem item : workItems) {
            if (workItemResolvedTimes.get(i).get("workitem_resolved_at") != null) {
                item = item.toBuilder().workItemResolvedAt(Timestamp.from(DateUtils.parseDateTime(workItemResolvedTimes.get(i).get("workitem_resolved_at")))).build();
            }
            workItemService.insert(COMPANY, item);
            i++;
        }
    }

    @Test
    public void testCFRWithWorkflowFiltersOnly() throws SQLException, IOException, BadRequestException {
        String filtersPath = "json/dora/ado_velocity_filters.json";
        String reqFilterPath = "json/dora/ado_req_resolved_filter.json";
        String key = "change_failure_rate_only_workflow_filters";
        String reqKey = "only_resolved";
        String filterString = ResourceUtils.getResourceAsString(filtersPath);
        String reqFilterString = ResourceUtils.getResourceAsString(reqFilterPath);
        Map<String, Object> filter = mapper.readValue(filterString, new TypeReference<>() {
        });
        Map<String, Object> reqFilter = mapper.readValue(reqFilterString, new TypeReference<>() {
        });
        DefaultListRequest requestForWorkflow = DefaultListRequest.builder().filter((Map<String, Object>) filter.get(key)).build();
        DefaultListRequest request = DefaultListRequest.builder().filter((Map<String, Object>) reqFilter.get(reqKey)).build();

        WorkItemsFilter workflowFilters = WorkItemsFilter.fromDefaultListRequest(requestForWorkflow, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter workflowMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(requestForWorkflow, "workitem_");

        WorkItemsFilter reqFilters = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter reqMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

        DoraResponseDTO responseDTO = adoDoraService.getTimeSeriesDataForDeployment(
                COMPANY, workflowFilters, workflowMilestoneFilters, reqFilters, reqMilestoneFilters, null, "workitem_resolved_at"
        );
        assertChangeFailureRate(responseDTO);
    }

    @Test
    public void testCFRWithRequestFilter() throws SQLException, IOException, BadRequestException {
        String filtersPath = "json/dora/ado_velocity_filters.json";
        String reqFilterPath = "json/dora/ado_req_resolved_filter.json";
        String key = "change_failure_rate_workflow_filters_with_req_filter";
        String reqKey = "resolved_with_status";
        String filterString = ResourceUtils.getResourceAsString(filtersPath);
        String reqFilterString = ResourceUtils.getResourceAsString(reqFilterPath);
        Map<String, Object> filter = mapper.readValue(filterString, new TypeReference<>() {
        });
        Map<String, Object> reqFilter = mapper.readValue(reqFilterString, new TypeReference<>() {
        });
        DefaultListRequest requestForWorkflow = DefaultListRequest.builder().filter((Map<String, Object>) filter.get(key)).build();
        DefaultListRequest request = DefaultListRequest.builder().filter((Map<String, Object>) reqFilter.get(reqKey)).build();

        WorkItemsFilter workflowFilters = WorkItemsFilter.fromDefaultListRequest(requestForWorkflow, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter workflowMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(requestForWorkflow, "workitem_");

        WorkItemsFilter reqFilters = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter reqMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

        DoraResponseDTO responseDTO = adoDoraService.getTimeSeriesDataForDeployment(
                COMPANY, workflowFilters, workflowMilestoneFilters, reqFilters, reqMilestoneFilters, null, "workitem_resolved_at"
        );
        assertChangeFailureRate(responseDTO);
    }

    @Test
    public void testDFWithWorkflowFiltersOnly() throws SQLException, IOException, BadRequestException {
        String filtersPath = "json/dora/ado_velocity_filters.json";
        String reqFilterPath = "json/dora/ado_req_resolved_filter.json";
        String key = "deployment_frequency_rate_only_workflow_filters";
        String reqKey = "only_resolved";
        String filterString = ResourceUtils.getResourceAsString(filtersPath);
        String reqFilterString = ResourceUtils.getResourceAsString(reqFilterPath);
        Map<String, Object> filter = mapper.readValue(filterString, new TypeReference<>() {
        });
        Map<String, Object> reqFilter = mapper.readValue(reqFilterString, new TypeReference<>() {
        });
        DefaultListRequest requestForWorkflow = DefaultListRequest.builder().filter((Map<String, Object>) filter.get(key)).build();
        DefaultListRequest request = DefaultListRequest.builder().filter((Map<String, Object>) reqFilter.get(reqKey)).build();

        WorkItemsFilter workflowFilters = WorkItemsFilter.fromDefaultListRequest(requestForWorkflow, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter workflowMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(requestForWorkflow, "workitem_");

        WorkItemsFilter reqFilters = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter reqMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

        DoraResponseDTO responseDTO = adoDoraService.getTimeSeriesDataForDeployment(
                COMPANY, workflowFilters, workflowMilestoneFilters, reqFilters, reqMilestoneFilters, null, "workitem_resolved_at"
        );
        assertDeploymentFrequency(responseDTO);
    }

    @Test
    public void testDFWithRequestFilter() throws SQLException, IOException, BadRequestException {
        String filtersPath = "json/dora/ado_velocity_filters.json";
        String reqFilterPath = "json/dora/ado_req_resolved_filter.json";
        String key = "deployment_frequency_workflow_filters_with_req_filter";
        String reqKey = "resolved_with_status";
        String filterString = ResourceUtils.getResourceAsString(filtersPath);
        String reqFilterString = ResourceUtils.getResourceAsString(reqFilterPath);
        Map<String, Object> filter = mapper.readValue(filterString, new TypeReference<>() {
        });
        Map<String, Object> reqFilter = mapper.readValue(reqFilterString, new TypeReference<>() {
        });
        DefaultListRequest requestForWorkflow = DefaultListRequest.builder().filter((Map<String, Object>) filter.get(key)).build();
        DefaultListRequest request = DefaultListRequest.builder().filter((Map<String, Object>) reqFilter.get(reqKey)).build();

        WorkItemsFilter workflowFilters = WorkItemsFilter.fromDefaultListRequest(requestForWorkflow, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter workflowMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(requestForWorkflow, "workitem_");

        WorkItemsFilter reqFilters = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.none, WorkItemsFilter.CALCULATION.issue_count);
        WorkItemsMilestoneFilter reqMilestoneFilters = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

        DoraResponseDTO responseDTO = adoDoraService.getTimeSeriesDataForDeployment(
                COMPANY, workflowFilters, workflowMilestoneFilters, reqFilters, reqMilestoneFilters, null, "workitem_resolved_at"
        );
        assertDeploymentFrequency(responseDTO);
    }

    private void assertChangeFailureRate(DoraResponseDTO responseDTO) {
        Assert.assertNotNull(responseDTO.getStats());
        Assert.assertNotNull(responseDTO.getStats().getTotalDeployment());
        Assert.assertEquals(2, (int) responseDTO.getStats().getTotalDeployment());

        Assert.assertNotNull(responseDTO.getTimeSeries());
        Assert.assertNotNull(responseDTO.getTimeSeries().getDay());
        Assert.assertNotNull(responseDTO.getTimeSeries().getWeek());
        Assert.assertNotNull(responseDTO.getTimeSeries().getMonth());
        Assert.assertEquals(30, responseDTO.getTimeSeries().getDay().size());
        Assert.assertEquals(5, responseDTO.getTimeSeries().getWeek().size());
        Assert.assertEquals(1, responseDTO.getTimeSeries().getMonth().size());
        String day = "11-03-2022";
        String week = "07-03-2022";
        String month = "01-03-2022";
        Assert.assertEquals(2, (long)responseDTO.getTimeSeries().getDay().stream().filter(data -> data.getAdditionalKey().equals(day)).findFirst().get().getCount());
        Assert.assertEquals(2, (long)responseDTO.getTimeSeries().getWeek().stream().filter(data -> data.getAdditionalKey().equals(week)).findFirst().get().getCount());
        Assert.assertEquals(2, (long)responseDTO.getTimeSeries().getMonth().stream().filter(data -> data.getAdditionalKey().equals(month)).findFirst().get().getCount());
    }

    private void assertDeploymentFrequency(DoraResponseDTO responseDTO){
        Assert.assertNotNull(responseDTO.getStats());
        Assert.assertNotNull(responseDTO.getStats().getTotalDeployment());
        Assert.assertEquals(39, (int) responseDTO.getStats().getTotalDeployment());

        Assert.assertNotNull(responseDTO.getTimeSeries());
        Assert.assertNotNull(responseDTO.getTimeSeries().getDay());
        Assert.assertNotNull(responseDTO.getTimeSeries().getWeek());
        Assert.assertNotNull(responseDTO.getTimeSeries().getMonth());
        Assert.assertEquals(30, responseDTO.getTimeSeries().getDay().size());
        Assert.assertEquals(5, responseDTO.getTimeSeries().getWeek().size());
        Assert.assertEquals(1, responseDTO.getTimeSeries().getMonth().size());

        List< DoraTimeSeriesDTO.TimeSeriesData> daysData = responseDTO.getTimeSeries().getDay().stream().filter(data -> data.getCount() > 0).collect(Collectors.toList());
        List< DoraTimeSeriesDTO.TimeSeriesData> weeksData = responseDTO.getTimeSeries().getWeek().stream().filter(data -> data.getCount() > 0).collect(Collectors.toList());
        List< DoraTimeSeriesDTO.TimeSeriesData> monthsData = responseDTO.getTimeSeries().getMonth().stream().filter(data -> data.getCount() > 0).collect(Collectors.toList());

        Assert.assertEquals(3, daysData.size());
        Assert.assertEquals(3, weeksData.size());
        Assert.assertEquals(1, monthsData.size());
    }

    @Test
    public void testGetDrillDownData() throws SQLException {
        DbListResponse<DbWorkItem> items = adoDoraService.getDrillDownData(COMPANY,
                WorkItemsFilter.builder().build(), WorkItemsMilestoneFilter.builder().build(),
                WorkItemsFilter.builder()
                        .statusCategories(List.of("Completed"))
                        .statuses(List.of("Done"))
                        .workItemTypes(List.of("Issue"))
                        .workItemResolvedRange(ImmutablePair.of(1646912471L, 1647063305L))
                        .build(), WorkItemsMilestoneFilter.builder().build(),
                OUConfiguration.builder().build(),
                0,1000, false);

        DefaultObjectMapper.prettyPrint(items.getRecords());
        Assert.assertEquals(5, items.getRecords().size());
        items.getRecords().forEach(workItem -> {
            Assert.assertNotNull(workItem.getStatus());
            Assert.assertEquals("Completed", workItem.getStatusCategory());
            Assert.assertEquals("Done", workItem.getStatus());
            Assert.assertEquals("Issue", workItem.getWorkItemType());
        });
    }
}
