package io.levelops.commons.inventory.utils;

import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper.TokenHandler;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryHelperTest {

    private final Token abcToken = new Token("1", "2", OauthToken.builder().token("abc").build(), null);
    private final Token defApiKey = new Token("2", "2", ApiKey.builder().apiKey("def").build(), null);

    @Test
    public void testInventoryHelper() throws InventoryException {
        String token = InventoryHelper.handleTokens("GitHub", new IntegrationKey("coke", "123"),
                List.of(abcToken),
                TokenHandler.forType(OauthToken.TOKEN_TYPE, (Token base, OauthToken t) -> t.getToken()),
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token base, ApiKey t) -> t.getApiKey()));
        assertThat(token).isEqualTo("abc");

        token = InventoryHelper.handleTokens("GitHub", new IntegrationKey("coke", "123"),
                List.of(defApiKey),
                TokenHandler.forType(OauthToken.TOKEN_TYPE, (Token base, OauthToken t) -> t.getToken()),
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token base, ApiKey t) -> t.getApiKey()));
        assertThat(token).isEqualTo("def");

        token = InventoryHelper.handleTokens("GitHub", new IntegrationKey("coke", "123"),
                List.of(abcToken, defApiKey),
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token base, ApiKey t) -> t.getApiKey()));
        assertThat(token).isEqualTo("def");

        token = InventoryHelper.handleTokens("GitHub", new IntegrationKey("coke", "123"),
                List.of(defApiKey, abcToken),
                TokenHandler.forType(OauthToken.TOKEN_TYPE, (Token base, OauthToken t) -> t.getToken()));
        assertThat(token).isEqualTo("abc");
    }

    @Test(expected = InventoryException.class)
    public void testUnsupported() throws InventoryException {
        InventoryHelper.<String>handleTokens("GitHub", new IntegrationKey("coke", "123"),
                List.of(abcToken),
                TokenHandler.forType(ApiKey.TOKEN_TYPE, (Token base, ApiKey t) -> t.getApiKey()));
    }

    @Test
    public void testInventoryHelperName() throws InventoryException {
        Token nameToken = new Token("3", "2", OauthToken.builder().token("x").name("bot_token").build(), null);
        String token = InventoryHelper.handleTokens("Slack", new IntegrationKey("coke", "123"),
                List.of(abcToken, defApiKey, nameToken),
                TokenHandler.forTypeAndName(OauthToken.TOKEN_TYPE, "bot_token", (Token base, OauthToken t) -> t.getToken()));
        assertThat(token).isEqualTo("x");
    }

    @Test(expected = InventoryException.class)
    public void testInventoryHelperNameUnsupported() throws InventoryException {
        Token nameToken = new Token("3", "2", OauthToken.builder().token("x").name("bot_token").build(), null);
        String token = InventoryHelper.handleTokens("Slack", new IntegrationKey("coke", "123"),
                List.of(abcToken, defApiKey, nameToken),
                TokenHandler.forTypeAndName(OauthToken.TOKEN_TYPE, "won't find", (Token base, OauthToken t) -> t.getToken()));
        assertThat(token).isEqualTo("x");
    }
}