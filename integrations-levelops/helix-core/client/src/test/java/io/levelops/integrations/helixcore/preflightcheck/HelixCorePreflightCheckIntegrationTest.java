package io.levelops.integrations.helixcore.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.HelixCorePreflightCheck;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class HelixCorePreflightCheckIntegrationTest {

    public static final String HELIX_CORE = "helix_core";
    public static final String TEST = "test";
    public static final String ACTIVE = "ACTIVE";
    private static final String TENANT_ID = "test";
    private static final String HELIX_CORE_HOST = System.getenv("HELIX_CORE_URL");
    private static final String HELIX_CORE_USERNAME = System.getenv("HELIX_CORE_USERNAME");
    private static final String HELIX_CORE_PASSWORD = System.getenv("HELIX_CORE_PASSWORD");
    private HelixCorePreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new HelixCorePreflightCheck();
        integration = Integration.builder()
                .application(HELIX_CORE)
                .name(TEST)
                .status(ACTIVE)
                .url(HELIX_CORE_HOST)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(HELIX_CORE_USERNAME)
                        .userName(HELIX_CORE_PASSWORD)
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
