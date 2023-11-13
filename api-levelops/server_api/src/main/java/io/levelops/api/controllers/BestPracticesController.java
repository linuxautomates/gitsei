package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.model.BestPracticesItemDTO;
import io.levelops.api.services.TagItemService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.BestPracticesService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.MsgTemplateService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.notification.services.NotificationService;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/bestpractices")
@Log4j2
@SuppressWarnings("unused")
public class BestPracticesController {
    private static final String ASSESSMENT_LINK_FORMAT = "%s/#/admin/answer-questionnaire-page?questionnaire=%s&tenant=%s";
    private static final String WORKITEM_LINK_FORMAT = "%s/#/admin/workitems/details?workitem=%s";
    private static final String NOTE_DEFAULT = "Sent Knowledge Base Item: %s to user: %s.";
    private static final String ACTIVITY_LOG_TEXT = "%s Knowledge Base Item: %s.";
    private static final TagItemType TAG_ITEM_TYPE = TagItemType.BEST_PRACTICE;

    private final TagItemService tagItemService;
    private final ActivityLogService activityLogService;
    private final MsgTemplateService msgTemplateService;
    private final IntegrationService integrationService;
    private final NotificationService notificationService;
    private final BestPracticesService bestpracticesService;
    private final String appBaseUrl;

