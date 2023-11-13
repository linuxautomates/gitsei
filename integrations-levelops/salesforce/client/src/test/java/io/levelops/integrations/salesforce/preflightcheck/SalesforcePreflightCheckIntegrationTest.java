package io.levelops.integrations.salesforce.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.SalesforcePreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test case for preflight checks. Credentials are passed as sys variable.
 */
public class SalesforcePreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String SALESFORCE_URL = System.getenv("SALESFORCE_URL");
    private static final String SALESFORCE_TOKEN = System.getenv("SALESFORCE_TOKEN");;
    private static final String SALESFORCE_REFRESH_TOKEN = System.getenv("SALESFORCE_REFRESH_TOKEN");;

    private SalesforcePreflightCheck preflightCheck;
    private Integration integration;
    private Token token;
    private InventoryService inventoryService;

    @Before
    public void setup() {
        preflightCheck = new SalesforcePreflightCheck(inventoryService, DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("salesforce")
                .name("test")
                .url(SALESFORCE_URL)
                .status("ACTIVE")
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(OauthToken.builder()
                        .token(SALESFORCE_TOKEN)
                        .refreshToken(SALESFORCE_REFRESH_TOKEN)
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
