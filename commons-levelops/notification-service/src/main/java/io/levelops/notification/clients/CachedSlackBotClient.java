package io.levelops.notification.clients;

import io.levelops.notification.models.SlackApiChannel;
import io.levelops.notification.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.notification.models.SlackApiFileUploadResponse;
import io.levelops.notification.models.SlackApiUser;
import io.levelops.notification.models.SlackApiViewResponse;
import io.levelops.notification.models.SlackChatPostMessageResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@Log4j2
public class CachedSlackBotClient implements SlackBotClient {
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRATION_IN_MIN = 60;
    private final SlackBotClient delegate;

    @Builder
    public CachedSlackBotClient(SlackBotClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public SlackApiViewResponse openView(String triggerId, String viewMessage) throws SlackClientException {
        return delegate.openView(triggerId, viewMessage);
    }

    @Override
    public SlackChatPostMessageResponse postChatInteractiveMessage(String channelId, String text, @Nullable String botName) throws SlackClientException {
        return delegate.postChatInteractiveMessage(channelId, text, botName);
    }

    @Override
    public Optional<SlackApiUser> lookupUserByEmail(String email) throws SlackClientException {
        return delegate.lookupUserByEmail(email);
    }

    @Override
    public Optional<SlackApiChannel> openImChannel(String userId) throws SlackClientException {
        return delegate.openImChannel(userId);
    }

    @Override
    public SlackApiFileUploadResponse fileUpload(List<String> channelNamesOrIds, String fileName, String fileContent, String threadId) throws SlackClientException {
        return delegate.fileUpload(channelNamesOrIds, fileName, fileContent, threadId);
    }

    @Override
    public SlackApiChatMessagePermalinkResponse getChatMessagePermalink(String channelId, String messageTs) throws SlackClientException {
        return delegate.getChatMessagePermalink(channelId, messageTs);
    }

    @Override
    public Optional<SlackApiUser> getUserInfo(String userId) throws SlackClientException {
        return delegate.getUserInfo(userId);
    }
}