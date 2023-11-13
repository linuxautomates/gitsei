package io.levelops.internal_api.services;

import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.databases.models.database.IntegrationSecretMapping;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.AtlassianConnectJwtToken;
import io.levelops.commons.databases.models.database.tokens.DBAuth;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.models.database.tokens.TokenData;
import io.levelops.commons.databases.services.IntegrationSecretMappingsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.utils.MapUtils;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class IntegrationSecretsService {

    private static final long PROPAGATION_DELAY_MS = 250;
    private static boolean ENABLE_LINKED_CREDENTIALS_DELETION = false;
    private static final String OAUTH__ACCESS_TOKEN_FIELD = "access_token";
    private static final String OAUTH__REFRESH_TOKEN_FIELD = "refresh_token";
    private static final String OAUTH__BOT_TOKEN_FIELD = "bot_token";
    private static final String OAUTH__INSTANCE_URL_FIELD = "instance_url";
    private static final String OAUTH__REFRESHED_AT_FIELD = "refreshed_at";
    private static final String API_KEY__API_KEY_FIELD = "api_key";
    private static final String API_KEY__USER_NAME_FIELD = "user_name";
    private static final String DB__SERVER_FIELD = "server";
    private static final String DB__USER_NAME_FIELD = "user_name";
    private static final String DB__PASSWORD_FIELD = "password";
    private static final String DB__DATABASE_NAME_FIELD = "database_name";

    private static final String MULTIPLE_API_KEY__API_KEY_FIELD = "%d_api_key";
    private static final String MULTIPLE_API_KEY__USER_NAME_FIELD = "%d_user_name";

    private static final String ATLASSIAN_CONNECT_JWT__CLIENT_KEY_FIELD = "client_key";
    private static final String ATLASSIAN_CONNECT_JWT__NAME_FIELD = "name";

    private static final String SM_KEY_FORMAT = "integration_%s_field_%s"; // <integration id> <field name>
    private static final String DEFAULT_TOKEN_ID = "0";
    private final IntegrationSecretMappingsDatabaseService integrationSecretMappingsDatabaseService;
    private final SecretsManagerServiceClient secretsManagerServiceClient;
    private final AtlassianConnectServiceClient atlassianConnectServiceClient;
    private final IntegrationService integrationService;

    @Autowired
    public IntegrationSecretsService(IntegrationSecretMappingsDatabaseService integrationSecretMappingsDatabaseService,
                                     SecretsManagerServiceClient secretsManagerServiceClient,
                                     IntegrationService integrationService,
                                     AtlassianConnectServiceClient atlassianConnectServiceClient) {
        this.integrationSecretMappingsDatabaseService = integrationSecretMappingsDatabaseService;
        this.secretsManagerServiceClient = secretsManagerServiceClient;
        this.integrationService = integrationService;
        this.atlassianConnectServiceClient = atlassianConnectServiceClient;
    }

    /**
     * We introduced linked credentials where an integration can use credentials from another (the linked integration).
     * This method returns the effective integration that actually holds the credentials.
     */
    private Integration getEffectiveIntegration(String company, String integrationId) throws NotFoundException {
        Integration integration = integrationService.get(company, integrationId)
                .orElseThrow(() -> new NotFoundException("Could not find integration with id=" + integrationId + " for tenant=" + company));

        String linkedIntegrationId = integration.getLinkedCredentials();
        if (StringUtils.isBlank(linkedIntegrationId)) {
            return integration;
        }

        return integrationService.get(company, linkedIntegrationId)
                .orElseThrow(() -> new NotFoundException("Could not find linked integration with id=" + linkedIntegrationId + " from integration id=" + integrationId + " for tenant=" + company));
    }

    public void insert(String company, String smConfigId, String integrationId, Token token) throws SQLException, SecretsManagerServiceClientException, NotFoundException, BadRequestException {
        Integration integration = getEffectiveIntegration(company, integrationId);
        log.info("Storing integration secrets for company={}, integrationId={} (effective={}), smConfigId={}", company, integrationId, integration.getId(), smConfigId);
        // Phase2: This will have to change because each field could have a different config (new vs existing secret, different SMs)
        Map<String, String> secretFields = flattenTokenIntoFields(integration.getAuthentication(), token);
        TreeSet<String> keys = new TreeSet<>(secretFields.keySet());
        for (var key : keys) {
            var value = secretFields.get(key);
            if (StringUtils.isAnyBlank(key, value)) {
                // we don't need to store blank secrets
                continue;
            }
            writeIntegrationSecret(company, smConfigId, integration.getId(), key, value);
        }
    }

    public void update(String company, String smConfigId, String integrationId, Token token) throws SQLException, SecretsManagerServiceClientException, NotFoundException, BadRequestException {
        Integration integration = getEffectiveIntegration(company, integrationId);
        log.info("Updating integration secrets for company={}, integrationId={} (effective={}), smConfigId={}", company, integrationId, integration.getId(), smConfigId);
        // 1- delete
        deleteAllIntegrationSecrets(company, integration.getId());
        // 2- allow for data propagation in secrets-mgr
        try {
            Thread.sleep(PROPAGATION_DELAY_MS);
        } catch (InterruptedException e) {
            // we do not want to abort this task because of interrupt
        }
        // 3- re-create
        insert(company, smConfigId, integration.getId(), token);
    }

    public Optional<Token> get(String company, String smConfigId, String integrationId) throws SQLException, SecretsManagerServiceClientException, NotFoundException, AtlassianConnectServiceClientException {
        Integration integration = getEffectiveIntegration(company, integrationId);
        log.debug("Reading integration secrets for company={}, integrationId={} (effective={}), smConfigId={}", company, integrationId, integration.getId(), smConfigId);
        Map<String, String> fields = readIntegrationSecrets(company, smConfigId, integration.getId());
        return Optional.ofNullable(expandFieldsIntoToken(integrationId, integration.getAuthentication(), fields));
    }

    public void delete(String company, String integrationId) throws NotFoundException {
        Integration integration = getEffectiveIntegration(company, integrationId);
        if (!ENABLE_LINKED_CREDENTIALS_DELETION && !integrationId.equals(integration.getId())) {
            return;
        }

        // delete all mappings and selectively clean secrets
        deleteAllIntegrationSecrets(company, integration.getId());
    }

    public void copy(String company, String smConfigId, String sourceIntegrationId, String destinationIntegrationId) throws SQLException, SecretsManagerServiceClientException, NotFoundException, BadRequestException, AtlassianConnectServiceClientException {
        log.debug("Copying integration secrets for company={}, smConfigId={}, from integrationId={} to {}", company, smConfigId, sourceIntegrationId, destinationIntegrationId);
        Optional<Token> token = get(company, smConfigId, sourceIntegrationId);
        if (token.isEmpty()) {
            log.debug("company={}, smConfigId={}, integrationId={}: nothing to copy over to {}", company, smConfigId, sourceIntegrationId, destinationIntegrationId);
            return;
        }
        update(company, smConfigId, destinationIntegrationId, token.get());
    }

    // region Token parsing

    @Nonnull
    protected static Map<String, String> flattenTokenIntoFields(Authentication authentication, Token token) throws BadRequestException {
        if (token == null || token.getTokenData() == null || token.getTokenData().getType() == null) {
            return Collections.emptyMap();
        }
        // NB: this could be customized on an app basis if needed
        Map<String, String> fields = new HashMap<>();
        authentication = ObjectUtils.firstNonNull(authentication, Authentication.UNKNOWN);
        switch (authentication) {
            case UNKNOWN:
            case NONE:
                log.info("Not storing token for integration with authentication set to '{}'", authentication);
                return Collections.emptyMap();
            case OAUTH:
                validateTokenDataType(token, authentication, OauthToken.TOKEN_TYPE);
                OauthToken oauthToken = (OauthToken) token.getTokenData();
                fields = MapUtils.append(fields, OAUTH__ACCESS_TOKEN_FIELD, oauthToken.getToken());
                fields = MapUtils.append(fields, OAUTH__REFRESH_TOKEN_FIELD, oauthToken.getRefreshToken());
                fields = MapUtils.append(fields, OAUTH__BOT_TOKEN_FIELD, oauthToken.getBotToken());
                fields = MapUtils.append(fields, OAUTH__INSTANCE_URL_FIELD, oauthToken.getInstanceUrl());
                fields = MapUtils.append(fields, OAUTH__REFRESHED_AT_FIELD, String.valueOf(oauthToken.getRefreshedAt()));
                return fields;
            case API_KEY:
                validateTokenDataType(token, authentication, ApiKey.TOKEN_TYPE);
                ApiKey apiKey = (ApiKey) token.getTokenData();
                fields = MapUtils.append(fields, API_KEY__API_KEY_FIELD, apiKey.getApiKey());
                fields = MapUtils.append(fields, API_KEY__USER_NAME_FIELD, apiKey.getUserName());
                return fields;
            case MULTIPLE_API_KEYS:
                validateTokenDataType(token, authentication, MultipleApiKeys.TOKEN_TYPE);
                MultipleApiKeys multipleApiKey = (MultipleApiKeys) token.getTokenData();
                for (int i = 0; i < multipleApiKey.getKeys().size(); i++) {
                    fields = MapUtils.append(fields, String.format(MULTIPLE_API_KEY__API_KEY_FIELD, i), multipleApiKey.getKeys().get(i).getApiKey());
                    fields = MapUtils.append(fields, String.format(MULTIPLE_API_KEY__USER_NAME_FIELD, i), multipleApiKey.getKeys().get(i).getUserName());
                }
                return fields;
            case DB:
                validateTokenDataType(token, authentication, DBAuth.TOKEN_TYPE);
                DBAuth dbAuth = (DBAuth) token.getTokenData();
                fields = MapUtils.append(fields, DB__SERVER_FIELD, dbAuth.getServer());
                fields = MapUtils.append(fields, DB__USER_NAME_FIELD, dbAuth.getUserName());
                fields = MapUtils.append(fields, DB__PASSWORD_FIELD, dbAuth.getPassword());
                fields = MapUtils.append(fields, DB__DATABASE_NAME_FIELD, dbAuth.getDatabaseName());
                return fields;
            case ATLASSIAN_CONNECT_JWT:
                validateTokenDataType(token, authentication, AtlassianConnectJwtToken.TOKEN_TYPE);
                AtlassianConnectJwtToken atlassianConnectJwtToken = (AtlassianConnectJwtToken) token.getTokenData();
                // We only store the client key in the secret manager. The rest comes from the global atlassian connect secrets
                // and metadata
                fields = MapUtils.append(fields, ATLASSIAN_CONNECT_JWT__CLIENT_KEY_FIELD, atlassianConnectJwtToken.getClientKey());
                fields = MapUtils.append(fields, ATLASSIAN_CONNECT_JWT__NAME_FIELD, atlassianConnectJwtToken.getName());
                return fields;
            default:
                throw new UnsupportedOperationException("Authentication method not supported: " + authentication);
        }
    }

    private static void validateTokenDataType(Token token, Authentication authentication, String tokenType) throws BadRequestException {
        if (!tokenType.equalsIgnoreCase(token.getTokenData().getType())) {
            throw new BadRequestException("Expected token of type=" + authentication + " ( " + tokenType + ") but got: " + token.getTokenData().getType());
        }
    }

    @Nullable
    private Token expandFieldsIntoToken(String integrationId, Authentication authentication, Map<String, String> fields) throws AtlassianConnectServiceClientException {
        if (MapUtils.isEmpty(fields)) {
            return null;
        }
        TokenData tokenData = parseTokenData(integrationId, authentication, fields, atlassianConnectServiceClient);
        if (tokenData == null) {
            return null;
        }
        return Token.builder()
                .id(DEFAULT_TOKEN_ID)
                .integrationId(integrationId)
                .createdAt(null) // TODO do we need this?
                .tokenData(tokenData)
                .build();
    }

    @Nullable
    protected static TokenData parseTokenData(
            String integrationId,
            Authentication authentication,
            Map<String, String> fields,
            AtlassianConnectServiceClient atlassianConnectServiceClient) throws AtlassianConnectServiceClientException {
        authentication = ObjectUtils.firstNonNull(authentication, Authentication.UNKNOWN);
        switch (authentication) {
            case UNKNOWN:
            case NONE:
                return null;
            case OAUTH:
                String refreshedAtString = fields.get(OAUTH__REFRESHED_AT_FIELD);
                Long refreshedAt = null;
                try {
                    refreshedAt = (refreshedAtString == null || refreshedAtString.equalsIgnoreCase("null") || refreshedAtString.equalsIgnoreCase("none")) ? null : Long.parseLong(refreshedAtString);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse refreshed_at for integration_id={}... ignoring.", integrationId, e);
                }
                return OauthToken.builder()
                        .token(fields.get(OAUTH__ACCESS_TOKEN_FIELD))
                        .refreshToken(fields.get(OAUTH__REFRESH_TOKEN_FIELD))
                        .botToken(fields.get(OAUTH__BOT_TOKEN_FIELD))
                        .instanceUrl(fields.get(OAUTH__INSTANCE_URL_FIELD))
                        .refreshedAt(refreshedAt)
                        .build();
            case API_KEY:
                return ApiKey.builder()
                        .apiKey(fields.get(API_KEY__API_KEY_FIELD))
                        .userName(fields.get(API_KEY__USER_NAME_FIELD))
                        .build();
            case MULTIPLE_API_KEYS:
                List<MultipleApiKeys.Key> keys = new ArrayList<>();
                int i = 0;
                boolean fetch = true;
                while (fetch) {
                    String apiKey = fields.get(String.format(MULTIPLE_API_KEY__API_KEY_FIELD, i));
                    String userName = fields.get(String.format(MULTIPLE_API_KEY__USER_NAME_FIELD, i));
                    if (StringUtils.isEmpty(apiKey) && StringUtils.isEmpty(userName)) {
                        fetch = false;
                    } else {
                        keys.add(MultipleApiKeys.Key.builder().userName(userName).apiKey(apiKey).build());
                        i++;
                    }
                }
                return MultipleApiKeys.builder()
                        .keys(keys)
                        .build();
            case DB:
                return DBAuth.builder()
                        .server(fields.get(DB__SERVER_FIELD))
                        .userName(fields.get(DB__USER_NAME_FIELD))
                        .password(fields.get(DB__PASSWORD_FIELD))
                        .databaseName(fields.get(DB__DATABASE_NAME_FIELD))
                        .build();
            case ATLASSIAN_CONNECT_JWT:
                String clientKey = fields.get(ATLASSIAN_CONNECT_JWT__CLIENT_KEY_FIELD);
                String name = fields.get(ATLASSIAN_CONNECT_JWT__NAME_FIELD);
                String sharedSecret = atlassianConnectServiceClient.getSecret(clientKey);
                AtlassianConnectAppMetadata metadata = atlassianConnectServiceClient.getMetadata(clientKey);
                return AtlassianConnectJwtToken.builder()
                        .clientKey(fields.get(ATLASSIAN_CONNECT_JWT__CLIENT_KEY_FIELD))
                        .sharedSecret(sharedSecret)
                        .appKey(metadata.getInstalledAppKey())
                        .name(name)
                        .baseUrl(metadata.getAtlassianBaseUrl())
                        .build();
            default:
                throw new UnsupportedOperationException("Authentication method not supported: " + authentication);
        }
    }
    //endregion

    // region secrets manager methods

    /**
     * Writes one field to the SM and persists the corresponding integration-secrets mapping.
     */
    private void writeIntegrationSecret(String company, String smConfigId, String integrationId, @NotBlank String key, @NotBlank String value) throws SQLException, SecretsManagerServiceClientException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(smConfigId, "smConfigId cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(key, "key cannot be null or empty.");
        Validate.notBlank(value, "value cannot be null or empty.");

        String smKey = generateIntegrationSecretKey(integrationId, key);

        IntegrationSecretMapping mapping = IntegrationSecretMapping.builder()
                .integrationId(integrationId)
                .smConfigId(smConfigId)
                .name(key)
                .smKey(smKey)
                .build();

        secretsManagerServiceClient.storeKeyValue(company, smConfigId, SecretsManagerServiceClient.KeyValue.builder()
                .key(smKey)
                .value(value)
                .build());

        integrationSecretMappingsDatabaseService.insert(company, mapping);
    }

    /**
     * Reads ALL the secrets for a given integration id.
     *
     * @return Map of field names to secret values
     */
    private Map<String, String> readIntegrationSecrets(String company, String smConfigId, String integrationId) throws SQLException, SecretsManagerServiceClientException {
        return streamIntegrationSecretMappings(company, smConfigId, integrationId)
                .map(mapping -> {
                    SecretsManagerServiceClient.KeyValue kv;
                    try {
                        kv = secretsManagerServiceClient.getKeyValue(company, mapping.getSmConfigId(), mapping.getSmKey());
                        return Map.entry(mapping.getName(), kv.getValue());
                    } catch (SecretsManagerServiceClientException e) {
                        log.warn("Failed to retrieve secret for mapping: {}", mapping, e);
                        return null;
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        // in case of duplication (shouldn't happen unless failed to delete),
                        // pick the first one because the stream is ordered by DESC updated time
                        (a, b) -> a));
    }

    /**
     * Deletes **ALL** secrets mappings for a given integration id, and selectively cleans up secrets.
     */
    private void deleteAllIntegrationSecrets(String company, String integrationId) {
        List<IntegrationSecretMapping> mappings = streamIntegrationSecretMappings(company, null, integrationId).collect(Collectors.toList());
        if (mappings.isEmpty()) {
            log.debug("Deleting 0 secrets from company={}, integrationId={}", company, integrationId);
            return;
        }
        log.info("Deleting {} secrets from company={}, integrationId={}", mappings.size(), company, integrationId);
        MutableInt nbDeletedSecretsFailures = new MutableInt(0);
        MutableInt nbDeletedMappingsFailures = new MutableInt(0);
        mappings.forEach(mapping -> {
            if (SecretsManagerServiceClient.DEFAULT_CONFIG_ID.equals(mapping.getSmConfigId())) {
                // ONLY DELETE SECRETS FROM DEFAULT SM
                // Phase2: When we support external secret managers, we may want to
                //         give an option to delete newly stored secrets, but we must
                //         not delete pre-existing secrets...
                try {
                    secretsManagerServiceClient.deleteKeyValue(company, mapping.getSmConfigId(), mapping.getSmKey());
                } catch (SecretsManagerServiceClientException e) {
                    log.warn("Failed to delete secret from SM for company={}: {}", mapping, e);
                    nbDeletedSecretsFailures.increment();
                }
            }
            try {
                integrationSecretMappingsDatabaseService.delete(company, mapping.getId());
            } catch (SQLException e) {
                log.warn("Failed to delete secret mapping for company={}: {}", mapping, e);
                nbDeletedMappingsFailures.increment();
            }
        });
        log.info("Done deleting {} secrets from company={}, integrationId={}" + (nbDeletedSecretsFailures.intValue() + nbDeletedMappingsFailures.intValue() > 0 ? "with {} secret deletion failures and {} mapping deletion failures" : ""), mappings.size(), company, integrationId, nbDeletedSecretsFailures.getValue(), nbDeletedMappingsFailures.getValue());
    }

    private Stream<IntegrationSecretMapping> streamIntegrationSecretMappings(String company, @Nullable String smConfigId, String integrationId) {
        var filter = IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId(integrationId)
                .smConfigId(smConfigId)
                .build();
        return integrationSecretMappingsDatabaseService.stream(company, filter);
    }

    private static String generateIntegrationSecretKey(String integrationId, String key) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notBlank(key, "key cannot be null or empty.");
        return String.format(SM_KEY_FORMAT, integrationId, key);
    }

    // endregion

}
