package io.levelops.integrations.azureDevops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.AzureDevopsPreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureDevopsPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String AZURE_DEVOPS_URL = System.getenv("AZURE_DEVOPS_URL");
    private static final String AZURE_DEVOPS_ORGANIZATION = System.getenv("AZURE_DEVOPS_ORGANIZATION");
    private static final String AZURE_DEVOPS_ACCESS_TOKEN = System.getenv("AZURE_DEVOPS_ACCESS_TOKEN");
    private static final String AZURE_DEVOPS_REFRESH_TOKEN = System.getenv("AZURE_DEVOPS_REFRESH_TOKEN");

    private AzureDevopsPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new AzureDevopsPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("azure_devops")
                .name("test")
                .status("ACTIVE")
                .url(AZURE_DEVOPS_URL)
                .metadata(Map.of("organization", AZURE_DEVOPS_ORGANIZATION))
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(OauthToken.builder()
                        .token(AZURE_DEVOPS_ACCESS_TOKEN)
                        .refreshToken(AZURE_DEVOPS_REFRESH_TOKEN)
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
