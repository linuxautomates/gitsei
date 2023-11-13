package io.levelops.commons.inventory.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deserializeGithub() throws IOException {
        Integration integration = mapper.readValue(ResourceUtils.getResourceAsString("json/integration_github.json"), Integration.class);

        assertThat(integration.getId()).isEqualTo("uuid1");
        assertThat(integration.getName()).isEqualTo("github1");
        assertThat(integration.getApplication()).isEqualTo("github");
        assertThat(integration.getStatus()).isEqualTo("not_verified");
        assertThat(integration.getUrl()).isEqualTo("https://api.github.com");
    }

    @Test
    public void deserializeTokens() throws IOException {
        Token token = mapper.readValue(ResourceUtils.getResourceAsString("json/token.json"), Token.class);

        assertThat(token.getId()).isEqualTo("t");
        assertThat(token.getIntegrationId()).isEqualTo("test1");
        assertThat(token.getTokenData()).isInstanceOf(OauthToken.class);

        OauthToken tokendata = (OauthToken) token.getTokenData();
        assertThat(tokendata.getToken()).isEqualTo("aksjdnkjnasd");
        assertThat(tokendata.getRefreshToken()).isEqualTo("aksjdnkasjdnk");
    }

    @Test(expected = InvalidTypeIdException.class)
    public void deserializeUnmapped() throws IOException {
        String input = "{\"id\":\"t\",\"integration_id\":\"test1\",\"token_data\":" +
                "{\"type\":\"kakakakakaka\",\"token\":\"aksjdnkjnasd\",\"refresh_token\":\"aksjdnkasjdnk\"," +
                "\"refreshed_at\":\"29382828\", \"created_at\":\"293929282\"}}";
        Token integration = mapper.readValue(input, Token.class);
    }
}