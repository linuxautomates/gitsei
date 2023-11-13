package io.levelops.integrations.sonarqube.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.sonarqube.models.*;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "sonarqube1";
    private static final String APPLICATION = "sonarqube";
    private static final String SONARQUBE_URL = System.getenv("SONARQUBE_URL");
    private static final String SONARQUBE_API_KEY = System.getenv("SONARQUBE_API_KEY");
    private static final String PROJECT_KEY = System.getenv("PROJECT_KEY");
    private static final String PULL_REQUEST_ID = System.getenv("PULL_REQUEST_ID");
    private static final String SONARQUBE_USER_GROUP_OWNER = System.getenv("SONARQUBE_USER_GROUP_OWNER");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey
            .builder().integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private SonarQubeClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, SONARQUBE_URL,
                        Collections.emptyMap(),"", SONARQUBE_API_KEY).build());
        clientFactory = SonarQubeClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .pageSize(20)
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getProjects() throws SonarQubeClientException {
        ProjectResponse projectResponse = clientFactory.get(TEST_INTEGRATION_KEY).getProjects(true, 1);
        DefaultObjectMapper.prettyPrint(projectResponse);
        assertThat(projectResponse).isNotNull();
        assertThat(projectResponse.getProjects()).isNotNull();
    }

    @Test
    public void getIssuesTest() throws SonarQubeClientException {
        IssueResponse issueResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getIssues(PROJECT_KEY, PULL_REQUEST_ID, null,null, null, null, null, null, 1);
        DefaultObjectMapper.prettyPrint(issueResponse);
        assertThat(issueResponse).isNotNull();
    }

    @Test
    public void getProjectAnalyses() throws SonarQubeClientException {
        ProjectAnalysesResponse projectAnalysesResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getProjectAnalyses(PROJECT_KEY, Date.from(Instant.now().minus(Duration.ofDays(90))),Date.from(Instant.now()),  1);
        DefaultObjectMapper.prettyPrint(projectAnalysesResponse);
        assertThat(projectAnalysesResponse).isNotNull();
        assertThat(projectAnalysesResponse.getAnalyses()).isNotNull();
    }

    @Test
    public void getProjectBranches() throws SonarQubeClientException {
        ProjectBranchResponse projectBranchResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getProjectBranches(PROJECT_KEY).toBuilder().build();
        DefaultObjectMapper.prettyPrint(projectBranchResponse);
        assertThat(projectBranchResponse).isNotNull();
        assertThat(projectBranchResponse.getBranches()).isNotNull();
    }

    @Test
    public void getPullRequest() throws SonarQubeClientException {
        PullRequestResponse pullRequestResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getPullRequests(PROJECT_KEY).toBuilder().build();
        DefaultObjectMapper.prettyPrint(pullRequestResponse);
        assertThat(pullRequestResponse).isNotNull();
        assertThat(pullRequestResponse.getPullRequests()).isNotNull();
    }

    @Test
    public void getQualityGates() throws SonarQubeClientException {
        QualityGateResponse qualityGateResponse = clientFactory.get(TEST_INTEGRATION_KEY).getQualityGates();
        DefaultObjectMapper.prettyPrint(qualityGateResponse);
        assertThat(qualityGateResponse).isNotNull();
    }

    @Test
    public void getUserGroups() throws SonarQubeClientException {
        UserGroupResponse userGroupResponse = clientFactory.get(TEST_INTEGRATION_KEY).getUserGroups(1);
        DefaultObjectMapper.prettyPrint(userGroupResponse);
        assertThat(userGroupResponse).isNotNull();
    }

    @Test
    public void getUsers() throws SonarQubeClientException {
        UserResponse userResponse = clientFactory.get(TEST_INTEGRATION_KEY).getUsers(SONARQUBE_USER_GROUP_OWNER,1);
        DefaultObjectMapper.prettyPrint(userResponse);
        assertThat(userResponse).isNotNull();
    }
}
