package io.levelops.commons.inventory.oauth;

import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class InventoryOAuthJwtTokenProvider extends InventoryOAuthTokenProviderBase {
    private final BiFunction<String, IntegrationKey, String> jwtTokenMapper;
    private String jwtToken;

    @Builder
    public InventoryOAuthJwtTokenProvider(
            InventoryService inventoryService,
            IntegrationKey integrationKey,
            String tokenId,
            Token token,
            BiFunction<String, IntegrationKey, String> jwtTokenMapper) {
        super(inventoryService, integrationKey, tokenId, token);
        this.jwtTokenMapper = jwtTokenMapper;
        this.jwtToken = null;
    }

    @Override
    @Nullable
    public String getToken() {
        if (jwtToken != null) {
            return jwtToken;
        }
        Token oauthToken = super.getOAuthToken();
        if (oauthToken == null || oauthToken.getTokenData() == null) {
            return null;
        }
        var tokenData = oauthToken.getTokenData();
        var refreshToken = ((OauthToken) tokenData).getRefreshToken();
        jwtToken = jwtTokenMapper.apply(refreshToken, integrationKey);
        return jwtToken;
    }

    @Nullable
    @Override
    public String refreshToken() {
        Token token = super.refreshOAuthToken();
        if (token == null || token.getTokenData() == null) {
            return null;
        }
        var tokenData = token.getTokenData();
        var refreshToken = ((OauthToken) tokenData).getRefreshToken();
        jwtToken = jwtTokenMapper.apply(refreshToken, integrationKey);
        return jwtToken;
    }
}
