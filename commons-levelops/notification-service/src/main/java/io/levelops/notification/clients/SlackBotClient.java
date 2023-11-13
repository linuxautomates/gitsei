package io.levelops.notification.clients;

import io.levelops.notification.models.SlackApiChannel;
import io.levelops.notification.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.notification.models.SlackApiFileUploadResponse;
import io.levelops.notification.models.SlackApiUser;
import io.levelops.notification.models.SlackApiViewResponse;
import io.levelops.notification.models.SlackChatPostMessageResponse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface SlackBotClient {
    SlackApiViewResponse openView(String triggerId, String viewMessage) throws SlackClientException;

    SlackChatPostMessageResponse postChatInteractiveMessage(String channelId, String text, @Nullable String botName) throws SlackClientException;

    Optional<SlackApiUser> lookupUserByEmail(String email) throws SlackClientException;

    Optional<SlackApiChannel> openImChannel(String userId) throws SlackClientException;

    SlackApiFileUploadResponse fileUpload(List<String> channelNamesOrIds, String fileName, String fileContent, String threadId) throws SlackClientException;

    SlackApiChatMessagePermalinkResponse getChatMessagePermalink(String channelId, String messageTs) throws SlackClientException;

    Optional<SlackApiUser> getUserInfo(String userId) throws SlackClientException;
}
