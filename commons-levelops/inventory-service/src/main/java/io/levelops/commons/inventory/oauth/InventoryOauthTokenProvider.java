package io.levelops.commons.inventory.oauth;


import io.levelops.commons.client.oauth.OauthTokenProvider;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

@Log4j2
public class InventoryOauthTokenProvider extends InventoryOAuthTokenProviderBase {
    @Builder
    public InventoryOauthTokenProvider(InventoryService inventoryService, IntegrationKey integrationKey,
                                       String tokenId, Token token) {
        super(inventoryService, integrationKey, tokenId, token);
    }

    @Override
    @Nullable
    public String getToken() {
        Token oauthToken = super.getOAuthToken();
        if (oauthToken == null || oauthToken.getTokenData() == null) {
            return null;
        }
        var tokenData = oauthToken.getTokenData();
        return ((OauthToken) tokenData).getToken();
    }

    @Nullable
    @Override
    public String refreshToken() {
        Token token = super.refreshOAuthToken();
        if (token == null || token.getTokenData() == null) {
            return null;
        }
        var tokenData = token.getTokenData();
        return ((OauthToken) tokenData).getToken();
    }
}
