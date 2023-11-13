package io.levelops.api.services;

import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class WorkspaceBulkOperationService {
    private static final String ACTIVITY_LOG_TEXT = "%s Products item: %s.";
    private final ProductService productService;
    private final OrgUnitCategoryDatabaseService categoryService;
    private final OrgUnitsDatabaseService unitsService;
    private final ActivityLogService activityLogService;
    private final OUDashboardService ouDashboardService;
    private final DashboardWidgetService dashboardWidgetService;
    private final OrgUnitHelper unitsHelper;

    public WorkspaceBulkOperationService(ProductService productService,
                                         OrgUnitCategoryDatabaseService categoryService,
                                         ActivityLogService activityLogService,
                                         OrgUnitsDatabaseService unitsService,
                                         OUDashboardService ouDashboardService,
                                         DashboardWidgetService dashboardWidgetService,
                                         OrgUnitHelper unitsHelper
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.activityLogService = activityLogService;
        this.ouDashboardService = ouDashboardService;
        this.unitsService = unitsService;
        this.unitsHelper = unitsHelper;
        this.dashboardWidgetService = dashboardWidgetService;
    }

    // A lightweight version of ProductController.createProduct that does not create default categories and their
    // Root OUs
    private String createWorkspace(Product workspace, String company, String sessionUser) throws SQLException {
        String workspaceId = productService.insert(company, workspace);
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(workspaceId)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                .body(String.format(ACTIVITY_LOG_TEXT, "Created", workspaceId))
                .details(Collections.singletonMap("item", workspace))
                .action(ActivityLog.Action.CREATED)
                .build());
        return workspaceId;
    }

    public String cloneWorkspace(
            String oldWorkspaceId,
            String newWorkspaceName,
            String newWorkspaceKey,
            String company,
            String sessionUser) throws BadRequestException, SQLException {
        Optional<Product> originalWorkspaceOption = productService.get(company, oldWorkspaceId);
        if (originalWorkspaceOption.isEmpty()) {
            throw new BadRequestException("Given workspace does not exist: " + oldWorkspaceId);
        }
        Product originalWorkspace = originalWorkspaceOption.get();
        Product newWorkspace = Product.builder()
                .name(newWorkspaceName)
                .description(originalWorkspace.getDescription())
                .integrationIds(originalWorkspace.getIntegrationIds())
                .integrations(originalWorkspace.getIntegrations())
                .bootstrapped(originalWorkspace.getBootstrapped())
                .immutable(originalWorkspace.getImmutable())
                .ownerId(originalWorkspace.getOwnerId())
                .key(newWorkspaceKey)
                .build();

        String newWorkspaceId = createWorkspace(newWorkspace, company, sessionUser);
        log.info("Cloned workspace id: " + newWorkspaceId);

        HashMap<UUID, String> oldCategoryIdToNewCategoryId = new HashMap<>();
        HashMap<Integer, Integer> oldOuRefIdToNewOuRefId = new HashMap<>();

        var oldCategories = PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(
                page -> categoryService.list(
                        company,
                        QueryFilter.builder().strictMatch("workspace_id", Integer.parseInt(oldWorkspaceId)).build(),
                        page,
                        100
                ).getRecords())).collect(Collectors.toList());
        log.info("Old categories: " + oldCategories);

        // Create new categories. This does not create the ous because the root ou name is null for the old categories
        oldCategories.forEach(oldCategory -> {
            var newCategory = OrgUnitCategory.builder()
                    .name(oldCategory.getName())
                    .description(oldCategory.getDescription())
                    .enabled(oldCategory.getEnabled())
                    .workspaceId(Integer.parseInt(newWorkspaceId))
                    .build();
            try {
                String newCategoryId = categoryService.insert(company, newCategory);
                oldCategoryIdToNewCategoryId.put(oldCategory.getId(), newCategoryId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        var oldOus = PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(
                        page -> unitsService.filter(
                                company,
                                QueryFilter.builder().strictMatch("workspace_id", Integer.parseInt(oldWorkspaceId)).build(),
                                page,
                                100).getRecords()))
                .collect(Collectors.toList());
        log.info("Old OUs: " + oldOus);

        oldOus.forEach(oldOu -> {
            var newOu = DBOrgUnit.builder()
                    .name(oldOu.getName())
                    .description(oldOu.getDescription())
                    .workspaceId(Integer.parseInt(newWorkspaceId))
                    .active(oldOu.isActive())
                    .defaultDashboardId(oldOu.getDefaultDashboardId())
                    .ouCategoryId(UUID.fromString(oldCategoryIdToNewCategoryId.get(oldOu.getOuCategoryId())))
                    .defaultDashboardId(oldOu.getDefaultDashboardId())
                    .tagIds(oldOu.getTagIds())
                    .managers(oldOu.getManagers())
                    .sections(oldOu.getSections())
                    .tags(oldOu.getTags())
                    .build();
            var newId = unitsHelper.insertNewOrgUnits(company, Stream.of(newOu)).stream().findFirst().get();
            oldOuRefIdToNewOuRefId.put(oldOu.getRefId(), newId);
        });

        var updatedOuStream = oldOus.stream().map(oldOu -> {
            // Correct the parent mapping now since we did not set the parent ref id while creating the new ous
            DBOrgUnit newOu = null;
            try {
                newOu = unitsService.get(company, oldOuRefIdToNewOuRefId.get(oldOu.getRefId())).get();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (oldOu.getParentRefId() != null) {
                var newParentRefId = oldOuRefIdToNewOuRefId.get(oldOu.getParentRefId());
                return newOu.toBuilder()
                        .parentRefId(newParentRefId)
                        .build();
            }
            return null;
        }).filter(Objects::nonNull);
        unitsHelper.updateUnits(company, updatedOuStream);

        // Clone dashboards and set the dashboard mappings correctly
        oldOus.forEach(oldOu -> {
            var oldOuDashList = PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(
                            page -> ouDashboardService.listByOuFilters(
                                    company,
                                    OUDashboardService.DashboardFilter.builder().ouId(oldOu.getId()).build(),
                                    oldOu, page, 100, null).getRecords()))
                    .collect(Collectors.toList());
            var oldDashIdToNewDashIdMap = cloneDashboards(oldOuDashList, company);
            var newOuDashList = oldOuDashList.stream().map(oldOuDash -> OUDashboard.builder()
                    .name(oldOuDash.getName())
                    .dashboardId(oldDashIdToNewDashIdMap.get(oldOuDash.getDashboardId()))
                    .dashboardOrder(oldOuDash.getDashboardOrder())
                    .build()).collect(Collectors.toList());
            try {
                DBOrgUnit newOu = unitsService.get(company, oldOuRefIdToNewOuRefId.get(oldOu.getRefId())).get();
                ouDashboardService.updateOuMapping(company, newOuDashList, newOu.getId());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return newWorkspaceId;
    }

    private Map<Integer, Integer> cloneDashboards(List<OUDashboard> ouDashboardList, String company) {
        HashMap<Integer, Integer> oldDashboardIdToNewDashboardIdMap = new HashMap<>();
        HashMap<String, String> oldWidgetIdToNewWidgetId = new HashMap<>();
        ouDashboardList.forEach(ouDashboard -> {
            var originalDash = dashboardWidgetService
                    .get(company, String.valueOf(ouDashboard.getDashboardId())).get();
            var newWidgets = originalDash.getWidgets().stream().map(oldWidget -> {
                        var newWidget = Widget.builder()
                                .id(UUID.randomUUID().toString())
                                .name(oldWidget.getName())
                                .metadata(oldWidget.getMetadata())
                                .precalculate(oldWidget.getPrecalculate())
                                .precalculateFrequencyInMins(oldWidget.getPrecalculateFrequencyInMins())
                                .displayInfo(oldWidget.getDisplayInfo())
                                .query(oldWidget.getQuery())
                                .type(oldWidget.getType())
                                .build();
                        oldWidgetIdToNewWidgetId.put(oldWidget.getId(), newWidget.getId());
                        return newWidget;
                    }
            ).collect(Collectors.toList());
            var newDash = Dashboard.builder()
                    .name(originalDash.getName())
                    .email(originalDash.getEmail())
                    .firstName(originalDash.getFirstName())
                    .lastName(originalDash.getLastName())
                    .ownerId(originalDash.getOwnerId())
                    .dashboardOrder(originalDash.getDashboardOrder())
                    .demo(originalDash.getDemo())
                    .isDefault(originalDash.getIsDefault())
                    .isPublic(originalDash.isPublic())
                    .metadata(originalDash.getMetadata())
                    .type(originalDash.getType())
                    .widgets(newWidgets)
                    .build();
            try {
                String newDashId = dashboardWidgetService.insert(company, newDash);
                oldDashboardIdToNewDashboardIdMap.put(Integer.parseInt(originalDash.getId()), Integer.parseInt(newDashId));
                // Widgets have a metadata json field with a 'children' key. The value for this key is a list of widget
                // ids. We need to correctly map these to the new versions.
                newDash = dashboardWidgetService.get(company, newDashId).get();
                newDash.getWidgets().forEach(widget -> {
                    var metadata = (Map<String, Object>) (widget.getMetadata());
                    var children = (List<String>) (metadata.get("children"));
                    List<String> newChildren = null;
                    if (!CollectionUtils.isEmpty(children)) {
                        newChildren = children.stream().map(oldWidgetIdToNewWidgetId::get).collect(Collectors.toList());
                        metadata.put("children", newChildren);
                        widget.toBuilder().metadata(metadata);
                        try {
                            dashboardWidgetService.updateWidget(company, widget);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return oldDashboardIdToNewDashboardIdMap;
    }
}