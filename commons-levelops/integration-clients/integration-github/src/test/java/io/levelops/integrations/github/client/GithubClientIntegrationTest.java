package io.levelops.integrations.github.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflow;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Ignore
public class GithubClientIntegrationTest {

    public static final int PER_PAGE = 10;
    private static final String TENANT_ID = "foo";
    private static final String INTEGRATION_ID = "test";
    private static final String APPLICATION = "github_actions";

    private static String GITHUB_ACTIONS_TOKEN = System.getenv("GITHUB_ACTIONS_TOKEN");
    private static String GITHUB_ACTIONS_URL = System.getenv("GITHUB_ACTIONS_URL");
    private static final String ORGANIZATION_NAME = System.getenv("GITHUB_ACTIONS_ORGANIZATION");
    private static final String REPOSITORY_FULL_NAME = System.getenv("GITHUB_ACTIONS_REPOSITORY_FULL_NAME");
    private static final String WORKFLOW_RUN_ID = System.getenv("GITHUB_ACTIONS_WORKFLOW_RUN_ID");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private GithubClientFactory clientFactory;

    @Before
    public void setup() {

        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, GITHUB_ACTIONS_URL, Collections.emptyMap(),
                        ORGANIZATION_NAME, GITHUB_ACTIONS_TOKEN)
                .build());

        clientFactory = new GithubClientFactory(inventoryService, DefaultObjectMapper.get(), new OkHttpClient(), 10);
    }

    @Test
    public void organizations() throws GithubClientException {
        List<GithubOrganization> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamOrganizations()
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint("Organizations: " + response);
    }

    @Test
    public void repos() throws GithubClientException {
        List<GithubRepository> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamRepositories(ORGANIZATION_NAME)
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint("Repositories: " + response);
    }

    @Test
    public void workflowRuns() throws GithubClientException {
        List<GithubActionsWorkflowRun> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamWorkflowRuns(REPOSITORY_FULL_NAME, PER_PAGE)
                .limit(2)
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint("WorkflowRuns: " + response);
    }

    @Test
    public void workflowRunJobs() throws GithubClientException {
        List<GithubActionsWorkflowRunJob> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamWorkflowRunJobs(REPOSITORY_FULL_NAME, Long.parseLong(WORKFLOW_RUN_ID), PER_PAGE)
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint("Workflow Run Jobs: " + response);
    }

    @Test
    public void workflows() throws GithubClientException {
        List<GithubActionsWorkflow> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamWorkflows(REPOSITORY_FULL_NAME, PER_PAGE)
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint("Workflows: " + response);
    }
}
