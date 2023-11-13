package io.levelops.integrations.slack.client;

import io.levelops.integrations.slack.models.SlackApiChannel;
import io.levelops.integrations.slack.models.SlackApiChannelResponse;
import io.levelops.integrations.slack.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.integrations.slack.models.SlackApiFileUploadResponse;
import io.levelops.integrations.slack.models.SlackApiUser;
import io.levelops.integrations.slack.models.SlackChatPostMessageResponse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface SlackBotClient {

    void postChatMessage(String channelId, String text, @Nullable String botName) throws SlackClientException;

    SlackChatPostMessageResponse postChatInteractiveMessage(String channelId, String text, @Nullable String botName) throws SlackClientException;

    Optional<SlackApiUser> lookupUserByEmail(String email) throws SlackClientException;

    Optional<SlackApiUser> lookupUserById(String userId) throws SlackClientException;

    Optional<SlackApiChannel> openImChannel(String userId) throws SlackClientException;

    SlackApiChannelResponse joinChannel(String channelNameOrId) throws SlackClientException;

    SlackApiChannelResponse joinConversation(String channelId) throws SlackClientException;

    SlackApiFileUploadResponse fileUpload(List<String> channelNamesOrIds, String fileName, String fileContent, String threadId) throws SlackClientException;

    SlackApiChatMessagePermalinkResponse getChatMessagePermalink(String channelId, String messageTs) throws SlackClientException;
}
