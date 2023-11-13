package io.levelops.integrations.github.client;

import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflow;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Ignore
public class GithubClientResilienceTest {
    public static final int PAGE = 1;
    public static final int PAGE_SIZE = 10;
    private static final String ORGANIZATION_NAME = System.getenv("GITHUB_ACTIONS_ORGANIZATION");
    private static final String REPOSITORY_NAME = System.getenv("GITHUB_ACTIONS_REPOSITORY_NAME");
    private static final String REPOSITORY_FULL_NAME = System.getenv("GITHUB_ACTIONS_REPOSITORY_FULL_NAME");
    private static final String WORKFLOW_RUN_ID = System.getenv("GITHUB_ACTIONS_WORKFLOW_RUN_ID");

    public static GithubClient client;


    @Before
    public void setup() throws GithubClientException {
        client = Mockito.mock(GithubClient.class);

        when(client.streamOrganizations()).thenCallRealMethod();
        when(client.streamAppInstallationOrgs()).thenCallRealMethod();
        when(client.getOrganizations(PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));
        when(client.getAppInstallationOrgs(PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));

        when(client.streamRepositories(ORGANIZATION_NAME)).thenCallRealMethod();
        when(client.streamRepositories(ORGANIZATION_NAME, true)).thenCallRealMethod();
        when(client.streamRepositories(ORGANIZATION_NAME, false)).thenCallRealMethod();
        when(client.streamInstallationRepositories()).thenCallRealMethod();
        when(client.getRepositories(ORGANIZATION_NAME, false, PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));
        when(client.getRepository(REPOSITORY_NAME)).thenThrow(new GithubClientException("Not Authorised"));
        when(client.getInstallationRepositories(PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));

        when(client.streamWorkflowRuns(REPOSITORY_FULL_NAME, PAGE_SIZE)).thenCallRealMethod();
        when(client.streamWorkflowRunJobs(REPOSITORY_FULL_NAME, Long.parseLong(WORKFLOW_RUN_ID), PAGE_SIZE)).thenCallRealMethod();
        when(client.streamWorkflows(REPOSITORY_FULL_NAME, PAGE_SIZE)).thenCallRealMethod();
        when(client.getWorkflowRuns(REPOSITORY_FULL_NAME, PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));
        when(client.getWorkflowRunJobs(REPOSITORY_FULL_NAME, Long.parseLong(WORKFLOW_RUN_ID), PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));
        when(client.getWorkflows(REPOSITORY_FULL_NAME, PAGE, PAGE_SIZE)).thenThrow(new GithubClientException("Not Authorised"));
    }

    @Test
    public void testResilience() {
        List<GithubOrganization> organizations = client.streamOrganizations().collect(Collectors.toList());
        assertThat(organizations).hasSize(0);

        organizations = client.streamAppInstallationOrgs().collect(Collectors.toList());
        assertThat(organizations).hasSize(0);

        List<GithubRepository> repositories = client.streamRepositories(ORGANIZATION_NAME).collect(Collectors.toList());
        assertThat(repositories).hasSize(0);

        repositories = client.streamRepositories(ORGANIZATION_NAME, true).collect(Collectors.toList());
        assertThat(repositories).hasSize(0);

        repositories = client.streamRepositories(ORGANIZATION_NAME, false).collect(Collectors.toList());
        assertThat(repositories).hasSize(0);

        repositories = client.streamInstallationRepositories().collect(Collectors.toList());
        assertThat(repositories).hasSize(0);

        List<GithubActionsWorkflowRun> workflowRuns = client.streamWorkflowRuns(REPOSITORY_FULL_NAME, PAGE_SIZE).collect(Collectors.toList());
        assertThat(workflowRuns).hasSize(0);

        List<GithubActionsWorkflowRunJob> workflowRunJobs = client.streamWorkflowRunJobs(REPOSITORY_FULL_NAME, Long.parseLong(WORKFLOW_RUN_ID), PAGE_SIZE).collect(Collectors.toList());
        assertThat(workflowRunJobs).hasSize(0);

        List<GithubActionsWorkflow> workflows = client.streamWorkflows(REPOSITORY_FULL_NAME, PAGE_SIZE).collect(Collectors.toList());
        assertThat(workflows).hasSize(0);
    }
}
