package io.levelops.integrations.helix;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.HelixPreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class HelixPreflightCheckIntegrationTest {

    public static final String HELIX = "helix";
    public static final String TEST = "test";
    public static final String ACTIVE = "ACTIVE";
    private static final String TENANT_ID = "test";
    private static final String HELIX_CORE_HOST = System.getenv("HELIX_CORE_URL");
    private static final String HELIX_CORE_USERNAME = System.getenv("HELIX_CORE_USERNAME");
    private static final String HELIX_CORE_PASSWORD = System.getenv("HELIX_CORE_PASSWORD");
    private static final String HELIX_SWARM_URL = System.getenv("HELIX_SWARM_URL");
    private static final String SSL_ENABLED = System.getenv("SSL_ENABLED");
    private static final String SSL_AUTO_ACCEPT = System.getenv("SSL_AUTO_ACCEPT");
    private static final String SSL_FINGERPRINT = System.getenv("SSL_FINGERPRINT");
    private HelixPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        Map<String, Object> metadata = Map.of("ssl_enabled", Boolean.valueOf(SSL_ENABLED), "ssl_auto_accept",
                Boolean.valueOf(SSL_AUTO_ACCEPT), "ssl_fingerprint", SSL_FINGERPRINT,
                "helix_swarm_url", HELIX_SWARM_URL);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        preflightCheck = new HelixPreflightCheck(DefaultObjectMapper.get(), okHttpClient);
        integration = Integration.builder()
                .application(HELIX)
                .name(TEST)
                .status(ACTIVE)
                .url(HELIX_CORE_HOST)
                .satellite(false)
                .metadata(metadata)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(HELIX_CORE_PASSWORD)
                        .userName(HELIX_CORE_USERNAME)
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
