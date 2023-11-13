package io.levelops.commons.databases.services.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dashboard_widget.models.DashboardMetadata;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.VelocityStageTime;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.jira.models.VelocityStageTimesReportSubType;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getStageStatusesMap;

@Log4j2
@Service
public class VelocityStageTimesReportPrecalculateWidgetService {
    private final ObjectMapper objectMapper;
    private final JiraIssueService jiraIssueService;
    private final OrgUnitHelper orgUnitHelper;
    private final DashboardWidgetService dashboardWidgetService;
    private final VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private final JiraFilterParser jiraFilterParser;
    private final WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;

    @Autowired
    public VelocityStageTimesReportPrecalculateWidgetService(
            ObjectMapper objectMapper,
            JiraIssueService jiraIssueService,
            OrgUnitHelper orgUnitHelper,
            DashboardWidgetService dashboardWidgetService,
            VelocityConfigsDatabaseService velocityConfigsDatabaseService,
            JiraFilterParser jiraFilterParser,
            WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService) {
        this.objectMapper = objectMapper;
        this.jiraIssueService = jiraIssueService;
        this.orgUnitHelper = orgUnitHelper;
        this.dashboardWidgetService = dashboardWidgetService;
        this.velocityConfigsDatabaseService = velocityConfigsDatabaseService;
        this.jiraFilterParser = jiraFilterParser;
        this.widgetPrecalculatedResultsDBService = widgetPrecalculatedResultsDBService;
    }

    @Value
    @Builder(toBuilder = true)
    public static class SortedIssues{
        private Long timeSpentInStage;
        private DbJiraIssue issue;
    }

    public Optional<DbListResponse<DbAggregationResult>> calculateTimeSpentInStageReport(final String company,
                                                                                        DefaultListRequest originalRequest,
                                                                                        Boolean disablePrecalculatedResult) {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        DbListResponse<DbAggregationResult> aggResult = null;

        try {
            OUConfiguration ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            final DefaultListRequest filter = ouConfig.getRequest();
            VelocityConfigDTO velocityConfigDTO = getVelocityConfig(company, filter);
            Validate.notNull(velocityConfigDTO, "velocityConfigDTO cant be missing for velocity_stage_times_report.");
            List<VelocityConfigDTO.Stage> developmentStages = getDevelopmentStages(velocityConfigDTO);
            Map<String, List<String>> velocityStageStatusesMap = getStageStatusesMap(developmentStages);
            aggResult = this.getAggResult(company, JiraIssuesFilter.CALCULATION.velocity_stage_times_report,
                    filter, ouConfig, velocityStageStatusesMap, velocityConfigDTO);
        } catch (Exception e) {
            log.warn("Failed to get velocity_stage_times_report", e);
            throw new RuntimeStreamException(e);
        }

        return Optional.of(aggResult);
    }

    public Optional<DbListResponse<DbJiraIssue>> calculateTimeSpentInStageReportValues(final String company,
                                                                                         DefaultListRequest originalRequest,
                                                                                         Boolean disablePrecalculatedResult) {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }

        DbListResponse<DbJiraIssue> issues = null;
        try {
            OUConfiguration ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            final DefaultListRequest request = ouConfig.getRequest();
            VelocityConfigDTO velocityConfigDTO = getVelocityConfig(company, request);
            Validate.notNull(velocityConfigDTO, "velocityConfigDTO cant be missing for velocity_stage_times_report.");

            JiraIssuesFilter.DISTINCT across = JiraIssuesFilter.DISTINCT.fromString(request.getAcross());
            String interval = StringUtils.defaultString(request.getAggInterval()).trim().toLowerCase();
            JiraIssuesFilter issueFilter = jiraFilterParser.createFilter(company, request, null, across, null, interval, "", false, true);
            JiraSprintFilter jiraSprintFilter = JiraSprintFilter.fromDefaultListRequest(request);

            velocityConfigDTO = checkStagePresentInWorkflowProfile(velocityConfigDTO, issueFilter);

            var page = request.getPage();
            var pageSize = request.getPageSize();

            issues = jiraIssueService.list(company, jiraSprintFilter, issueFilter, Optional.empty(),
                    ouConfig, Optional.ofNullable(velocityConfigDTO),
                    SortingConverter.fromFilter(List.of()),
                    page,
                    pageSize);
        }
        catch (Exception e) {
            log.warn("Failed to get velocity_stage_times_report drill down", e);
            throw new RuntimeStreamException(e);
        }

