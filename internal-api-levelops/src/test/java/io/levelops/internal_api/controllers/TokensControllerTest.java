package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TokenDataService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.token_services.AzureDevopsTokenService;
import io.levelops.commons.token_services.BitbucketTokenService;
import io.levelops.commons.token_services.BlackDuckTokenService;
import io.levelops.commons.token_services.CxSastTokenService;
import io.levelops.commons.token_services.GitlabTokenService;
import io.levelops.commons.token_services.MSTeamsTokenService;
import io.levelops.commons.token_services.SalesforceTokenService;
import io.levelops.commons.token_services.SlackTokenService;
import io.levelops.integrations.github.client.GithubAppTokenService;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.services.IntegrationSecretsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class TokensControllerTest {
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TokenDataService tokenDataService;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private SlackTokenService tokenService;
    @Autowired
    private MSTeamsTokenService msTeamsTokenService;
    @Autowired
    private BitbucketTokenService bitbucketTokenService;
    @Autowired
    private SalesforceTokenService salesforceTokenService;
    @Autowired
    private GitlabTokenService gitlabTokenService;
    @Autowired
    private AzureDevopsTokenService azureDevopsTokenService;
    @Autowired
    private CxSastTokenService cxSastTokenService;
    @Autowired
    private BlackDuckTokenService blackDuckTokenService;
    @Autowired
    private GithubAppTokenService githubAppTokenService;
    @Autowired
    private IntegrationSecretsService integrationSecretsService;
    @Autowired
    @Qualifier("tokenRefreshLockRegistry")
    private LockRegistry tokenRefreshLockRegistry;

    @Before
    public void setup() {
        boolean readTokensFromSecretsManagerService = false;
        mvc = MockMvcBuilders.standaloneSetup(new TokensController(
                tokenDataService, integrationService, tokenService, msTeamsTokenService,bitbucketTokenService, salesforceTokenService, gitlabTokenService, azureDevopsTokenService, cxSastTokenService,
                blackDuckTokenService, integrationSecretsService, githubAppTokenService, tokenRefreshLockRegistry, 10, true, readTokensFromSecretsManagerService, List.of())).build();
    }

    @Test
    public void testCreateToken() throws Exception {
        when(tokenDataService.insert("test", Token.builder()
                .integrationId("1").tokenData(OauthToken.builder().token("a").refreshToken("b")
                        .createdAt(1L).build()).build())).thenReturn("1");
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/integrations/1/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"integration_id\":\"1\",\"token_data\":{\"type\":\"oauth\",\"token\":\"a\"," +
                        "\"refresh_token\":\"b\",\"created_at\":1}}"))
                .andReturn()))
                .andExpect(content().json("{\"token_id\":\"1\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testListTokens() throws Exception {
        List<Token> l = Collections.singletonList(Token.builder()
                .integrationId("1").tokenData(OauthToken.builder().token("a")
                        .refreshToken("b").createdAt(1L).build()).build());
        DbListResponse<Token> expectedResponse = DbListResponse.of(l, 1);
        when(tokenDataService.listByIntegration("test", "1", 2, 1))
                .thenReturn(expectedResponse);
        mvc.perform(asyncDispatch(mvc.perform(post("/internal/v1/tenants/test/integrations/1/tokens/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"page_size\":1,\"page\":2}"))
                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
    }

    @Test
    public void parseReadTokensFromSecretsManagerServiceForIntegrations() {
        Map<String, Set<String>> map = TokensController.parseListOfTenantAtIntegrationIds(List.of("foo@1", "bar@456", "foo@2"));
        assertThat(map.get("foo")).containsExactlyInAnyOrder("1", "2");
        assertThat(map.get("bar")).containsExactlyInAnyOrder("456");
        assertThat(map).containsOnlyKeys("foo", "bar");
    }
}
