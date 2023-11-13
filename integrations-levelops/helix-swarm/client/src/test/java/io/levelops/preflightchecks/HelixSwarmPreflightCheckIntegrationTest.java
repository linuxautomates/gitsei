package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class HelixSwarmPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String HELIX_SWARM_INTEGRATION_ID = "1";
    private static final String HELIX_CORE_INTEGRATION_ID = "2";
    private static final String HELIX_SWARM_APPLICATION = "helix_swarm";
    private static final String HELIX_CORE_APPLICATION = "helix_core";
    private static final String HELIX_SWARM_URL = System.getenv("HELIX_SWARM_URL");
    private static final String HELIX_CORE_URL = System.getenv("HELIX_CORE_URL");
    private static final String USERNAME = System.getenv("HELIX_SWARM_USERNAME");
    private static final String HELIX_SWARM_PWD = System.getenv("HELIX_SWARM_PWD");
    private static final String SSL_ENABLED = System.getenv("SSL_ENABLED");
    private static final String SSL_AUTO_ACCEPT = System.getenv("SSL_AUTO_ACCEPT");
    private static final String SSL_FINGERPRINT = System.getenv("SSL_FINGERPRINT");

    private HelixSwarmPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;


    @Before
    public void setup() throws InventoryException {
        Map<String, Object> metadata = Map.of("ssl_enabled", Boolean.valueOf(SSL_ENABLED), "ssl_auto_accept",
                Boolean.valueOf(SSL_AUTO_ACCEPT), "ssl_fingerprint", SSL_FINGERPRINT);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, HELIX_SWARM_INTEGRATION_ID, HELIX_SWARM_APPLICATION, HELIX_SWARM_URL,
                        Map.of("helix_core_integration_key", 2), USERNAME, HELIX_SWARM_PWD)
                .apiKey(TENANT_ID, HELIX_CORE_INTEGRATION_ID, HELIX_CORE_APPLICATION, HELIX_CORE_URL,
                        metadata, USERNAME, HELIX_SWARM_PWD)
                .build());
        preflightCheck = new HelixSwarmPreflightCheck(DefaultObjectMapper.get(), okHttpClient);
        integration = Integration.builder()
                .application(HELIX_SWARM_APPLICATION)
                .name("test")
                .status("ACTIVE")
                .url(HELIX_SWARM_URL)
                .satellite(false)
                .metadata(Map.of("helix_core_integration_key", HELIX_CORE_INTEGRATION_ID))
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(HELIX_SWARM_PWD)
                        .userName(USERNAME)
                        .createdAt(Instant.now().getEpochSecond())
                        .build())
                .build();
    }

    @Test
    public void test() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results).isNotNull();
        assertThat(results.isSuccess());
        assertThat(results.getChecks().stream()
                .filter(check -> Boolean.FALSE.equals(check.getSuccess()))
                .findAny()).isEmpty();
    }
}