        return Optional.of(issues);
    }

    public <T> Optional<DbListResponse<T>> getVelocityStageTimeReportPreCalculation(final String company, DefaultListRequest originalRequest, VelocityStageTimesReportSubType reportSubType, Boolean disablePrecalculatedResult) throws BadRequestException, JsonProcessingException {
        if(BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        ImmutablePair<Long, Object> reportWithCalculatedAt = retrievePrecalculatedResult(company, originalRequest, reportSubType.getDisplayName());

        if(reportWithCalculatedAt == null) {
            log.info("Velocity retrieve precalculated report is null");
            return Optional.empty();
        }

        Long calculatedAt = reportWithCalculatedAt.getLeft();
        Object report = reportWithCalculatedAt.getRight();
        if(report == null) {
            log.info("Velocity retrieve precalculated report is null");
            return Optional.empty();
        }
        log.info("Velocity retrieve precalculated report is NOT null");
        log.info("report = {}", report);
        List<T> results = null;

        Integer totalRecords;
        if (reportSubType.getDisplayName().equals(VelocityStageTimesReportSubType.VELOCITY_STAGE_TIME_REPORT.getDisplayName())) {
            results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
            totalRecords = CollectionUtils.size(results);
        }
        else {
            results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbJiraIssue.class));
            ImmutablePair<Integer, List<DbJiraIssue>> drillDownWithTotal = this.parseDrillDownResult((List<DbJiraIssue>)results, originalRequest);
            totalRecords = drillDownWithTotal.getLeft();
            results = (List<T>) drillDownWithTotal.getRight();
        }
        log.info("Velocity retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(DbListResponse.of(results, totalRecords, calculatedAt));
    }

    private ImmutablePair<Integer, List<DbJiraIssue>> parseDrillDownResult(List<DbJiraIssue> result, DefaultListRequest request) throws BadRequestException {
        List<String> velocityStages = (List)request.getFilter().get("velocity_stages");
        String velocityStageName;
        int page = request.getPage();
        int pageSize = request.getPageSize();

        if (CollectionUtils.isNotEmpty(velocityStages)) {
            velocityStageName = velocityStages.get(0);
        } else {
            throw new BadRequestException("velocity stages must be present in filter");
        }

        List<SortedIssues> filteredResult = CollectionUtils.emptyIfNull(result)
                .stream()
                .filter(issue -> filterStage(issue, velocityStageName))
                .map(issue -> mapResultForSorting(issue, velocityStageName))
                .sorted(Comparator.comparing(SortedIssues::getTimeSpentInStage, Comparator.nullsLast(Long::compareTo)).reversed()
                        .thenComparing((issue1, issue2) -> issue1.getIssue().getKey().compareTo(issue2.getIssue().getKey())))
                .collect(Collectors.toList());

        List<DbJiraIssue> paginatedResult = CollectionUtils.emptyIfNull(filteredResult)
                .stream()
                .skip(page * pageSize)
                .limit(pageSize)
                .map(sortedIssue -> sortedIssue.getIssue())
                .collect(Collectors.toList());

        return ImmutablePair.of(CollectionUtils.size(filteredResult), paginatedResult);
    }

    private SortedIssues mapResultForSorting(DbJiraIssue issue, String velocityStageName) {
        if (!"$$ALL_STAGES$$".equals(velocityStageName)) {
            issue = issue.toBuilder()
                    .velocityStage(velocityStageName)
                    .velocityStageTime(getTimeSpentInStage(issue, velocityStageName))
                    .build();
        }

        return SortedIssues.builder()
                .timeSpentInStage(issue.getIssueCreatedAt())
                .issue(issue)
                .build();
    }

    private Long getTimeSpentInStage(DbJiraIssue issue, String stageName) {
        return CollectionUtils.emptyIfNull(issue.getVelocityStages()).stream()
                .filter(x -> x.getStage().equals(stageName))
                .map(x -> x.getTimeSpent())
                .findFirst().get();
    }

    private Boolean filterStage(DbJiraIssue issue, String stageName) {
        if ("$$ALL_STAGES$$".equals(stageName))
            return true;

        List<VelocityStageTime> records = issue.getVelocityStages()
                .stream()
                .filter(x -> x.getStage().equals(stageName))
                .filter(x -> x.getTimeSpent() != 0)
                .collect(Collectors.toList());

        return !records.isEmpty();
    }

    private List<VelocityConfigDTO.Stage> getDevelopmentStages(VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> developmentStages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentStages.addAll(preDevSortedStages);
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> postDevSortedStages = velocityConfigDTO.getPostDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentStages.addAll(postDevSortedStages);
        }

        developmentStages = developmentStages.stream()
                .filter(stage ->
                        stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_STATUS)
                                || stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_RELEASE)
                )
                .collect(Collectors.toList());

        return developmentStages;
    }

    private DbListResponse<DbAggregationResult> getAggResult(String company,
                                                             JiraIssuesFilter.CALCULATION calc,
                                                             DefaultListRequest request,
                                                             OUConfiguration ouConfig,
                                                             Map<String, List<String>> velocityStageStatusesMap,
                                                             VelocityConfigDTO velocityConfigDTO) throws SQLException, BadRequestException {
        JiraIssuesFilter.DISTINCT across = JiraIssuesFilter.DISTINCT.fromString(request.getAcross());
        if (across == null) {
            across = JiraIssuesFilter.DISTINCT.assignee;
        }
        //
        JiraIssuesFilter issuesFilter = jiraFilterParser.createFilter(company, request, calc, across, null,
                request.getAggInterval(), false, false);
        JiraIssuesFilter finalIssuesFilter = issuesFilter;
        final var finalOuConfig = ouConfig;

        return jiraIssueService.stackedGroupBy(company, finalIssuesFilter, null, null, finalOuConfig, velocityConfigDTO, velocityStageStatusesMap);
    }

    private VelocityConfigDTO getVelocityConfig(final String company, DefaultListRequest filter) throws SQLException, NotFoundException, BadRequestException {
        if (filter.getFilterValue("velocity_config_id", String.class).isEmpty()) {
            throw new BadRequestException("Velocity config id must be present for this report.");
        }
        String velocityConfigId = filter.getFilterValue("velocity_config_id", String.class).orElse(null);

        VelocityConfig velocityConfig = velocityConfigsDatabaseService.get(company, velocityConfigId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Company %s, Velocity Config Id %s not found!", company, velocityConfigId)
                ));
        VelocityConfigDTO velocityConfigDTO = velocityConfig.getConfig();

        return velocityConfigDTO;
    }

    private Optional<UUID> parseWidgetID(DefaultListRequest originalRequest) {
        if (originalRequest == null) {
            return null;
        }
        //Use list request -> widget_id if present
        if (StringUtils.isNotBlank(originalRequest.getWidgetId())) {
            return Optional.ofNullable(UUID.fromString(originalRequest.getWidgetId()));
        }
        //Use list request -> filter -> widget_id if present
        String widgetIdFromFilter = originalRequest.getFilterValue("widget_id", String.class).orElse(null);
        if (StringUtils.isNotBlank(widgetIdFromFilter)) {
            return Optional.ofNullable(UUID.fromString(widgetIdFromFilter));
        }
        return Optional.empty();
    }

    private Optional<Integer> parseOURefId(DefaultListRequest originalRequest) {
        if (CollectionUtils.size(originalRequest.getOuIds()) != 1) {
            return Optional.empty();
        }
        return originalRequest.getOuIds().stream().findFirst();
    }

    private String parseInterval(String company, UUID widgetId) {
        Optional<Widget> optionalWidget = null;
        try {
            optionalWidget = dashboardWidgetService.getWidget(company, widgetId.toString(), null);
        } catch (SQLException e) {
            log.error("Error retrieving widget from db!", e);
            return null;
        }
        if (optionalWidget.isEmpty()) {
            return null;
        }
        Optional<Dashboard> optionalDashboard = dashboardWidgetService.get(company, optionalWidget.get().getDashboardId());
        if (optionalDashboard.isEmpty()) {
            return null;
        }
        Dashboard dashboard = optionalDashboard.get();

        DashboardMetadata dashboardMetadata = null;
        try {
            dashboardMetadata = objectMapper.readValue(objectMapper.writeValueAsString(dashboard.getMetadata()), DashboardMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
            return null;
        }
        if (dashboardMetadata.getDashboardTimeRangeFilter() == null) {
            return "default";
        }
        return dashboardMetadata.getDashboardTimeRangeFilter().toString();
    }

    private ImmutablePair<Long, Object> retrievePrecalculatedResult(final String company, DefaultListRequest originalRequest, String reportSubType) {
        Optional<UUID> optWidgetId = parseWidgetID(originalRequest);
        if (optWidgetId.isEmpty()) {
            log.warn("Velocity lead time by time spent in stage report retrieve precalculated reports optWidgetId is empty");
            return null;
        }
        Optional<Integer> optOURefId = parseOURefId(originalRequest);
        Integer ouRefId = -1;
        if (optOURefId.isEmpty()) {
            log.warn("Velocity lead time by time spent in stage report retrieve precalculated reports optOURefId is empty");
            ouRefId = -1;
        } else {
            ouRefId = optOURefId.get();
        }

        if (StringUtils.isBlank(reportSubType)) {
            log.warn("Velocity lead time by time spent in stage report retrieve precalculated reports reportSubType is blank");
            return null;
        }
        String interval = parseInterval(company, optWidgetId.get());
        if (StringUtils.isBlank(interval)) {
            log.warn("Velocity lead time by time spent in stage report retrieve precalculated reports reportSubType is interval");
            return null;
        }
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = null;
        try {
            log.info("Velocity lead time by time spent in stage report retrieve precalculated reports {},{},{},{}", optWidgetId.get(), ouRefId, reportSubType, interval);
            dbListResponse = widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(optWidgetId.get()), List.of(ouRefId), List.of(reportSubType), List.of(interval), null);
        } catch (SQLException e) {
            log.error("Error fetching widget precalculated results!", e);
            return null;
        }
        if ((dbListResponse == null) || CollectionUtils.size(dbListResponse.getRecords()) != 1) {
            log.warn("Velocity lead time by time spent in stage report retrieve precalculated reports dbListResponse.getRecords().size() is not 1");
            return null;
        }
        WidgetPrecalculatedReport widgetPrecalculatedReport = dbListResponse.getRecords().get(0);

        return ImmutablePair.of(widgetPrecalculatedReport.getCalculatedAt().getEpochSecond(), widgetPrecalculatedReport.getReport());
    }

    private VelocityConfigDTO checkStagePresentInWorkflowProfile(VelocityConfigDTO velocityConfigDTO,
                                                                 JiraIssuesFilter issueFilter) throws SQLException, NotFoundException {
        List<VelocityConfigDTO.Stage> developmentStages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            developmentStages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            developmentStages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
        }
        List<String> workFlowStageNames = new ArrayList<>(getStageStatusesMap(developmentStages).keySet());
        workFlowStageNames.add("Other");
        workFlowStageNames.add("$$ALL_STAGES$$");
        if (issueFilter != null && CollectionUtils.isNotEmpty(issueFilter.getVelocityStages())) {
            issueFilter.getVelocityStages().forEach(velocityStage -> {
                if (!workFlowStageNames.contains(velocityStage)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The velocity stage: " + velocityStage + "" +
                            " is not present in the provided workflow profile");
                }
            });
        }

        return velocityConfigDTO;
    }
}
