package io.levelops.integrations.harnessng.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.HarnessNGPreflightCheck;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class HarnessNGPreflightCheckIntegrationTest {
    private static final String TENANT_ID = "foo";
    private static final String HARNESSNG_URL = System.getenv("HARNESSNG_URL");
    private static final String HARNESSNG_TOKEN = System.getenv("HARNESSNG_TOKEN");

    private static final String DEFAULT_HARNESSNG_URL = System.getenv("DRONECI_HARNESSNG_URL");
    private static final String INTEGRATION_ID = "test";

    private HarnessNGPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        var client = new OkHttpClient().newBuilder().followRedirects(true).build();

        integration = Integration.builder()
                .id(INTEGRATION_ID)
                .application("harnessng")
                .name("test")
                .status("ACTIVE")
                .url(StringUtils.firstNonBlank(HARNESSNG_URL, DEFAULT_HARNESSNG_URL))
                .satellite(true)
                .build();
        token = Token.builder()
                .id("test")
                .integrationId("test")
                .tokenData(ApiKey.builder()
                        .apiKey(HARNESSNG_TOKEN)
                        .createdAt(new Date().getTime())
                        .build())
                .build();
        preflightCheck = new HarnessNGPreflightCheck(DefaultObjectMapper.get(), client);
    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }

    @Test
    public void getIntegrationType() {
        assertThat(preflightCheck.getIntegrationType()).isEqualTo("harnessng");
    }
}
