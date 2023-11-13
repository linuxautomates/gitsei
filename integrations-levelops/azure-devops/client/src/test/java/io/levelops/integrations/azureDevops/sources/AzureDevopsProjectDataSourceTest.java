package io.levelops.integrations.azureDevops.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientFactory;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.models.AzureDevopsRelease;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseEnvironment;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseStep;
import io.levelops.integrations.azureDevops.models.Build;
import io.levelops.integrations.azureDevops.models.BuildChange;
import io.levelops.integrations.azureDevops.models.BuildChangeResponse;
import io.levelops.integrations.azureDevops.models.BuildResponse;
import io.levelops.integrations.azureDevops.models.BuildTimelineResponse;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ChangeSetChange;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitem;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.CommitChangesResponse;
import io.levelops.integrations.azureDevops.models.CommitResponse;
import io.levelops.integrations.azureDevops.models.Configuration;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Item;
import io.levelops.integrations.azureDevops.models.Label;
import io.levelops.integrations.azureDevops.models.Pipeline;
import io.levelops.integrations.azureDevops.models.PipelineResponse;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.ProjectProperty;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.PullRequestResponse;
import io.levelops.integrations.azureDevops.models.ReleaseResponse;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.RepositoryResponse;
import io.levelops.integrations.azureDevops.models.Run;
import io.levelops.integrations.azureDevops.models.RunResponse;
import io.levelops.integrations.azureDevops.models.Tag;
import io.levelops.integrations.azureDevops.models.TagResponse;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.BUILDS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.CHANGESETS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.COMMITS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.PIPELINE_RUNS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.PULL_REQUESTS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.RELEASES;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.TAGS;
import static io.levelops.integrations.azureDevops.sources.AzureDevopsProjectDataSource.Enrichment.WORK_ITEMS;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class AzureDevopsProjectDataSourceTest {

    public static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY)
            .tenantId(EMPTY)
            .build();
    private static final Date from = Date.from(Instant.now().minus(Duration.ofDays(2)));
    private static final Date to = Date.from(Instant.now());
    public AzureDevopsProjectDataSource azureDevopsPipelineRunsDataSource;
    public AzureDevopsProjectDataSource azureDevopsBuildsDataSource;
    public AzureDevopsProjectDataSource azureDevopsReleaseDataSource;
    public AzureDevopsProjectDataSource azureDevopsCommitsDataSource;
    public AzureDevopsProjectDataSource azureDevopsPullRequestDataSource;
    public AzureDevopsProjectDataSource azureDevopsWorkItemsDataSource;
    public AzureDevopsProjectDataSource azureDevopsChangesetDataSource;
    public AzureDevopsProjectDataSource azureDevopsTagsDataSource;
    public AzureDevopsClient azureDevopsClient;
    public ObjectMapper mapper;

    @Before
    public void setup() throws AzureDevopsClientException, IOException {
        azureDevopsClient = Mockito.mock(AzureDevopsClient.class);
        AzureDevopsClientFactory azureDevopsClientFactory = Mockito.mock(AzureDevopsClientFactory.class);
        IngestionCachingService ingestionCachingService = Mockito.mock(IngestionCachingService.class);
        when(ingestionCachingService.isEnabled()).thenReturn(false);
        mapper = DefaultObjectMapper.get();
        
        azureDevopsPipelineRunsDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(PIPELINE_RUNS));
        azureDevopsBuildsDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(BUILDS));
        azureDevopsReleaseDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(RELEASES));
        azureDevopsCommitsDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(COMMITS));
        azureDevopsPullRequestDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(PULL_REQUESTS));
        azureDevopsWorkItemsDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(WORK_ITEMS));
        azureDevopsChangesetDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(CHANGESETS));
        azureDevopsTagsDataSource = new AzureDevopsProjectDataSource(azureDevopsClientFactory, ingestionCachingService, EnumSet.of(TAGS));

        when(azureDevopsClientFactory.get(eq(TEST_KEY))).thenReturn(azureDevopsClient);

        setupOrganization();

        setupProjects();

        setupRepos();

        setupPipelineRuns();

        setupBuilds();

        setupCommits();

        setupPullRequests();

        setupWorkitems();

        setupChangesets();

        setupTags();

        setupBuildStageSteps();

        setupBuildStepLogs();

        setUpReleases();

        setUpReleaseDetails();

    }

    private void setupOrganization() {
        List<String> orgs = List.of("org-1", "org-2");
        List<String> orgProjects = List.of("org-1/project-test-1", "org-1/project-test-3", "org-2/project-test-2");
        when(azureDevopsClient.getQualifiedProjects()).thenReturn(orgProjects);
        when(azureDevopsClient.getOrganizations()).thenReturn(orgs);
    }

    private void setupRepos() throws AzureDevopsClientException {
        List<Repository> repositories1 = List.of(
                Repository.builder().id("1")
                        .project(Project.builder()
                                .name("project-test-1")
                                .id("1")
                                .lastUpdateTime(String.valueOf(Date.from(Instant.now().minus(Duration.ofDays(10)))))
                                .organization("org-1")
                                .build())
                        .build());
        RepositoryResponse response1 = RepositoryResponse.builder()
                .repositories(repositories1)
                .build();
        when(azureDevopsClient.getRepositories(eq("org-1"),
                eq("1"))).thenReturn(response1.getRepositories());
        List<Repository> repositories2 = List.of(
                Repository.builder().id("2")
                        .project(Project.builder()
                                .name("project-test-2")
                                .id("2")
                                .lastUpdateTime(String.valueOf(Date.from(Instant.now().minus(Duration.ofDays(10)))))
                                .organization("org-2")
                                .build())
                        .build());
        RepositoryResponse response2 = RepositoryResponse.builder()
                .repositories(repositories2)
                .build();
        when(azureDevopsClient.getRepositories(eq("org-2"),
                eq("2"))).thenReturn(response2.getRepositories());
        when(azureDevopsClient.getRepositories(eq("org-1"),
                eq("3"))).thenReturn(response1.getRepositories());
    }

    private void setupProjects() throws AzureDevopsClientException {
        List<Project> projects1 = List.of(
                Project.builder()
                        .name("project-test-1")
                        .id("1")
                        .lastUpdateTime(String.valueOf(Date.from(Instant.now().minus(Duration.ofDays(10)))))
                        .organization("org-1")
                        .build(),
                Project.builder()
                        .name("project-test-3")
                        .id("3")
                        .lastUpdateTime(String.valueOf(Date.from(Instant.now().minus(Duration.ofDays(10)))))
                        .organization("org-1")
                        .build());
        List<Project> projects2 = List.of(
                Project.builder()
                        .name("project-test-2")
                        .id("2")
                        .lastUpdateTime(String.valueOf(Date.from(Instant.now().minus(Duration.ofDays(10)))))
                        .organization("org-2")
                        .build());
        ProjectResponse projectResponse1 = ProjectResponse.builder()
                .projects(projects1)
                .continuationToken(null)
                .build();
        when(azureDevopsClient.getProjects(eq("org-1"), eq(""))).thenReturn(projectResponse1);
        ProjectResponse projectResponse2 = ProjectResponse.builder()
                .projects(projects2)
                .continuationToken(null)
                .build();
        when(azureDevopsClient.getProjects(eq("org-2"), eq(""))).thenReturn(projectResponse2);

        List<ProjectProperty> properties1 = List.of(
                ProjectProperty.builder()
                        .name("System.SourceControlGitEnabled")
                        .value("true")
                        .build());
        List<ProjectProperty> properties2 = List.of(
                ProjectProperty.builder()
                        .name("System.SourceControlGitEnabled")
                        .value("true")
                        .build());
        List<ProjectProperty> properties3 = List.of(
                ProjectProperty.builder()
                        .name("System.SourceControlTfvcEnabled")
                        .value("true")
                        .build());
        when(azureDevopsClient.getProjectProperties(eq("org-1"), eq("1"))).thenReturn(properties1);
        when(azureDevopsClient.getProjectProperties(eq("org-2"), eq("2"))).thenReturn(properties2);
        when(azureDevopsClient.getProjectProperties(eq("org-1"), eq("3"))).thenReturn(properties3);
    }

    private void setupChangesets() throws AzureDevopsClientException {
        List<ChangeSet> changeSets = List.of(
                ChangeSet.builder()
                        .changesetId(1)
                        .build(),
                ChangeSet.builder()
                        .changesetId(2)
                        .build());
        when(azureDevopsClient.getChangesets(eq("org-1"), eq("project-test-3"), eq(from), eq(0))).thenReturn(changeSets);

        // changeset changes
        List<ChangeSetChange> changeSetChanges1 = List.of(
                ChangeSetChange.builder()
                        .changeType("add")
                        .item(Item.builder().version("1").build())
                        .build());
        List<ChangeSetChange> changeSetChanges2 = List.of(
                ChangeSetChange.builder()
                        .changeType("edit")
                        .item(Item.builder().version("2").build())
                        .build());
        when(azureDevopsClient.getChangesetChanges(eq("org-1"), eq("1"), eq(0))).thenReturn(changeSetChanges1);
        when(azureDevopsClient.getChangesetChanges(eq("org-1"), eq("2"), eq(0))).thenReturn(changeSetChanges2);

        // changeset workitems
        List<ChangeSetWorkitem> changeSetWorkitems1 = List.of(
                ChangeSetWorkitem.builder()
                        .id(10)
                        .build());

        List<ChangeSetWorkitem> changeSetWorkitems2 = List.of(
                ChangeSetWorkitem.builder()
                        .id(20)
                        .build());
        when(azureDevopsClient.getChangesetWorkitems(eq("org-1"), eq("1"))).thenReturn(changeSetWorkitems1);
        when(azureDevopsClient.getChangesetWorkitems(eq("org-1"), eq("2"))).thenReturn(changeSetWorkitems2);
    }

    private void setupTags() throws AzureDevopsClientException {
        List<Tag> tags = List.of(
                Tag.builder()
                        .name("first")
                        .build(),
                Tag.builder()
                        .name("second")
                        .build());
        Tag.GitUserDate taggedBy = Tag.GitUserDate.builder().name("abc").build();
        Tag.GitTaggedObject taggedObject = Tag.GitTaggedObject.builder().objectId("1234").build();
        TagResponse tagResponse = TagResponse.builder().tags(tags).build();
        when(azureDevopsClient.getAnnotatedTags(eq("org-1"), any(), any(), any())).
                thenReturn(Optional.of(Tag.builder().taggedBy(taggedBy).taggedObject(taggedObject).build()));
        when(azureDevopsClient.getAnnotatedTags(eq("org-2"), any(), any(), any())).
                thenReturn(Optional.of(Tag.builder().taggedBy(taggedBy).taggedObject(taggedObject).build()));
        when(azureDevopsClient.getTags(any(), any(), any(), eq(""))).thenReturn(tagResponse);
    }

    private void setupWorkitems() throws AzureDevopsClientException {
        List<WorkItem> workItems1 = List.of(
                WorkItem.builder().id("1").rev(123).build()
        );
        List<WorkItem> workItems2 = List.of(
                WorkItem.builder().id("2").rev(345).build()
        );
        List<WorkItemQueryResult.WorkItemReference> workItemQueryResultReference1 = List.of(WorkItemQueryResult.WorkItemReference.builder().id(1).build());
        WorkItemQueryResult workItemQueryResult1 = WorkItemQueryResult.builder().workItems(workItemQueryResultReference1).build();
        List<WorkItemQueryResult.WorkItemReference> workItemQueryResultReference2 = List.of(WorkItemQueryResult.WorkItemReference.builder().id(2).build());
        WorkItemQueryResult workItemQueryResult2 = WorkItemQueryResult.builder().workItems(workItemQueryResultReference2).build();
        when(azureDevopsClient.getWorkItems(eq("org-1"), eq("1"), eq(List.of("1"))))
                .thenReturn(workItems1);
        when(azureDevopsClient.getWorkItems(eq("org-2"), eq("2"), eq(List.of("2"))))
                .thenReturn(workItems2);
        when(azureDevopsClient.getWorkItems(eq("org-1"), eq("3"), eq(List.of("1"))))
                .thenReturn(workItems1);
        when(azureDevopsClient.getTeams(eq("org-1"), eq("1"), eq(0)))
                .thenReturn(List.of(Team.builder().name("project-test-1 Team").id("1").projectName("project-test-1").build()));
        when(azureDevopsClient.getTeams(eq("org-2"), eq("2"), eq(0)))
                .thenReturn(List.of(Team.builder().name("project-test-2 Team").id("2").projectName("project-test-2").build()));
        when(azureDevopsClient.getTeams(eq("org-1"), eq("3"), eq(0)))
                .thenReturn(List.of(Team.builder().name("project-test-3 Team").id("3").projectName("project-test-3").build()));
        when(azureDevopsClient.getWorkItemQuery(eq("org-1"), eq("project-test-1"),
                any(), any())).thenReturn(workItemQueryResult1);
        when(azureDevopsClient.getWorkItemQuery(eq("org-2"), eq("project-test-2"),
                any(), any())).thenReturn(workItemQueryResult2);
        when(azureDevopsClient.getWorkItemQuery(eq("org-1"), eq("project-test-3"),
                any(), any())).thenReturn(workItemQueryResult2);
    }

    private void setupPullRequests() throws AzureDevopsClientException {
        List<PullRequest> pullRequests = List.of(
                PullRequest.builder().id("1").build());
        PullRequestResponse pullRequestResponse = PullRequestResponse.builder()
                .pullRequests(pullRequests)
                .build();
        when(azureDevopsClient.getPullRequests(eq("org-1"), eq("1"), eq("1"), eq(0)))
                .thenReturn(pullRequestResponse.getPullRequests());
        when(azureDevopsClient.getPullRequests(eq("org-2"), eq("2"), eq("2"), eq(0)))
                .thenReturn(pullRequestResponse.getPullRequests());
        when(azureDevopsClient.getPullRequestsWithCommiterInfo(eq("org-1"), eq("project-test-1"),
                eq("1"), eq("0")))
                .thenReturn(PullRequest.builder()
                        .id("1")
                        .lastMergeCommit(PullRequest.CommitInfo.builder()
                                .commitId("123")
                                .build())
                        .build());
        when(azureDevopsClient.getPullRequestsWithCommiterInfo(eq("org-2"), eq("project-test-2"),
                eq("2"), eq("0")))
                .thenReturn(PullRequest.builder()
                        .id("1")
                        .lastMergeCommit(PullRequest.CommitInfo.builder()
                                .commitId("123")
                                .build())
                        .build());
        when(azureDevopsClient.getPullRequestsWithLabelInfo(eq("org-1"), eq("project-test-1"),
                eq("1"), eq("1")))
                .thenReturn(List.of(Label.builder()
                        .id("1").build()));
        when(azureDevopsClient.getPullRequestsWithLabelInfo(eq("org-2"), eq("project-test-2"),
                eq("2"), eq("1")))
                .thenReturn(List.of(Label.builder()
                        .id("1").build()));
    }

    private void setupCommits() throws AzureDevopsClientException {
        List<Commit> commits = List.of(
                Commit.builder().commitId("1").build());
        CommitResponse commitResponse = CommitResponse.builder()
                .commits(commits)
                .build();
        when(azureDevopsClient.getCommits(eq("org-1"), eq("project-test-1"), any(), eq("1"), eq(
                from), eq(to), eq(0))).thenReturn(commitResponse.getCommits());
        when(azureDevopsClient.getCommits(eq("org-2"), eq("project-test-2"), any(), eq("2"), eq(
                from), eq(to), eq(0))).thenReturn(commitResponse.getCommits());
        List<Change> changes = List.of(
                Change.builder().item(Change.Item.builder().build()).build());
        CommitChangesResponse commitChangesResponse = CommitChangesResponse.builder()
                .changes(changes)
                .build();
        when(azureDevopsClient.getChanges(eq("org-1"), eq("1"), eq("1"), eq("1"), eq(0)))
                .thenReturn(commitChangesResponse.getChanges());
        when(azureDevopsClient.getChanges(eq("org-2"), eq("2"), eq("2"), eq("1"), eq(0)))
                .thenReturn(commitChangesResponse.getChanges());
    }

    private void setupBuilds() throws AzureDevopsClientException {
        List<BuildChange> buildChanges = List.of(
                BuildChange.builder().id("commit-id-1")
                        .build());
        BuildChangeResponse buildChangeResponse = BuildChangeResponse.builder()
                .buildChanges(buildChanges)
                .build();
        when(azureDevopsClient.getBuildCommits(eq("org-1"), eq("project-test-1"), eq(1), eq(2)))
                .thenReturn(buildChangeResponse.getBuildChanges());
        when(azureDevopsClient.getBuildCommits(eq("org-1"), eq("project-test-3"), eq(1), eq(2)))
                .thenReturn(buildChangeResponse.getBuildChanges());
        List<BuildChange> buildChanges2 = List.of(
                BuildChange.builder().id("commit-id-1")
                        .build());
        BuildChangeResponse buildChangeResponse2 = BuildChangeResponse.builder()
                .buildChanges(buildChanges2)
                .build();
        when(azureDevopsClient.getBuildCommits(eq("org-2"), eq("project-test-2"), eq(1), eq(2)))
                .thenReturn(buildChangeResponse2.getBuildChanges());
        List<Build> builds = List.of(
                Build.builder().id(1).build());
        BuildResponse buildResponse = BuildResponse.builder()
                .builds(builds)
                .continuationToken(null)
                .build();
        when(azureDevopsClient.getBuilds(eq("org-1"), eq("project-test-1"), eq(
                from), eq(to), eq("")))
                .thenReturn(buildResponse);
        when(azureDevopsClient.getBuilds(eq("org-1"), eq("project-test-3"), eq(
                from), eq(to), eq("")))
                .thenReturn(buildResponse);
        List<Build> builds2 = List.of(
                Build.builder().id(2).build());
        BuildResponse buildResponse2 = BuildResponse.builder()
                .builds(builds2)
                .continuationToken(null)
                .build();
        when(azureDevopsClient.getBuilds(eq("org-2"), eq("project-test-2"), eq(
                from), eq(to), eq("")))
                .thenReturn(buildResponse2);

    }

    private void setupBuildStageSteps() throws IOException, AzureDevopsClientException {
        BuildTimelineResponse buildTimelineResponse = ResourceUtils.getResourceAsObject("timeline.json", BuildTimelineResponse.class);
        when(azureDevopsClient.getBuildTimeline(eq("org-2"), eq("project-test-2"), eq(2)))
                .thenReturn(buildTimelineResponse.getStages());
    }

    private void setupBuildStepLogs() throws AzureDevopsClientException {
        List<String> stepLogs = List.of("2023-05-31T08:55:31.0724951Z ##[section]Starting: Lint_Job",
                "2023-05-31T08:55:31.2366003Z ##[section]Starting: Initialize job",
                "2023-05-31T08:55:31.2368338Z Agent name: 'Hosted Agent'",
                "2023-05-31T08:55:31.2368629Z Agent machine name: 'fv-az637-983'",
                "2023-05-31T08:55:31.2368757Z Current agent version: '3.220.2'",
                "2023-05-31T08:55:31.2398555Z ##[group]Operating System",
                "2023-05-31T08:55:31.2398691Z Ubuntu",
                "2023-05-31T08:55:31.2398758Z 22.04.2",
                "2023-05-31T08:55:31.2398839Z LTS",
                "2023-05-31T08:55:31.2398912Z ##[endgroup]",
                "2023-05-31T08:55:31.2398993Z ##[group]Runner Image",
                "2023-05-31T08:55:31.2399087Z Image: ubuntu-22.04",
                "2023-05-31T08:55:31.2399189Z Version: 20230507.1",
                "2023-05-31T08:55:31.2399358Z Included Software: https://github.com/actions/runner-images/blob/ubuntu22/20230507.1/images/linux/Ubuntu2204-Readme.md",
                "2023-05-31T08:55:31.2399590Z Image Release: https://github.com/actions/runner-images/releases/tag/ubuntu22%2F20230507.1",
                "2023-05-31T08:55:31.2399728Z ##[endgroup]",
                "2023-05-31T08:55:31.2399831Z ##[group]Runner Image Provisioner",
                "2023-05-31T08:55:31.2400081Z 2.0.171.1",
                "2023-05-31T08:55:31.2400177Z ##[endgroup]",
                "2023-05-31T08:55:31.2401167Z Current image version: '20230507.1'",
                "2023-05-31T08:55:31.2402343Z Agent running as: 'vsts'",
                "2023-05-31T08:55:31.2436239Z Prepare build directory.",
                "2023-05-31T08:55:31.2691982Z Set build variables.",
                "2023-05-31T08:55:31.2721523Z Download all required tasks.",
                "2023-05-31T08:55:31.2863168Z Downloading task: Bash (3.214.0)",
                "2023-05-31T08:55:31.6069961Z Downloading task: PublishPipelineMetadata (0.216.0)",
                "2023-05-31T08:55:32.2970315Z Checking job knob settings.",
                "2023-05-31T08:55:32.2980601Z    Knob: DockerActionRetries = true Source: $(VSTSAGENT_DOCKER_ACTION_RETRIES) ",
                "2023-05-31T08:55:32.2981191Z    Knob: AgentToolsDirectory = /opt/hostedtoolcache Source: ${AGENT_TOOLSDIRECTORY} ",
                "2023-05-31T08:55:32.2982151Z    Knob: AgentPerflog = /home/vsts/perflog Source: ${VSTS_AGENT_PERFLOG} ",
                "2023-05-31T08:55:32.2983927Z    Knob: ContinueAfterCancelProcessTreeKillAttempt = true Source: $(VSTSAGENT_CONTINUE_AFTER_CANCEL_PROCESSTREEKILL_ATTEMPT) ",
                "2023-05-31T08:55:32.2984380Z Finished checking job knob settings.",
                "2023-05-31T08:55:32.3239751Z Start tracking orphan processes.",
                "2023-05-31T08:55:32.3404809Z ##[section]Finishing: Initialize job",
                "2023-05-31T08:55:32.3581347Z ##[section]Async Command Start: DetectDockerContainer",
                "2023-05-31T08:55:32.3581605Z ##[section]Async Command End: DetectDockerContainer",
                "2023-05-31T08:55:32.3582251Z ##[section]Async Command Start: DetectDockerContainer",
                "2023-05-31T08:55:32.3582488Z ##[section]Async Command End: DetectDockerContainer",
                "2023-05-31T08:55:32.3754923Z ##[section]Starting: Checkout pipelines-testing-nish@main to s",
                "2023-05-31T08:55:32.3958127Z ==============================================================================",
                "2023-05-31T08:55:32.3958735Z Task         : Get sources",
                "2023-05-31T08:55:32.3958984Z Description  : Get sources from a repository. Supports Git, TfsVC, and SVN repositories.",
                "2023-05-31T08:55:32.3959147Z Version      : 1.0.0",
                "2023-05-31T08:55:32.3959350Z Author       : Microsoft",
                "2023-05-31T08:55:32.3959694Z Help         : [More Information](https://go.microsoft.com/fwlink/?LinkId=798199)",
                "2023-05-31T08:55:32.3959841Z ==============================================================================",
                "2023-05-31T08:55:32.8795352Z Syncing repository: pipelines-testing-nish (Git)",
                "2023-05-31T08:55:32.9257928Z ##[command]git version",
                "2023-05-31T08:55:32.9724320Z git version 2.40.1",
                "2023-05-31T08:55:32.9746797Z ##[command]git lfs version",
                "2023-05-31T08:55:33.0321325Z git-lfs/3.3.0 (GitHub; linux amd64; go 1.19.3)",
                "2023-05-31T08:55:33.0475878Z ##[command]git init \"/home/vsts/work/1/s\"",
                "2023-05-31T08:55:33.0588113Z hint: Using 'master' as the name for the initial branch. This default branch name",
                "2023-05-31T08:55:33.0588650Z hint: is subject to change. To configure the initial branch name to use in all",
                "2023-05-31T08:55:33.0590178Z hint: of your new repositories, which will suppress this warning, call:",
                "2023-05-31T08:55:33.0590560Z hint: ",
                "2023-05-31T08:55:33.0591804Z hint: \tgit config --global init.defaultBranch <name>",
                "2023-05-31T08:55:33.0592434Z hint: ",
                "2023-05-31T08:55:33.0594220Z hint: Names commonly chosen instead of 'master' are 'main', 'trunk' and",
                "2023-05-31T08:55:33.0594821Z hint: 'development'. The just-created branch can be renamed via this command:",
                "2023-05-31T08:55:33.0595088Z hint: ",
                "2023-05-31T08:55:33.0595295Z hint: \tgit branch -m <name>",
                "2023-05-31T08:55:33.0621234Z Initialized empty Git repository in /home/vsts/work/1/s/.git/",
                "2023-05-31T08:55:33.0631314Z ##[command]git remote add origin https://vinayanayak@dev.azure.com/vinayanayak/Propelo_internal/_git/pipelines-testing-nish",
                "2023-05-31T08:55:33.0663538Z ##[command]git config gc.auto 0",
                "2023-05-31T08:55:33.0703137Z ##[command]git config --get-all http.https://vinayanayak@dev.azure.com/vinayanayak/Propelo_internal/_git/pipelines-testing-nish.extraheader",
                "2023-05-31T08:55:33.0738947Z ##[command]git config --get-all http.extraheader",
                "2023-05-31T08:55:33.0772356Z ##[command]git config --get-regexp .*extraheader",
                "2023-05-31T08:55:33.1156355Z ##[command]git config --get-all http.proxy",
                "2023-05-31T08:55:33.1166213Z ##[command]git config http.version HTTP/1.1",
                "2023-05-31T08:55:33.1242456Z ##[command]git --config-env=http.extraheader=env_var_http.extraheader fetch --force --tags --prune --prune-tags --progress --no-recurse-submodules origin --depth=1 +5d9244775978457bb10d1ea08d6cb7fae8f9590a:refs/remotes/origin/5d9244775978457bb10d1ea08d6cb7fae8f9590a",
                "2023-05-31T08:55:33.4218248Z remote: Azure Repos        ",
                "2023-05-31T08:55:33.4221724Z remote: ",
                "2023-05-31T08:55:33.4222553Z remote: Found 15 objects to send. (20 ms)        ",
                "2023-05-31T08:55:33.4223326Z From https://dev.azure.com/vinayanayak/Propelo_internal/_git/pipelines-testing-nish",
                "2023-05-31T08:55:33.4224143Z  * [new ref]         5d9244775978457bb10d1ea08d6cb7fae8f9590a -> origin/5d9244775978457bb10d1ea08d6cb7fae8f9590a",
                "2023-05-31T08:55:33.4697034Z ##[command]git --config-env=http.extraheader=env_var_http.extraheader fetch --force --tags --prune --prune-tags --progress --no-recurse-submodules origin --depth=1 +5d9244775978457bb10d1ea08d6cb7fae8f9590a",
                "2023-05-31T08:55:33.6234553Z remote: Azure Repos        ",
                "2023-05-31T08:55:33.6234699Z remote: ",
                "2023-05-31T08:55:33.6235051Z remote: Found 0 objects to send. (0 ms)        ",
                "2023-05-31T08:55:33.6235489Z From https://dev.azure.com/vinayanayak/Propelo_internal/_git/pipelines-testing-nish",
                "2023-05-31T08:55:33.6235796Z  * branch            5d9244775978457bb10d1ea08d6cb7fae8f9590a -> FETCH_HEAD",
                "2023-05-31T08:55:33.6613139Z ##[command]git checkout --progress --force refs/remotes/origin/5d9244775978457bb10d1ea08d6cb7fae8f9590a",
                "2023-05-31T08:55:33.6616441Z Note: switching to 'refs/remotes/origin/5d9244775978457bb10d1ea08d6cb7fae8f9590a'.",
                "2023-05-31T08:55:33.6616538Z ",
                "2023-05-31T08:55:33.6616800Z You are in 'detached HEAD' state. You can look around, make experimental",
                "2023-05-31T08:55:33.6616975Z changes and commit them, and you can discard any commits you make in this",
                "2023-05-31T08:55:33.6617144Z state without impacting any branches by switching back to a branch.",
                "2023-05-31T08:55:33.6617274Z ",
                "2023-05-31T08:55:33.6617414Z If you want to create a new branch to retain commits you create, you may",
                "2023-05-31T08:55:33.6617651Z do so (now or later) by using -c with the switch command. Example:",
                "2023-05-31T08:55:33.6617727Z ",
                "2023-05-31T08:55:33.6617933Z   git switch -c <new-branch-name>",
                "2023-05-31T08:55:33.6617990Z ",
                "2023-05-31T08:55:33.6618089Z Or undo this operation with:",
                "2023-05-31T08:55:33.6618516Z ",
                "2023-05-31T08:55:33.6618663Z   git switch -",
                "2023-05-31T08:55:33.6618706Z ",
                "2023-05-31T08:55:33.6618846Z Turn off this advice by setting config variable advice.detachedHead to false",
                "2023-05-31T08:55:33.6618929Z ",
                "2023-05-31T08:55:33.6619191Z HEAD is now at 5d92447 Update azure-pipelines.yml for Azure Pipelines",
                "2023-05-31T08:55:33.6671143Z ##[section]Finishing: Checkout pipelines-testing-nish@main to s",
                "2023-05-31T08:55:33.6686087Z ##[section]Starting: Lint_Step_1 echo \"Lint_Step_1 is Completed\"",
                "2023-05-31T08:55:33.6691089Z ==============================================================================",
                "2023-05-31T08:55:33.6691330Z Task         : Bash",
                "2023-05-31T08:55:33.6691378Z Description  : Run a Bash script on macOS, Linux, or Windows",
                "2023-05-31T08:55:33.6691453Z Version      : 3.214.0",
                "2023-05-31T08:55:33.6691503Z Author       : Microsoft Corporation",
                "2023-05-31T08:55:33.6691622Z Help         : https://docs.microsoft.com/azure/devops/pipelines/tasks/utility/bash",
                "2023-05-31T08:55:33.6691706Z ==============================================================================",
                "2023-05-31T08:55:34.0162407Z Generating script.",
                "2023-05-31T08:55:34.0163948Z Script contents:",
                "2023-05-31T08:55:34.0164369Z ",
                "2023-05-31T08:55:34.0165596Z ========================== Starting Command Output ===========================",
                "2023-05-31T08:55:34.0169571Z [command]/usr/bin/bash /home/vsts/work/_temp/4ce03c52-10a7-446c-a800-35f4a4c8ae21.sh",
                "2023-05-31T08:55:34.0189063Z ##[section]Finishing: Lint_Step_1 echo \"Lint_Step_1 is Completed\"",
                "2023-05-31T08:55:34.0199342Z ##[section]Starting: Publish Pipeline Metadata",
                "2023-05-31T08:55:34.0202192Z ==============================================================================",
                "2023-05-31T08:55:34.0202305Z Task         : Publish Pipeline Metadata",
                "2023-05-31T08:55:34.0202381Z Description  : Publish Pipeline Metadata to Evidence store",
                "2023-05-31T08:55:34.0202456Z Version      : 0.216.0",
                "2023-05-31T08:55:34.0202507Z Author       : Microsoft Corporation",
                "2023-05-31T08:55:34.0202567Z Help         : ",
                "2023-05-31T08:55:34.0202609Z ==============================================================================",
                "2023-05-31T08:55:34.2074280Z ##[section]Finishing: Publish Pipeline Metadata",
                "2023-05-31T08:55:34.2085225Z ##[section]Starting: Checkout pipelines-testing-nish@main to s",
                "2023-05-31T08:55:34.2087440Z ==============================================================================",
                "2023-05-31T08:55:34.2087548Z Task         : Get sources",
                "2023-05-31T08:55:34.2087601Z Description  : Get sources from a repository. Supports Git, TfsVC, and SVN repositories.",
                "2023-05-31T08:55:34.2087708Z Version      : 1.0.0",
                "2023-05-31T08:55:34.2087756Z Author       : Microsoft",
                "2023-05-31T08:55:34.2087820Z Help         : [More Information](https://go.microsoft.com/fwlink/?LinkId=798199)",
                "2023-05-31T08:55:34.2087901Z ==============================================================================",
                "2023-05-31T08:55:34.5414121Z Cleaning any cached credential from repository: pipelines-testing-nish (Git)",
                "2023-05-31T08:55:34.5508064Z ##[section]Finishing: Checkout pipelines-testing-nish@main to s",
                "2023-05-31T08:55:34.5600612Z ##[section]Starting: Finalize Job",
                "2023-05-31T08:55:34.5629600Z Cleaning up task key",
                "2023-05-31T08:55:34.5630661Z Start cleaning up orphan processes.",
                "2023-05-31T08:55:34.5902401Z ##[section]Finishing: Finalize Job",
                "2023-05-31T08:55:34.5961007Z ##[section]Finishing: Lint_Job");
        when(azureDevopsClient.getStepLogs(anyString())).thenReturn(stepLogs);
    }

    private void setupPipelineRuns() throws AzureDevopsClientException {
        List<Pipeline> pipelines = List.of(
                Pipeline.builder()
                        .name("pipeline-1").
                        configuration(Configuration.builder()
                                .variables(Map.of("test",
                                        Configuration.Variable.builder()
                                                .value("test-value")
                                                .build()))
                                .build())
                        .id(1)
                        .build());
        PipelineResponse pipelineResponse = PipelineResponse.builder()
                .pipelines(pipelines)
                .continuationToken(null)
                .build();
        when(azureDevopsClient.getPipelines(eq("org-1"), eq("project-test-1"), eq("")))
                .thenReturn(pipelineResponse);
        when(azureDevopsClient.getPipelines(eq("org-2"), eq("project-test-2"), eq("")))
                .thenReturn(pipelineResponse);
        when(azureDevopsClient.getPipelines(eq("org-1"), eq("project-test-3"), eq("")))
                .thenReturn(pipelineResponse);
        when(azureDevopsClient.getPipeline(eq("org-1"), eq("project-test-1"), eq(1)))
                .thenReturn(pipelineResponse.getPipelines().get(0));
        when(azureDevopsClient.getPipeline(eq("org-2"), eq("project-test-2"), eq(1)))
                .thenReturn(pipelineResponse.getPipelines().get(0));
        when(azureDevopsClient.getPipeline(eq("org-1"), eq("project-test-3"), eq(1)))
                .thenReturn(pipelineResponse.getPipelines().get(0));
        List<Run> runs = List.of(
                Run.builder().name("run-1")
                        .id(1)
                        .createdDate(Instant.now().minus(Duration.ofDays(2)).toString())
                        .finishedDate(Instant.now().minus(Duration.ofDays(1)).toString())
                        .variables(Map.of("VAR_1", Configuration.Variable.builder().value("value1").build()))
                        .build(), Run.builder().name("run-2")
                        .id(2)
                        .createdDate(Instant.now().minus(Duration.ofDays(2)).toString())
                        .finishedDate(Instant.now().minus(Duration.ofDays(1)).toString())
                        .variables(Map.of("VAR_1", Configuration.Variable.builder().value("value2").build()))
                        .build());
        RunResponse runResponse = RunResponse.builder()
                .runs(runs)
                .build();
        when(azureDevopsClient.getRuns(eq("org-1"), eq("project-test-1"), eq(1)))
                .thenReturn(runResponse);
        when(azureDevopsClient.getRuns(eq("org-2"), eq("project-test-2"), eq(1)))
                .thenReturn(runResponse);
        when(azureDevopsClient.getRuns(eq("org-1"), eq("project-test-3"), eq(1)))
                .thenReturn(runResponse);
    }

    private void setUpReleases() throws AzureDevopsClientException, IOException {
        ReleaseResponse releaseResponse = ResourceUtils.getResourceAsObject("releases.json", ReleaseResponse.class);
        releaseResponse  = releaseResponse.toBuilder()
                .releases(releaseResponse.getReleases().stream()
                        .map(release -> release.toBuilder()
                                .createdOn(Instant.now().minus(Duration.ofDays(1)).toString())
                                .modifiedOn(Instant.now().minus(Duration.ofHours(5)).toString())
                                .startTime(Instant.now().minus(Duration.ofDays(5)).toString())
                                .finishTime(Instant.now().minus(Duration.ofDays(1)).toString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        doReturn(releaseResponse).when(azureDevopsClient).getReleases(anyString(), anyString(), anyString());
    }

    private void setUpReleaseDetails() throws AzureDevopsClientException, IOException {
        AzureDevopsRelease release = ResourceUtils.getResourceAsObject("release-details.json", AzureDevopsRelease.class);
        release = release.toBuilder()
                .createdOn(Instant.now().minus(Duration.ofDays(1)).toString())
                .modifiedOn(Instant.now().minus(Duration.ofHours(5)).toString())
                .build();
        doReturn(release).when(azureDevopsClient).getRelease(anyString(), anyString(), anyInt());
    }

    @Test
    public void testCommits() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsCommitsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<Commit> commits = projects.stream()
                .map(Data::getPayload)
                .flatMap(project -> project.getCommits().stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(commits);
        assertThat(commits).hasSize(2);
    }

    @Test
    public void testPullRequests() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsPullRequestDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<PullRequest> pullRequests = projects.stream()
                .map(Data::getPayload)
                .flatMap(pr -> pr.getPullRequests().stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(pullRequests);
        assertThat(pullRequests).hasSize(2);
    }

    @Test
    public void testBuilds() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsBuildsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<Build> builds = projects.stream()
                .map(Data::getPayload)
                .flatMap(build -> build.getBuilds().stream())
                .collect(Collectors.toList());
        assertThat(builds).hasSize(3);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> azureDevopsPipelineRunsDataSource.fetchOne(AzureDevopsIterativeScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void testPipelineRuns() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsPipelineRunsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<Run> pipelineRuns = projects.stream()
                .map(Data::getPayload)
                .flatMap(pipeline -> pipeline.getPipelineRuns().stream())
                .collect(Collectors.toList());
        assertThat(pipelineRuns).hasSize(6);
    }

    @Test
    public void testWorkItems() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsWorkItemsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<WorkItem> workItems = projects.stream()
                .map(Data::getPayload)
                .flatMap(projectData -> projectData.getWorkItems().stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(workItems);
        assertThat(workItems).hasSize(2);
    }

    @Test
    public void testChangeset() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsChangesetDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<ChangeSet> changeSets = projects.stream()
                .map(Data::getPayload)
                .flatMap(projectData -> projectData.getChangeSets().stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(changeSets);
        assertThat(changeSets).hasSize(2);
    }

    @Test
    public void testTag() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsTagsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<Tag> tags = projects.stream()
                .map(Data::getPayload)
                .flatMap(projectData -> projectData.getTags().stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(tags);
        assertThat(tags).hasSize(4);
    }

    @Test
    public void testBuildStageSteps() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsPipelineRunsDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());
        List<Run> pipelineRuns = projects.stream()
                .map(Data::getPayload)
                .flatMap(pipeline -> pipeline.getPipelineRuns().stream())
                .collect(Collectors.toList());
        Run pipelineRun = pipelineRuns.stream().filter(b->b.getId() == 2 && !b.getStages().isEmpty()).collect(Collectors.toList()).get(0);
        AzureDevopsPipelineRunStageStep stage = pipelineRun.getStages().stream().filter(s-> Objects.equals(s.getName(), "Build_Stage")).collect(Collectors.toList()).get(0);
        assertThat(pipelineRun.getStages()).hasSize(3);
        assertThat(stage.getSteps()).hasSize(15);
        assertThat(stage.getSteps().stream().allMatch(step -> step.getStepLogs() != null)).isTrue();

    }

    @Test
    public void testDefinitionsReleases() throws FetchException {
        List<Data<EnrichedProjectData>> projects = azureDevopsReleaseDataSource.fetchMany(JobContext.builder().build(),
                AzureDevopsIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList());

        List<AzureDevopsRelease> releases = projects.stream()
                .map(Data::getPayload)
                .flatMap(project -> project.getReleases().stream())
                .collect(Collectors.toList());

        List<AzureDevopsReleaseEnvironment> firstReleaseStages = releases.get(0).getStages();
        if(firstReleaseStages.size() > 0) {
            List<AzureDevopsReleaseStep> lastStageSteps = firstReleaseStages.get(firstReleaseStages.size()-1).getSteps();
            if(lastStageSteps.size() > 0) {
                assertThat(releases.get(0).getStartTime())
                        .isEqualTo(releases.get(0).getStages().get(0).getSteps().get(0).getStartTime());
                assertThat(releases.get(0).getFinishTime()).isEqualTo(lastStageSteps.get(lastStageSteps.size()-1).getFinishTime());
            }
        }
        assertThat(releases).isNotNull();
        assertThat(projects.get(0).getPayload().getProject()).isNotNull();
        assertThat(projects.get(0).getPayload().getDefinition().getName()).isEqualTo("New release pipeline (1) - With Variables");
        assertThat(releases.size()).isEqualTo(3);
        assertThat(releases.get(0).getVariables()).isNotNull();
        assertThat(releases.get(0).getVariables().size()).isEqualTo(2);
        assertThat(releases.get(0).getVariableGroups()).isNotNull();
        assertThat(releases.get(0).getStages()).isNotNull();
        assertThat(releases.get(0).getStages().size()).isEqualTo(2);
        assertThat(releases.get(0).getStartTime()).isEqualTo("2023-06-22T06:03:30.0866667Z");
        assertThat(releases.get(0).getFinishTime()).isEqualTo("2023-06-22T06:05:17.04Z");
        assertThat(releases.get(0).getStages().get(0).getSteps()).isNotNull();
        assertThat(releases.get(0).getStages().get(0).getSteps().size()).isEqualTo(4);
    }
}