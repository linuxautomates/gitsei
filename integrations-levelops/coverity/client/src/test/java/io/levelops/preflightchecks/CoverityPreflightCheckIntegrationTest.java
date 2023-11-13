package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.models.PreflightCheckResults;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverityPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";

    private static final String COVERITY_URL = System.getenv("COVERITY_URL");
    private static final String COVERITY_USERNAME = System.getenv("COVERITY_USERNAME");
    private static final String COVERITY_API_KEY = System.getenv("COVERITY_API_KEY");

    private CoverityPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new CoverityPreflightCheck();
        integration = Integration.builder()
                .application("coverity")
                .name("test")
                .status("ACTIVE")
                .url(COVERITY_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(COVERITY_API_KEY)
                        .userName(COVERITY_USERNAME)
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
