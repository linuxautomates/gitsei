package io.levelops.integrations.jira.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import io.levelops.commons.client.oauth.BasicAuthInterceptor;
import io.levelops.commons.client.throttling.ThrottlingInterceptor;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;

import javax.annotation.Nullable;

/*
 * This class is only to be used with internal tools that want to create Propelo jira tickets. For integration related
 * slack operations please use JiraClientFactory instead
 */
@Log4j2
public class JiraInternalClientFactory {
    private static final String URL = "https://levelops.atlassian.net";
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    @Nullable
    private final Double rateLimitPerSecond; // Can be fractional
    private final String token;
    private final String userName;

    public JiraInternalClientFactory(
            ObjectMapper objectMapper,
            OkHttpClient okHttpClient,
            @Nullable Double rateLimitPerSecond,
            String apiToken,
            String userName) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.rateLimitPerSecond = rateLimitPerSecond;
        this.token = apiToken;
        this.userName = userName;
    }

    public JiraClient get() {
        var apiKey = ApiKey.builder()
                .apiKey(token)
                .userName(userName)
                .build();
        OkHttpClient authenticatedHttpClient = this.okHttpClient.newBuilder()
                .addInterceptor(new BasicAuthInterceptor(apiKey.getAuthorizationHeader()))
                .build();

        if (rateLimitPerSecond != null && rateLimitPerSecond > 0.01) {
            log.info("Jira rate limit enabled for internal jira client: {} request(s) per second", rateLimitPerSecond);
            authenticatedHttpClient = authenticatedHttpClient.newBuilder()
                    .addInterceptor(new ThrottlingInterceptor(rateLimitPerSecond))
                    .build();
        }

        return JiraClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(authenticatedHttpClient)
                .jiraUrlSupplier(Suppliers.ofInstance(URL))
                .allowUnsafeSSL(false)
                .disableUrlSanitation(false)
                .build();
    }
}
