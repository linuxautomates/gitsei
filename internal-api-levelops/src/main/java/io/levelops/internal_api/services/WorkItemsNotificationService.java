package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.WorkItemNotificationRequest;
import io.levelops.commons.databases.models.database.cicd.FailureTriageSlackMessage;
import io.levelops.commons.databases.services.StateDBService;
import io.levelops.commons.databases.services.WorkItemFailureTriageViewService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.notification.models.SlackNotificationResult;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.SlackWorkItemCacheService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WorkItemsNotificationService {
    private final WorkItemFailureTriageViewService workItemFailureTriageViewService;
    private final FailureTriageSlackMessageService failureTriageSlackMessageService;
    private final NotificationService notificationService;
    private final FailureTriageSlackMessageBuilder failureTriageSlackMessageBuilder;
    private final StateDBService stateDBService;
    private final WorkItemSlackMessageBuilder workItemSlackMessageBuilder;
    private final SnippetWorkItemSlackMessageBuilder snippetWorkItemSlackMessageBuilder;
    private final SlackWorkItemCacheService slackWorkItemCacheService;
    private final SlackInteractiveMessageResultService slackInteractiveMessageResultService;
    private final String internalApiUrl;
    private final String appBaseUrl;

    @Autowired
    public WorkItemsNotificationService(WorkItemFailureTriageViewService workItemFailureTriageViewService,
                                        FailureTriageSlackMessageService failureTriageSlackMessageService, NotificationService notificationService, FailureTriageSlackMessageBuilder failureTriageSlackMessageBuilder,
                                        StateDBService stateDBService, WorkItemSlackMessageBuilder workItemSlackMessageBuilder,
                                        SnippetWorkItemSlackMessageBuilder snippetWorkItemSlackMessageBuilder, SlackWorkItemCacheService slackWorkItemCacheService, SlackInteractiveMessageResultService slackInteractiveMessageResultService,
                                        @Value("${INTERNAL_API_URL:http://internal-api-lb}") String internalApiUrl,
                                        @Value("${APP_BASE_URL}") final String appBaseUrl) {
        this.workItemFailureTriageViewService = workItemFailureTriageViewService;
        this.failureTriageSlackMessageService = failureTriageSlackMessageService;
        this.notificationService = notificationService;
        this.failureTriageSlackMessageBuilder = failureTriageSlackMessageBuilder;
        this.stateDBService = stateDBService;
        this.workItemSlackMessageBuilder = workItemSlackMessageBuilder;
        this.snippetWorkItemSlackMessageBuilder = snippetWorkItemSlackMessageBuilder;
        this.slackWorkItemCacheService = slackWorkItemCacheService;
        this.slackInteractiveMessageResultService = slackInteractiveMessageResultService;
        this.internalApiUrl = internalApiUrl;
        this.appBaseUrl = appBaseUrl;
    }

    private SlackNotificationResult queueFailureTriageNotification(final String company, final WorkItemNotificationRequest workItemNotificationRequest, final WorkItem workItem, final List<String> states, boolean sendSync) throws SQLException, IOException {
        UUID workItemId = workItemNotificationRequest.getWorkItemId();

        //Using WI Id fetch WI &  Using WI fetch Jenkins Job Runs, Stages, rule hits and snippets
        List<WorkItemFailureTriageViewService.WIFailureTriageView> views = workItemFailureTriageViewService.getFailureTriageForWorkItem(company, workItemId);
        log.debug("views = {}", views);
        FailureTriageSlackMessage failureTriageSlackMessage = failureTriageSlackMessageService.convertViewToFailureTriageSlackMessage(views);
        log.debug("failureTriageSlackMessage = {}", failureTriageSlackMessage);

        //Using above data create interactive message text
        List<ImmutablePair<String,String>> fileUploads = new ArrayList<>();
        String interactiveMessage = failureTriageSlackMessageBuilder.buildInteractiveMessage(appBaseUrl, workItem.getVanityId(), failureTriageSlackMessage, states, fileUploads);
        log.debug("interactiveMessage = {}", interactiveMessage);

        //Calculate callback url
        String callbackUrl = internalApiUrl + "/internal/v1/tenants/" + company + "/workitems_notifications/callbacks/slack";
        log.debug("callbackUrl = {}", callbackUrl);

        //Use notification service to queue slack message with interactive message text & callback url
        SlackNotificationResult result = notificationService.sendWorkItemInteractiveNotification(company, workItemId, workItemNotificationRequest.getMode(), interactiveMessage, fileUploads, workItemNotificationRequest.getRecipients(), "LevelOps", callbackUrl, sendSync);
        log.debug("result = {}", result);
        return result;
    }

    private SlackNotificationResult queueGeneralWorkItemNotification(final String company, final WorkItemNotificationRequest workItemNotificationRequest, final WorkItem workItem, final List<String> states, final Map<UUID, String> uploadIdTextAttachmentsMap, boolean sendSync) throws IOException, SQLException {
        UUID workItemId = workItemNotificationRequest.getWorkItemId();

        //Create WI Slack Interactive message
        String botName = "LevelOps";
        WorkItemSlackMessageBuilder.WorkItemSlackMessages workItemSlackMessages = null;
        if(WorkItem.TicketType.SNIPPET.equals(workItem.getTicketType())) {
            workItemSlackMessages = snippetWorkItemSlackMessageBuilder.buildInteractiveMessage(company, appBaseUrl, workItem, states, uploadIdTextAttachmentsMap, workItemNotificationRequest.getRequestorName(), workItemNotificationRequest.getMessage());
            botName = "Security Awareness"; // used to be Failure Triage Bot, hardcoding to LevelOps in the meantime while we make it tenant specific 
        } else {
            workItemSlackMessages = workItemSlackMessageBuilder.buildInteractiveMessage(company, appBaseUrl, workItem, states);
            botName = "LevelOps";
        }

        String interactiveMessage = workItemSlackMessages.getMessage();
        log.debug("interactiveMessage = {}", interactiveMessage);

        //Save modal message to cache
        List<ImmutablePair<UUID, String>> modalMessages = workItemSlackMessages.getModalMessages();
        if(CollectionUtils.isNotEmpty(modalMessages)) {
            for(ImmutablePair<UUID, String> modalMessage : modalMessages) {
                UUID uploadId = modalMessage.getLeft();
                slackWorkItemCacheService.saveWITextAttachmentMessage(company, workItemId, uploadId, modalMessage.getRight());
                log.debug("Saved modal message to cache, company {}, workItemId {}, uploadId {}", company, workItemId, uploadId);
            }
        }

        //Calculate callback url
        String callbackUrl = internalApiUrl + "/internal/v1/tenants/" + company + "/workitems_notifications/callbacks/slack";
        log.debug("callbackUrl = {}", callbackUrl);

        //Use notification service to queue slack message with interactive message text & callback url
        SlackNotificationResult result = notificationService.sendWorkItemInteractiveNotification(company, workItemId, workItemNotificationRequest.getMode(), interactiveMessage, null, workItemNotificationRequest.getRecipients(), botName, callbackUrl, sendSync);
        if (result.getMessageResult() != null) {
            //handle callback if message is sent synchronously
            final List<String> ids = slackInteractiveMessageResultService.handleMessageResult(company, result.getMessageResult());
            log.debug("inserted notifications {}", ids);
        }
        log.debug("result = {}", result);
        return result;
    }

    public SlackNotificationResult queueWorkItemNotification(final String company, final WorkItemNotificationRequest workItemNotificationRequest, WorkItem workItem, final Map<UUID, String> uploadIdTextAttachmentsMap, Boolean sendSync) throws SQLException, IOException {
        UUID workItemId = workItemNotificationRequest.getWorkItemId();

        DbListResponse<State> dbListResponse = stateDBService.list(company, 0, 100);
        List<String> states = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream().map(State::getName).collect(Collectors.toList());
        log.debug("states {}", states);

        //Fetch WI
        if(WorkItem.TicketType.FAILURE_TRIAGE.equals(workItem.getTicketType())) {
            return queueFailureTriageNotification(company, workItemNotificationRequest, workItem, states, BooleanUtils.isTrue(sendSync));
        }
        return queueGeneralWorkItemNotification(company, workItemNotificationRequest, workItem, states, uploadIdTextAttachmentsMap, BooleanUtils.isTrue(sendSync));
    }

    public void rebuildWorkItemViewTextAttachmentSlackMessageCache(final String company, WorkItem workItem, final Map<UUID, String> uploadIdTextAttachmentsMap) throws IOException {
        UUID workItemId = UUID.fromString(workItem.getId());

        //Create WI Slack Interactive message
        WorkItemSlackMessageBuilder.WorkItemSlackMessages workItemSlackMessages = snippetWorkItemSlackMessageBuilder.buildInteractiveMessage(company, appBaseUrl, workItem, Collections.emptyList(), uploadIdTextAttachmentsMap, "", "");

        //Save modal message to cache
        List<ImmutablePair<UUID, String>> modalMessages = workItemSlackMessages.getModalMessages();
        if(CollectionUtils.isNotEmpty(modalMessages)) {
            for(ImmutablePair<UUID, String> modalMessage : modalMessages) {
                UUID uploadId = modalMessage.getLeft();
                slackWorkItemCacheService.saveWITextAttachmentMessage(company, workItemId, uploadId, modalMessage.getRight());
                log.debug("Saved modal message to cache, company {}, workItemId {}, uploadId {}", company, workItemId, uploadId);
            }
        }
    }
}
