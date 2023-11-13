package io.levelops.notification.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import lombok.Builder;
import okhttp3.OkHttpClient;

/*
 * This class is only to be used with internal tools that want to post to Propelo slack. For integration related
 * slack operations please use SlackBotClientFactory instead
 */
public class SlackBotInternalClientFactory {


    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final String token;

    @Builder
    public SlackBotInternalClientFactory(ObjectMapper objectMapper,
                                         OkHttpClient okHttpClient,
                                         String token) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.token = token;
    }

    public SlackBotClient get() throws SlackClientException {
        var staticTokenProvider = StaticOauthTokenProvider.builder().token(this.token).build();
        return SlackBotClientImpl.builder()
                .objectMapper(this.objectMapper)
                .okHttpClient(this.okHttpClient.newBuilder()
                        .addInterceptor(new OauthTokenInterceptor(staticTokenProvider))
                        .build())
                .build();
    }
}
