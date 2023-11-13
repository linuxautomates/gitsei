package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/dashboards/{dashboard-id:[0-9]+}/widgets")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN', 'ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class WidgetsController {

    private final DashboardWidgetService dashboardWidgetService;

    @Autowired
    public WidgetsController(DashboardWidgetService dashboardWidgetService) {
        this.dashboardWidgetService = dashboardWidgetService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createWidget(@RequestBody Widget widget,
                                                                            @PathVariable("dashboard-id") String dashboardId,
                                                                            @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(widget.getName()) || StringUtils.isEmpty(widget.getType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name or type.");
            }
            String id = dashboardWidgetService.insertWidget(company, widget, dashboardId);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{widget-id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Object>> deleteWidget(@PathVariable("dashboard-id") String dashboardId,
                                                               @PathVariable("widget-id") String widgetId,
                                                               @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            Boolean success = dashboardWidgetService.deleteWidget(company, widgetId, dashboardId);
            return ResponseEntity.ok(Map.of("deleted_row",widgetId,"success",success));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, ? extends Serializable>>> deleteBlukWidget(@RequestBody List<String> ids,
                                                                                                @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            int count = dashboardWidgetService.bulkWidgetsDelete(company, ids);
            return ResponseEntity.ok(Map.of("deleted_rows", count, "success", (count == CollectionUtils.size(ids))));
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{widget-id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Object>> updateWidget(@RequestBody Widget widget,
                                                               @PathVariable("dashboard-id") String dashboardId,
                                                               @PathVariable("widget-id") String widgetId,
                                                               @SessionAttribute(name = "company") String company) {
        if (StringUtils.isEmpty(widget.getId())) {
            widget = widget.toBuilder().id(widgetId).build();
        }
        final Widget widgetToUpdate = widget;
        return SpringUtils.deferResponse(() -> {
            Boolean success = dashboardWidgetService.updateWidget(company, widgetToUpdate);
            return ResponseEntity.ok(Map.of("updated_row",widgetId,"success",success));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{widget-id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Widget>> getWidget(@PathVariable("dashboard-id") String dashboardId,
                                                            @PathVariable("widget-id") String widgetId,
                                                            @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(dashboardWidgetService.getWidget(company, widgetId, dashboardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Widget "+widgetId+" not found."))
                .toBuilder()
                .build()));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Widget>>> listWidgets(@PathVariable("dashboard-id") String dashboardId,
                                                                                 @SessionAttribute(name = "company") String company,
                                                                                 @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<Widget> widgets = dashboardWidgetService.listByFilters(
                    company,
                    dashboardId,
                    filter.getPage(),
                    filter.getPageSize());
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            widgets.getTotalCount(),
                            ListUtils.emptyIfNull(widgets.getRecords()).stream()
                                    .map(item -> item.toBuilder()
                                            .build())
                                    .collect(Collectors.toList())));
        });
    }
}
