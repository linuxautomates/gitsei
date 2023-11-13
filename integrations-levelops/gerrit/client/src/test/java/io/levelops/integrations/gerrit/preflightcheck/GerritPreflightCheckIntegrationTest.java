package io.levelops.integrations.gerrit.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.GerritPreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class GerritPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String GERRIT_URL = System.getenv("GERRIT_URL");
    private static final String GERRIT_USERNAME = System.getenv("GERRIT_USERNAME");
    private static final String GERRIT_PASSWORD = System.getenv("GERRIT_PASSWORD");

    private GerritPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new GerritPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("gerrit")
                .name("test")
                .status("ACTIVE")
                .url(GERRIT_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(GERRIT_PASSWORD)
                        .userName(GERRIT_USERNAME)
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
