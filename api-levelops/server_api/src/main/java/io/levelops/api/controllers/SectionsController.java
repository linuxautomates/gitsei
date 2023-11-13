package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.services.TagItemService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SectionsService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/sections")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class SectionsController {
    private static final String ACTIVITY_LOG_TEXT = "%s Section Template: %s.";
    private static final TagItemType TAG_ITEM_TYPE = TagItemType.SECTION;

    private final SectionsService sectionsService;
    private final TagItemService tagItemService;
    private final ActivityLogService activityLogService;

    @Autowired
    public SectionsController(final SectionsService sectionsService, ActivityLogService activityLogService,
                              final TagItemService tagItemService) {
        this.tagItemService = tagItemService;
        this.sectionsService = sectionsService;
        this.activityLogService = activityLogService;
    }

    /**
     * POST - Creates a sections object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createSections(@SessionAttribute("company") final String company,
                                                                              @SessionAttribute("session_user") final String sessionUser,
                                                                              @RequestBody final Section sectionDTO) {
        return SpringUtils.deferResponse(() -> {
            String id = sectionsService.insert(company, sectionDTO);
            Section section = sectionDTO.toBuilder().id(id).build();
            tagItemService.batchInsert(company, UUID.fromString(id), TAG_ITEM_TYPE, section.getTags());
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.SECTION)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Created", id))
                    .details(Collections.singletonMap("item", section))
                    .action(ActivityLog.Action.CREATED)
                    .build());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * GET - Retrieves a sections object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Section>> sectionsDetails(@SessionAttribute("company") final String company,
                                                                   @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(sectionsService.get(company, id.toString())
                .orElseThrow(() -> new NotFoundException("Section with id '" + id + "' not found."))));
    }

    /**
     * PUT - Updates a sections object.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> sectionUpdate(@SessionAttribute("company") final String company,
                                                                             @SessionAttribute("session_user") final String sessionUser,
                                                                             @PathVariable("id") final UUID sectionId,
                                                                             @RequestBody Section sectionDTO) {
        return SpringUtils.deferResponse(() -> {
            final String id = sectionId.toString();
            var section = sectionDTO.toBuilder().id(id).build();
            if (section.getTags() != null) { // for testing only...
                // delete previous tag associations.
                tagItemService.deleteTagsForItem(company, sectionId, TAG_ITEM_TYPE);
                tagItemService.batchInsert(company, sectionId, TAG_ITEM_TYPE, section.getTags());
            }
            sectionsService.update(company, section);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.SECTION)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Edited", id))
                    .details(Collections.singletonMap("item", section))
                    .action(ActivityLog.Action.EDITED)
                    .build());
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    /**
     * DELETE - Deletes a sections object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> sectionsDelete(@SessionAttribute("company") final String company,
                                                                 @SessionAttribute("session_user") final String sessionUser,
                                                                 @PathVariable("id") final UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            if (!sectionsService.delete(company, id)) {
                throw new ServerApiException("Unabled to delete the sections with id '" + id + "'.");
            }
            tagItemService.deleteTagsForItem(company, uuid, TAG_ITEM_TYPE);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.SECTION)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", id))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok("{}");
        });
    }

    // List
    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Section>>> sectionsList(@SessionAttribute("company") final String company,
                                                                   @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> tagIdsString = filter.getFilterValue("tag_ids", List.class).orElse(Collections.emptyList());
            List<Integer> tagIds = tagIdsString.stream()
                    .map(i -> NumberUtils.toInt(i, -1))
                    .filter(i -> i > 0)
                    .collect(Collectors.toList());
            if (tagIds.size() != tagIdsString.size()) {
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Invalid tag_ids provided.");
            }
            String name = filter.getFilterValue("partial", Map.class)
                    .map(m -> (String) m.get("name"))
                    .orElse(null);
            List<String> questionTagIds = filter.getFilterValue("question_tag_ids", List.class).orElse(Collections.emptyList());
            var results = sectionsService.listByTagIds(company, tagIds, name,
                    filter.getPage(), filter.getPageSize(), questionTagIds);
            if (results == null || results.getCount() < 1) {
                return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        0, Collections.emptyList()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    results.getTotalCount(),
                    results.getRecords()));
        });
    }
}