package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Moving dashboard controller to internal api to facilitate data-science access
 */
@RestController
@RequestMapping("/internal/v1/tenants/{company}/dashboards")
public class DashboardsController {
    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_DASHBOARD";

    private final DefaultConfigService configService;
    private final DashboardWidgetService dashboardWidgetService;

    @Autowired
    public DashboardsController(DashboardWidgetService dashService,
                                DefaultConfigService configService) {
        this.configService = configService;
        this.dashboardWidgetService = dashService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createDashboard(@RequestBody Dashboard dashboard,
                                                                               @PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(dashboard.getName()) || StringUtils.isEmpty(dashboard.getType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name or type.");
            }
            String id = dashboardWidgetService.insert(company, dashboard);
            if (Boolean.TRUE.equals(dashboard.getIsDefault())) {
                configService.handleDefault(company, id, DEFAULT_TENANT_CONFIG_NAME);
            }
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> dashDelete(@PathVariable("dashboardid") String dashId,
                                                                     @PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                dashboardWidgetService.delete(company, dashId);
                configService.deleteIfDefault(company, dashId, DEFAULT_TENANT_CONFIG_NAME);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(dashId).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(dashId).success(true).build());
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> dashBulkDelete(@PathVariable(name = "company") String company,
                                                                             @RequestBody List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            List<String> filteredIds = ids.stream()
                    .map(NumberUtils::toInt)
                    .map(Number::toString)
                    .collect(Collectors.toList());
            try {
                dashboardWidgetService.bulkDelete(company, filteredIds);
                for (String dashId : ListUtils.emptyIfNull(filteredIds)) {
                    configService.deleteIfDefault(company, dashId, DEFAULT_TENANT_CONFIG_NAME);
                }
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> dashUpdate(@RequestBody Dashboard dashboard,
                                                           @PathVariable("dashboardid") String dashboardId,
                                                           @PathVariable("company") String company) {
        if (StringUtils.isEmpty(dashboard.getId())) {
            dashboard = dashboard.toBuilder().id(dashboardId).build();
        }
        final Dashboard dashToUpdate = dashboard;
        return SpringUtils.deferResponse(() -> {
            dashboardWidgetService.update(company, dashToUpdate);
            if (Boolean.TRUE.equals(dashToUpdate.getIsDefault())) {
                configService.handleDefault(company, dashToUpdate.getId(), DEFAULT_TENANT_CONFIG_NAME);
            }
            return ResponseEntity.ok().build();
        });
    }


    @RequestMapping(method = RequestMethod.GET, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Dashboard>> dashDetails(@PathVariable("dashboardid") String dashboardid,
                                                                 @PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(dashboardWidgetService.get(company, dashboardid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found."))
                .toBuilder()
                .isDefault(dashboardid.equalsIgnoreCase(
                        configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME)))
                .build()));
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Dashboard>>> dashboardsList(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (filter.getFilterValue("default", Boolean.class).orElse(false)) {
                String resultId = configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
                List<Dashboard> dto = new ArrayList<>();
                if (StringUtils.isNotEmpty(resultId)) {
                    dashboardWidgetService.get(company, resultId)
                            .ifPresent(dash -> dto.add(dash.toBuilder().isDefault(true).build()));
                }
                return ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                filter.getPage(),
                                filter.getPageSize(),
                                dto.size(),
                                dto));
            }
            Map<String, Object> partial = (Map<String, Object>) filter.getFilter().get("partial");

            DbListResponse<Dashboard> dashboards = dashboardWidgetService.listByFilters(
                    company,
                    (String) filter.getFilter().get("type"),
                    (String) filter.getFilter().get("owner_id"),
                    (String) (partial != null ?
                            partial.getOrDefault("name", null) : null),
                    (Boolean) filter.getFilter().get("public"),
                    (Integer) filter.getFilter().get("workspace_id"),
                    filter.getPage(),
                    filter.getPageSize(),
                    filter.getSort());
            String defaultId = configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            dashboards.getTotalCount(),
                            ListUtils.emptyIfNull(dashboards.getRecords()).stream()
                                    .map(item -> item.toBuilder()
                                            .isDefault(defaultId.equalsIgnoreCase(item.getId()))
                                            .build())
                                    .collect(Collectors.toList())));
        });
    }


}