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

public class PrometheusPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";

    private static final String PROMETHEUS_URL = System.getenv("PROMETHEUS_URL");
    private static final String PROMETHEUS_USERNAME = "";
    private static final String PROMETHEUS_PASSWORD = "";


    private PrometheusPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new PrometheusPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("prometheus")
                .name("test")
                .status("ACTIVE")
                .url(PROMETHEUS_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(PROMETHEUS_USERNAME)
                        .userName(PROMETHEUS_PASSWORD)
                        .createdAt(new Date().getTime()).build())
                .build();
    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }
}
