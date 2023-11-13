package io.levelops.api.controllers;

import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.PermissionCheckDTO;
import io.harness.authz.acl.model.ResourceScope;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.JsonDiffService;
import io.levelops.api.model.DashboardDTO;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.models.JsonDiff;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/dashboards")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class DashboardsController {
    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_DASHBOARD";
    private static final String RBAC_KEY = "rbac";

    private final DefaultConfigService configService;
    private final DashboardWidgetService dashboardWidgetService;
    private final UserService userService;
    private final OUDashboardService ouDashboardService;
    private final OrgUnitCategoryDatabaseService categoryService;
    private final OrgUnitsDatabaseService ouService;
    private final ActivityLogService activityLogService;
    private final JsonDiffService jsonDiffService = new JsonDiffService(DefaultObjectMapper.get());
    private final Set<String> activityLogWidgetDiffForTenants;

    private final Auth auth;

    @Autowired
    public DashboardsController(DashboardWidgetService dashService,
                                DefaultConfigService configService,
                                UserService userService,
                                final OUDashboardService ouDashboardService,
                                final OrgUnitCategoryDatabaseService categoryService,
                                final OrgUnitsDatabaseService ouService,
                                ActivityLogService activityLogService,
                                @Value("${ACTIVITY_LOG_WIDGET_DIFF_FOR_TENANTS:}") String activityLogWidgetDiffForTenants, Auth auth) {
        this.configService = configService;
        this.dashboardWidgetService = dashService;
        this.userService = userService;
        this.ouDashboardService = ouDashboardService;
        this.categoryService = categoryService;
        this.ouService = ouService;
        this.activityLogService = activityLogService;
        this.activityLogWidgetDiffForTenants = CommaListSplitter.splitToStream(activityLogWidgetDiffForTenants)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.auth = auth;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createDashboard(@RequestBody DashboardDTO dashboard,
                                                                               @SessionAttribute(name = "company") String company,
                                                                               @SessionAttribute(name = "session_user") String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            String id = null;
            boolean success = false;
            try {
                if (StringUtils.isEmpty(dashboard.getName()) || StringUtils.isEmpty(dashboard.getType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name or type.");
                }
                if (CollectionUtils.isEmpty(dashboard.getCategory())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one category is required with a new dashboard.");
                }
                // validate categories exists
                var categories = categoryService.list(company, QueryFilter.builder().strictMatch("id", dashboard.getCategory()).build(), 0, 1000);
                if (categories.getTotalCount() != dashboard.getCategory().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verify the categories sent. The request contained '" + dashboard.getCategory().size() + "' but we could only find '" + categories.getTotalCount() + "'.");
                }
                // get root ou ids for all the categories
                var ouIds = categories.getRecords().stream()
                        .map(OrgUnitCategory::getRootOuId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                id = dashboardWidgetService.insert(company, dashboard);
                if (Boolean.TRUE.equals(dashboard.getIsDefault())) {
                    configService.handleDefault(company, id, DEFAULT_TENANT_CONFIG_NAME);
                }
                // get OUs to get dashboard list to add the new one at the end (direct dashboards since this is a root OU)
                final String finalId = id;
                ouIds.forEach(ouId -> {
                    var ouDashboards = ouService.getDashboardsIdsForOuId(company, ouId);
                    int dashboardOrder = ouDashboards.getRecords().stream().map(OUDashboard::getDashboardOrder).max(Integer::compare).orElse(0);
                    try {
                        ouDashboardService.insert(company, OUDashboard.builder()
                                .dashboardId(Integer.parseInt(finalId))
                                .dashboardOrder(dashboardOrder == 0 ? 1 : dashboardOrder + 1)
                                .ouId(ouId) // this needs to be ref_id if not versioned
                                .build());
                    } catch (NumberFormatException | SQLException e) {
                        log.warn("Unable to save the root ou mapping: dashboard_id={}, ou_id={}", finalId, ouId, e);
                    }
                });

                success = true;

                return ResponseEntity.ok(Map.of("id", id));
            } finally {
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(id)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.DASHBOARD)
                        .action(ActivityLog.Action.CREATED)
                        .details(Map.of())
                        .body("Dashboard creation: status=" + (success ? "success" : "failure") + ", id=" + id)
                        .build());
            }
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> dashDelete(@PathVariable("dashboardid") String dashId,
                                                                     @SessionAttribute(name = "company") String company,
                                                                     @SessionAttribute(name = "session_user") String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            boolean success = false;
            try {
                try {
                    dashboardWidgetService.delete(company, dashId);
                    configService.deleteIfDefault(company, dashId, DEFAULT_TENANT_CONFIG_NAME);
                } catch (Exception e) {
                    return ResponseEntity.ok(DeleteResponse.builder().id(dashId).success(false).error(e.getMessage()).build());
                }
                success = true;
                return ResponseEntity.ok(DeleteResponse.builder().id(dashId).success(true).build());
            } finally {
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(dashId)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.DASHBOARD)
                        .action(ActivityLog.Action.DELETED)
                        .details(Map.of())
                        .body("Dashboard delete: status=" + (success ? "success" : "failure") + ", id=" + dashId)
                        .build());
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> dashBulkDelete(@SessionAttribute(name = "company") String company,
                                                                             @SessionAttribute(name = "session_user") String sessionUser,
                                                                             @RequestBody List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            List<String> filteredIds = ids.stream()
                    .map(NumberUtils::toInt)
                    .map(Number::toString)
                    .collect(Collectors.toList());
            try {
                dashboardWidgetService.bulkDelete(company, filteredIds);
                for (String dashId : ListUtils.emptyIfNull(filteredIds)) {
                    boolean success = false;
                    try {
                        configService.deleteIfDefault(company, dashId, DEFAULT_TENANT_CONFIG_NAME);
                        success = true;
                    } finally {
                        activityLogService.insert(company, ActivityLog.builder()
                                .targetItem(dashId)
                                .email(sessionUser)
                                .targetItemType(ActivityLog.TargetItemType.DASHBOARD)
                                .action(ActivityLog.Action.DELETED)
                                .details(Map.of())
                                .body("Dashboard delete: status=" + (success ? "success" : "failure") + ", id=" + dashId)
                                .build());
                    }
                }
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public Object dashUpdate(@RequestBody Dashboard dashboard,
                             @PathVariable("dashboardid") String dashboardId,
                             @SessionAttribute(name = "company") String company,
                             @SessionAttribute("session_user") final String sessionUser) throws NotFoundException, ForbiddenException {
        // LEV-4954: validate permission to edit RBAC settings
        Map<String, Object> metadata = parseMetadata(dashboard);
        // LEV-5063: disabling validation for now as it is too strict - we need to implement granular updates to metadata field
        /*
        if (metadata.containsKey(RBAC_KEY)) {
            Dashboard dbDashboard = dashboardWidgetService.get(company, dashboardId)
                    .orElseThrow(() -> new NotFoundException("Could not find dashboard with id=" + dashboardId));
            if (!isUserOwnerOfDashboard(company, dbDashboard, sessionUser)) {
                log.error("Only the creator of the dashboard can edit RBAC settings: tenant={} dashboardId={} user={}", company, dashboardId, sessionUser);
                throw new ForbiddenException("Only the creator of the dashboard can edit RBAC settings");
            }
        }
        */

        Dashboard.DashboardBuilder<?, ?> dashboardBuilder = dashboard.toBuilder();
        dashboardBuilder.ownerId(null); // LEV-4954: since we rely on ownerId for RBAC, we don't want it to be changed
        if (StringUtils.isEmpty(dashboard.getId())) {
            dashboardBuilder.id(dashboardId);
        }
        final Dashboard dashToUpdate = dashboardBuilder.build();
        return SpringUtils.deferResponse(() -> {
            boolean success = false;
            Dashboard dashboardBefore = null;
            boolean logWidgetDiff = activityLogWidgetDiffForTenants.contains(company);
            try {
                // for audit log purposes, get current data to find out the diff
                dashboardBefore = logWidgetDiff ? getDashboardSafe(company, dashboardId) : null;

                dashboardWidgetService.update(company, dashToUpdate);
                if (Boolean.TRUE.equals(dashToUpdate.getIsDefault())) {
                    configService.handleDefault(company, dashToUpdate.getId(), DEFAULT_TENANT_CONFIG_NAME);
                }
                success = true;
                return ResponseEntity.ok().build();
            } finally {
                Dashboard dashboardAfter = logWidgetDiff ? getDashboardSafe(company, dashboardId) : null;
                Map<String, Object> widgetsDiff = logWidgetDiff ? getWidgetsDiff(company, dashboardId, dashboardBefore, dashboardAfter) : null;
                Map<String, Object> dashboardDiff = logWidgetDiff? getDashboardDiff(company, dashboardId, dashboardBefore, dashboardAfter) : null;

                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(dashboardId)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.DASHBOARD)
                        .action(ActivityLog.Action.EDITED)
                        .details(logWidgetDiff
                                ? Map.of(
                                    "widgets_diff", widgetsDiff,
                                    "dashboard", dashboardDiff)
                                : Map.of())
                        .body("Dashboard edit: status=" + (success ? "success" : "failure") + ", id=" + dashboardId)
                        .build());
            }
        });
    }

    private Dashboard getDashboardSafe(String company, String dashboardId) {
        try {
            return dashboardWidgetService.get(company, dashboardId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get dashboard for company={}, id={}", company, dashboardId, e);
            return null;
        }
    }

    private Map<String, Object> getDashboardDiff(String company, String dashboardId,
                                                 @Nullable Dashboard dashboardBefore, @Nullable Dashboard dashboardAfter) {
        if (dashboardBefore == null || dashboardAfter == null) {
            return Map.of(
                    "missing_before", dashboardBefore == null,
                    "missing_after", dashboardAfter == null
            );
        }

        try {
            Map<String, Object> dashboardDiff = new HashMap<>();

            Object metadataBefore = dashboardBefore.getMetadata();
            Object metadataAfter = dashboardAfter.getMetadata();
            try {
                calculateDiff(metadataBefore, metadataAfter)
                        .ifPresent(diff -> dashboardDiff.put("metadata", diff));
            } catch (IOException e) {
                log.warn("Failed to get dashboard metadata diff for dashboardId={}", dashboardId, e);
            }

            return dashboardDiff;
        } catch (Exception e) {
            log.warn("Failed to calculate dashboard for company={}, dashboardId={}", company, dashboardId, e);
            return Map.of("error", e.getMessage());
        }
    }

    private Map<String, Object> getWidgetsDiff(String company, String dashboardId,
                                               @Nullable Dashboard dashboardBefore, @Nullable Dashboard dashboardAfter) {
        if (dashboardBefore == null || dashboardAfter == null) {
            return Map.of(
                    "missing_before", dashboardBefore == null,
                    "missing_after", dashboardAfter == null
            );
        }

        try {
            List<Widget> widgetsBefore = ListUtils.emptyIfNull(dashboardBefore.getWidgets());
            List<Widget> widgetsAfter = ListUtils.emptyIfNull(dashboardAfter.getWidgets());
            Map<String, Widget> widgetsBeforeById = widgetsBefore.stream().collect(Collectors.toMap(Widget::getId, w -> w));
            Map<String, Widget> widgetsAfterById = widgetsAfter.stream().collect(Collectors.toMap(Widget::getId, w -> w));
            Set<String> idsBefore = widgetsBeforeById.keySet();
            Set<String> idsAfter = widgetsAfterById.keySet();

            SetUtils.SetView<String> idsRemoved = SetUtils.difference(idsBefore, idsAfter);
            SetUtils.SetView<String> idsAdded = SetUtils.difference(idsAfter, idsBefore);
            SetUtils.SetView<String> idsIntersection = SetUtils.intersection(idsAfter, idsBefore);

            Map<String, JsonDiff> queryDiffByWidgetId = new HashMap<>();
            Map<String, JsonDiff> metadataDiffByWidgetId = new HashMap<>();
            idsIntersection.forEach(id -> {
                Widget widgetBefore = widgetsBeforeById.get(id);
                Widget widgetAfter = widgetsAfterById.get(id);
                Object queryBefore = widgetBefore.getQuery();
                Object queryAfter = widgetAfter.getQuery();
                Object metadataBefore = widgetBefore.getMetadata();
                Object metadataAfter = widgetAfter.getMetadata();
                try {
                    calculateDiff(queryBefore, queryAfter)
                            .ifPresent(diff -> queryDiffByWidgetId.put(id, diff));
                } catch (IOException e) {
                    log.warn("Failed to get widget query diff for widgetId={}", id, e);
                }
                try {
                    calculateDiff(metadataBefore, metadataAfter)
                            .ifPresent(diff -> metadataDiffByWidgetId.put(id, diff));
                } catch (IOException e) {
                    log.warn("Failed to get widget query diff for widgetId={}", id, e);
                }
            });

            return Map.of(
                    "added", idsAdded,
                    "removed", idsRemoved,
                    "edited_query", queryDiffByWidgetId,
                    "edited_metadata", metadataDiffByWidgetId
            );
        } catch (Exception e) {
            log.warn("Failed to calculate widget diff for company={}, dashboardId={}", company, dashboardId, e);
            return Map.of("error", e.getMessage());
        }
    }

    private Optional<JsonDiff> calculateDiff(Object jsonBefore, Object jsonAfter) throws IOException {
        Map<String, JsonDiff> diff = jsonDiffService.diff(
                DefaultObjectMapper.get().writeValueAsString(jsonBefore),
                DefaultObjectMapper.get().writeValueAsString(jsonAfter));
        JsonDiff jsonDiff = diff.get("/");
        if (jsonDiff != null && !(jsonDiff.getAdded().isEmpty() && jsonDiff.getRemoved().isEmpty() && jsonDiff.getChanged().isEmpty())) {
            return Optional.of(jsonDiff);
        }
        return Optional.empty();
    }

    private boolean isUserOwnerOfDashboard(String company, Dashboard dbDashboard, String userEmail) throws NotFoundException {
        String ownerEmail = null;
        if (StringUtils.isNotEmpty(dbDashboard.getOwnerId())) {
            try {
                ownerEmail = userService.get(company, dbDashboard.getOwnerId())
                        .map(User::getEmail)
                        .orElse(null);
            } catch (SQLException e) {
                throw new NotFoundException("Could not find owner of dashboard id=" + dbDashboard.getId() + " with userId=" + dbDashboard.getOwnerId());
            }
        }
        return ownerEmail != null && ownerEmail.equalsIgnoreCase(userEmail);
    }

    @Nonnull
    protected static Map<String, Object> parseMetadata(Dashboard dashboard) {
        if (dashboard == null || dashboard.getMetadata() == null || !(dashboard.getMetadata() instanceof Map)) {
            return Map.of();
        }
        return MapUtils.emptyIfNull(ParsingUtils.toJsonObject(DefaultObjectMapper.get(), dashboard.getMetadata()));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{dashboardid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Dashboard>> dashDetails(@PathVariable("dashboardid") String dashboardid,
                                                                 @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(dashboardWidgetService.get(company, dashboardid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dashboard not found."))
                .toBuilder()
                .isDefault(dashboardid.equalsIgnoreCase(
                        configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME)))
                .build()));
    }

    @SuppressWarnings("unchecked")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public ResponseEntity<PaginatedResponse<Dashboard>> dashboardsList(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @SessionAttribute(name = "user_type") String userType,
            @RequestBody DefaultListRequest filter,
            @SessionAttribute(name = "accessContext") AccessContext accessContext) throws ACLClientException, SQLException {

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

        boolean isRbacFilterEnabled = filter.getFilter().get("has_rbac_access") != null;
        String rbacFilterEmail = null;

        if (isRbacFilterEnabled && userType.equals(RoleType.PUBLIC_DASHBOARD.toString())) {
            log.info("PUBLIC_DASHBOARD user type, setting rbacFilterEmail. User: " + sessionUser + ", tenant: " + company);
            rbacFilterEmail = sessionUser;
        } else if (isRbacFilterEnabled) {
            log.info("Admin or equivalent user type, setting rbacFilterEmail to null. User: " + sessionUser + ", tenant: " + company);
        }

        DbListResponse<Dashboard> dashboards = dashboardWidgetService.listByFilters(company,
                DashboardWidgetService.DashboardFilter.builder()
                        .ids((List<String>) filter.getFilter().getOrDefault("ids", List.of()))
                        .type((String) filter.getFilter().get("type"))
                        .ownerId((String) filter.getFilter().get("owner_id"))
                        .name((String) (partial != null ?
                                partial.getOrDefault("name", null) : null))
                        .isPublic((Boolean) filter.getFilter().get("public"))
                        .workspaceId((Integer) filter.getFilter().get("workspace_id"))
                        .rbacUserEmail(rbacFilterEmail)
                        .build(),
                filter.getPage(), filter.getPageSize(), filter.getSort());
        if (!auth.isLegacy()) {
            dashboards = getOusWithAccessControlDetails(company, dashboards.getRecords(), accessContext);
        }

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
    }

    private DbListResponse<Dashboard> getOusWithAccessControlDetails(String company, List<Dashboard> dashboards, AccessContext accessContext) throws ACLClientException {
        List<PermissionCheckDTO> permissionCheckDTOList = new ArrayList();
        ResourceScope resourceScope = accessContext.getResourceScope();
        log.info("Insights: will look for resorcescoupe {}", resourceScope);
        for(Dashboard ouDashboard : dashboards){
            PermissionCheckDTO permission = PermissionCheckDTO.builder()
                    .resourceScope(resourceScope)
                    .resourceType(ResourceType.SEI_INSIGHTS.name())
                    .resourceIdentifier(ouDashboard.getId())
                    .permission(Permission.INSIGHTS_VIEW.getPermission())
                    .build();
            permissionCheckDTOList.add(permission);
        }
        log.info("Insights: will look for permissions {}", permissionCheckDTOList);
        AccessCheckRequestDTO accessCheckRequestDTO = AccessCheckRequestDTO.builder()
                .principal(accessContext.getPrincipal())
                .permissions(permissionCheckDTOList)
                .build();

        ACLClient aclClient = accessContext.getAclClient();

        AccessCheckResponseDTO response = aclClient.checkAccess(accessCheckRequestDTO);

        Set<String> permittedList = response.getAccessCheckDataResponse().getAccessControlList().stream().filter(f  -> f.isPermitted()).map(a -> a.getResourceIdentifier()).collect(Collectors.toSet());
        List<Dashboard> dashboards2 = dashboards.stream().filter(d -> permittedList.contains(d.getId())).collect(Collectors.toList());
        log.info("Original list size is {}, List size with access control details is {}", dashboards.size(), dashboards2.size());
        return DbListResponse.of(dashboards2, dashboards2.size());
    }
}