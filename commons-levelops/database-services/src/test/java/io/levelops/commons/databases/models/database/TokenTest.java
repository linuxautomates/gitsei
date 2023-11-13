package io.levelops.commons.databases.models.database;

import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TokenTest {

    @Test
    public void name() throws IOException {
        String input = "{\"id\": \"123\", \"token_data\" : { \"type\" : \"oauth\" , \"token\" : \"abc\" } }";


        Token token = DefaultObjectMapper.get().readValue(input, Token.class);
        assertThat(token.getId()).isEqualTo("123");
        assertThat(token.getTokenData().getType()).isEqualTo("oauth");

        OauthToken tokenData = (OauthToken) token.getTokenData();
        assertThat(tokenData.getType()).isEqualTo("oauth");
        assertThat(tokenData.getToken()).isEqualTo("abc");
    }
}