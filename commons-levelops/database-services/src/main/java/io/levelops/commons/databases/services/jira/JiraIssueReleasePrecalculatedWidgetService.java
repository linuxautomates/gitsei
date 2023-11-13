package io.levelops.commons.databases.services.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.precalculation.WidgetPrecalculatedReport;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.jira.models.JiraReleaseReportSubType;
import io.levelops.commons.databases.services.precalculation.WidgetPrecalculatedResultsDBService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.levelops.commons.dashboard_widget.models.DashboardMetadata;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraIssueReleasePrecalculatedWidgetService {

    private final ObjectMapper objectMapper;
    private final DashboardWidgetService dashboardWidgetService;
    private final WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService;

    @Autowired
    public JiraIssueReleasePrecalculatedWidgetService(ObjectMapper objectMapper,
                                                      DashboardWidgetService dashboardWidgetService,
                                                      WidgetPrecalculatedResultsDBService widgetPrecalculatedResultsDBService) {
        this.objectMapper = objectMapper;
        this.dashboardWidgetService = dashboardWidgetService;
        this.widgetPrecalculatedResultsDBService = widgetPrecalculatedResultsDBService;
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

    public Optional<DbListResponse<JiraReleaseResponse>> jiraReleaseTableReport(
            final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult
    ) throws JsonProcessingException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }

        Optional<UUID> optWidgetId = parseWidgetID(originalRequest);
        if (optWidgetId.isEmpty()) {
            log.warn("Jira release report retrieve precalculated reports optWidgetId is empty");
            return Optional.empty();
        }
        Optional<Integer> optOURefId = parseOURefId(originalRequest);
        Integer ouRefId = -1;
        if (optOURefId.isEmpty()) {
            log.warn("Jira release report retrieve precalculated reports optOURefId is empty");
            ouRefId = -1;
        } else {
            ouRefId = optOURefId.get();
        }

        String interval = parseInterval(company, optWidgetId.get());
        if (StringUtils.isBlank(interval)) {
            log.warn("Jira release report retrieve precalculated reports reportSubType is interval");
            return Optional.empty();
        }
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = null;
        try {
            log.info("Jira release report retrieve precalculated reports {},{},{}", optWidgetId.get(), ouRefId, interval);
            dbListResponse = widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(optWidgetId.get()), List.of(ouRefId), List.of(JiraReleaseReportSubType.JIRA_RELEASE_TABLE_REPORT.getDisplayName()), List.of(interval), null);
        } catch (SQLException e) {
            log.error("Error fetching widget precalculated results!", e);
            return Optional.empty();
        }
        if ((dbListResponse == null) || CollectionUtils.size(dbListResponse.getRecords()) != 1) {
            log.warn("Jira release report retrieve precalculated reports dbListResponse.getRecords().size() is not 1");
            return Optional.empty();
        }
        Object report = dbListResponse.getRecords().get(0).getReport();
        Instant calculatedAt = dbListResponse.getRecords().get(0).getCalculatedAt();

        if (report == null) {
            log.info("Jira release report retrieve precalculated reports report is null");
            return Optional.empty();
        }

        log.info("Jira release report retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);

        List<JiraReleaseResponse> records = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraReleaseResponse.class));

        Integer page = originalRequest.getPage();
        Integer pageSize = originalRequest.getPageSize();
        Integer totalSize = records.size();

        records = CollectionUtils.emptyIfNull(records).stream()
                .skip(page * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        DbListResponse<JiraReleaseResponse> finalResults = DbListResponse.of(
                records,
                totalSize,
                calculatedAt.getEpochSecond()
        );

        log.info("Jira release report retrieve precalculated reports results = {}", finalResults);
        return Optional.ofNullable(finalResults);
    }

    public Optional<DbListResponse<DbJiraIssue>> jiraReleaseTableReportDrillDown(
            final String company, DefaultListRequest originalRequest, Boolean disablePrecalculatedResult
    ) throws JsonProcessingException, BadRequestException {
        if (BooleanUtils.isTrue(disablePrecalculatedResult)) {
            return Optional.empty();
        }

        Optional<UUID> optWidgetId = parseWidgetID(originalRequest);
        if (optWidgetId.isEmpty()) {
            log.warn("Jira release report drilldown retrieve precalculated reports optWidgetId is empty");
            return Optional.empty();
        }
        Optional<Integer> optOURefId = parseOURefId(originalRequest);
        Integer ouRefId = -1;
        if (optOURefId.isEmpty()) {
            log.warn("Jira release report drilldown retrieve precalculated reports optOURefId is empty");
            ouRefId = -1;
        } else {
            ouRefId = optOURefId.get();
        }

        String interval = parseInterval(company, optWidgetId.get());
        if (StringUtils.isBlank(interval)) {
            log.warn("Jira release report drilldown retrieve precalculated reports reportSubType is interval");
            return Optional.empty();
        }
        DbListResponse<WidgetPrecalculatedReport> dbListResponse = null;
        try {
            log.info("Jira release report drilldown retrieve precalculated reports {},{},{}", optWidgetId.get(), ouRefId, interval);
            dbListResponse = widgetPrecalculatedResultsDBService.listByFilter(company, 0, 100, null, List.of(optWidgetId.get()), List.of(ouRefId), List.of(JiraReleaseReportSubType.JIRA_RELEASE_TABLE_REPORT_VALUES.getDisplayName()), List.of(interval), null);
        } catch (SQLException e) {
            log.error("Error fetching widget precalculated results!", e);
            return Optional.empty();
        }
        if ((dbListResponse == null) || CollectionUtils.size(dbListResponse.getRecords()) != 1) {
            log.warn("Jira release report drilldown retrieve precalculated reports dbListResponse.getRecords().size() is not 1");
            return Optional.empty();
        }
        Object report = dbListResponse.getRecords().get(0).getReport();
        if (report == null) {
            log.info("Jira release report drilldown retrieve precalculated reports report is null");
            return Optional.empty();
        }

        log.info("Jira release report drilldown retrieve precalculated reports report is NOT null");
        log.info("report = {}", report);

        List<DbJiraIssue> records = objectMapper.readValue(report.toString(), objectMapper.getTypeFactory().constructCollectionLikeType(List.class, DbJiraIssue.class));

        Integer page = originalRequest.getPage();
        Integer pageSize = originalRequest.getPageSize();

        List<DbJiraIssue> issues = null;
        List<String> fixVersions = (List<String>)originalRequest.getFilter().get("fix_versions");
        Integer totalCount = -1;

        if (fixVersions != null && CollectionUtils.isNotEmpty(fixVersions)) {
            issues = CollectionUtils.emptyIfNull(records).stream()
                    .filter(x -> x.getFixVersion().equals(fixVersions.get(0)))
                    .sorted(Comparator.comparing(DbJiraIssue::getIssueCreatedAt))
                    .collect(Collectors.toList());
            totalCount = issues.size();

            issues = CollectionUtils.emptyIfNull(issues).stream()
                    .skip(page * pageSize)
                    .limit(pageSize)
                    .collect(Collectors.toList());
        }
        else {
            throw new BadRequestException("Fix versions is not present");
        }

        DbListResponse<DbJiraIssue> finalResults = DbListResponse.of(
                issues,
                totalCount
        );

        log.info("Jira release report drilldown retrieve precalculated reports results = {}", finalResults);
        return Optional.ofNullable(finalResults);
    }
}
