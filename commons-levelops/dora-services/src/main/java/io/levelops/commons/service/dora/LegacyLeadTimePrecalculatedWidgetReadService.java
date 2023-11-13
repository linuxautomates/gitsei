package io.levelops.commons.service.dora;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dashboard_widget.models.DashboardMetadata;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DoraWidgetReportSubType;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsValuesResultFilterSortService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class LegacyLeadTimePrecalculatedWidgetReadService {
    private final ObjectMapper objectMapper;
    private final DashboardWidgetService dashboardWidgetService;
    private final WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;
    private final VelocityAggsValuesResultFilterSortService velocityAggsValuesResultFilterSortService;

    @Autowired
    public LegacyLeadTimePrecalculatedWidgetReadService(
            ObjectMapper objectMapper,
            DashboardWidgetService dashboardWidgetService,
            WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService,
            VelocityAggsValuesResultFilterSortService velocityAggsValuesResultFilterSortService
    ) {
        this.objectMapper = objectMapper;
        this.dashboardWidgetService = dashboardWidgetService;
        this.widgetPrecalculatedResultsDBService = widgetPrecalculatedResultsDBService;
        this.velocityAggsValuesResultFilterSortService = velocityAggsValuesResultFilterSortService;
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

    private Object retrievePrecalculatedResult(final String company, DefaultListRequest originalRequest, String reportSubType) {
        Optional<UUID> optWidgetId = parseWidgetID(originalRequest);
        if (optWidgetId.isEmpty()) {
            log.warn("Dora lead time retrieve precalculated reports optWidgetId is empty");
            return null;
        }
        Optional<Integer> optOURefId = parseOURefId(originalRequest);
        Integer ouRefId = -1;
        if (optOURefId.isEmpty()) {
            log.warn("Dora lead time retrieve precalculated reports optOURefId is empty");
            ouRefId = -1;
        } else {
            ouRefId = optOURefId.get();
        }

        if (StringUtils.isBlank(reportSubType)) {
            log.warn("Dora lead time retrieve precalculated reports reportSubType is blank");
            return null;
        }
        String interval = parseInterval(company, optWidgetId.get());
        if (StringUtils.isBlank(interval)) {
            log.warn("Dora lead time retrieve precalculated reports reportSubType is interval");
            return null;
        }
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = null;
        try {
            log.info("Dora lead time retrieve precalculated reports {},{},{},{}", optWidgetId.get(), ouRefId, reportSubType, interval);
            dbListResponse = widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(optWidgetId.get()), List.of(ouRefId), List.of(reportSubType), List.of(interval), null);
        } catch (SQLException e) {
            log.error("Error fetching widget precalculated results!", e);
            return null;
        }
        if ((dbListResponse == null) || CollectionUtils.size(dbListResponse.getRecords()) != 1) {
            log.warn("Dora lead time retrieve precalculated reports dbListResponse.getRecords().size() is not 1");
            return null;
        }
        return dbListResponse.getRecords().get(0).getReport();
    }

    public Optional<List<DbAggregationResult>> getNewVelocityAggsForLeadTime(final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws JsonProcessingException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, DoraWidgetReportSubType.LEAD_TIME_CALCULATION.getDisplayName());
        if (report == null) {
            log.info("Dora lead time retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("Dora lead time retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        List<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        log.info("Dora lead time retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);
    }

    public Optional<List<DbAggregationResult>> getNewVelocityAggsForMeanTime(final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws JsonProcessingException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        Object report = retrievePrecalculatedResult(company, originalRequest, DoraWidgetReportSubType.MEAN_TIME_CALCULATION.getDisplayName());
        if (report == null) {
            log.info("Dora mean time retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("Dora mean time retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        List<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        log.info("Dora mean time retrieve precalculated reports results = {}", results);
        return Optional.ofNullable(results);
    }

    public Optional<DbListResponse<DbAggregationResult>> getVelocityValuesForLeadTime(final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws JsonProcessingException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(originalRequest);
        log.info("Dora lead time retrieve precalculated reports velocityFilter = {}", velocityFilter);
        Set<VelocityConfigDTO.Rating> filterRatings = CollectionUtils.emptyIfNull(velocityFilter.getRatings()).stream().collect(Collectors.toSet());
        log.info("Dora lead time retrieve precalculated reports filterRatings = {}", filterRatings);
        DoraWidgetReportSubType reportSubType = (filterRatings.contains(VelocityConfigDTO.Rating.MISSING)) ? DoraWidgetReportSubType.LEAD_TIME_VALUES_CALCULATION_RATING_MISSING : DoraWidgetReportSubType.LEAD_TIME_VALUES_CALCULATION_RATING_NON_MISSING;
        log.info("Dora lead time retrieve precalculated reports reportSubType = {}", reportSubType);

        Object report = retrievePrecalculatedResult(company, originalRequest, reportSubType.getDisplayName());
        if (report == null) {
            log.info("Dora lead time retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("Dora lead time retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        List<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        log.info("Dora lead time retrieve precalculated reports results = {}", results);

        DbListResponse<DbAggregationResult> filteredSortedVelocityValues = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, originalRequest);
        log.info("Dora lead time retrieve precalculated reports results post_filter_sort.size = {}", CollectionUtils.size(filteredSortedVelocityValues.getRecords()));

        return Optional.ofNullable(filteredSortedVelocityValues);
    }

    public Optional<DbListResponse<DbAggregationResult>> getVelocityValuesForMeanTime(final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult) throws JsonProcessingException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(originalRequest);
        log.info("Dora mean time retrieve precalculated reports velocityFilter = {}", velocityFilter);
        Set<VelocityConfigDTO.Rating> filterRatings = CollectionUtils.emptyIfNull(velocityFilter.getRatings()).stream().collect(Collectors.toSet());
        log.info("Dora mean time retrieve precalculated reports filterRatings = {}", filterRatings);
        DoraWidgetReportSubType reportSubType = (filterRatings.contains(VelocityConfigDTO.Rating.MISSING)) ? DoraWidgetReportSubType.MEAN_TIME_VALUES_CALCULATION_RATING_MISSING : DoraWidgetReportSubType.MEAN_TIME_VALUES_CALCULATION_RATING_NON_MISSING;
        log.info("Dora mean time retrieve precalculated reports reportSubType = {}", reportSubType);

        Object report = retrievePrecalculatedResult(company, originalRequest, reportSubType.getDisplayName());
        if (report == null) {
            log.info("Dora lead time retrieve precalculated reports report is null");
            return Optional.empty();
        }
        log.info("Dora mean time retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);
        List<DbAggregationResult> results = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbAggregationResult.class));
        log.info("Dora mean time retrieve precalculated reports results = {}", results);

        DbListResponse<DbAggregationResult> filteredSortedVelocityValues = velocityAggsValuesResultFilterSortService.filterAndSortVelocityValues(results, originalRequest);
        log.info("Dora mean time retrieve precalculated reports results post_filter_sort.size = {}", CollectionUtils.size(filteredSortedVelocityValues.getRecords()));

        return Optional.ofNullable(filteredSortedVelocityValues);
    }
}

