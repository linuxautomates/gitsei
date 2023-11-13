package io.levelops.preflightchecks;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.models.PreflightCheckResults;

import io.levelops.models.ScmRepository;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

public class GithubPreflightCheckIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String GITHUB_URL = System.getenv("GITHUB_URL");
    private static final boolean IS_GITHUB_APP = true;
    private static final String GITHUB_ACCESS_TOKEN = System.getenv("GITHUB_ACCESS_TOKEN");
    private static final String GITHUB_REFRESH_TOKEN = System.getenv("GITHUB_REFRESH_TOKEN");
    private static final String DEFAULT_GITHUB_URL = "https://api.github.com";
    private static final String INTEGRATION_ID = "github1";
    private static final String APPLICATION = "github";

    private GithubPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        var client = new OkHttpClient().newBuilder().followRedirects(true).build();

        integration = Integration.builder()
                .id(INTEGRATION_ID)
                .application("github")
                .name("test")
                .status("ACTIVE")
                .url(StringUtils.firstNonBlank(GITHUB_URL, DEFAULT_GITHUB_URL))
                .satellite(false)
                .metadata(IS_GITHUB_APP? Map.of("app_id", "dummy") : Map.of())
                .build();
        token = Token.builder()
                .id("test")
                .integrationId("1")
                .tokenData(OauthToken.builder()
                        .token(GITHUB_ACCESS_TOKEN)
                        .refreshToken(GITHUB_REFRESH_TOKEN)
                        .createdAt(new Date().getTime())
                        .build())
                .build();
        InventoryService inventoryService = new InMemoryInventoryService(
                InMemoryInventoryService.Inventory.builder()
                        .oauthToken(
                                TENANT_ID,
                                INTEGRATION_ID,
                                APPLICATION,
                                StringUtils.firstNonBlank(GITHUB_URL, DEFAULT_GITHUB_URL),
                                Collections.emptyMap(),
                                GITHUB_ACCESS_TOKEN,
                                GITHUB_REFRESH_TOKEN,
                                null)
                        .build());
        preflightCheck = new GithubPreflightCheck(inventoryService, DefaultObjectMapper.get(), client);
    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }


    @Test
    public void getScmRepositories() throws Exception {
        List<ScmRepository> scmRepositories = preflightCheck.getScmRepositories(TENANT_ID, integration, List.of(),1, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);

        scmRepositories = preflightCheck.getScmRepositories(TENANT_ID, integration, List.of(),2, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);

        scmRepositories = preflightCheck.searchScmRepository(TENANT_ID, integration, "commons", null, 0, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);

        System.out.println(preflightCheck.getTotalRepositoriesCount(TENANT_ID, integration, null, null));
        System.out.println(preflightCheck.getTotalRepositoriesCount(TENANT_ID, integration, "commons", null));
    }

    @Test
    public void name() {
        GithubPreflightCheck githubPreflightCheck = new GithubPreflightCheck(null, null,null);

        PreflightCheckResults r = githubPreflightCheck.check("foo",
                Integration.builder()
                        .build(),
                Token.builder()
                        .tokenData(ApiKey.builder()
                                .apiKey(System.getenv("API_KEY"))
                                .build())
                        .build());

        DefaultObjectMapper.prettyPrint(r);
    }
}