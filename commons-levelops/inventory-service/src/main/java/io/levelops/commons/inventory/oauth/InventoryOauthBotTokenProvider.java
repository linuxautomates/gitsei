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
public class InventoryOauthBotTokenProvider implements OauthTokenProvider {
    private final InventoryService inventoryService;
    private final IntegrationKey integrationKey;
    private final String tokenId;

    @Builder
    public InventoryOauthBotTokenProvider(InventoryService inventoryService, IntegrationKey integrationKey, String tokenId) {
        this.inventoryService = inventoryService;
        this.integrationKey = integrationKey;
        this.tokenId = tokenId;
    }

    @Override
    @Nullable
    public String getToken() {
        Token token;
        try {
            token = inventoryService.getToken(integrationKey.getTenantId(), integrationKey.getIntegrationId(), tokenId);
        } catch (InventoryException e) {
            log.warn("Could not get token for {}, tokenId={}", integrationKey, tokenId);
            return null;
        }
        if (token == null || token.getTokenData() == null) {
            log.warn("Token not found or missing token data for {}, tokenId={}", integrationKey, tokenId);
            return null;
        }
        var tokenData = token.getTokenData();
        if (!(tokenData instanceof OauthToken)) {
            log.warn("Token type not supported for OAuth: {}", tokenData.getType());
            return null;
        }

        return ((OauthToken) tokenData).getBotToken();
    }

    @Nullable
    @Override
    public String refreshToken() {
        throw new UnsupportedOperationException("Refresh unsupported for bot token");
    }
}
