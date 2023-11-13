package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.QuestionnaireNotificationRequest;
import io.levelops.web.util.SpringUtils;
import io.levelops.workitems.clients.QuestionnaireNotificationClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/v1/questionnaires_notifications")
public class QuestionnairesNotificationsController {
    private final QuestionnaireNotificationClient questionnaireNotificationClient;

    @Autowired
    public QuestionnairesNotificationsController(QuestionnaireNotificationClient questionnaireNotificationClient) {
        this.questionnaireNotificationClient = questionnaireNotificationClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> queueQuestionnaireNotification(@SessionAttribute("company") String company,
                                                                                              @RequestBody QuestionnaireNotificationRequest questionnaireNotificationRequest) {
        return SpringUtils.deferResponse(() -> {
            log.info("questionnaireNotificationRequest = {}", questionnaireNotificationRequest);
            String jobId = questionnaireNotificationClient.queueRequest(company, questionnaireNotificationRequest).getId();
            log.info("jobId = {}", jobId);
            return ResponseEntity.ok().body(Map.of("id", jobId));
        });
    }

}
