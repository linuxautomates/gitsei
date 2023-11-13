package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dashboard_widget.models.DashboardMetadata;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class BaJiraAggsPrecalculatedWidgetReadService {
    private final ObjectMapper objectMapper;
    private final DashboardWidgetService dashboardWidgetService;
    private final WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;

    @Autowired
    public BaJiraAggsPrecalculatedWidgetReadService(ObjectMapper objectMapper, DashboardWidgetService dashboardWidgetService, WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService) {
        this.objectMapper = objectMapper;
        this.dashboardWidgetService = dashboardWidgetService;
        this.widgetPrecalculatedResultsDBService = widgetPrecalculatedResultsDBService;
    }

    private Optional<UUID> parseWidgetID(DefaultListRequest originalRequest) {
        if(originalRequest == null) {
            return Optional.empty();
        }
        //Use list request -> widget_id if present
        if(StringUtils.isNotBlank(originalRequest.getWidgetId())) {
            return Optional.ofNullable(UUID.fromString(originalRequest.getWidgetId()));
        }
        //Use list request -> filter -> widget_id if present
        String widgetIdFromFilter = originalRequest.getFilterValue("widget_id", String.class).orElse(null);
        if(StringUtils.isNotBlank(widgetIdFromFilter)) {
            return Optional.ofNullable(UUID.fromString(widgetIdFromFilter));
        }
        return Optional.empty();
    }
    private Optional<Integer> parseOURefId(DefaultListRequest originalRequest) {
        if(CollectionUtils.size(originalRequest.getOuIds()) != 1) {
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
        if(optionalWidget.isEmpty()) {
            return null;
        }
        Optional<Dashboard> optionalDashboard = dashboardWidgetService.get(company, optionalWidget.get().getDashboardId());
        if(optionalDashboard.isEmpty()) {
            return null;
        }
        Dashboard dashboard = optionalDashboard.get();

        DashboardMetadata dashboardMetadata = null;
        try {
            dashboardMetadata =  objectMapper.readValue(objectMapper.writeValueAsString(dashboard.getMetadata()), DashboardMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Exception", e);
            return null;
        }
        if(dashboardMetadata.getDashboardTimeRangeFilter() == null) {
            return "default";
        }
        return dashboardMetadata.getDashboardTimeRangeFilter().toString();
    }
    private String parseTicketCategorizationSchemeId(DefaultListRequest originalRequest) {
        if(originalRequest == null) {
            return null;
        }
        String ticketCategorizationSchemeId = originalRequest.getFilterValue("ticket_categorization_scheme", String.class).orElse(null);
        return ticketCategorizationSchemeId;
    }
    private String parseBACategory(DefaultListRequest originalRequest) {
        if(originalRequest == null) {
            return null;
        }
        List<String> baCategories = DefaultListRequestUtils.getListOrDefault(originalRequest, "ticket_categories");
        if(CollectionUtils.isEmpty(baCategories)) {
            return null;
        }
        return baCategories.get(0);
    }

    private Object retrievePrecalculatedResult(final String company, DefaultListRequest originalRequest, String uriUnit) {
        Optional<UUID> optWidgetId = parseWidgetID(originalRequest);
        if(optWidgetId.isEmpty()) {
            log.warn("BA Jira retrieve precalculated reports optWidgetId is empty");
            return null;
        }
        Optional<Integer> optOURefId = parseOURefId(originalRequest);
        Integer ouRefId = -1;
        if(optOURefId.isEmpty()) {
            log.warn("BA Jira retrieve precalculated reports optOURefId is empty");
            ouRefId = -1;
        } else {
            ouRefId = optOURefId.get();
        }

        String ticketCategorizationSchemeId = parseTicketCategorizationSchemeId(originalRequest);
        String baCategory = parseBACategory(originalRequest);
        String reportSubType = BaJiraReportSubType.generateReportSubType(uriUnit, ticketCategorizationSchemeId, baCategory);

        if(StringUtils.isBlank(reportSubType)) {
            log.warn("BA Jira retrieve precalculated reports reportSubType is blank");
            return null;
        }
        String interval = parseInterval(company, optWidgetId.get());
        if(StringUtils.isBlank(interval)) {
            log.warn("BA Jira retrieve precalculated reports reportSubType is interval");
            return null;
        }
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = null;
        try {
            log.info("BA Jira retrieve precalculated reports {},{},{},{}", optWidgetId.get(), ouRefId, reportSubType, interval);
            dbListResponse = widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(optWidgetId.get()), List.of(ouRefId), List.of(reportSubType), List.of(interval), null);
        } catch (SQLException e) {
            log.error("Error fetching widget precalculated results!", e);
            return null;
        }
        if ((dbListResponse == null) || CollectionUtils.size(dbListResponse.getRecords()) != 1) {
            log.warn("BA Jira retrieve precalculated reports dbListResponse.getRecords().size() is not 1");
            return null;
        }
        return dbListResponse.getRecords().get(0).getReport();
    }

    public Optional<PaginatedResponse<DbAggregationResult>> calculateTicketTimeSpentFTE(String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws Exception {
        if(BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, "effort_investment_time_spent");
        if(report == null) {
            log.info("BA Jira TicketTimeSpentFTE retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("BA Jira TicketTimeSpentFTE retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        PaginatedResponse<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
        log.info("BA Jira TicketTimeSpentFTE retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);
    }

    public Optional<PaginatedResponse<DbAggregationResult>> calculateTicketCountFTE(String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws Exception {
        if(BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, "tickets_report");
        if(report == null) {
            log.info("BA Jira TicketCountFTE retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("BA Jira TicketCountFTE retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        PaginatedResponse<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
        log.info("BA Jira TicketCountFTE retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);

    }

    public Optional<PaginatedResponse<DbAggregationResult>> calculateStoryPointsFTE(String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws Exception {
        if(BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, "story_point_report");
        if(report == null) {
            log.info("BA Jira StoryPointsFTE retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("BA Jira StoryPointsFTE retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        PaginatedResponse<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
        log.info("BA Jira StoryPointsFTE retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);

    }

    public Optional<PaginatedResponse<DbAggregationResult>> calculateScmCommitCountFTE(String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws Exception {
        if(BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, "commit_count_fte");
        if(report == null) {
            log.info("BA Jira ScmCommitCountFTE retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("BA Jira ScmCommitCountFTE retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        PaginatedResponse<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
        log.info("BA Jira ScmCommitCountFTE retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);

    }
}

