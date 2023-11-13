package io.levelops.notification.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Notification;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.integrations.slack.models.SlackChatInteractiveMessageQuery;
import io.levelops.notification.clients.SlackBotClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.notification.models.SlackApiChannel;
import io.levelops.notification.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.notification.models.SlackApiUser;
import io.levelops.notification.models.SlackChatPostMessageResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class SlackService {

    private final SlackBotClientFactory slackBotClientFactory;

    public SlackService(SlackBotClientFactory slackBotClientFactory) {
        this.slackBotClientFactory = slackBotClientFactory;
    }

    public SlackChatInteractiveMessageResult sendWorkItemInteractiveNotification(
            final IntegrationKey integrationKey, final UUID workItemId, final UUID questionnaireId, final String text,
            final List<ImmutablePair<String, String>> fileUploads, final List<String> recipients,
            final String botName) {
        final List<SlackChatInteractiveMessageQuery.FileUpload> fileUploadList = ListUtils.emptyIfNull(fileUploads)
                .stream()
                .map(fileUpload -> SlackChatInteractiveMessageQuery.FileUpload.builder()
                        .fileName(fileUpload.getLeft())
                        .fileContent(fileUpload.getRight())
                        .build())
                .collect(Collectors.toList());
        int totalRecipients = CollectionUtils.size(recipients);
        List<Notification> notifications = ListUtils.emptyIfNull(recipients).stream()
                .map(recipient -> postOneMessage(workItemId, questionnaireId, integrationKey, recipient, botName,
                        text, fileUploadList))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("notifications = {}", notifications);
        boolean success = (totalRecipients == notifications.size());
        SlackChatInteractiveMessageResult result = SlackChatInteractiveMessageResult.builder()
                .success(success)
                .totalRecipients(totalRecipients)
                .notifications(notifications)
                .build();
        log.info("SlackChatInteractiveMessageResult = {}", result);
        return result;
    }

    private Notification postOneMessage(UUID workItemId, UUID questionnaireId, IntegrationKey integrationKey,
                                        String recipient, String botName, String text,
                                        List<SlackChatInteractiveMessageQuery.FileUpload> fileUploads) {
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
            InteractiveMessageResult messageResult = postInteractiveMessage(integrationKey, text, fileUploads,
                    channel, botName);
            return Notification.builder()
                    .workItemId(workItemId)
                    .questionnaireId(questionnaireId)
                    .mode(NotificationMode.SLACK)
                    .recipient(recipient)
                    .referenceId(messageResult.getMessageTs())
                    .channelId(messageResult.getChannelId())
                    .url(messageResult.getPermaLink())
                    .build();
        } catch (SlackClientException e) {
            log.error("Could not send Slack message to channel=" + channel, e);
            return null;
        }
    }

    private InteractiveMessageResult postInteractiveMessage(
            IntegrationKey integrationKey, String text,
            List<SlackChatInteractiveMessageQuery.FileUpload> fileUploads, String channelId,
            String botName) throws SlackClientException {
        SlackChatPostMessageResponse response = slackBotClientFactory.get(integrationKey)
                .postChatInteractiveMessage(channelId, text, botName);
        String interactiveMessageId = response.getTs();
        for (SlackChatInteractiveMessageQuery.FileUpload fileUpload : fileUploads) {
            try {
                slackBotClientFactory.get(integrationKey).fileUpload(List.of(channelId), fileUpload.getFileName(),
                        fileUpload.getFileContent(), interactiveMessageId);
            } catch (SlackClientException e) {
                log.error("Error uploading file as reply to message id {}, channelNameOrId {}, fileName {}",
                        interactiveMessageId, channelId, fileUpload.getFileName());
            }
        }
        SlackApiChatMessagePermalinkResponse permalinkResponse = slackBotClientFactory.get(integrationKey)
                .getChatMessagePermalink(response.getChannel(), response.getTs());
        return InteractiveMessageResult.builder()
                .channelId(response.getChannel())
                .messageTs(response.getTs())
                .permaLink(permalinkResponse.getPermalink())
                .build();
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

    private Optional<SlackApiChannel> getDirectMessageChannelFromEmail(IntegrationKey key,
                                                                       String userEmail) throws SlackClientException {
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

    private Optional<SlackApiUser> lookUpUserByEmail(IntegrationKey integrationKey,
                                                     String userEmail) throws SlackClientException {
        return slackBotClientFactory.get(integrationKey).lookupUserByEmail(userEmail);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = InteractiveMessageResult.InteractiveMessageResultBuilder.class)
    public static class InteractiveMessageResult {
        String channelId;
        String messageTs;
        String permaLink;
    }
}
