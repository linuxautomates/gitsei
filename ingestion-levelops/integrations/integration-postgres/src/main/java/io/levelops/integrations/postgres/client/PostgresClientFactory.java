package io.levelops.integrations.postgres.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.DBAuth;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Log4j2
public class PostgresClientFactory {
    private static final String JDBC_PATTERN = "jdbc:postgresql://%s/%s";

    private InventoryService inventoryService;
    private LoadingCache<IntegrationKey, PostgresClient> clientCache;

    @Builder
    public PostgresClientFactory(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
        this.clientCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getInternal));
    }

    public PostgresClient get(IntegrationKey integrationKey) throws PostgresClientException {
        try {
            return clientCache.get(integrationKey);
        } catch (ExecutionException e) {
            throw new PostgresClientException(e);
        }
    }

    private PostgresClient getInternal(IntegrationKey key) {
        Validate.notNull(key, "key cannot be null");
        try {
            List<Token> tokens = inventoryService.listTokens(key);

            JdbcTemplate template = InventoryHelper.handleTokens("Postgres", key, tokens,
                    InventoryHelper.TokenHandler.forType(DBAuth.TOKEN_TYPE, (Token token, DBAuth dbAuth) -> {
                        HikariConfig config = new HikariConfig();
                        config.setJdbcUrl(String.format(JDBC_PATTERN, dbAuth.getServer(), dbAuth.getDatabaseName()));
                        config.setUsername(dbAuth.getUserName());
                        config.setPassword(dbAuth.getPassword());
                        config.addDataSourceProperty("sslmode", "require");
                        HikariDataSource dataSource = new HikariDataSource(config);
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                        return jdbcTemplate;
                    }));
            return new PostgresClient(template);
        } catch (InventoryException e) {
            throw new RuntimeException(e); // for cache loader
        }
    }
}
