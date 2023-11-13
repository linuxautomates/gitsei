package io.levelops.integrations.gitlab.preflightcheck;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.OauthToken;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.ScmRepository;
import io.levelops.preflightchecks.GitlabPreflightCheck;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabPreflightCheckIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String GITLAB_URL = System.getenv("GITLAB_URL");
    private static final String GITLAB_ACCESS_TOKEN = System.getenv("GITLAB_ACCESS_TOKEN");
    private static final String GITLAB_REFRESH_TOKEN = System.getenv("GITLAB_REFRESH_TOKEN");
    private static final String DEFAULT_GITLAB_URL = "https://gitlab.com";
    private static final String INTEGRATION_ID = "gitlab1";
    private static final String APPLICATION = "gitlab";

    private GitlabPreflightCheck preflightCheck;
    private Integration integration;
    private Token token;

    @Before
    public void setup() {
        var client = new OkHttpClient().newBuilder().followRedirects(true).build();

        integration = Integration.builder()
                .id(INTEGRATION_ID)
                .application("gitlab")
                .name("test")
                .status("ACTIVE")
                .url(StringUtils.firstNonBlank(GITLAB_URL, DEFAULT_GITLAB_URL))
                .satellite(false)
                .build();
        token = Token.builder()
                .id("test")
                .integrationId("gitlab")
                .tokenData(OauthToken.builder()
                        .token(GITLAB_ACCESS_TOKEN)
                        .refreshToken(GITLAB_REFRESH_TOKEN)
                        .createdAt(new Date().getTime())
                        .build())
                .build();
        InventoryService inventoryService = new InMemoryInventoryService(
                InMemoryInventoryService.Inventory.builder()
                        .oauthToken(
                                TENANT_ID,
                                INTEGRATION_ID,
                                APPLICATION,
                                StringUtils.firstNonBlank(GITLAB_URL, DEFAULT_GITLAB_URL),
                                Collections.emptyMap(),
                                GITLAB_ACCESS_TOKEN,
                                GITLAB_REFRESH_TOKEN,
                                null)
                        .build());
        preflightCheck = new GitlabPreflightCheck(DefaultObjectMapper.get(), client, inventoryService);

    }

    @Test
    public void check() {
        PreflightCheckResults results = preflightCheck.check(TENANT_ID, integration, token);
        assertThat(results.isSuccess()).isTrue();
    }

    @Test
    public void getScmRepositories() throws Exception {
        List<ScmRepository> scmRepositories = preflightCheck.getScmRepositories(TENANT_ID, integration, List.of(), 1, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);
        scmRepositories = preflightCheck.getScmRepositories(TENANT_ID, integration, List.of(), 2, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);

        scmRepositories = preflightCheck.searchScmRepository(TENANT_ID, integration, "Recover", "", 1, 2);
        DefaultObjectMapper.prettyPrint(scmRepositories);
    }
}
