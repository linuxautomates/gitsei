package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.questionnaires.clients.QuestionnaireTemplateClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/qtemplates")
@Log4j2
@SuppressWarnings("unused")
public class QuestionnaireTemplateController {
    private static final String ACTIVITY_LOG_TEXT = "%s Assessment Template: %s.";
    private static final String ACTIVITY_LOG_TEXT_BULK = "%s Assessment Template: %s.";

    private final QuestionnaireTemplateClient questionnaireTemplateService;
    private final ActivityLogService activityLogService;

    @Autowired
    public QuestionnaireTemplateController(final QuestionnaireTemplateClient questionnaireTemplateService,
                                           final ActivityLogService activityLogService) {
        this.questionnaireTemplateService = questionnaireTemplateService;
        this.activityLogService = activityLogService;
    }

    // region Create

    /**
     * POST - Creates a QuestionnaireTemplate object.
     *
     * @param company
     * @param questionnaireTemplate
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> createQuestionnaireTemplate(@SessionAttribute("company") final String company,
                                                                          @SessionAttribute(name = "session_user") String sessionUser,
                                                                          @RequestBody final QuestionnaireTemplate questionnaireTemplate) {
        return SpringUtils.deferResponse(() -> {
                Id id = questionnaireTemplateService.create(company, questionnaireTemplate);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(id.getId())
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.ASSESSMENT_TEMPLATE)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Created", id.getId()))
                        .details(Collections.singletonMap("item", questionnaireTemplate.toBuilder()
                                .id(id.getId())
                                .build()))
                        .action(ActivityLog.Action.CREATED)
                        .build());
                return ResponseEntity.ok().body(id);
        });
    }

    /**
     * GET - Retrieves a questionnare object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireTemplate>> questionnaireTemplateDetails(
            @SessionAttribute("company") final String company, @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(questionnaireTemplateService.get(company, id.toString())));
    }
    // endregion

    // region Update

    /**
     * PUT - Updates a QuestionnaireTemplate object.
     *
     * @param company
     * @param questionnaireTemplate
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> questionnaireTemplateUpdate(@SessionAttribute("company") final String company,
                                                                      @SessionAttribute(name = "session_user") String sessionUser,
                                                                      @PathVariable("id") final UUID uuid,
                                                                      @RequestBody final QuestionnaireTemplate questionnaireTemplate) {
        return SpringUtils.deferResponse(() -> {
                // Assemble Questionnaire object
                final String id = uuid.toString();
                questionnaireTemplateService.update(company, id, questionnaireTemplate);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(id)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.ASSESSMENT_TEMPLATE)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Edited", id))
                        .details(Collections.singletonMap("item", questionnaireTemplate.toBuilder()
                                .id(id)
                                .build()))
                        .action(ActivityLog.Action.EDITED)
                        .build());
                return ResponseEntity.ok().body(Id.from(id));
        });
    }
    // endregion

    // region Delete

    /**
     * DELETE - Deletes a QuestionnaireTemplate object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> questionnaireTemplateDelete(@SessionAttribute("company") final String company,
                                                                          @SessionAttribute(name = "session_user") String sessionUser,
                                                                          @PathVariable("id") final String id) {
        return SpringUtils.deferResponse(() -> {
            DeleteResponse response = questionnaireTemplateService.delete(company, id);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.ASSESSMENT_TEMPLATE)
                    .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", id))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkQuestionnaireTemplateDelete(@SessionAttribute("company") final String company,
                                                                                               @SessionAttribute(name = "session_user") String sessionUser,
                                                                                               @RequestBody final List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> ids = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            BulkDeleteResponse response = questionnaireTemplateService.bulkDelete(company, ids);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(DefaultObjectMapper.writeAsPrettyJson(ids))
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.ASSESSMENT_TEMPLATE)
                    .body(String.format(ACTIVITY_LOG_TEXT_BULK, "Deleted Bulk", String.join(", ", ids)))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
    public DeferredResult<ResponseEntity<PaginatedResponse<QuestionnaireTemplate>>> questionnaireTemplateList(
            @SessionAttribute("company") final String company, @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(questionnaireTemplateService.list(company, filter)));
    }
}