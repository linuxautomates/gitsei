package io.levelops.api.services;

import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.config.IngestionConfig;
import io.levelops.api.exceptions.PreflightCheckFailedException;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.requests.ModifyIntegrationRequest;
import io.levelops.api.services.IntegrationManagementService.AdoSubtype;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.AtlassianConnectJwtToken;
import io.levelops.commons.databases.models.database.tokens.MultipleApiKeys;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.SlackTenantLookupDatabaseService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import io.levelops.commons.token_services.AzureDevopsTokenService;
import io.levelops.commons.token_services.BitbucketTokenService;
import io.levelops.commons.token_services.BlackDuckTokenService;
import io.levelops.commons.token_services.CxSastTokenService;
import io.levelops.commons.token_services.GithubTokenService;
import io.levelops.commons.token_services.GitlabTokenService;
import io.levelops.commons.token_services.MSTeamsTokenService;
import io.levelops.commons.token_services.SalesforceTokenService;
import io.levelops.commons.token_services.ServicenowTokenService;
import io.levelops.commons.token_services.SlackTokenService;
import io.levelops.commons.token_services.TokenService;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.CreateTriggerRequest;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.models.PreflightCheckResults;
import io.levelops.services.PreflightCheckService;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class IntegrationManagementServiceTest {
    private static final String SECRETS = "__secrets__";
    private static final String WEBHOOK_SECRETS = "webhook_secrets";
    private IntegrationManagementService integrationManagementService;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private SlackTokenService slackTokenService;
    @Autowired
    private MSTeamsTokenService msTeamsTokenService;
    @Autowired
    private GithubTokenService githubTokenService;
    @Autowired
    private GitRepositoryService gitRepositoryService;
    @Autowired
    private BitbucketTokenService bitbucketTokenService;
    @Autowired
    private ControlPlaneService controlPlaneService;
    @Autowired
    private PreflightCheckService preflightCheckService;
    @Autowired
    private SalesforceTokenService salesForceTokenService;
    @Autowired
    private SlackTenantLookupDatabaseService slackTenantLookupDatabaseService;
    @Autowired
    private GitlabTokenService gitlabTokenService;
    @Autowired
    private AzureDevopsTokenService azureDevopsTokenService;
    @Autowired
    private CxSastTokenService cxSastTokenService;
    @Autowired
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    @Autowired
    BlackDuckTokenService blackDuckTokenService;
    @Autowired
    ServicenowTokenService servicenowTokenService;
    @Autowired
    ProductService productService;
    @Autowired
    ProductIntegMappingService productIntegMappingService;
    @Autowired
    GithubAppTokenService githubAppTokenService;

    @Autowired
    AtlassianConnectServiceClient atlassianConnectServiceClient;

    @Before
    public void setup() throws IngestionServiceException {
        when(controlPlaneService.getTriggers(anyString(), anyInt(), anyString())).thenReturn(PaginatedResponse.of(0, 1, List.of()));
        integrationManagementService = new IntegrationManagementService(inventoryService, githubTokenService, gitRepositoryService, "", bitbucketTokenService, gitlabTokenService,
                azureDevopsTokenService, "http://internal-api-lb", slackTokenService, msTeamsTokenService, salesForceTokenService, controlPlaneService, preflightCheckService, slackTenantLookupDatabaseService, ciCdInstancesDatabaseService, cxSastTokenService,
                blackDuckTokenService, servicenowTokenService, githubAppTokenService, productService, productIntegMappingService,
                IngestionConfig.IngestionTriggerSettings.builder().defaultTriggerFrequency(60).appSpecificTriggerFrequency(null).build(),
                atlassianConnectServiceClient, "", true, "", "");
    }

    @Test(expected = ServerApiException.class)
    public void testInvalidCreateIntegrationRequest() throws Exception {
        when(githubTokenService.getTokensFromCode("a", ""))
                .thenThrow(new InvalidTokenDataException("Bad Request"));
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder().type("oauth").application("github").url("url")
                        .state("").code("a").build());
    }

    @Test(expected = ServerApiException.class)
    public void testInvalidCreateIntegrationApplication() throws Exception {
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder().type("oauth").application("wompwomp").build());
    }

    @Test(expected = ServerApiException.class)
    public void testInvalidCreateIntegrationTokenType() throws Exception {
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder().type("asdfasdf").application("github").build());
    }

    @Test
    public void testValidCreateOauthIntegration() throws Exception {
        reset(inventoryService);
        reset(controlPlaneService);
        when(githubTokenService.getTokensFromCode(eq("asd"), eq("bsd")))
                .thenReturn(TokenService.Tokens.builder().accessToken("a").build());
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.postToken(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder().build());
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder().type("oauth").code("asd").state("bsd").url("url")
                        .name("ahha").description("some").url("https://test").application("github").build());
        verify(inventoryService, times(1)).postIntegration("test",
                Integration.builder()
                        .application("github")
                        .name("ahha")
                        .description("some")
                        .status("ACTIVE")
                        .url("https://test")
                        .satellite(false)
                        .authentication(Integration.Authentication.OAUTH)
                        .metadata(Map.of())
                        .build());
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(inventoryService, times(1)).postToken(eq("test"), eq("1"),
                captor.capture());
        verify(controlPlaneService, times(1)).createTrigger(any());
        assertThat(captor.getValue().equals(Token.builder().integrationId("1").tokenData(OauthToken.builder()
                .token("a").type("oauth").build()).build()));
    }

    @Test
    public void testValidCreateAtlassianConectIntegration() throws Exception {
        reset(inventoryService);
        reset(controlPlaneService);
        when(githubTokenService.getTokensFromCode(eq("asd"), eq("bsd")))
                .thenReturn(TokenService.Tokens.builder().accessToken("a").build());
        when(atlassianConnectServiceClient.getSecret("test_client_key")).thenReturn("secret");
        when(atlassianConnectServiceClient.getMetadata("test_client_key")).thenReturn(AtlassianConnectAppMetadata.builder()
                        .atlassianClientKey("test_client_key")
                        .atlassianBaseUrl("https://test")
                        .installedAppKey("test_installed_app_key")
                        .enabled(true)
                        .productType("jira")
                        .description("some")
                        .build());
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.postToken(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder().build());
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .type("atlassian_connect_jwt")
                        .clientKey("test_client_key")
                        .name("ahha")
                        .description("some")
                        .application("jira")
                        .build());
        verify(inventoryService, times(1)).postIntegration("test",
                Integration.builder()
                        .application("jira")
                        .name("ahha")
                        .description("some")
                        .satellite(false)
                        .authentication(Integration.Authentication.ATLASSIAN_CONNECT_JWT)
                        .metadata(Map.of())
                        .status("ACTIVE")
                        .build());
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(inventoryService, times(1)).postToken(eq("test"), eq("1"),
                captor.capture());
        verify(controlPlaneService, times(1)).createTrigger(any());
        assertThat(captor.getValue().getTokenData()).isInstanceOf(AtlassianConnectJwtToken.class);
        var tokenData = (AtlassianConnectJwtToken) captor.getValue().getTokenData();
        assertThat(tokenData.getAppKey()).isEqualTo("test_installed_app_key");
        assertThat(tokenData.getBaseUrl()).isEqualTo("https://test");
        assertThat(tokenData.getSharedSecret()).isEqualTo("secret");
        assertThat(tokenData.getClientKey()).isEqualTo("test_client_key");
    }

    @Test
    public void testValidCreateApikeyIntegration() throws Exception {
        // mock
        reset(inventoryService);
        reset(controlPlaneService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.postToken(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));

        // run
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .type("apikey")
                        .apikey("asd")
                        .username("bsd")
                        .name("ahha")
                        .description("some")
                        .url("https://test")
                        .application("jira")
                        .build());

        // verify
        verify(inventoryService, times(1)).postIntegration("test",
                Integration.builder()
                        .application("jira")
                        .name("ahha")
                        .description("some")
                        .status("ACTIVE")
                        .url("https://test")
                        .satellite(false)
                        .authentication(Integration.Authentication.API_KEY)
                        .metadata(Map.of())
                        .build());
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(inventoryService, times(1)).postToken(eq("test"), eq("1"), captor.capture());
        verify(controlPlaneService, times(1)).createTrigger(any());
        assertThat(captor.getValue().equals(Token.builder()
                .integrationId("1")
                .tokenData(ApiKey.builder()
                        .apiKey("asd")
                        .userName("bsd")
                        .type("apikey")
                        .build())
                .build()));
    }

    @Test
    public void testValidCreateMultiApikeyIntegration() throws Exception {
        // mock
        reset(inventoryService);
        reset(controlPlaneService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.postToken(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder().build());

        // run
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .type("multiple_api_keys")
                        .keys(List.of(
                                ModifyIntegrationRequest.Key.builder().apikey("api-key-1").username("username-1").build(),
                                ModifyIntegrationRequest.Key.builder().apikey("api-key-2").username("username-2").build()
                        ))
                        .name("multiple api keys")
                        .description("some")
                        .url("https://test")
                        .application("github")
                        .build());

        // verify
        verify(inventoryService, times(1)).postIntegration("test",
                Integration.builder()
                        .application("github")
                        .name("multiple api keys")
                        .description("some")
                        .status("ACTIVE")
                        .url("https://test")
                        .satellite(false)
                        .authentication(Integration.Authentication.MULTIPLE_API_KEYS)
                        .metadata(Map.of())
                        .build());
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(inventoryService, times(1)).postToken(eq("test"), eq("1"), captor.capture());
        verify(controlPlaneService, times(1)).createTrigger(any());
        assertThat(captor.getValue().equals(Token.builder()
                .integrationId("1")
                .tokenData(MultipleApiKeys.builder()
                        .keys(List.of(
                                MultipleApiKeys.Key.builder().apiKey("api-key-1").userName("username-1").build(),
                                MultipleApiKeys.Key.builder().apiKey("api-key-2").userName("username-2").build()
                        ))
                        .type("multiple_api_keys")
                        .build())
                .build()));
    }

    @Test
    public void testCreateApikeyIntegrationWithFailedPreflightCheck() throws Exception {
        // mock
        reset(inventoryService);
        reset(controlPlaneService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.postToken(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));

        // run
        assertThatThrownBy(() -> integrationManagementService.createIntegrationFromRequest(
                "failPreflightCheck",
                ModifyIntegrationRequest.builder()
                        .type("apikey")
                        .apikey("asd")
                        .username("bsd")
                        .name("ahha")
                        .description("some")
                        .url("https://test")
                        .application("jira")
                        .build()))
                .isExactlyInstanceOf(PreflightCheckFailedException.class)
                .extracting(e -> ((PreflightCheckFailedException) e).getPreflightCheckResults())
                .extracting(PreflightCheckResults::isSuccess)
                .isEqualTo(false);

        // verify
        verify(inventoryService, never()).postIntegration(anyString(), any(Integration.class));
        verify(inventoryService, never()).postToken(anyString(), anyString(), any(Token.class));
        verify(controlPlaneService, never()).createTrigger(any());
    }

    @Test
    public void testValidUpdateIntegrationWithExtraFields() throws Exception {
        reset(inventoryService);
        when(inventoryService.updateIntegration(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(
                Integration.builder().id("1").name("ahha").description("some").url("https://test")
                        .status("ACTIVE").application("jira").build());
        integrationManagementService.updateIntegrationFromRequest("test", "1",
                ModifyIntegrationRequest.builder().name("ahha").application("jira").description("some").build());
        verify(inventoryService, times(1)).updateIntegration("test", "1",
                Integration.builder().name("ahha").description("some").id("1").build());
        verify(inventoryService, times(0)).postToken(any(), any(),
                any());
    }

    @Test
    public void testValidFullUpdateIntegration() throws Exception {
        reset(inventoryService);
        when(inventoryService.listTokens("test", "1"))
                .thenReturn(Arrays.asList(Token.builder().id("1").build(), Token.builder().id("2").build()));
        when(inventoryService.updateIntegration(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(
                Integration.builder().id("1").name("ahha").metadata(Map.of(SECRETS, Map.of(WEBHOOK_SECRETS, "abcdesfghij")))
                        .description("some").url("https://test").status("ACTIVE").application("github").build());
        integrationManagementService.updateIntegrationFromRequest("test", "1",
                ModifyIntegrationRequest.builder().name("ahha").description("some").application("github")
                        .url("https://api").apikey("aksdasndkjsand").type("apikey").username("something").build());
        verify(inventoryService, times(1)).updateIntegration("test", "1",
                Integration.builder().application("github").name("ahha").description("some")
                        .status("ACTIVE").url("https://api").id("1").build());
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
        verify(inventoryService, times(1)).deleteTokensByIntegration(eq("test"),
                eq("1"));
        verify(inventoryService, times(1)).postToken(eq("test"), eq("1"),
                captor.capture());
        assertThat(captor.getValue().equals(Token.builder().integrationId("1").tokenData(ApiKey.builder()
                .apiKey("aksdasndkjsand").userName("something").type("apikey").build()).build()));
    }

    @Test
    public void testValidPartialUpdateIntegration() throws Exception {
        reset(inventoryService);
        when(inventoryService.updateIntegration(eq("test"), eq("1"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(
                Integration.builder().id("1").name("ahha").metadata(Map.of(SECRETS, Map.of(WEBHOOK_SECRETS, "abcdesfghij")))
                        .description("some").url("https://test").status("ACTIVE").application("github").build());
        integrationManagementService.updateIntegrationFromRequest("test", "1",
                ModifyIntegrationRequest.builder().name("ahha").application("github").description("some").build());
        verify(inventoryService, times(1)).updateIntegration("test", "1",
                Integration.builder().name("ahha").description("some").id("1").build());
        verify(inventoryService, times(0)).postToken(any(), any(),
                any());
    }

    @Test
    public void testGithubWebhookSecret() throws Exception {
        reset(inventoryService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder()
                .name("ahha")
                .metadata(Map.of(SECRETS, Map.of(WEBHOOK_SECRETS, "abcdesfghij")))
                .url("https://test")
                .application("github")
                .id("1")
                .description("some")
                .build());
        when(inventoryService.updateIntegration(eq("test"), eq("1"),
                eq(Integration.builder()
                        .id("1")
                        .name("ahha")
                        .description("something")
                        .url("https://test")
                        .status("ACTIVE")
                        .application("github")
                        .build())))
                .thenReturn(Optional.of("1"));
        when(inventoryService.listIntegrationsFullFilter(eq("test"), eq(DefaultListRequest.builder()
                .page(0)
                .pageSize(100)
                .acrossLimit(90)
                .aggInterval("day")
                .build())))
                .thenReturn(DbListResponse.<Integration>builder()
                        .records(List.of(Integration.builder()
                                .id("1")
                                .name("ahha")
                                .metadata(Map.of(SECRETS, Map.of(WEBHOOK_SECRETS, "abcdesfghij")))
                                .description("something")
                                .url("https://test")
                                .status("ACTIVE")
                                .application("github")
                                .build()))
                        .totalCount(1)
                        .build());
        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .type("apikey")
                        .apikey("asd")
                        .username("bsd")
                        .name("ahha")
                        .description("some")
                        .url("https://test")
                        .application("github")
                        .build());
        assertThat(integrationManagementService.getIntegration("test", "1").getMetadata()
                .get(SECRETS)).isNull();
        assertThat(inventoryService.getIntegration("test", "1").getMetadata()
                .get(SECRETS)).isNotNull();
        assertThat(integrationManagementService.listIntegrations("test",
                        DefaultListRequest.builder()
                                .page(0)
                                .pageSize(100)
                                .acrossLimit(90)
                                .aggInterval("day")
                                .build())
                .getRecords()
                .stream()
                .map(integration -> integration.getMetadata().get(SECRETS))
                .collect(Collectors.toList()).get(0)).isNull();
        assertThat(integrationManagementService.listIntegrations("test",
                        DefaultListRequest.builder()
                                .page(0)
                                .pageSize(100)
                                .acrossLimit(90)
                                .aggInterval("day")
                                .build())
                .getTotalCount()).isEqualTo(1);
        assertThat(inventoryService.listIntegrationsFullFilter("test",
                        DefaultListRequest.builder()
                                .page(0)
                                .pageSize(100)
                                .acrossLimit(90)
                                .aggInterval("day")
                                .build())
                .getRecords()
                .stream()
                .map(integration -> integration.getMetadata().get(SECRETS))
                .collect(Collectors.toList()).get(0)).isNotNull();
        integrationManagementService.updateIntegrationFromRequest("test", "1",
                ModifyIntegrationRequest.builder()
                        .type("apikey")
                        .apikey("asd")
                        .username("bsd")
                        .name("ahha")
                        .description("something")
                        .url("https://test")
                        .application("github")
                        .build());
        assertThat(integrationManagementService.getIntegration("test", "1").getMetadata()
                .get(SECRETS)).isNull();
        assertThat(inventoryService.getIntegration("test", "1").getMetadata()
                .get(SECRETS)).isNotNull();
        assertThat(integrationManagementService.listIntegrations("test",
                        DefaultListRequest.builder()
                                .page(0)
                                .pageSize(100)
                                .acrossLimit(90)
                                .aggInterval("day")
                                .build())
                .getRecords()
                .stream()
                .map(integration -> integration.getMetadata().get(SECRETS))
                .collect(Collectors.toList()).get(0)).isNull();
        assertThat(inventoryService.listIntegrationsFullFilter("test",
                        DefaultListRequest.builder()
                                .page(0)
                                .pageSize(100)
                                .acrossLimit(90)
                                .aggInterval("day")
                                .build())
                .getRecords()
                .stream()
                .map(integration -> integration.getMetadata().get(SECRETS))
                .collect(Collectors.toList()).get(0)).isNotNull();
    }

    @Test
    public void testSemiAutomatedIntegrations() throws PreflightCheckFailedException, InventoryException, BadRequestException, AtlassianConnectServiceClientException {
        reset(inventoryService);
        reset(controlPlaneService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder()
                .name("brakeman1")
                .metadata(Map.of("project", "test"))
                .application("brakeman")
                .id("1")
                .description("some")
                .build());

        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .name("brakeman1")
                        .description("some")
                        .metadata(Map.of("project", "test"))
                        .application("sast_brakeman")
                        .type("apikey")
                        .build());
        assertThat(integrationManagementService.getIntegration("test", "1").getMetadata())
                .isEqualTo(Map.of("project", "test"));
        assertThat(inventoryService.getIntegration("test", "1").getName()).isNotNull();

        reset(inventoryService);
        reset(controlPlaneService);
        when(inventoryService.postIntegration(eq("test"), any())).thenReturn(Optional.of("1"));
        when(inventoryService.getIntegration(eq("test"), eq("1"))).thenReturn(Integration.builder()
                .name("microsoft_threat_modeling")
                .metadata(Map.of("project", "test"))
                .application("microsoft_threat_modeling")
                .id("1")
                .description("some")
                .build());

        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder()
                        .name("microsoft_threat_modeling")
                        .description("some")
                        .metadata(Map.of("project", "test"))
                        .application("REPORT_MS_TMT")
                        .type("apikey")
                        .build());
        assertThat(integrationManagementService.getIntegration("test", "1").getMetadata())
                .isEqualTo(Map.of("project", "test"));
        assertThat(inventoryService.getIntegration("test", "1").getName()).isNotNull();
    }

    @Test
    public void adoSubType() {
        assertThat(AdoSubtype.WI.getIngestionFlagsComplement())
                .containsExactlyInAnyOrderElementsOf(CollectionUtils.union(AdoSubtype.SCM.getIngestionFlags(), AdoSubtype.CICD.getIngestionFlags()));
        assertThat(AdoSubtype.SCM.getIngestionFlagsComplement())
                .containsExactlyInAnyOrderElementsOf(CollectionUtils.union(AdoSubtype.WI.getIngestionFlags(), AdoSubtype.CICD.getIngestionFlags()));
        assertThat(AdoSubtype.CICD.getIngestionFlagsComplement())
                .containsExactlyInAnyOrderElementsOf(CollectionUtils.union(AdoSubtype.SCM.getIngestionFlags(), AdoSubtype.WI.getIngestionFlags()));
    }

    @Test
    public void splitAdoIntegration() {
        Integration in = Integration.builder()
                .name("My ADO")
                .application("ado")
                .metadata(null)
                .build();
        List<Integration> out = IntegrationManagementService.splitAdoIntegration(in);
        assertThat(out).isEmpty();

        out = IntegrationManagementService.splitAdoIntegration(in.toBuilder()
                .metadata(Map.of("a", "1",
                        "wi", Map.of("enabled", true, "b", "1"),
                        "scm", Map.of("enabled", true, "b", "2"),
                        "cicd", Map.of("enabled", true, "b", "3")))
                .build());

        Map<String, Object> wiMetadata = new HashMap<>();
        wiMetadata.put("subtype", "wi");
        wiMetadata.put("a", "1");
        wiMetadata.put("b", "1");
        wiMetadata.put("fetch_commits", false);
        wiMetadata.put("fetch_tags", false);
        wiMetadata.put("fetch_prs", false);
        wiMetadata.put("fetch_branches", false);
        wiMetadata.put("fetch_change_sets", false);
        wiMetadata.put("fetch_labels", false);
        wiMetadata.put("fetch_pipelines", false);
        wiMetadata.put("fetch_releases", false);
        wiMetadata.put("fetch_builds", false);
        wiMetadata.put("fetch_work_items", true);
        wiMetadata.put("fetch_workitem_fields", true);
        wiMetadata.put("fetch_metadata", true);
        wiMetadata.put("fetch_work_items_comments", true);
        wiMetadata.put("fetch_teams", true);
        wiMetadata.put("fetch_iterations", true);
        wiMetadata.put("fetch_workitem_histories", true);

        Map<String, Object> scmMetadata = new HashMap<>();
        scmMetadata.put("subtype", "scm");
        scmMetadata.put("a", "1");
        scmMetadata.put("b", "2");
        scmMetadata.put("fetch_commits", true);
        scmMetadata.put("fetch_tags", true);
        scmMetadata.put("fetch_prs", true);
        scmMetadata.put("fetch_branches", true);
        scmMetadata.put("fetch_change_sets", true);
        scmMetadata.put("fetch_labels", true);
        scmMetadata.put("fetch_pipelines", false);
        scmMetadata.put("fetch_releases", false);
        scmMetadata.put("fetch_builds", false);
        scmMetadata.put("fetch_work_items", false);
        scmMetadata.put("fetch_workitem_fields", false);
        scmMetadata.put("fetch_metadata", false);
        scmMetadata.put("fetch_work_items_comments", false);
        scmMetadata.put("fetch_teams", false);
        scmMetadata.put("fetch_iterations", false);
        scmMetadata.put("fetch_workitem_histories", false);

        Map<String, Object> cicdMetadata = new HashMap<>();
        cicdMetadata.put("subtype", "cicd");
        cicdMetadata.put("a", "1");
        cicdMetadata.put("b", "3");
        cicdMetadata.put("fetch_commits", false);
        cicdMetadata.put("fetch_tags", false);
        cicdMetadata.put("fetch_prs", false);
        cicdMetadata.put("fetch_branches", false);
        cicdMetadata.put("fetch_change_sets", false);
        cicdMetadata.put("fetch_labels", false);
        cicdMetadata.put("fetch_pipelines", true);
        cicdMetadata.put("fetch_releases", true);
        cicdMetadata.put("fetch_builds", true);
        cicdMetadata.put("fetch_work_items", false);
        cicdMetadata.put("fetch_workitem_fields", false);
        cicdMetadata.put("fetch_metadata", false);
        cicdMetadata.put("fetch_work_items_comments", false);
        cicdMetadata.put("fetch_teams", false);
        cicdMetadata.put("fetch_iterations", false);
        cicdMetadata.put("fetch_workitem_histories", false);
        assertThat(out).containsExactly(
                Integration.builder()
                        .name("My ADO - Boards")
                        .application("ado")
                        .metadata(wiMetadata)
                        .build(),
                Integration.builder()
                        .name("My ADO - Repos")
                        .application("ado")
                        .metadata(scmMetadata)
                        .build(),
                Integration.builder()
                        .name("My ADO - Pipelines")
                        .application("ado")
                        .metadata(cicdMetadata)
                        .build());

        out = IntegrationManagementService.splitAdoIntegration(in.toBuilder()
                .metadata(Map.of("a", "1",
                        "wi", Map.of("enabled", false, "b", "1"),
                        "cicd", Map.of("enabled", true, "b", "3")))
                .build());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getName()).isEqualTo("My ADO - Pipelines");
    }

    private String getCallbackUrl(IntegrationType integrationType, String callbackDisabledApps) {
        var set = IntegrationManagementService.commaListToIntegrationTypeSet(callbackDisabledApps);
        return IntegrationManagementService.getCallbackUrl(integrationType, "foo", "int1", "a", set);
    }

    @Test
    public void testCallback() {
        assertThat(getCallbackUrl(IntegrationType.GITHUB, "")).hasSizeGreaterThan(0);
        assertThat(getCallbackUrl(IntegrationType.GITHUB, "github")).hasSize(0);
        assertThat(getCallbackUrl(IntegrationType.JIRA, "github")).hasSizeGreaterThan(0);
        assertThat(getCallbackUrl(IntegrationType.JIRA, "jira")).hasSize(0);
        assertThat(getCallbackUrl(IntegrationType.JIRA, "jira, github")).hasSize(0);
        assertThat(getCallbackUrl(IntegrationType.JIRA, "harnessng, github")).hasSizeGreaterThan(0);
    }

    @Test
    public void testIntegrationDisabledTenants() throws PreflightCheckFailedException, InventoryException, BadRequestException, TokenException, IngestionServiceException, AtlassianConnectServiceClientException {
        reset(inventoryService);
        reset(controlPlaneService);

        when(inventoryService.postIntegration(any(), any())).thenReturn(Optional.of("integrationId"));
        when(inventoryService.getIntegration(any(), any())).thenReturn(Integration.builder().metadata(Map.of()).build());
        when(githubTokenService.getTokensFromCode(any(), any()))
                .thenReturn(TokenService.Tokens.builder().accessToken("a").instanceUrl("url").build());
        integrationManagementService = new IntegrationManagementService(inventoryService, githubTokenService, gitRepositoryService, "", bitbucketTokenService, gitlabTokenService,
                azureDevopsTokenService, "http://internal-api-lb", slackTokenService, msTeamsTokenService, salesForceTokenService, controlPlaneService, preflightCheckService, slackTenantLookupDatabaseService, ciCdInstancesDatabaseService, cxSastTokenService,
                blackDuckTokenService, servicenowTokenService, githubAppTokenService, productService, productIntegMappingService,
                IngestionConfig.IngestionTriggerSettings.builder().defaultTriggerFrequency(60).appSpecificTriggerFrequency(null).build(),
                atlassianConnectServiceClient, "jira", false, "", "foo,training");

        integrationManagementService.createIntegrationFromRequest("test",
                ModifyIntegrationRequest.builder().type("oauth").application("github").url("url")
                        .state("").code("a").name("name").build());
        ArgumentCaptor<CreateTriggerRequest> captor = ArgumentCaptor.forClass(CreateTriggerRequest.class);
        verify(controlPlaneService).createTrigger(captor.capture());
        assertThat(captor.getValue().getFrequency()).isNotEqualTo(0);


        integrationManagementService.createIntegrationFromRequest("training",
                ModifyIntegrationRequest.builder().type("oauth").application("github").url("url")
                        .state("").code("a").name("name").build());
        captor = ArgumentCaptor.forClass(CreateTriggerRequest.class);
        verify(controlPlaneService, times(2)).createTrigger(captor.capture());
        assertThat(captor.getValue().getFrequency()).isEqualTo(0);
    }
}
