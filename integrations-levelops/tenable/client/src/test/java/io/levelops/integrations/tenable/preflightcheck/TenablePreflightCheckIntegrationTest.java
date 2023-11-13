package io.levelops.integrations.tenable.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.TenablePreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test case for preflight checks. Credentials are passed as sys variable.
 */
public class TenablePreflightCheckIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String TENABLE_USERNAME = System.getenv("TENABLE_USERNAME");
    private static final String TENABLE_API_KEY = System.getenv("TENABLE_API_KEY");

    private TenablePreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new TenablePreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("tenable")
                .name("test")
                .status("ACTIVE")
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(TENABLE_API_KEY)
                        .userName(TENABLE_USERNAME)
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
