package io.levelops.commons.databases.models.database.tokens;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class TokenDataTest {


    @Test
    public void testDeserializationOauth() throws IOException {
        String input = "{ \"type\" : \"oauth\" , \"token\" : \"abc\", \"created_at\" : 123 }";


        TokenData token = DefaultObjectMapper.get().readValue(input, TokenData.class);
        assertThat(token.getType()).isEqualTo("oauth");
        OauthToken tokenData = (OauthToken) token;
        assertThat(tokenData.getType()).isEqualTo("oauth");
        assertThat(tokenData.getToken()).isEqualTo("abc");
        assertThat(tokenData.getCreatedAt()).isEqualTo(123);
        assertThat(tokenData.getRefreshedAt()).isEqualTo(null);
        assertThat(tokenData.getRefreshToken()).isEqualTo(null);
    }

    @Test
    public void testBuilderDefaultOauth() throws IOException {
        OauthToken oauth = OauthToken.builder()
                // no type!
                .token("abc")
                .build();

        OauthToken output = DefaultObjectMapper.get().readValue(
                DefaultObjectMapper.get().writeValueAsString(oauth),
                OauthToken.class);

        assertThat(output.getToken()).isEqualTo("abc");
        assertThat(output.getType()).isEqualTo("oauth");
    }

    @Test
    public void testDeserializationApiKey() throws IOException {
        String input = "{ \"type\" : \"apikey\" , \"apikey\" : \"abc\", \"username\" : \"u\" }";


        TokenData token = DefaultObjectMapper.get().readValue(input, TokenData.class);
        assertThat(token.getType()).isEqualTo("apikey");
        ApiKey tokenData = (ApiKey) token;
        assertThat(tokenData.getType()).isEqualTo("apikey");
        assertThat(tokenData.getApiKey()).isEqualTo("abc");
        assertThat(tokenData.getUserName()).isEqualTo("u");
        assertThat(tokenData.getCreatedAt()).isEqualTo(null);
    }

    @Test
    public void testBuilderDefaultApiKey() throws IOException {
        ApiKey oauth = ApiKey.builder()
                // no type!
                .apiKey("abc")
                .build();

        ApiKey output = DefaultObjectMapper.get().readValue(
                DefaultObjectMapper.get().writeValueAsString(oauth),
                ApiKey.class);

        assertThat(output.getApiKey()).isEqualTo("abc");
        assertThat(output.getType()).isEqualTo("apikey");
    }

    @Test
    public void testDeserializationDBAuth() throws IOException {
        String input = "{ \"type\" : \"dbauth\" , \"server\" : \"srv1\", \"username\" : \"uname\", \"password\" : \"pass1234\", \"database_name\" : \"dbname\" }";

        TokenData token = DefaultObjectMapper.get().readValue(input, TokenData.class);
        assertThat(token.getType()).isEqualTo("dbauth");
        DBAuth tokenData = (DBAuth) token;
        assertThat(tokenData.getType()).isEqualTo(DBAuth.TOKEN_TYPE);
        assertThat(tokenData.getServer()).isEqualTo("srv1");
        assertThat(tokenData.getUserName()).isEqualTo("uname");
        assertThat(tokenData.getPassword()).isEqualTo("pass1234");
        assertThat(tokenData.getDatabaseName()).isEqualTo("dbname");
    }

    @Test
    public void testBuilderDefaultDBAuth() throws IOException {
        DBAuth dbAuth = DBAuth.builder()
                .type(DBAuth.TOKEN_TYPE)
                .server("srv1")
                .userName("uname")
                .password("pass1234")
                .databaseName("dbname")
                .build();

        DBAuth output = DefaultObjectMapper.get().readValue(
                DefaultObjectMapper.get().writeValueAsString(dbAuth),
                DBAuth.class);
        assertEquals(output, dbAuth);
    }
}