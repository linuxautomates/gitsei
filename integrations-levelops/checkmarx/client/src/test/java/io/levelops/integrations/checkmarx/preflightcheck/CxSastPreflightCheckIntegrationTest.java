package io.levelops.integrations.checkmarx.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.CxSastPreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastPreflightCheckIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String CXSAST_URL = System.getenv("CXSAST_URL");
    private static final String CXSAST_ACCESS_TOKEN = System.getenv("CXSAST_ACCESS_TOKEN");
    private static final String CXSAST_REFRESH_TOKEN = System.getenv("CXSAST_REFRESH_TOKEN");
    private static final String CXSAST = "cxsast";
    private CxSastPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new CxSastPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application(CXSAST)
                .name("test")
                .status("ACTIVE")
                .url(CXSAST_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(OauthToken.builder()
                        .token(CXSAST_ACCESS_TOKEN)
                        .refreshToken(CXSAST_REFRESH_TOKEN)
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
