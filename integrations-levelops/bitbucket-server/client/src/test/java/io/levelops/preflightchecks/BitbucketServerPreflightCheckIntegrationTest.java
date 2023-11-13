package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketServerPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String BITBUCKET_SERVER_URL = System.getenv("BITBUCKET_SERVER_URL");
    private static final String BITBUCKET_SERVER_USERNAME = System.getenv("BITBUCKET_SERVER_USERNAME");
    private static final String BITBUCKET_SERVER_PASSWORD = System.getenv("BITBUCKET_SERVER_PASSWORD");

    private BitbucketServerPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new BitbucketServerPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient(), null);
        integration = Integration.builder()
                .id("1")
                .application("bitbucket_server")
                .name("test")
                .status("ACTIVE")
                .url(BITBUCKET_SERVER_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .id("1")
                .integrationId(integration.getId())
                .tokenData(ApiKey.builder()
                        .userName(BITBUCKET_SERVER_USERNAME)
                        .apiKey(BITBUCKET_SERVER_PASSWORD)
                        .createdAt(new Date().getTime())
                        .build())
                .build();
    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }
}
