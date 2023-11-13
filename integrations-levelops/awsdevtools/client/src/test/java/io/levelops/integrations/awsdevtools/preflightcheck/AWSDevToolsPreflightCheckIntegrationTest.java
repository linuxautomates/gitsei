package io.levelops.integrations.awsdevtools.preflightcheck;

import com.amazonaws.regions.Regions;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightcheck.AWSDevToolsPreflightCheck;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AWSDevToolsPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY");

    private AWSDevToolsPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        preflightCheck = new AWSDevToolsPreflightCheck();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("regions", Regions.US_EAST_2.toString());
        integration = Integration.builder()
                .application("awsdevtools")
                .name("test")
                .status("ACTIVE")
                .metadata(metadata)
                .satellite(false)
                .build();
        token = Token.builder()
                .tokenData(ApiKey.builder()
                        .userName(AWS_ACCESS_KEY_ID)
                        .apiKey(AWS_SECRET_KEY)
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
