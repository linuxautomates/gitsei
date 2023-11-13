package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.services.WorkItemService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import io.levelops.workitems.clients.WorkItemsClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/workitems")
@Log4j2
public class WorkItemsController {

    private static final String ACTIVITY_LOG_TEXT = "%s Work item.";

    private final UserService userService;
    private final WorkItemsClient workItemClient;
    private final ActivityLogService activityLogService;
    private final WorkItemService workItemService;

    @Autowired
    public WorkItemsController(UserService userService,
                               WorkItemsClient workItemsClient,
                               ActivityLogService activityLogService,
                               WorkItemService workItemService) {
        this.userService = userService;
        this.workItemClient = workItemsClient;
        this.activityLogService = activityLogService;
        this.workItemService = workItemService;
    }

    /**
     * POST - Creates a workitems object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createWorkItem(@SessionAttribute("company") final String company,
                                                                   @SessionAttribute(name = "session_user") String sessionUser,
                                                                   @Valid @RequestBody WorkItem workitem) {
        if (Strings.isBlank(workitem.getTicketTemplateId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a valid ticket template id is required.");
        }
        return SpringUtils.deferResponse(() -> {
            //Ensure senderemail is accurate and targetemail is accurate too
            WorkItem sanitized = workitem;
            if (!sessionUser.equals(workitem.getReporter())) {
                sanitized = workitem.toBuilder()
                        .reporter(sessionUser)
                        .build();
            }
            WorkItem createdWi = workItemClient.create(company, sanitized);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(createdWi.getId())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Created"))
                    .details(Collections.singletonMap("item", createdWi))
                    .action(ActivityLog.Action.CREATED)
                    .build());
            return ResponseEntity.ok().body(createdWi);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping(path = "/snippet_workitems/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createSnippetWorkItemMultipart(@SessionAttribute(name = "company") String company,
                                                                                   @RequestPart("json") MultipartFile createSnippetWorkItemRequest,
                                                                                   @RequestPart(name = "file") MultipartFile snippetFile) {
        return SpringUtils.deferResponse(() -> {
            WorkItem createdWi = workItemClient.createSnippetWorkItemMultipart(company, createSnippetWorkItemRequest, snippetFile);
            return ResponseEntity.ok().body(createdWi);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping(path = "/snippet_workitems", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createSnippetWorkItem(@SessionAttribute(name = "company") String company,
                                                                          @Valid @RequestBody CreateSnippetWorkitemRequestWithText createSnippetWorkitemRequest) {
        return SpringUtils.deferResponse(() -> {
            WorkItem createdWi = workItemClient.createSnippetWorkItem(company, createSnippetWorkitemRequest);
            return ResponseEntity.ok().body(createdWi);
        });
    }

    /**
     * GET - Retrieves a workitem object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'AUDITOR', 'ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id:[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workitemsDetails(@SessionAttribute("company") final String company,
                                                                     @SessionAttribute(name = "session_user") String sessionUser,
                                                                     @SessionAttribute(name = "user_type") String userType,
                                                                     @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> {
            WorkItem item = workItemClient.getById(company, id);
            if (item == null) {
                throw new ServerApiException(HttpStatus.NOT_FOUND, "Ticket not found.");
            }
            if (RoleType.fromString(userType) == RoleType.ASSIGNED_ISSUES_USER) {
                List<WorkItem.Assignee> assigneeList = item.getAssignees();
                if (assigneeList == null || assigneeList.stream()
                        .filter(assignee -> StringUtils.equals(assignee.getUserEmail(), sessionUser))
                        .findFirst()
                        .isEmpty()) {
                    throw new ServerApiException(HttpStatus.FORBIDDEN, "Insufficient permissions.");
                }
            }
            return ResponseEntity.ok(item);
        });
    }

    /**
     * GET by Vanity Id - Retrieves a workitem object.
     *
     * @param company
     * @param vanityId
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'AUDITOR', 'ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/vanity-id/{vanity-id}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workitemsDetailsByVanityId(@SessionAttribute("company") final String company,
                                                                               @SessionAttribute(name = "session_user") String sessionUser,
                                                                               @SessionAttribute(name = "user_type") String userType,
                                                                               @PathVariable("vanity-id") final String vanityId) throws Exception {
        return SpringUtils.deferResponse(() -> {
            WorkItem item = workItemClient.getByVanityId(company, vanityId);
            if (item == null) {
                throw new ServerApiException(HttpStatus.NOT_FOUND, "Ticket not found.");
            }
            if (RoleType.fromString(userType) == RoleType.ASSIGNED_ISSUES_USER) {
                List<WorkItem.Assignee> assigneeList = item.getAssignees();
                if (assigneeList == null || assigneeList.stream()
                        .filter(assignee -> StringUtils.equals(assignee.getUserEmail(), sessionUser))
                        .findFirst()
                        .isEmpty()) {
                    throw new ServerApiException(HttpStatus.FORBIDDEN, "Insufficient permissions.");
                }
            }
            return ResponseEntity.ok(workItemClient.getByVanityId(company, vanityId));
        });
    }

    /**
     * PUT - Updates a workitem object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT, path = "/{id:[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> updateWorkItem(@SessionAttribute("company") final String company,
                                                                   @SessionAttribute(name = "session_user") String sessionUser,
                                                                   @SessionAttribute(name = "user_type") String userType,
                                                                   @PathVariable("id") final UUID id,
                                                                   @RequestBody final WorkItem workitem) {
        return SpringUtils.deferResponse(() -> {
            WorkItem updated = workItemService.updateWorkItem(company, sessionUser, userType, id, workitem);
            return ResponseEntity.accepted().body(updated);
        });
    }

    /**
     * PUT - Updates a workitem object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PATCH, path = "/{id:[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workItemChangeProduct(@SessionAttribute("company") final String company,
                                                                          @SessionAttribute(name = "session_user") String sessionUser,
                                                                          @PathVariable("id") final UUID id,
                                                                          @RequestBody final WorkItem workitem) {
        return SpringUtils.deferResponse(() -> {
            workItemClient.changeProductId(company, id, workitem.getProductId());
            WorkItem updated = workItemClient.getById(company, id);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id.toString())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited"))
                    .details(Collections.singletonMap("item", updated))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            return ResponseEntity.accepted().body(updated);
        });
    }

    /**
     * PUT - Updates a workitem object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PATCH, path = "/{id:[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}}/parent-id", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workItemChangeParentId(@SessionAttribute("company") final String company,
                                                                           @SessionAttribute(name = "session_user") String sessionUser,
                                                                           @PathVariable("id") final UUID id,
                                                                           @RequestBody final WorkItem workitem) {
        return SpringUtils.deferResponse(() -> {
            workItemClient.changeParentId(company, id, workitem.getParentId());
            WorkItem updated = workItemClient.getById(company, id);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id.toString())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited"))
                    .details(Collections.singletonMap("item", updated))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            return ResponseEntity.accepted().body(updated);
        });
    }

    /**
     * DELETE - Deletes a workitem object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id:[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> workItemsDelete(@SessionAttribute("company") final String company,
                                                                          @SessionAttribute(name = "session_user") String sessionUser,
                                                                          @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> {
            var response = workItemClient.delete(company, id);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id.toString())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted"))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> workItemsBulkDelete(@SessionAttribute("company") final String company,
                                                                                  @SessionAttribute(name = "session_user") String sessionUser,
                                                                                  @RequestBody List<UUID> ids) {
        return SpringUtils.deferResponse(() -> {
            var responses = workItemClient.bulkDelete(company, ids);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(String.valueOf(ids))
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.TICKET)
                    .body(String.format("Deleted workitems: %s", responses.getRecords().size()))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());

            return ResponseEntity.ok(responses);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<WorkItem>>> workItemsList(
            @SessionAttribute("company") final String company,
            @SessionAttribute(name = "session_user") final String sessionUser,
            @SessionAttribute(name = "user_type") final String userType,
            @RequestBody final DefaultListRequest filter) {
        log.debug("listing workitems for {}", company);
        return SpringUtils.deferResponse(() -> {
            DefaultListRequest req = filter;
            if (RoleType.fromString(userType) == RoleType.ASSIGNED_ISSUES_USER) {
                String id = userService.getByEmail(company, sessionUser)
                        .orElseThrow(() -> new ServerApiException(HttpStatus.NOT_FOUND, "User missing."))
                        .getId();
                Map<String, Object> objectMap = filter.getFilter();
                objectMap.put("assignee_user_ids", List.of(id));
                objectMap.remove("unassigned");
                req = filter.toBuilder().filter(objectMap).build();
            }
            return ResponseEntity.ok().body(workItemClient.list(company, req));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/aggregations/{calc}", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAgg(@SessionAttribute(name = "company") String company,
                                                                                         @PathVariable("calc") WorkItemFilter.Calculation calculation,
                                                                                         @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        workItemClient.aggregate(company, calculation, filter))));
    }

}