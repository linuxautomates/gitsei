package io.levelops.commons.inventory.utils;

import com.google.common.base.Suppliers;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.TokenData;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Log4j2
public class InventoryHelper {

    /**
     * Attempts to apply one of the handlers to the first token that is supported (based on a map of token types to handlers).
     * Example:
     * {@code
     * output = InventoryHelper.handleTokens("MyIntegrationName (for logging purposes)", integrationKey, tokensFromInventoryService, Map.of(
     * OauthToken.TOKEN_TYPE, (Token t, OauthToken oauth) -> do something with oauth token ... ),
     * ApiKey.TOKEN_TYPE, (Token t, ApiKey apiKey) -> do something with api key ... ));
     * }
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> T handleTokens(String integrationName,
                                     IntegrationKey key,
                                     List<Token> tokens,
                                     TokenHandler<T, ? extends TokenData>... handlers) throws InventoryException {
        if (CollectionUtils.isEmpty(tokens)) {
            throw new InventoryException("No token found for " + key);
        }

        for (Token token : tokens) {
            TokenData tokenData = token.getTokenData();
            if (tokenData == null) {
                continue;
            }
            for (TokenHandler<T, ? extends TokenData> tokenHandler : handlers) {
                if (tokenHandler.accepts(token)) {
                    //noinspection unchecked
                    return ((TokenHandler<T, TokenData>) tokenHandler).handle(token, tokenData);
                }
            }
        }

        throw new InventoryException(String.format("%s: Could not find supported token for %s", integrationName, key));
    }

    private static <T> BiFunction<Token, TokenData, T> castToTokenDataFunction(BiFunction<Token, ? extends TokenData, T> f) {
        //noinspection unchecked
        return (BiFunction<Token, TokenData, T>) f;
    }

    public interface TokenHandler<T, TD extends TokenData> {

        boolean accepts(Token token);

        T handle(Token token, TD tokenData);

        static <T, TD extends TokenData> TokenHandler<T, TD> forType(String tokenType, BiFunction<Token, TD, T> handler) {
            Validate.notBlank(tokenType, "tokenType cannot be null or empty.");
            return new TokenHandler<>() {
                @Override
                public boolean accepts(Token token) {
                    return tokenType.equalsIgnoreCase(token.getTokenData().getType());
                }

                @Override
                public T handle(Token token, TD tokenData) {
                    return handler.apply(token, tokenData);
                }
            };
        }

        static <T, TD extends TokenData> TokenHandler<T, TD> forTypeAndName(String tokenType, String tokenName, BiFunction<Token, TD, T> handler) {
            Validate.notBlank(tokenType, "tokenType cannot be null or empty.");
            Validate.notBlank(tokenName, "tokenName cannot be null or empty.");
            return new TokenHandler<>() {
                @Override
                public boolean accepts(Token token) {
                    return tokenType.equalsIgnoreCase(token.getTokenData().getType())
                            && tokenName.equalsIgnoreCase(token.getTokenData().getName());
                }

                @Override
                public T handle(Token token, TD tokenData) {
                    return handler.apply(token, tokenData);
                }
            };
        }
    }

    public static Supplier<String> integrationUrlSupplier(InventoryService inventoryService, IntegrationKey key, long duration, TimeUnit unit) {
        return Suppliers.memoizeWithExpiration(() -> {
            try {
                return inventoryService.getIntegration(key).getUrl();
            } catch (InventoryException e) {
                log.warn("Failed to get integration url for key= " + key + " and exception :  ", e);
                return null;
            }
        } , duration, unit);
    }

    public static Supplier<Map<String, Object>> integrationMetadataSupplier(InventoryService inventoryService,
                                                                            IntegrationKey key,
                                                                            long duration,
                                                                            TimeUnit unit) {
        return Suppliers.memoizeWithExpiration(() -> {
            try {
                return inventoryService.getIntegration(key).getMetadata();
            } catch (InventoryException e) {
                log.warn("Failed to get integration metadata for key= " +  key + " and exception : ", e);
                return null;
            }
        } , duration, unit);
    }

    public static Optional<String> extractTokenType(Token token) {
        return Optional.ofNullable(token)
                .map(Token::getTokenData)
                .map(TokenData::getType);
    }
}
