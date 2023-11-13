package io.levelops.integrations.slack.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.integrations.slack.models.SlackApiChannel;
import io.levelops.integrations.slack.models.SlackApiChannelResponse;
import io.levelops.integrations.slack.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.integrations.slack.models.SlackApiFileUploadResponse;
import io.levelops.integrations.slack.models.SlackApiUser;
import io.levelops.integrations.slack.models.SlackChatPostMessageResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Log4j2
public class CachedSlackBotClient implements SlackBotClient {

    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRATION_IN_MIN = 60;
    private final SlackBotClient delegate;

    private final LoadingCache<String, Optional<SlackApiUser>> EMAIL_TO_USER_CACHE = CacheBuilder.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(CACHE_EXPIRATION_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Optional<SlackApiUser> load(@NotNull String userEmail) throws Exception {
                    log.debug("Caching user email lookup for {}", userEmail);
                    return delegate.lookupUserByEmail(userEmail);
                }
            });
    private final LoadingCache<String, Optional<SlackApiChannel>> USER_IM_CHANNEL_CACHE = CacheBuilder.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(CACHE_EXPIRATION_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Optional<SlackApiChannel> load(@NotNull String userId) throws Exception {
                    log.debug("Caching IM channel for {}", userId);
                    return delegate.openImChannel(userId);
                }
            });

    @Builder
    public CachedSlackBotClient(SlackBotClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void postChatMessage(String channelId, String text, @Nullable String botName) throws SlackClientException {
        delegate.postChatMessage(channelId, text, botName);
    }

    @Override
    public SlackChatPostMessageResponse postChatInteractiveMessage(String channelId, String text, @org.jetbrains.annotations.Nullable String botName) throws SlackClientException {
        return delegate.postChatInteractiveMessage(channelId, text, botName);
    }

    @Override
    public Optional<SlackApiUser> lookupUserByEmail(String email) throws SlackClientException {
        return handleCacheException(() -> EMAIL_TO_USER_CACHE.get(email));
    }

    @Override
    public Optional<SlackApiUser> lookupUserById(String userId) throws SlackClientException {
        return delegate.lookupUserById(userId);
    }

    @Override
    public Optional<SlackApiChannel> openImChannel(String userId) throws SlackClientException {
        return handleCacheException(() -> USER_IM_CHANNEL_CACHE.get(userId));
    }

    @Override
    public SlackApiChannelResponse joinChannel(String channelNameOrId) throws SlackClientException {
        return delegate.joinChannel(channelNameOrId);
    }

    @Override
    public SlackApiChannelResponse joinConversation(String channelId) throws SlackClientException {
        return delegate.joinConversation(channelId);
    }

    @Override
    public SlackApiFileUploadResponse fileUpload(List<String> channelNamesOrIds, String fileName, String fileContent, String threadId) throws SlackClientException {
        return delegate.fileUpload(channelNamesOrIds, fileName, fileContent, threadId);
    }

    @Override
    public SlackApiChatMessagePermalinkResponse getChatMessagePermalink(String channelId, String messageTs) throws SlackClientException {
        return delegate.getChatMessagePermalink(channelId, messageTs);
    }

    private static <T> T handleCacheException(Callable<T> cacheAccess) throws SlackClientException {
        try {
            return cacheAccess.call();
        } catch (Exception e) {
            if (e.getCause() instanceof SlackClientException) {
                throw (SlackClientException) e.getCause();
            } else {
                throw new SlackClientException(e);
            }
        }
    }
}
