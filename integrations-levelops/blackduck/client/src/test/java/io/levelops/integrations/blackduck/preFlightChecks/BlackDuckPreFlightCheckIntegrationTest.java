package io.levelops.integrations.blackduck.preFlightChecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.BlackDuckPreFlightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class BlackDuckPreFlightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String BLACKDUCK_URL = System.getenv("BLACKDUCK_URL");
    private static final String BLACKDUCK_BEARER_TOKEN = System.getenv("BLACKDUCK_BEARER_TOKEN");

    private BlackDuckPreFlightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new BlackDuckPreFlightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("blackduck")
                .name("test")
                .status("ACTIVE")
                .url(BLACKDUCK_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(OauthToken.builder()
                        .token(BLACKDUCK_BEARER_TOKEN)
                        .refreshToken(BLACKDUCK_BEARER_TOKEN)
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
