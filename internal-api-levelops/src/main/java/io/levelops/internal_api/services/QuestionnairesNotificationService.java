package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.QuestionnaireNotificationRequest;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.SlackQuestionnaireCacheService;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Log4j2
@Service
public class QuestionnairesNotificationService {
    private final QuestionnaireSlackMessageBuilder questionnaireSlackMessageBuilder;
    private final NotificationService notificationService;
    private final SlackQuestionnaireCacheService slackQuestionnaireCacheService;
    private final String internalApiUrl;
    private final String appBaseUrl;

    @Autowired
    public QuestionnairesNotificationService(QuestionnaireSlackMessageBuilder questionnaireSlackMessageBuilder, NotificationService notificationService,
                                             SlackQuestionnaireCacheService slackQuestionnaireCacheService, @Value("${INTERNAL_API_URL:http://internal-api-lb}") String internalApiUrl, @Value("${APP_BASE_URL}") final String appBaseUrl) {
        this.questionnaireSlackMessageBuilder = questionnaireSlackMessageBuilder;
        this.notificationService = notificationService;
        this.slackQuestionnaireCacheService = slackQuestionnaireCacheService;
        this.internalApiUrl = internalApiUrl;
        this.appBaseUrl = appBaseUrl;
    }

    public String queueQuestionnaireNotification(final String company, final QuestionnaireNotificationRequest questionnaireNotificationRequest, String templatedText, String botName, QuestionnaireDTO questionnaireDetail) throws IOException {
        UUID questionnaireId = questionnaireNotificationRequest.getQuestionnaireId();

        QuestionnaireSlackMessageBuilder.QuestionnaireSlackMessages slackMessages = questionnaireSlackMessageBuilder.buildInteractiveMessages(company, templatedText, questionnaireDetail);
        log.debug("slackMessages = {}", slackMessages);

        //Save modal message to cache
        slackQuestionnaireCacheService.saveQuestionnaireMessage(company, questionnaireId, slackMessages.getModalMessage());
        log.debug("Saved modal message to cache, company {}, questionnaireId {}", company, questionnaireId);

        //Calculate callback url
        String callbackUrl = internalApiUrl + "/internal/v1/tenants/" + company + "/questionnaires_notifications/callbacks/slack";
        log.debug("callbackUrl = {}", callbackUrl);

        //Use notification service to queue slack message with interactive message text & callback url
        String jobId = notificationService.sendQuestionnaireInteractiveNotification(company, questionnaireId, questionnaireNotificationRequest.getMode(), slackMessages.getMessage(), questionnaireNotificationRequest.getRecipients(), botName, callbackUrl);
        log.info("queueQuestionnaireNotification jobId = {}", jobId);
        return jobId;
    }

    public void rebuildQuestionnaireSlackMessageCache(final String company, String questionnaireId, QuestionnaireDTO questionnaireDetail) throws IOException {
        QuestionnaireSlackMessageBuilder.QuestionnaireSlackMessages slackMessages = questionnaireSlackMessageBuilder.buildInteractiveMessages(company, "", questionnaireDetail);
        log.debug("slackMessages = {}", slackMessages);

        //Save modal message to cache
        slackQuestionnaireCacheService.saveQuestionnaireMessage(company, UUID.fromString(questionnaireId), slackMessages.getModalMessage());
        log.info("Saved modal message to cache, company {}, questionnaireId {}", company, questionnaireId);
    }
}
