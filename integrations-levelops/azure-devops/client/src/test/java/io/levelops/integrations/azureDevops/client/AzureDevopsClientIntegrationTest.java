package io.levelops.integrations.azureDevops.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import io.levelops.integrations.azureDevops.models.Branch;
import io.levelops.integrations.azureDevops.models.BuildResponse;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ChangeSetChange;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitem;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.Label;
import io.levelops.integrations.azureDevops.models.PipelineResponse;
import io.levelops.integrations.azureDevops.models.ProjectProperty;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.ReleaseResponse;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.RunResponse;
import io.levelops.integrations.azureDevops.models.Tag;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.TeamSetting;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.models.WorkItemHistory;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import io.levelops.integrations.azureDevops.models.WorkItemType;
import io.levelops.integrations.azureDevops.models.WorkItemTypeCategory;
import io.levelops.integrations.azureDevops.models.WorkItemTypeState;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureDevopsClientIntegrationTest {


    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "azuredevops1";
    private static final String APPLICATION = "azuredevops";
    private static final String ORGANIZATION = System.getenv("ORGANIZATION");
    private static final String AZURE_DEVOPS_URL = StringUtils.firstNonBlank(System.getenv("AZURE_DEVOPS_URL"), "https://dev.azure.com");
    private static final String AZURE_DEVOPS_PROJECT = System.getenv("AZURE_DEVOPS_PROJECT");
    private static final String AZURE_DEVOPS_ACCESS_TOKEN = System.getenv("AZURE_DEVOPS_ACCESS_TOKEN");
    private static final String AZURE_DEVOPS_REFRESH_TOKEN = System.getenv("AZURE_DEVOPS_REFRESH_TOKEN");
    private static final String AZURE_DEVOPS_REPOSITORY_ID = System.getenv("AZURE_DEVOPS_REPOSITORY_ID");
    private static final String AZURE_DEVOPS_CHANGESETS_ID = System.getenv("AZURE_DEVOPS_CHANGESETS_ID");
    private static final String AZURE_DEVOPS_TEAM = System.getenv("AZURE_DEVOPS_TEAM");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey
            .builder().integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private AzureDevopsClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();

        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, AZURE_DEVOPS_URL,
                        (StringUtils.isNotEmpty(ORGANIZATION) ? Map.of("organization", ORGANIZATION) : Map.of()),
                        AZURE_DEVOPS_ACCESS_TOKEN, AZURE_DEVOPS_REFRESH_TOKEN, null)
                .build());
        clientFactory = AzureDevopsClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .pageSize(1)
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getTeams() throws AzureDevopsClientException {
        List<Team> teams = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTeams(ORGANIZATION, AZURE_DEVOPS_PROJECT, 0);
        DefaultObjectMapper.prettyPrint(teams);
        assertThat(teams).isNotNull();
        assertThat(teams).isNotNull();
    }

    @Test
    public void getTeamsSetting() throws AzureDevopsClientException {
        TeamSetting teamSetting = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTeamSettings(ORGANIZATION, AZURE_DEVOPS_PROJECT, AZURE_DEVOPS_TEAM);
        assertThat(teamSetting).isNotNull();
    }

    @Test
    public void getPipelines() throws AzureDevopsClientException {
        PipelineResponse pipelineResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getPipelines(ORGANIZATION, AZURE_DEVOPS_PROJECT, "");
        DefaultObjectMapper.prettyPrint(pipelineResponse);
        assertThat(pipelineResponse).isNotNull();
        assertThat(pipelineResponse.getPipelines()).isNotNull();
    }

    @Test
    public void getRuns() throws AzureDevopsClientException {
        RunResponse runResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getRuns(ORGANIZATION, AZURE_DEVOPS_PROJECT, 1);
        DefaultObjectMapper.prettyPrint(runResponse);
        assertThat(runResponse).isNotNull();
        assertThat(runResponse.getRuns()).isNotNull();
        if(runResponse.getRuns().size() > 0) {
            List<AzureDevopsPipelineRunStageStep> stageSteps = clientFactory.get(TEST_INTEGRATION_KEY)
                    .getBuildTimeline(ORGANIZATION, AZURE_DEVOPS_PROJECT, runResponse.getRuns().get(0).getId());
            DefaultObjectMapper.prettyPrint(stageSteps);
            assertThat(stageSteps).isNotNull();
        }
    }

    @Test
    public void getReleases() throws AzureDevopsClientException {
        ReleaseResponse releaseResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getReleases(ORGANIZATION, AZURE_DEVOPS_PROJECT, "");
        DefaultObjectMapper.prettyPrint(releaseResponse);
        assertThat(releaseResponse).isNotNull();
        assertThat(releaseResponse.getReleases()).isNotNull();
    }

    @Test
    public void getBuilds() throws AzureDevopsClientException {
        BuildResponse buildResponse = clientFactory.get(TEST_INTEGRATION_KEY)
                .getBuilds(ORGANIZATION, AZURE_DEVOPS_PROJECT, Date.from(Instant.now().minus(Duration.ofDays(90))),
                        Date.from(Instant.now()), "");
        DefaultObjectMapper.prettyPrint(buildResponse);
        assertThat(buildResponse).isNotNull();
        assertThat(buildResponse.getBuilds()).isNotNull();
    }

    @Test
    public void getRepositories() throws AzureDevopsClientException {
        List<Repository> repositories = clientFactory.get(TEST_INTEGRATION_KEY)
                .getRepositories(ORGANIZATION, AZURE_DEVOPS_PROJECT);
        DefaultObjectMapper.prettyPrint(repositories);
        assertThat(repositories).isNotNull();
    }

    @Test
    public void getCommits() throws AzureDevopsClientException {
        List<Commit> commits = clientFactory.get(TEST_INTEGRATION_KEY)
                .getCommits(ORGANIZATION, AZURE_DEVOPS_PROJECT, "", AZURE_DEVOPS_REPOSITORY_ID,
                        null, null, 0);
        DefaultObjectMapper.prettyPrint(commits);
        assertThat(commits).isNotNull();
        getCommitsChanges(commits.get(0).getCommitId());
    }

    public void getCommitsChanges(String commitId) throws AzureDevopsClientException {
        List<Change> changes = clientFactory.get(TEST_INTEGRATION_KEY)
                .getChanges(ORGANIZATION, AZURE_DEVOPS_PROJECT, AZURE_DEVOPS_REPOSITORY_ID,
                        commitId, 0);
        DefaultObjectMapper.prettyPrint(changes);
        assertThat(changes).isNotNull();
    }

    @Test
    public void getPullRequests() throws AzureDevopsClientException {
        List<PullRequest> pullRequests = clientFactory.get(TEST_INTEGRATION_KEY)
                .getPullRequests(ORGANIZATION, AZURE_DEVOPS_PROJECT, AZURE_DEVOPS_REPOSITORY_ID, 0);
        DefaultObjectMapper.prettyPrint(pullRequests);
        assertThat(pullRequests).isNotNull();
    }

    @Test
    public void getProjects() throws AzureDevopsClientException {
        ProjectResponse projectResponse = clientFactory.get(TEST_INTEGRATION_KEY).getProjects(ORGANIZATION, "");
        DefaultObjectMapper.prettyPrint(projectResponse);
        assertThat(projectResponse).isNotNull();
        assertThat(projectResponse.getProjects()).isNotNull();
    }

    @Test
    public void getProjectProperties() throws AzureDevopsClientException {
        ProjectResponse projectResponse = clientFactory.get(TEST_INTEGRATION_KEY).getProjects(ORGANIZATION, "");
        String projectId = projectResponse.getProjects().get(0).getId();
        List<ProjectProperty> projectProperties = clientFactory.get(TEST_INTEGRATION_KEY)
                .getProjectProperties(ORGANIZATION, projectId);
        DefaultObjectMapper.prettyPrint(projectProperties);
        assertThat(projectProperties).isNotNull();
    }

    @Test
    public void getWorkItemQuery() throws AzureDevopsClientException {
        WorkItemQueryResult workItemQueryResult = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemQuery(ORGANIZATION, AZURE_DEVOPS_PROJECT,
                        Date.from(Instant.now().minus(Duration.ofDays(90))), Date.from(Instant.now()));
        DefaultObjectMapper.prettyPrint(workItemQueryResult);
        assertThat(workItemQueryResult).isNotNull();
    }

    @Test
    public void getChangesets() throws AzureDevopsClientException {
        List<ChangeSet> changeSets = clientFactory.get(TEST_INTEGRATION_KEY)
                .getChangesets(ORGANIZATION, AZURE_DEVOPS_PROJECT, null, 0);
        assertThat(changeSets).isNotNull();
    }

    @Test
    public void getChangesetChanges() throws AzureDevopsClientException {
        List<ChangeSetChange> changeSetChanges = clientFactory.get(TEST_INTEGRATION_KEY)
                .getChangesetChanges(ORGANIZATION, AZURE_DEVOPS_CHANGESETS_ID, 0);
        assertThat(changeSetChanges).isNotNull();
    }

    @Test
    public void getChangesetWorkItems() throws AzureDevopsClientException {
        List<ChangeSetWorkitem> changeSetWorkitems = clientFactory.get(TEST_INTEGRATION_KEY)
                .getChangesetWorkitems(ORGANIZATION, AZURE_DEVOPS_CHANGESETS_ID);
        assertThat(changeSetWorkitems).isNotNull();
    }

    @Test
    public void getBranches() throws AzureDevopsClientException {
        List<Branch> branches = clientFactory.get(TEST_INTEGRATION_KEY)
                .getBranches(ORGANIZATION, AZURE_DEVOPS_PROJECT);
        assertThat(branches).isNotNull();
    }

    @Test
    public void getLabels() throws AzureDevopsClientException {
        List<Label> labels = clientFactory.get(TEST_INTEGRATION_KEY)
                .getLabels(ORGANIZATION, AZURE_DEVOPS_PROJECT, 0);
        assertThat(labels).isNotNull();
    }

    @Test
    public void getWorkItemQueryOneDay() throws AzureDevopsClientException {
        WorkItemQueryResult workItemQueryResult = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemQuery(ORGANIZATION, AZURE_DEVOPS_PROJECT,
                        Date.from(Instant.now().minus(Duration.ofDays(1))), Date.from(Instant.now()));
        DefaultObjectMapper.prettyPrint(workItemQueryResult);
        assertThat(workItemQueryResult).isNotNull();
    }

    @Test
    public void getWorkItemQueryLessThanOneDay() throws AzureDevopsClientException {
        WorkItemQueryResult workItemQueryResult = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemQuery(ORGANIZATION, AZURE_DEVOPS_PROJECT,
                        Date.from(Instant.now().minusSeconds(900)), Date.from(Instant.now()));
        DefaultObjectMapper.prettyPrint(workItemQueryResult);
        assertThat(workItemQueryResult).isNotNull();
    }

    @Test
    public void getWorkItemQueryForOneId() throws AzureDevopsClientException {
        List<WorkItem> workItems = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItems(ORGANIZATION, AZURE_DEVOPS_PROJECT, List.of("108"));
        DefaultObjectMapper.prettyPrint(workItems);
        assertThat(workItems).isNotNull();
    }

    @Test
    public void getWorkItemsUpdates() throws AzureDevopsClientException {
        List<WorkItemHistory> workItemHistory = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemsUpdates(ORGANIZATION, AZURE_DEVOPS_PROJECT, "105");
        DefaultObjectMapper.prettyPrint(workItemHistory);
        assertThat(workItemHistory).isNotNull();
    }

    @Test
    public void getWorkItemTypeCategories() throws AzureDevopsClientException {
        List<WorkItemTypeCategory> workItemTypeCategories = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemTypeCategories(ORGANIZATION, AZURE_DEVOPS_PROJECT);
        DefaultObjectMapper.prettyPrint(workItemTypeCategories);
        assertThat(workItemTypeCategories).isNotNull();
    }

    @Test
    public void getWorkItemTypes() throws AzureDevopsClientException {
        List<WorkItemType> workItemTypes = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemTypes(ORGANIZATION, AZURE_DEVOPS_PROJECT);
        DefaultObjectMapper.prettyPrint(workItemTypes);
        assertThat(workItemTypes).isNotNull();
    }

    @Test
    public void getWorkItemTypeStates() throws AzureDevopsClientException {
        List<WorkItemTypeState> workItemTypeStates = clientFactory.get(TEST_INTEGRATION_KEY)
                .getWorkItemTypeStates(ORGANIZATION, AZURE_DEVOPS_PROJECT, "Bug");
        DefaultObjectMapper.prettyPrint(workItemTypeStates);
        assertThat(workItemTypeStates).isNotNull();
    }

    @Test
    public void getTags() throws AzureDevopsClientException {
        List<Tag> tags = clientFactory.get(TEST_INTEGRATION_KEY)
                .getTags(ORGANIZATION, AZURE_DEVOPS_PROJECT, "Bug", null).getTags();
        DefaultObjectMapper.prettyPrint(tags);
        assertThat(tags).isNotNull();
    }
}
