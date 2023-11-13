package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.TicketTemplateDBService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/ticket_templates")
@Log4j2
public class TicketTemplatesController {
    private static final String ACTIVITY_LOG_TEXT = "%s Ticket Template: %s.";
    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_TICKET_TEMPLATE";
    private final TicketTemplateDBService dbService;
    private final DefaultConfigService configService;
    private final ActivityLogService activityLogService;

    @Autowired
    public TicketTemplatesController(final TicketTemplateDBService dbService,
                                     final DefaultConfigService configService,
                                     final ActivityLogService activityLogService) {
        this.dbService = dbService;
        this.configService = configService;
        this.activityLogService = activityLogService;
    }

    /**
     * POST - Creates a TicketTemplate object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createTicketTemplate(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @RequestBody final TicketTemplate ticketTemplate) {

        return SpringUtils.deferResponse(() -> {
            String resultId = dbService.insert(company, ticketTemplate);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(resultId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET_TEMPLATE)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Created", resultId))
                    .details(Collections.singletonMap("item", ticketTemplate))
                    .action(ActivityLog.Action.CREATED)
                    .build());
            if (Boolean.TRUE.equals(ticketTemplate.getIsDefault())) {
                configService.handleDefault(company, resultId, DEFAULT_TENANT_CONFIG_NAME);
            }
            return ResponseEntity.accepted().body(Map.of("ticket_template_id", resultId));
        });
    }

    /**
     * GET - Retrieves a TicketTemplate object.
     *
     * @param company
     * @param uuid
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<TicketTemplate>> getTicketTemplate(
            @SessionAttribute("company") final String company,
            @PathVariable("id") final UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            return ResponseEntity.ok(dbService.get(company, id)
                    .orElseThrow(() -> new NotFoundException("Ticket template not found: " + id))
                    .toBuilder()
                    .isDefault(id.equalsIgnoreCase(
                            configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME)))
                    .build());
        });
    }

    /**
     * PUT - Updates a TicketTemplate object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> updateTicketTemplate(
            @PathVariable("id") final UUID uuid,
            @RequestBody TicketTemplate ticketTemplate,
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            Boolean result = dbService.update(company, ticketTemplate);
            if (BooleanUtils.isTrue(result)) {
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(id)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.TICKET_TEMPLATE)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Edited", id))
                        .details(Collections.singletonMap("item", ticketTemplate))
                        .action(ActivityLog.Action.EDITED)
                        .build());
                if (Boolean.TRUE.equals(ticketTemplate.getIsDefault())) {
                    configService.handleDefault(company, id, DEFAULT_TENANT_CONFIG_NAME);
                }
                return ResponseEntity.accepted().body(Map.of("ticket_template_id", id));
            } else {
                return ResponseEntity.notFound().build();
            }
        });
    }

    /**
     * DELETE - Deletes a TicketTemplate object.
     *
     * @param company
     * @param uuid
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteTicketTemplate(
            @PathVariable("id") final UUID uuid,
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            String id = uuid.toString();
            Boolean deleted;
            try {
                deleted = dbService.delete(company, id);
                if (BooleanUtils.isTrue(deleted)) {
                    activityLogService.insert(company, ActivityLog.builder()
                            .targetItem(id)
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.TICKET_TEMPLATE)
                            .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", id))
                            .details(Collections.emptyMap())
                            .action(ActivityLog.Action.DELETED)
                            .build());
                    configService.deleteIfDefault(company, id, DEFAULT_TENANT_CONFIG_NAME);
                }
            } catch (Exception e) {
                log.error(e);
                if (e instanceof PSQLException && ((PSQLException) e).getServerErrorMessage()
                        .getMessage().contains("violates foreign key constraint"))
                    return ResponseEntity.ok(DeleteResponse.builder().id(id).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(id).success(true).build());
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteTicketTemplates(
            @RequestBody List<String> ids,
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            int rowsDeleted = 0;
            try {
                rowsDeleted = dbService.bulkDelete(company, ids);
                if (rowsDeleted > 0) {
                    ids.forEach(id -> {
                        try {
                            activityLogService.insert(company, ActivityLog.builder()
                                    .targetItem(id)
                                    .email(sessionUser)
                                    .targetItemType(ActivityLog.TargetItemType.TICKET_TEMPLATE)
                                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", id))
                                    .details(Collections.emptyMap())
                                    .action(ActivityLog.Action.DELETED)
                                    .build());
                        } catch (SQLException e) {
                            log.error("Unable to insert the log in activity log for ticket template with id: " + id + ". " + e.getMessage());
                        }
                        try {
                            configService.deleteIfDefault(company, id, DEFAULT_TENANT_CONFIG_NAME);
                        } catch (SQLException e) {
                            log.error("Unable to delete the ticket template from Default Config Service ticket template with id: " + id + ". " + e.getMessage());
                        }
                    });
                }
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                if ((e instanceof PSQLException && ((PSQLException) e).getServerErrorMessage().getMessage().contains("violates foreign key constraint")) ||
                        (e instanceof DataIntegrityViolationException && ((DataIntegrityViolationException) e).getMessage().contains("violates foreign key constraint"))) {
                    return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
                }
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<TicketTemplate>>> ticketTemplateList(
            @SessionAttribute("company") final String company,
            @RequestBody final DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            if (listRequest.getFilterValue("default", Boolean.class).orElse(false)) {
                String resultId = configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
                List<TicketTemplate> dto = new ArrayList<>();
                if (StringUtils.isNotEmpty(resultId)) {
                    dbService.get(company, resultId)
                            .ifPresent(template -> dto.add(template.toBuilder().isDefault(true).build()));
                }
                return ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                listRequest.getPage(),
                                listRequest.getPageSize(),
                                dto.size(),
                                dto));
            }

            DbListResponse<TicketTemplate> ticketTemplates = dbService.listByFilters(
                    company,
                    (String) listRequest.getFilterValue("partial", Map.class)
                            .orElse(Collections.emptyMap())
                            .get("name"),
                    listRequest.getPage(),
                    listRequest.getPageSize());
            String defaultId = configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);
            PaginatedResponse<TicketTemplate> paginatedResponse = PaginatedResponse.of(
                    listRequest.getPage(),
                    listRequest.getPageSize(),
                    ticketTemplates.getTotalCount(),
                    ticketTemplates.getCount() == 0 ?
                            Collections.emptyList() : ticketTemplates.getRecords()
                            .stream()
                            .map(item -> item.toBuilder()
                                    .isDefault(defaultId.equalsIgnoreCase(item.getId()))
                                    .build())
                            .collect(Collectors.toList()));
            return ResponseEntity.ok().body(paginatedResponse);
        });
    }
}
