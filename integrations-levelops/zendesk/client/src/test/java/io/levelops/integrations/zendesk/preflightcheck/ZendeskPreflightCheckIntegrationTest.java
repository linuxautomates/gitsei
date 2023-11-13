package io.levelops.integrations.zendesk.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.ZendeskPreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ZendeskPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String ZENDESK_URL = System.getenv("ZENDESK_URL");
    private static final String ZENDESK_USERNAME = System.getenv("ZENDESK_USERNAME");
    private static final String ZENDESK_API_KEY = System.getenv("ZENDESK_API_KEY");

    private ZendeskPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new ZendeskPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("zendesk")
                .name("test")
                .status("ACTIVE")
                .url(ZENDESK_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(ZENDESK_API_KEY)
                        .userName(ZENDESK_USERNAME)
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
