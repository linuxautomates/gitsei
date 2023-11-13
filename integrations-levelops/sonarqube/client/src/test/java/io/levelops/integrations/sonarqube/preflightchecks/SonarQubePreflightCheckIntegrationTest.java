package io.levelops.integrations.sonarqube.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.SonarQubePreflightCheck;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubePreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String SONARQUBE_URL = System.getenv("SONARQUBE_URL");
    private static final String SONARQUBE_API_KEY = System.getenv("SONARQUBE_API_KEY");

    private SonarQubePreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new SonarQubePreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());
        integration = Integration.builder()
                .application("sonarqube")
                .name("test")
                .status("ACTIVE")
                .url(SONARQUBE_URL)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .apiKey(SONARQUBE_API_KEY)
                        .userName("")
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
