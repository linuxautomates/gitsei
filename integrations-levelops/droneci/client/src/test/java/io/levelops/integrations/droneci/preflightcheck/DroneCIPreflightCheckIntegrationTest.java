package io.levelops.integrations.droneci.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.DroneCIPreflightCheck;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class DroneCIPreflightCheckIntegrationTest {
    private static final String TENANT_ID = "foo";
    private static final String DRONECI_URL = System.getenv("DRONECI_URL");
    private static final String DRONECI_TOKEN = System.getenv("DRONECI_TOKEN");

    private static final String DEFAULT_DRONECI_URL = System.getenv("DRONECI_DEFAULT_URL");
    private static final String INTEGRATION_ID = "test";

    private DroneCIPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        var client = new OkHttpClient().newBuilder().followRedirects(true).build();

        integration = Integration.builder()
                .id(INTEGRATION_ID)
                .application("droneci")
                .name("test")
                .status("ACTIVE")
                .url(StringUtils.firstNonBlank(DRONECI_URL, DEFAULT_DRONECI_URL))
                .satellite(true)
                .build();
        token = Token.builder()
                .id("test")
                .integrationId("test")
                .tokenData(ApiKey.builder()
                        .apiKey(DRONECI_TOKEN)
                        .createdAt(new Date().getTime())
                        .build())
                .build();
        preflightCheck = new DroneCIPreflightCheck(DefaultObjectMapper.get(), client);
    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }

    @Test
    public void getIntegrationType() {
        assertThat(preflightCheck.getIntegrationType()).isEqualTo("droneci");
    }
}
