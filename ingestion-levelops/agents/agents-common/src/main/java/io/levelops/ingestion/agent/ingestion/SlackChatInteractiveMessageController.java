package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Notification;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.slack.models.SlackChatInteractiveMessageQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.slack.client.SlackBotClientFactory;
import io.levelops.integrations.slack.client.SlackClientException;
import io.levelops.integrations.slack.models.SlackApiChannel;
import io.levelops.integrations.slack.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.integrations.slack.models.SlackApiUser;
import io.levelops.integrations.slack.models.SlackChatPostMessageResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class SlackChatInteractiveMessageController implements DataController<SlackChatInteractiveMessageQuery> {
    private final ObjectMapper objectMapper;
    private final SlackBotClientFactory slackBotClientFactory;

    @Builder
    public SlackChatInteractiveMessageController(ObjectMapper objectMapper, SlackBotClientFactory slackBotClientFactory) {
        this.objectMapper = objectMapper;
        this.slackBotClientFactory = slackBotClientFactory;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, SlackChatInteractiveMessageQuery query) throws IngestException {
        log.info("Starting work on job id {}, query = {}", jobContext.getJobId(), query.toBuilder()
                .text(StringUtils.abbreviate(query.getText(), 256))
                .build());
        int totalRecipients = CollectionUtils.size(query.getRecipients());
        final UUID workItemId = query.getWorkItemId();
        final UUID questionnaireId = query.getQuestionnaireId();
        List<Notification> notifications = ListUtils.emptyIfNull(query.getRecipients()).stream()
                .map(recipient -> postOneMessage(workItemId, questionnaireId, query.getIntegrationKey(), recipient, query.getBotName(), query.getText(), query.getFileUploads()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("notifications = {}", notifications);
        boolean success = (totalRecipients == notifications.size());
        SlackChatInteractiveMessageResult result = SlackChatInteractiveMessageResult.builder().success(success).totalRecipients(totalRecipients).notifications(notifications).build();
        log.info("SlackChatInteractiveMessageResult = {}", result);
        return result;
    }

    private Notification postOneMessage(UUID workItemId, UUID questionnaireId, IntegrationKey integrationKey, String recipient, String botName, String text, List<SlackChatInteractiveMessageQuery.FileUpload> fileUploads) {
        // if empty, skip it silently
        if (StringUtils.isBlank(recipient)) {
            return null;
        }
        Optional<String> channelOpt = convertRecipientToChannel(integrationKey, recipient);
        if (channelOpt.isEmpty()) {
            log.info("recipient {}, channel is empty, cannot send slack notification", recipient);
            return null;
        }
        String channel = channelOpt.get();
        log.info("Sending chat message to channel={}", channel);
        try {
            InteractiveMessageResult messageResult = postInteractiveMessage(integrationKey, text, fileUploads, channel, botName);
            return Notification.builder()
                    .workItemId(workItemId).questionnaireId(questionnaireId).mode(NotificationMode.SLACK).recipient(recipient)
                    .referenceId(messageResult.getMessageTs()).channelId(messageResult.getChannelId()).url(messageResult.getPermaLink())
                    .build();
        } catch (SlackClientException e) {
            log.error("Could not send Slack message to channel=" + channel, e);
            return null;
        }
    }

    private Optional<String> convertRecipientToChannel(IntegrationKey integrationKey, String recipient) {
        if (StringUtils.isBlank(recipient)) {
            return Optional.empty();
        }
        // if it's a channel name or channel id, return as is
        if (!recipient.contains("@")) {
            return Optional.of(recipient);
        }
        // otherwise it's a user email, so look up channel id by email
        try {
            var channelOpt = getDirectMessageChannelFromEmail(integrationKey, recipient)
                    .map(SlackApiChannel::getId);
            if (channelOpt.isEmpty()) {
                log.error("Could not find user or failed to open IM channel for: " + recipient);
            }
            return channelOpt;
        } catch (SlackClientException e) {
            log.error("Failed to look up Slack user channel by email: " + recipient, e);
            return Optional.empty();
        }
    }

    private Optional<SlackApiChannel> getDirectMessageChannelFromEmail(IntegrationKey key, String userEmail) throws SlackClientException {
        Optional<String> userIdOpt = lookUpUserByEmail(key, userEmail)
                .map(SlackApiUser::getId);
        if (userIdOpt.isEmpty()) {
            return Optional.empty();
        }
        return openImChannel(key, userIdOpt.get());
    }

    private Optional<SlackApiChannel> openImChannel(IntegrationKey key, String userId) throws SlackClientException {
        return slackBotClientFactory.get(key).openImChannel(userId);
    }

    private Optional<SlackApiUser> lookUpUserByEmail(IntegrationKey integrationKey, String userEmail) throws SlackClientException {
        return slackBotClientFactory.get(integrationKey).lookupUserByEmail(userEmail);
    }

    private InteractiveMessageResult postInteractiveMessage(IntegrationKey integrationKey, String text, List<SlackChatInteractiveMessageQuery.FileUpload> fileUploads, String channelId, String botName) throws SlackClientException {
        SlackChatPostMessageResponse response = slackBotClientFactory.get(integrationKey).postChatInteractiveMessage(channelId, text, botName);
        String interactiveMessageId = response.getTs();
        for(SlackChatInteractiveMessageQuery.FileUpload fileUpload : fileUploads) {
            try {
                slackBotClientFactory.get(integrationKey).fileUpload(List.of(channelId), fileUpload.getFileName(), fileUpload.getFileContent(), interactiveMessageId);
            } catch (SlackClientException e) {
                log.error("Error uploading file as reply to message id {}, channelNameOrId {}, fileName {}", interactiveMessageId, channelId, fileUpload.getFileName());
            }
        }
        SlackApiChatMessagePermalinkResponse permalinkResponse = slackBotClientFactory.get(integrationKey).getChatMessagePermalink(response.getChannel(), response.getTs());

        return InteractiveMessageResult.builder()
                .channelId(response.getChannel()).messageTs(response.getTs()).permaLink(permalinkResponse.getPermalink())
                .build();
    }

    @Override
    public SlackChatInteractiveMessageQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, SlackChatInteractiveMessageQuery.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = InteractiveMessageResult.InteractiveMessageResultBuilder.class)
    public static class InteractiveMessageResult {
        String channelId;
        String messageTs;
        String permaLink;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackChatInteractiveMessageResult.SlackChatInteractiveMessageResultBuilder.class)
    public static class SlackChatInteractiveMessageResult implements ControllerIngestionResult {
        @JsonProperty("success")
        Boolean success;

        @JsonProperty("total_recipients")
        Integer totalRecipients;

        @JsonProperty("notifications")
        List<Notification> notifications;
    }
}
