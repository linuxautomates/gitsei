package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;
import okhttp3.OkHttpClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraPreflightCheckIntegrationTest {

    @Test
    public void name() {
        JiraPreflightCheck preflightCheck = new JiraPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());

        PreflightCheckResults out = preflightCheck.check("mock", Integration.builder()
                        .url("asdasdaskdkajskldas")
                        .build(),
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .apiKey("key#123")
                                .userName("user@localhost")
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(out);
        assertThat(out.isSuccess()).isFalse();
    }

    @Test
    public void test2() {
        JiraPreflightCheck preflightCheck = new JiraPreflightCheck(DefaultObjectMapper.get(), new OkHttpClient());

        PreflightCheckResults out = preflightCheck.check("levelops", Integration.builder()
                        .url("https://levelops.atlassian.net")
                        .build(),
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .apiKey(System.getenv("API_KEY"))
                                .userName(System.getenv("USERNAME"))
                                .build())
                        .build());
        DefaultObjectMapper.prettyPrint(out);
        assertThat(out.isSuccess()).isTrue();
    }
}