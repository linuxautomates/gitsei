package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import okhttp3.OkHttpClient;
import org.junit.Test;

public class SnykPreflightCheckIntegrationTest {
    @Test
    public void name() {
        SnykPreflightCheck preflightCheck = new SnykPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());

        PreflightCheckResults r = preflightCheck.check("foo",
                Integration.builder()
                        .build(),
                Token.builder()
                        .tokenData(ApiKey.builder()
                                //System.getenv("SPLUNK-OAUTH-TOKEN");
                                .apiKey(System.getenv("SNYK-API-KEY"))
                                .build())
                        .build());

        DefaultObjectMapper.prettyPrint(r);
    }

}