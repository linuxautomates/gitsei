package io.levelops.api.controllers;

import com.google.api.client.util.IOUtils;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireListItemDTO;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.questionnaires.clients.QuestionnaireClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = {"/v1/questionnaires", "/v1/assessments"})
@Log4j2
@SuppressWarnings("unused")
public class QuestionnairesController {
    private static final String SEND_DEFAULT = "Sent Assessment: %s to user: %s.";
    private static final String CREATE_DEFAULT = "Created Assessment: %s.";
    private static final String ACTIVITY_LOG_TEXT = "%s Assessment Item: %s.";

    private final QuestionnaireClient questionnaireClient;
    private final ActivityLogService activityLogService;

    @Autowired
    public QuestionnairesController(QuestionnaireClient questionnaireClient, ActivityLogService activityLogService) {
        this.questionnaireClient = questionnaireClient;
        this.activityLogService = activityLogService;
    }

    /**
     * POST - Creates a questionnaire object.
     *
     * @param company
     * @param questionnaire
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> createQuestionnaire(
            @RequestBody final QuestionnaireDTO questionnaire,
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser) {
        return SpringUtils.deferResponse(() -> {
            QuestionnaireDTO sanitized = null;
            //Ensure senderemail is accurate and targetemail is accurate too
            if (Boolean.TRUE.equals(questionnaire.getMain())) {
                sanitized = questionnaire.toBuilder()
                        .targetEmail(sessionUser)
                        .senderEmail(sessionUser)
                        .build();
            } else {
                sanitized = questionnaire.toBuilder()
                        .senderEmail(sessionUser)
                        .build();
            }
            String id = questionnaireClient.create(company, sanitized).getId();

            // record activity log
            Map<String, Object> details = Map.of();
            //its only being sent if the original has target email and its not a main questionnaire
            boolean sendingQuestionnaire = (StringUtils.isNotEmpty(questionnaire.getTargetEmail())
                    && !Boolean.TRUE.equals(questionnaire.getMain()));
            if (sendingQuestionnaire) {
                details = Map.of("target_user", questionnaire.getTargetEmail());
            }
            String templateName = StringUtils.firstNonBlank(sanitized.getQuestionnaireTemplateName(),
                    sanitized.getQuestionnaireTemplateId()); // TODO should be: questionnaireTemplate.getName();
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(sanitized.getWorkItemId())
                    .email(sessionUser)
                    .body(String.format(sendingQuestionnaire ? SEND_DEFAULT : CREATE_DEFAULT,
                            templateName, sanitized.getTargetEmail()))
                    .details(details)
                    .targetItemType(ActivityLog.TargetItemType.ASSESSMENT)
                    .action(sendingQuestionnaire ? ActivityLog.Action.SENT : ActivityLog.Action.CREATED)
                    .build());

            return ResponseEntity.ok(Id.from(id));
        });
    }

    /**
     * GET - Retrieves a questionnaire object.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR','RESTRICTED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireDTO>> questionnaireDetails(@SessionAttribute("company") final String company,
                                                                                 @PathVariable("qnid") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(questionnaireClient.get(company, id)));
    }

    /**
     * PUT - Updates a questionnaire object.
     *
     * @param company
     * @param questionnaire
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','RESTRICTED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<QuestionnaireDTO>> questionnaireUpdate(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @PathVariable("qnid") final UUID uuid,
            @RequestBody final QuestionnaireDTO questionnaire) {
        return SpringUtils.deferResponse(() -> {
            final String id = uuid.toString();
            if ((questionnaire.getId() != null) && (!id.equals(questionnaire.getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id in path param & request body do not match! ID path param : " + id + " Id Request Body : " + questionnaire.getId());
            }
            //we are not allowing updates to targetemail today and id is ensured here
            QuestionnaireDTO sanitized = questionnaire.toBuilder()
                    .id(id)
                    .targetEmail(null)
                    .build();
            QuestionnaireDTO finalDto = questionnaireClient.update(company, sessionUser, uuid, sanitized);

            activityLogService.insert(company,
                    ActivityLog.builder()
                            .targetItem(id)
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.ASSESSMENT)
                            .action(questionnaire.getCompleted() ?
                                    ActivityLog.Action.SUBMITTED : ActivityLog.Action.ANSWERED)
                            .details(Collections.emptyMap())
                            .body(String.format(ACTIVITY_LOG_TEXT, "Answered", id))
                            .build());

            return ResponseEntity.ok(finalDto);
        });
    }

    /**
     * DELETE - Deletes a questionnaire object.
     *
     * @param company
     * @param uuid
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{qnid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> questionnaireDelete(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @PathVariable("qnid") final UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            DeleteResponse response = questionnaireClient.delete(company, uuid);
            activityLogService.insert(company,
                    ActivityLog.builder()
                            .targetItem(uuid.toString())
                            .email(sessionUser)
                            .targetItemType(ActivityLog.TargetItemType.ASSESSMENT)
                            .action(ActivityLog.Action.DELETED)
                            .details(Collections.emptyMap())
                            .body(String.format(ACTIVITY_LOG_TEXT, "Deleted", uuid))
                            .build());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> questionnaireBulkDelete(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String sessionUser,
            @RequestBody final List<UUID> ids) {
        return SpringUtils.deferResponse(() -> {
            BulkDeleteResponse response = questionnaireClient.bulkDelete(company, ids);
            activityLogService.insert(company,
                    ActivityLog.builder()
                            .email(sessionUser)
                            .targetItem(ids.stream().map(UUID::toString).collect(Collectors.joining(", ")))
                            .targetItemType(ActivityLog.TargetItemType.ASSESSMENT)
                            .action(ActivityLog.Action.DELETED)
                            .details(Collections.emptyMap())
                            .body(String.format("Deleted questionnaire: %s", response.getRecords().size()))
                            .build());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','ASSIGNED_ISSUES_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    @SuppressWarnings("rawtypes")
    public DeferredResult<ResponseEntity<PaginatedResponse<QuestionnaireListItemDTO>>> questionnaireList(
            @SessionAttribute("user_type") final RoleType role,
            @SessionAttribute("company") final String company,
            @RequestBody final DefaultListRequest listRequest) {
        if (role == RoleType.ASSIGNED_ISSUES_USER
                && CollectionUtils.isEmpty((List) listRequest.getFilter().get("work_item_ids"))) {
            throw new ServerApiException(HttpStatus.FORBIDDEN, "You dont have permission to list all assessments.");
        }
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(questionnaireClient.list(company, listRequest)));
    }

    /**
     * GET - Retrieves an assessment (a.k.a. questionnaire) exported to PDF and packed in a zip file.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/download/{id}", produces = "application/zip")
    public DeferredResult<ResponseEntity<byte[]>> assessmentDownload(@SessionAttribute("company") final String company,
                                                                     @PathVariable("qnid") final UUID id,
                                                                     @RequestParam(value = "include_artifacts", defaultValue = "false", required = false) final boolean includeArtifacts) {
        return assessmentMultiDownload(company, List.of(Map.of("id", id.toString(), "include_artifacts", includeArtifacts)));
    }

    /**
     * POST - Retrieves a list of assessments (a.k.a. questionnaire) exported to PDF and packed in a zip file.
     *
     * @param company
     * @param request the list of assessments to be exported: [{"id": "fa334r-asdf3434-sadf2w34-sdff", "include_artifacts": true}]
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping(path = "/download", produces = "application/zip")
    public DeferredResult<ResponseEntity<byte[]>> assessmentMultiDownload(@SessionAttribute("company") final String company, @RequestBody List<Map<String, Object>> request) {

        return SpringUtils.deferResponse(() -> {
            try (var out = new ByteArrayOutputStream();) {
                try (var in = questionnaireClient.export(company, request);) {
                    IOUtils.copy(in, out);
                }
                var headers = new HttpHeaders();
                headers.setContentDisposition(
                        ContentDisposition
                                .builder("attachment")
                                .filename("assessments.zip")
                                .build());
                return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/aggregations/{calc}", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAgg(@SessionAttribute(name = "company") String company,
                                                                                         @PathVariable("calc") QuestionnaireAggFilter.Calculation calculation,
                                                                                         @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                questionnaireClient.aggregate(company, calculation, filter)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/aggregations_paginated/{calc}", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggPagination(@SessionAttribute(name = "company") String company,
                                                                                                   @PathVariable("calc") QuestionnaireAggFilter.Calculation calculation,
                                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                questionnaireClient.aggregatePaginated(company, calculation, filter)));
    }
}

