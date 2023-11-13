package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Notification;
import io.levelops.commons.databases.models.database.QuestionnaireNotification;
import io.levelops.commons.databases.models.database.QuestionnaireNotificationRequest;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.databases.services.QuestionnaireNotificationsDatabaseService;
import io.levelops.ingestion.models.Job;
import io.levelops.internal_api.services.QuestionnaireService;
import io.levelops.web.util.SpringUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/questionnaires_notifications")
public class QuestionnairesNotificationsController {
    private final ObjectMapper objectMapper;
    private final QuestionnaireNotificationsDatabaseService questionnaireNotificationsDatabaseService;
    private final QuestionnaireService questionnaireService;

    @Autowired
    public QuestionnairesNotificationsController(ObjectMapper objectMapper, QuestionnaireNotificationsDatabaseService questionnaireNotificationsDatabaseService, QuestionnaireService questionnaireService) {
        this.objectMapper = objectMapper;
        this.questionnaireNotificationsDatabaseService = questionnaireNotificationsDatabaseService;
        this.questionnaireService = questionnaireService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> queueQuestionnaireNotification(@PathVariable("company") final String company,
                                                                                         @RequestBody QuestionnaireNotificationRequest questionnaireNotificationRequest) {
        return SpringUtils.deferResponse(() -> {
            String jobId = questionnaireService.queueQuestionnaireNotification(company, questionnaireNotificationRequest);
            return ResponseEntity.ok().body(Map.of("id", jobId));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/callbacks/slack", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, List<String>>>> receiveQuestionnaireNotificationSlackCallback(@PathVariable("company") final String company,
                                                                                                       @RequestBody Job job) {
        return SpringUtils.deferResponse(() -> {
            SlackChatInteractiveMessageResult slackChatInteractiveMessageResult = objectMapper.readValue(objectMapper.writeValueAsString(job.getResult()), SlackChatInteractiveMessageResult.class);
            List<Notification> notifications =  slackChatInteractiveMessageResult.getNotifications();
            List<String> ids = new ArrayList<>();
            if(CollectionUtils.isNotEmpty(notifications)) {
                List<QuestionnaireNotification> questionnaireNotifications =  notifications.stream()
                        .map(x -> QuestionnaireNotification.builder()
                                .questionnaireId(x.getQuestionnaireId())
                                .mode(x.getMode())
                                .recipient(x.getRecipient())
                                .referenceId(x.getReferenceId())
                                .channelId(x.getChannelId())
                                .url(x.getUrl())
                                .build()).collect(Collectors.toList());
                for(QuestionnaireNotification questionnaireNotification : questionnaireNotifications) {
                    String id = questionnaireNotificationsDatabaseService.insert(company, questionnaireNotification);
                    ids.add(id);
                }
            }
            return ResponseEntity.ok().body(Map.of("ids", ids));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/slack/rebuild_cache/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> rebuildQuestionaireSlackMessageCache(@PathVariable("company") final String company,
                                                                                              @PathVariable("id") final String questionaireId) {
        return SpringUtils.deferResponse(() -> {
            questionnaireService.rebuildQuestionnaireSlackMessageCache(company, questionaireId);
            return ResponseEntity.ok().body(Map.of("id", questionaireId));
        });
    }
}