    @Autowired
    public BestPracticesController(final BestPracticesService bestpracticesService, final TagItemService tagItemService,
                                   final MsgTemplateService msgTemplateService, final ActivityLogService activityLogService,
                                   final NotificationService notificationService, final IntegrationService integService,
                                   @Value("${APP_BASE_URL:https://app.levelops.io}") final String appBaseUrl) {
        this.tagItemService = tagItemService;
        this.integrationService = integService;
        this.msgTemplateService = msgTemplateService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.bestpracticesService = bestpracticesService;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * POST - Creates a bestpractices object.
     *
     * @param company
     * @param bItem
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, UUID>>> createBestPractices(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @RequestBody final BestPracticesItem bItem) {
        return SpringUtils.deferResponse(() -> {
            UUID id = UUID.randomUUID();
            BestPracticesItem bestPracticesItem = bItem.toBuilder().id(id).build();

            tagItemService.batchInsert(company, id, TAG_ITEM_TYPE, bItem.getTags());
            bestpracticesService.insert(company, bestPracticesItem);
            activityLogService.insert(company,
                    ActivityLog.builder()
                            .targetItem(id.toString())
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.KB)
                            .action(ActivityLog.Action.CREATED)
                            .details(Collections.singletonMap("item", bestPracticesItem))
                            .body(String.format(ACTIVITY_LOG_TEXT, "Created", id.toString()))
                            .build());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * POST - Sends a bestpractices object.
     *
     * @param company
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.POST, path = "/send", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> sendBestPractices(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @RequestBody final BestPracticesItemDTO bestPracticesItemDTO) {
        return SpringUtils.deferResponse(() -> {
            BestPracticesItemDTO notificationItem = bestPracticesItemDTO.toBuilder()
                    .senderEmail(sessionUser).build();
            BestPracticesItem bpItem = bestpracticesService.get(company, notificationItem.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Best Practice not found."));
            // Generate message content
            MessageTemplate messageTemplate = msgTemplateService.get(
                    company, notificationItem.getNotificationTemplateId())
                    .orElseThrow(() -> new ServerApiException(HttpStatus.BAD_REQUEST, "MessageTemplate not found: "
                            + notificationItem.getNotificationTemplateId()));

            if (messageTemplate.getType() == MessageTemplate.TemplateType.SLACK) {
                if (integrationService.listByFilter(company, null, List.of(IntegrationType.SLACK.toString()), null,
                        null, null, 0, 100).getTotalCount() == 0) {
                    throw new ServerApiException(HttpStatus.BAD_REQUEST, "No slack integration available.");
                }
            }

            String link = (bpItem.getType() == BestPracticesItem.BestPracticeType.LINK) ? bpItem.getValue() : null;
            String text = (bpItem.getType() == BestPracticesItem.BestPracticeType.TEXT)
                    ? StringUtils.isNotEmpty(notificationItem.getArtifact())
                    ? bpItem.getValue() + "\n\nRegarding: " + notificationItem.getArtifact()
                    : bpItem.getValue()
                    : null;

            var values = new HashMap<String, Object>();
            values.put("title", bpItem.getName());
            values.put("link", link);
            values.put("text", text);
            values.put("info", notificationItem.getAdditionalInfo());
            values.put("issue_id", notificationItem.getWorkItemId());
            values.put("issue_link", String.format(WORKITEM_LINK_FORMAT, appBaseUrl, notificationItem.getWorkItemId()));
            values.put("issue_title", notificationItem.getArtifact());
            try {
                notificationService.sendNotification(company, messageTemplate, notificationItem.getTargetEmail(), values);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    log.error("Unable to send the kb to {} for {}...", notificationItem.getTargetEmail(), notificationItem.getWorkItemId(), e);
                } else {
                    log.error("Unable to send the kb to {} for {}... {}", notificationItem.getTargetEmail(), notificationItem.getWorkItemId(), e);
                }
            }

            activityLogService.insert(company,
                    ActivityLog.builder()
                            .targetItem(notificationItem.getWorkItemId())
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.KB)
                            .action(ActivityLog.Action.SENT)
                            .details(Collections.emptyMap())
                            .body(String.format(NOTE_DEFAULT, bpItem.getName(),
                                    notificationItem.getTargetEmail()))
                            .build());

            return ResponseEntity.ok().build();
        });
    }

    /**
     * GET - Retrieves a bestpractices object.
     *
     * @param company
     * @param id
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'SUPER_ADMIN')")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<BestPracticesItem>> bestPracticesDetails(@SessionAttribute("company") final String company,
                                                                                  @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(bestpracticesService.get(company, id.toString()).orElseThrow(() ->
                new NotFoundException("Couldn't find the requested best practice with id '" + id + "'"))));
    }

    // Update

    /**
     * PUT - Updates a bestpractices object.
     *
     * @param company
     * @param bPItem
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> bestPracticesUpdate(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @PathVariable("id") final UUID uuid,
            @RequestBody final BestPracticesItem bPItem) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            UUID itemId = UUID.fromString(id);
            if (bPItem.getTags() != null) {
                // delete previous tag associations.
                tagItemService.deleteTagsForItem(company, itemId, TAG_ITEM_TYPE);
                tagItemService.batchInsert(company, itemId, TAG_ITEM_TYPE, bPItem.getTags());
            }
            bestpracticesService.update(company, bPItem.toBuilder().id(UUID.fromString(id)).build());
            activityLogService.insert(company,
                    ActivityLog.builder()
                            .targetItem(id)
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.KB)
                            .action(ActivityLog.Action.EDITED)
                            .details(Collections.singletonMap("item", bPItem))
                            .body(String.format(ACTIVITY_LOG_TEXT, "Edited", id))
                            .build());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * DELETE - Deletes a bestpractices object.
     *
     * @param company
     * @param uuid
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> bestPracticesDelete(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @PathVariable("id") final UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            try {
                String id = uuid.toString();
                BestPracticesItem item = bestpracticesService.deleteAndReturn(company, id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "Unable to delete best practice with id '" + id + "' at this time"));
                activityLogService.insert(company,
                        ActivityLog.builder()
                                .targetItem(id)
                                .email(sessionUser)
                                .targetItemType(ActivityLog.TargetItemType.KB)
                                .action(ActivityLog.Action.DELETED)
                                .details(Collections.singletonMap("item", item))
                                .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", id))
                                .build());
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(String.valueOf(uuid)).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(String.valueOf(uuid)).success(true).build());
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bestPracticesDelete(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @RequestBody final List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            try {
                List<String> filteredIds = ids.stream()
                        .map(UUID::fromString)
                        .map(UUID::toString)
                        .collect(Collectors.toList());
                bestpracticesService.bulkDelete(company, filteredIds);
                activityLogService.insert(company,
                        ActivityLog.builder()
                                .targetItem(String.valueOf(ids))
                                .email(sessionUser)
                                .targetItemType(ActivityLog.TargetItemType.KB)
                                .action(ActivityLog.Action.DELETED)
                                .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", ids))
                                .build());
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    // List
    @SuppressWarnings("unchecked")
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<BestPracticesItem>>> bestPracticesList(
            @SessionAttribute("company") final String company,
            @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> tagIdsString = filter.getFilterValue("tag_ids", List.class)
                    .orElse(Collections.emptyList());
            List<Integer> tagIds = tagIdsString.stream()
                    .map(i -> NumberUtils.toInt(i, -1))
                    .filter(i -> i > 0)
                    .collect(Collectors.toList());
            if (tagIds.size() != tagIdsString.size()) {
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Invalid tag_ids provided.");
            }
            String bpName = filter.getFilterValue("partial", Map.class)
                    .map(m -> (String) m.get("name"))
                    .orElse(null);
            DbListResponse<BestPracticesItem> results = bestpracticesService.list(company,
                    tagIds, bpName, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), results));
        });
    }
}