package io.levelops.internal_api.services;

import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationSecretMapping;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.AtlassianConnectJwtToken;
import io.levelops.commons.databases.models.database.tokens.DBAuth;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.services.IntegrationSecretMappingsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IntegrationSecretsServiceTest {

    @Mock
    private IntegrationSecretMappingsDatabaseService integrationSecretMappingsDatabaseService;
    @Mock
    private SecretsManagerServiceClient secretsManagerServiceClient;
    @Mock
    private IntegrationService integrationService;

    @Mock
    private AtlassianConnectServiceClient atlassianConnectServiceClient;

    private IntegrationSecretsService integrationSecretsService;
    private static final String COMPANY = "foo";

    private static final IntegrationSecretMapping EXPECTED_MAPPING_API_KEY = IntegrationSecretMapping.builder()
            .id("001")
            .integrationId("123")
            .name("api_key")
            .smConfigId("0")
            .smKey("integration_123_field_api_key")
            .build();
    private static final IntegrationSecretMapping EXPECTED_MAPPING_USER_NAME = IntegrationSecretMapping.builder()
            .id("002")
            .integrationId("123")
            .name("user_name")
            .smConfigId("0")
            .smKey("integration_123_field_user_name")
            .build();
    private static final IntegrationSecretMapping EXPECTED_MAPPING_MULTIPLE_API_KEY_0 = IntegrationSecretMapping.builder()
            .id("004")
            .integrationId("124")
            .name("0_api_key")
            .smConfigId("0")
            .smKey("integration_124_field_0_api_key")
            .build();
    private static final IntegrationSecretMapping EXPECTED_MAPPING_MULTIPLE_API_USERNAME_0 = IntegrationSecretMapping.builder()
            .id("005")
            .integrationId("124")
            .name("0_user_name")
            .smConfigId("0")
            .smKey("integration_124_field_0_user_name")
            .build();
    private static final IntegrationSecretMapping EXPECTED_MAPPING_MULTIPLE_API_KEY_1 = IntegrationSecretMapping.builder()
            .id("006")
            .integrationId("124")
            .name("1_api_key")
            .smConfigId("0")
            .smKey("integration_124_field_1_api_key")
            .build();
    private static final IntegrationSecretMapping EXPECTED_MAPPING_MULTIPLE_API_USERNAME_1 = IntegrationSecretMapping.builder()
            .id("007")
            .integrationId("124")
            .name("1_user_name")
            .smConfigId("0")
            .smKey("integration_124_field_1_user_name")
            .build();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        integrationSecretsService = new IntegrationSecretsService(integrationSecretMappingsDatabaseService, secretsManagerServiceClient, integrationService, atlassianConnectServiceClient);

        when(integrationService.get(eq(COMPANY), eq("123"))).thenReturn(Optional.of(Integration.builder()
                .id("123")
                .authentication(Integration.Authentication.API_KEY)
                .build()));

        when(integrationSecretMappingsDatabaseService.stream(eq(COMPANY), eq(IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId("123")
                .smConfigId("0")
                .build()))).thenAnswer(invocation -> Stream.of(EXPECTED_MAPPING_API_KEY, EXPECTED_MAPPING_USER_NAME));
        when(integrationSecretMappingsDatabaseService.stream(eq(COMPANY), eq(IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId("123")
                .build())))
                .thenReturn(Stream.of(EXPECTED_MAPPING_API_KEY, EXPECTED_MAPPING_USER_NAME,
                        IntegrationSecretMapping.builder()
                                .id("003")
                                .integrationId("123")
                                .name("test")
                                .smConfigId("1")
                                .smKey("integration_123_field_test")
                                .build()));

        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_123_field_api_key"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_123_field_api_key")
                .value("apikey-secret")
                .build());
        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_123_field_user_name"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_123_field_user_name")
                .value("username-secret")
                .build());


        when(integrationService.get(eq(COMPANY), eq("124"))).thenReturn(Optional.of(Integration.builder()
                .id("124")
                .authentication(Integration.Authentication.MULTIPLE_API_KEYS)
                .build()));

        when(integrationSecretMappingsDatabaseService.stream(eq(COMPANY), eq(IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId("124")
                .smConfigId("0")
                .build()))).thenReturn(Stream.of(EXPECTED_MAPPING_MULTIPLE_API_KEY_0, EXPECTED_MAPPING_MULTIPLE_API_USERNAME_0, EXPECTED_MAPPING_MULTIPLE_API_KEY_1, EXPECTED_MAPPING_MULTIPLE_API_USERNAME_1));

        when(integrationSecretMappingsDatabaseService.stream(eq(COMPANY), eq(IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId("124")
                .build())))
                .thenReturn(Stream.of(EXPECTED_MAPPING_MULTIPLE_API_KEY_0, EXPECTED_MAPPING_MULTIPLE_API_USERNAME_0, EXPECTED_MAPPING_MULTIPLE_API_KEY_1, EXPECTED_MAPPING_MULTIPLE_API_USERNAME_1));

        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_api_key"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_124_field_0_api_key")
                .value("apikey-0")
                .build());
        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_user_name"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_124_field_0_user_name")
                .value("username-0")
                .build());
        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_1_api_key"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_124_field_1_api_key")
                .value("apikey-1")
                .build());
        when(secretsManagerServiceClient.getKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_1_user_name"))).thenReturn(SecretsManagerServiceClient.KeyValue.builder()
                .key("integration_124_field_1_user_name")
                .value("username-1")
                .build());

        when(integrationService.get(eq(COMPANY), eq("125"))).thenReturn(Optional.of(Integration.builder()
                .id("125")
                .authentication(Integration.Authentication.API_KEY)
                .linkedCredentials("123")
                .build()));
    }

    @Test
    public void insert() throws SecretsManagerServiceClientException, SQLException, BadRequestException, NotFoundException {
        integrationSecretsService.insert(COMPANY, "0", "123", Token.builder()
                .integrationId("123")
                .tokenData(ApiKey.builder()
                        .apiKey("abc")
                        .userName("maxime")
                        .name("test")
                        .build())
                .build());

        var keyValueArgumentCaptor = ArgumentCaptor.forClass(SecretsManagerServiceClient.KeyValue.class);
        verify(secretsManagerServiceClient, times(2)).storeKeyValue(eq(COMPANY), eq("0"), keyValueArgumentCaptor.capture());

        assertThat(keyValueArgumentCaptor.getAllValues().get(0).getKey()).isEqualTo("integration_123_field_api_key");
        assertThat(keyValueArgumentCaptor.getAllValues().get(0).getValue()).isEqualTo("abc");
        assertThat(keyValueArgumentCaptor.getAllValues().get(1).getKey()).isEqualTo("integration_123_field_user_name");
        assertThat(keyValueArgumentCaptor.getAllValues().get(1).getValue()).isEqualTo("maxime");
        assertThat(keyValueArgumentCaptor.getAllValues().size()).isEqualTo(2);

        var integrationSecretMappingArgumentCaptor = ArgumentCaptor.forClass(IntegrationSecretMapping.class);
        verify(integrationSecretMappingsDatabaseService, times(2)).insert(eq(COMPANY), integrationSecretMappingArgumentCaptor.capture());

        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(0)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_API_KEY, "id");
        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(1)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_USER_NAME, "id");
    }

    @Test
    public void insert2() throws SecretsManagerServiceClientException, SQLException, BadRequestException, NotFoundException {
        integrationSecretsService.insert(COMPANY, "0", "124", Token.builder()
                .integrationId("124")
                .tokenData(MultipleApiKeys.builder()
                        .keys(List.of(
                                        MultipleApiKeys.Key.builder().userName("username-0").apiKey("apikey-0").build(),
                                        MultipleApiKeys.Key.builder().userName("username-1").apiKey("apikey-1").build()
                                )
                        )
                        .build())
                .build());


        var keyValueArgumentCaptor = ArgumentCaptor.forClass(SecretsManagerServiceClient.KeyValue.class);
        verify(secretsManagerServiceClient, times(4)).storeKeyValue(eq(COMPANY), eq("0"), keyValueArgumentCaptor.capture());

        assertThat(keyValueArgumentCaptor.getAllValues().size()).isEqualTo(4);

        assertThat(keyValueArgumentCaptor.getAllValues().get(0).getKey()).isEqualTo("integration_124_field_0_api_key");
        assertThat(keyValueArgumentCaptor.getAllValues().get(0).getValue()).isEqualTo("apikey-0");
        assertThat(keyValueArgumentCaptor.getAllValues().get(1).getKey()).isEqualTo("integration_124_field_0_user_name");
        assertThat(keyValueArgumentCaptor.getAllValues().get(1).getValue()).isEqualTo("username-0");
        assertThat(keyValueArgumentCaptor.getAllValues().get(2).getKey()).isEqualTo("integration_124_field_1_api_key");
        assertThat(keyValueArgumentCaptor.getAllValues().get(2).getValue()).isEqualTo("apikey-1");
        assertThat(keyValueArgumentCaptor.getAllValues().get(3).getKey()).isEqualTo("integration_124_field_1_user_name");
        assertThat(keyValueArgumentCaptor.getAllValues().get(3).getValue()).isEqualTo("username-1");

        var integrationSecretMappingArgumentCaptor = ArgumentCaptor.forClass(IntegrationSecretMapping.class);
        verify(integrationSecretMappingsDatabaseService, times(4)).insert(eq(COMPANY), integrationSecretMappingArgumentCaptor.capture());

        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(0)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_MULTIPLE_API_KEY_0, "id");
        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(1)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_MULTIPLE_API_USERNAME_0, "id");
        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(2)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_MULTIPLE_API_KEY_1, "id");
        assertThat(integrationSecretMappingArgumentCaptor.getAllValues().get(3)).isEqualToIgnoringGivenFields(EXPECTED_MAPPING_MULTIPLE_API_USERNAME_1, "id");
    }

    @Test
    public void get() throws SecretsManagerServiceClientException, SQLException, BadRequestException, NotFoundException, AtlassianConnectServiceClientException {
        Token token = integrationSecretsService.get(COMPANY, "0", "123").orElse(null);

        assertThat(token).isNotNull();
        assertThat(token.getIntegrationId()).isEqualTo("123");
        assertThat(token.getId()).isEqualTo("0");
        assertThat(token.getTokenData()).isInstanceOf(ApiKey.class);
        assertThat(((ApiKey) token.getTokenData()).getApiKey()).isEqualTo("apikey-secret");
        assertThat(((ApiKey) token.getTokenData()).getUserName()).isEqualTo("username-secret");

        token = integrationSecretsService.get(COMPANY, "0", "124").orElse(null);
        assertThat(token).isNotNull();
        assertThat(token.getIntegrationId()).isEqualTo("124");
        assertThat(token.getId()).isEqualTo("0");
        assertThat(token.getTokenData()).isInstanceOf(MultipleApiKeys.class);
        assertThat(((MultipleApiKeys) token.getTokenData()).getKeys().size()).isEqualTo(2);
        assertThat(((MultipleApiKeys) token.getTokenData()).getKeys().get(0).getApiKey()).isEqualTo("apikey-0");
        assertThat(((MultipleApiKeys) token.getTokenData()).getKeys().get(0).getUserName()).isEqualTo("username-0");
        assertThat(((MultipleApiKeys) token.getTokenData()).getKeys().get(1).getApiKey()).isEqualTo("apikey-1");
        assertThat(((MultipleApiKeys) token.getTokenData()).getKeys().get(1).getUserName()).isEqualTo("username-1");

        token = integrationSecretsService.get(COMPANY, "0", "125").orElse(null);
        assertThat(token).isNotNull();
        assertThat(token.getIntegrationId()).isEqualTo("125");
        assertThat(token.getId()).isEqualTo("0");
        assertThat(token.getTokenData()).isInstanceOf(ApiKey.class);
        assertThat(((ApiKey) token.getTokenData()).getApiKey()).isEqualTo("apikey-secret");
        assertThat(((ApiKey) token.getTokenData()).getUserName()).isEqualTo("username-secret");
    }

    @Test
    public void delete() throws SecretsManagerServiceClientException, SQLException, BadRequestException, NotFoundException {
        integrationSecretsService.delete(COMPANY, "123");
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_123_field_api_key"));
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_123_field_user_name"));
        verify(secretsManagerServiceClient, times(2)).deleteKeyValue(anyString(), anyString(), anyString());
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("001"));
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("002"));
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("003"));
        verify(integrationSecretMappingsDatabaseService, times(3)).delete(anyString(), anyString());
    }

    @Test
    public void delete2() throws SecretsManagerServiceClientException, SQLException, BadRequestException, NotFoundException {
        integrationSecretsService.delete(COMPANY, "124");
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_api_key"));
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_user_name"));
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_api_key"));
        verify(secretsManagerServiceClient, times(1)).deleteKeyValue(eq(COMPANY), eq("0"), eq("integration_124_field_0_user_name"));
        verify(secretsManagerServiceClient, times(4)).deleteKeyValue(anyString(), anyString(), anyString());

        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("004"));
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("005"));
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("006"));
        verify(integrationSecretMappingsDatabaseService, times(1)).delete(eq(COMPANY), eq("007"));
        verify(integrationSecretMappingsDatabaseService, times(4)).delete(anyString(), anyString());
    }

    @Test
    public void flattenTokenIntoFields() throws BadRequestException {
        // -- test validation
        assertThatThrownBy(() -> IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.OAUTH,
                Token.builder()
                        .tokenData(ApiKey.builder().build())
                        .build())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.API_KEY,
                Token.builder()
                        .tokenData(OauthToken.builder().build())
                        .build())).isInstanceOf(BadRequestException.class);

        // -- oauth
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.OAUTH,
                Token.builder()
                        .tokenData(OauthToken.builder()
                                .token("a")
                                .refreshToken("b")
                                .botToken("c")
                                .instanceUrl("d")
                                .build())
                        .build())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "access_token", "a",
                "refresh_token", "b",
                "bot_token", "c",
                "instance_url", "d",
                "refreshed_at", "null"));

        // -- api key
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.API_KEY,
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .userName("a")
                                .apiKey("b")
                                .build())
                        .build())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "user_name", "a",
                "api_key", "b"));

        // -- multiple api key
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.MULTIPLE_API_KEYS,
                Token.builder()
                        .tokenData(MultipleApiKeys.builder()
                                .keys(List.of(
                                                MultipleApiKeys.Key.builder().userName("username-0").apiKey("apikey-0").build(),
                                                MultipleApiKeys.Key.builder().userName("username-1").apiKey("apikey-1").build()
                                        )
                                )
                                .build())
                        .build())).containsExactlyInAnyOrderEntriesOf(Map.of(

                "0_user_name", "username-0",
                "0_api_key", "apikey-0",
                "1_user_name", "username-1",
                "1_api_key", "apikey-1"));

        // -- db
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.DB,
                Token.builder()
                        .tokenData(DBAuth.builder()
                                .server("a")
                                .userName("b")
                                .password("c")
                                .databaseName("d")
                                .build())
                        .build())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "server", "a",
                "user_name", "b",
                "password", "c",
                "database_name", "d"));

        // -- atlassian connect
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.ATLASSIAN_CONNECT_JWT,
                Token.builder()
                        .tokenData( AtlassianConnectJwtToken.builder()
                                .clientKey("a")
                                .name("b")
                                .appKey("app-key")
                                .sharedSecret("secret")
                                .build())
                        .build())).containsExactlyInAnyOrderEntriesOf(Map.of(
                "client_key", "a",
                "name", "b"));

        // -- NONE
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.NONE,
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .userName("a")
                                .apiKey("b")
                                .build())
                        .build())).isEmpty();

        // -- UNKNOWN
        assertThat(IntegrationSecretsService.flattenTokenIntoFields(
                Integration.Authentication.UNKNOWN,
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .userName("a")
                                .apiKey("b")
                                .build())
                        .build())).isEmpty();
    }

    @Test
    public void parseTokenData() throws BadRequestException, AtlassianConnectServiceClientException {
        // -- oauth
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.OAUTH,
                Map.of(
                        "access_token", "a",
                        "refresh_token", "b",
                        "bot_token", "c",
                        "instance_url", "d",
                        "refreshed_at", "null"), atlassianConnectServiceClient)).isEqualTo(OauthToken.builder()
                .token("a")
                .refreshToken("b")
                .botToken("c")
                .instanceUrl("d")
                .build());

        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.OAUTH,
                Map.of(
                        "access_token", "a",
                        "refresh_token", "b",
                        "bot_token", "c",
                        "instance_url", "d",
                        "refreshed_at", "l;sdkf;lsdf"), atlassianConnectServiceClient)).isEqualTo(OauthToken.builder()
                .token("a")
                .refreshToken("b")
                .botToken("c")
                .instanceUrl("d")
                .build());

        // -- api key
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.API_KEY,
                Map.of(
                        "user_name", "a",
                        "api_key", "b"), atlassianConnectServiceClient)).isEqualTo(
                ApiKey.builder()
                        .userName("a")
                        .apiKey("b")
                        .build());
        // -- multiple api key
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.MULTIPLE_API_KEYS,
                Map.of(
                        "0_user_name", "username-0",
                        "0_api_key", "apikey-0",
                        "1_user_name", "username-1",
                        "1_api_key", "apikey-1"), atlassianConnectServiceClient)).isEqualTo(
                MultipleApiKeys.builder()
                        .keys(List.of(
                                        MultipleApiKeys.Key.builder().userName("username-0").apiKey("apikey-0").build(),
                                        MultipleApiKeys.Key.builder().userName("username-1").apiKey("apikey-1").build()
                                )
                        )
                        .build());

        // -- db
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.DB,
                Map.of(
                        "server", "a",
                        "user_name", "b",
                        "password", "c",
                        "database_name", "d"), atlassianConnectServiceClient)).isEqualTo(
                DBAuth.builder()
                        .server("a")
                        .userName("b")
                        .password("c")
                        .databaseName("d")
                        .build());

        // -- atlassian connect jwt
        when(atlassianConnectServiceClient.getSecret(eq("a"))).thenReturn("secret");
        when(atlassianConnectServiceClient.getMetadata(eq("a"))).thenReturn(
                        AtlassianConnectAppMetadata.builder()
                                .atlassianClientKey("key")
                                .productType("jira")
                                .description("desc")
                                .enabled(true)
                                .atlassianBaseUrl("https://atlassian.net")
                                .installedAppKey("app-key")
                                .build());
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.ATLASSIAN_CONNECT_JWT,
                Map.of(
                        "client_key", "a",
                        "name", "b"
                ), atlassianConnectServiceClient)).isEqualTo(
                AtlassianConnectJwtToken.builder()
                        .clientKey("a")
                        .name("b")
                        .appKey("app-key")
                        .sharedSecret("secret")
                        .baseUrl("https://atlassian.net")
                        .build());
        // -- NONE
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.NONE,
                Map.of(
                        "user_name", "a",
                        "api_key", "b"), atlassianConnectServiceClient)).isNull();
        // -- UNKNOWN
        assertThat(IntegrationSecretsService.parseTokenData(
                "1",
                Integration.Authentication.UNKNOWN,
                Map.of(
                        "user_name", "a",
                        "api_key", "b"), atlassianConnectServiceClient)).isNull();
    }
}