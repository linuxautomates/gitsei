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
abstract public class InventoryOAuthTokenProviderBase implements OauthTokenProvider {
        private final InventoryService inventoryService;
        protected final IntegrationKey integrationKey;
        private final String tokenId;
        private Token token;

        public InventoryOAuthTokenProviderBase(InventoryService inventoryService, IntegrationKey integrationKey,
                                           String tokenId, Token token) {
            this.inventoryService = inventoryService;
            this.integrationKey = integrationKey;
            this.tokenId = tokenId;
            this.token = token;
        }

        @Nullable
        public Token getOAuthToken() {
            Token oauthToken;
            if (this.token != null && this.token.getTokenData() != null) {
                oauthToken = token;
            } else {
                try {
                    oauthToken = inventoryService.getToken(integrationKey.getTenantId(), integrationKey.getIntegrationId(), tokenId);
                } catch (InventoryException e) {
                    log.warn("Could not get token for {}, tokenId={}", integrationKey, tokenId);
                    return null;
                }
            }
            if (oauthToken == null || oauthToken.getTokenData() == null) {
                log.warn("Token not found or missing token data for {}, tokenId={}", integrationKey, tokenId);
                return null;
            }
            var tokenData = oauthToken.getTokenData();
            if (!(tokenData instanceof OauthToken)) {
                log.warn("Token type not supported for OAuth: {}", tokenData.getType());
                return null;
            }

            this.token = oauthToken;
            return oauthToken;
        }

        @Nullable
        public Token refreshOAuthToken() {
            Token token;
            try {
                token = inventoryService.refreshToken(integrationKey.getTenantId(), integrationKey.getIntegrationId(), tokenId);
            } catch (InventoryException e) {
                log.warn("Could not refresh token for {}, tokenId={}", integrationKey, tokenId);
                return null;
            }
            if (token == null || token.getTokenData() == null) {
                log.warn("Token not found or missing token data after refreshing for {}, tokenId={}", integrationKey, tokenId);
                return null;
            }
            var tokenData = token.getTokenData();
            if (!(tokenData instanceof OauthToken)) {
                log.warn("Token type not supported for OAuth: {}", tokenData.getType());
                return null;
            }
            this.token = token;
            return token;
        }
}
