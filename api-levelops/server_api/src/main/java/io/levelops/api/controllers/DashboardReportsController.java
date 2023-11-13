package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.dashboard.DashboardReport;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/dashboard_reports")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class DashboardReportsController {

    private final DashboardWidgetService dashboardWidgetService;

    @Autowired
    public DashboardReportsController(DashboardWidgetService dashService) {
        this.dashboardWidgetService = dashService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> insertReport(@RequestBody DashboardReport report,
                                                                            @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(report.getName()) || StringUtils.isEmpty(report.getDashboardId())
                    || StringUtils.isEmpty(report.getFileId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name or dashboard_id or file_id.");
            }
            String id = dashboardWidgetService.insertReport(company, report);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{reportid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> reportDelete(@PathVariable("reportid") String reportId,
                                                                       @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                dashboardWidgetService.deleteReport(company, reportId);
                return ResponseEntity.ok(DeleteResponse.builder().id(reportId)
                        .success(true).build());
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(reportId)
                        .success(false).error(e.getMessage()).build());
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> reportBulkDelete(@SessionAttribute(name = "company") String company,
                                                                               @RequestBody List<String> reportIds) {
        return SpringUtils.deferResponse(() -> {
            List<String> filteredReportIds = reportIds.stream()
                    .map(NumberUtils::toInt)
                    .map(Number::toString)
                    .collect(Collectors.toList());
            try {
                dashboardWidgetService.deleteBulkReport(company, filteredReportIds);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(reportIds, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(reportIds, false, e.getMessage()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_DELETE)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DashboardReport>>> reportsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            Map<String, Object> partial = (Map<String, Object>) filter.getFilter().get("partial");
            DbListResponse<DashboardReport> dashboards = dashboardWidgetService.listReports(
                    company,
                    (String) (partial != null ?
                            partial.getOrDefault("name", null) : null),
                    (String) filter.getFilter().get("dashboard_id"),
                    (String) filter.getFilter().get("created_by"),
                    filter.getPage(),
                    filter.getPageSize());
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            dashboards));
        });
    }

}