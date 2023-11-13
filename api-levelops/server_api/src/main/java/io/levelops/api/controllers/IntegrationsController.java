package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.PreflightCheckFailedException;
import io.levelops.api.model.PreflightCheckFailedResponse;
import io.levelops.api.requests.ModifyIntegrationRequest;
import io.levelops.api.services.IntegrationManagementService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
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

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/v1/integrations")
public class IntegrationsController {
    private static final String ACTIVITY_LOG_TEXT = "%s Integrations item: %s.";

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final ActivityLogService activityLogService;
    private final IntegrationManagementService integrationManagementService;

    @Autowired
    public IntegrationsController(ObjectMapper objectMapper, InventoryService service, ActivityLogService logService,
                                  IntegrationManagementService integrationManagementService) {
        this.objectMapper = objectMapper;
        this.inventoryService = service;
        this.activityLogService = logService;
        this.integrationManagementService = integrationManagementService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createIntegration(@RequestBody ModifyIntegrationRequest request,
                                                                    @SessionAttribute(name = "session_user") String sessionUser,
                                                                    @SessionAttribute(name = "company") String company,
                                                                    @SessionAttribute(name = "entitlementsConfig") Map<String, String> entitlementsConfig) {
        return SpringUtils.deferResponse(() -> {
            try {

                if (MapUtils.isNotEmpty(entitlementsConfig) &&
                        entitlementsConfig.containsKey("SETTING_SCM_INTEGRATIONS_COUNT")) {
                    boolean isCountExhausted = integrationManagementService.isIntegrationCountMaxedOut(company, request, entitlementsConfig);
                    if (isCountExhausted) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max Integration count reached for trial user");
                    }
                }

                Integration integration = integrationManagementService.createIntegrationFromRequest(company, request);

                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(integration.getId())
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.INTEGRATION)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Created", integration.getId()))
                        .details(Collections.singletonMap("item", integration))
                        .action(ActivityLog.Action.CREATED)
                        .build());

                return ResponseEntity.ok(objectMapper.writeValueAsString(integration));
            } catch (PreflightCheckFailedException e) {
                return ResponseEntity.badRequest().body(objectMapper.writeValueAsString(PreflightCheckFailedResponse.builder()
                        .timestamp(Instant.now().toEpochMilli())
                        .status(400)
                        .error("Bad Request")
                        .message("preflight_check_failed")
                        .preflightCheck(e.getPreflightCheckResults())
                        .path("/v1/integrations")
                        .build()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{integrationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Integration>> updateIntegration(@RequestBody ModifyIntegrationRequest request,
                                                                         @SessionAttribute(name = "session_user") String sessionUser,
                                                                         @PathVariable("integrationid") String integrationId,
                                                                         @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            Integration integration = integrationManagementService
                    .updateIntegrationFromRequest(company, integrationId, request);

            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(integrationId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.INTEGRATION)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited", integration.getId()))
                    .details(Collections.singletonMap("item", integration))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            return ResponseEntity.ok(integration);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{integrationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Integration>> integrationDetails(@PathVariable("integrationid") String integrationId,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(integrationManagementService.getIntegration(company, integrationId)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{integrationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteIntegration(@PathVariable("integrationid") String integrationId,
                                                                            @SessionAttribute(name = "session_user") String sessionUser,
                                                                            @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                integrationManagementService.dropIngestionTrigger(company, integrationId);
                inventoryService.deleteIntegration(company, integrationId);
                insertLogForEachDeletedIntegration(integrationId, sessionUser, company);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder()
                        .id(integrationId).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(integrationId).success(true).build());
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteIntegrations(@RequestBody List<String> integrationIds,
                                                                                 @SessionAttribute(name = "session_user") String sessionUser,
                                                                                 @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                integrationIds.forEach(integrationId -> {
                    try {
                        integrationManagementService.dropIngestionTrigger(company, integrationId);
                    } catch (IngestionServiceException e) {
                        log.error("Unable to delete the ingestion trigger with integration id: " + integrationId + ". " + e.getMessage());
                    }
                    insertLogForEachDeletedIntegration(integrationId, sessionUser, company);
                });
                inventoryService.deleteIntegrations(company, integrationIds);
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(integrationIds, false, e.getMessage()));
            }
            return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(integrationIds, true, null));
        });
    }

    private void insertLogForEachDeletedIntegration(String integrationId, String sessionUser, String company) {
        try {
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(integrationId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.INTEGRATION)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", integrationId))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
        } catch (SQLException e) {
            log.error("Unable to insert the log in activity log for integration with id: " + integrationId + ". " + e.getMessage());
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS,permission = Permission.INSIGHTS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'LIMITED_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Integration>>> integrationsList(@SessionAttribute(name = "company") String company,
                                                                                           @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<Integration> appResponse = integrationManagementService.listIntegrations(company, filter);
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), appResponse));
        });
    }


}
