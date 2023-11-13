package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientFactory;
import io.levelops.integrations.azureDevops.models.BuildResponse;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.PipelineResponse;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.ProjectProperty;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.RunResponse;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.CHANGESETS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_BUILDS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_COMMITCHANGES_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_COMMITS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_ITERATIONS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_PIPELINES_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_PIPELINE_RUNS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_PROJECTS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_PULLREQUESTS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_REPOSITORIES_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_TEAMS_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_WIQL_API;
import static io.levelops.integrations.azureDevops.client.AzureDevopsClient.LIST_WORKITEMS_API;

@Log4j2
@Component
public class AzureDevopsPreflightCheck implements PreflightCheck {

    private static final String AZURE_DEVOPS = "azure_devops";

    private static final int RESPONSE_PAGE_SIZE = 1;

    private final AzureDevopsClientFactory clientFactory;

    @Autowired
    public AzureDevopsPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = AzureDevopsClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link AzureDevopsPreflightCheck#AZURE_DEVOPS}
     */
    @Override
    public String getIntegrationType() {
        return AZURE_DEVOPS;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list tickets,
     * list requests and jira links api. Validates successful response.
     *
     * @param tenantId    {@link String} id of the tenant for which the {@code integration} is being validated
     * @param integration {@link Integration} to validate
     * @param token       {@link Token} containing the credentials for the {@code integration}
     * @return {@link PreflightCheckResults} containing {@link PreflightCheckResult}
     */
    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder parentChecks = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        try {
            AzureDevopsClient client = clientFactory.buildFromToken(tenantId, integration, token, 1);
            var organizations = client.getOrganizations();
            if (CollectionUtils.isNotEmpty(organizations)) {
                String organization = organizations.get(0);
                Project project = checkProjects(client, organization, parentChecks);
                if (project != null && project.getGitEnabled()) {
                    Team team = checkTeams(client, organization, project.getName(), parentChecks);
                    checkPipelines(client, organization, project.getName(), parentChecks);
                    checkBuilds(client, organization, project.getName(), parentChecks);
                    checkRepositories(client, organization, project.getName(), parentChecks);
                    if (team != null) {
                        checkWorkItemQuery(client, organization, project.getName(), team.getName(), parentChecks);
                        checkIterations(client, organization, project.getName(), team.getId(), parentChecks);
                    } else {
                        log.warn("check: tenant = {}, integration = {}, project = {}, team = {}, " +
                                "so skipping some of the other checks", tenantId, integration.getId(), project, team);
                    }
                } else if (project != null && project.getTfvcEnabled()) {
                    checkChangesets(client, organization, project.getName(), parentChecks);
                    checkLabels(client, organization, project.getName());
                    checkBranches(client, organization, project.getName());
                } else {
                    log.warn("check: tenant = {}, integration = {}, project = {}, " +
                            "so skipping some of the other checks", tenantId, integration.getId(), project);
                }
            }
        } catch (InventoryException | AzureDevopsClientException e) {
            log.error("check: error creating client for tenant: "
                    + tenantId + " , integration: " + integration + e.getMessage(), e);
            return parentChecks
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        return parentChecks.build();
    }

    /**
     * validates the response from {@link AzureDevopsClient#getProjects(String, String)}
     *
     * @param client {@link AzureDevopsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private Project checkProjects(AzureDevopsClient client, String organization,
                                 PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder projectsCheck = PreflightCheckResult.builder()
                .name(LIST_PROJECTS_API)
                .success(true);
        Project project = null;
        try {
            ProjectResponse projectResponse = client.getProjects(organization, "", null);
            if (projectResponse != null && !projectResponse.getProjects().isEmpty()) {
                Optional<Project> projectOptional = projectResponse.getProjects().stream().findFirst();
                if (projectOptional.isPresent()) {
                    project = projectOptional.get();
                    log.debug("checkProjects: Fetched projects :" + project.getName());
                    project = checkProjectProperties(client, organization, project, parentChecks);
                } else {
                    log.warn("checkProjects: No projects found, skipping further checks");
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkProjects: encountered error while fetching project" +
                    " or projects might be empty: " + e.getMessage(), e);
            projectsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(projectsCheck.build());
        return project;
    }

    private Project checkProjectProperties(AzureDevopsClient client, String organization, Project project,
                                        PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder projectPropertiesCheck = PreflightCheckResult.builder()
                .name(LIST_PROJECTS_API)
                .success(true);
        try {
            List<ProjectProperty> projectProperties = client.getProjectProperties(organization, project.getId(), null);
            if (projectProperties != null) {
                AtomicBoolean isGit = new AtomicBoolean(false);
                AtomicBoolean isTfvc = new AtomicBoolean(false);
                projectProperties.forEach(projectProperty -> {
                    if ("System.SourceControlTfvcEnabled".equals(projectProperty.getName())) {
                        isTfvc.set(true);
                    } else if ("System.SourceControlGitEnabled".equals(projectProperty.getName())) {
                        isGit.set(true);
                    }
                });
                project = project.toBuilder()
                        .gitEnabled(isGit.get())
                        .tfvcEnabled(isTfvc.get())
                        .build();
                log.debug("checkProjectProperties: Fetched checkProjectProperties for id :" + project.getId());
            } else {
                log.warn("checkProjectProperties: No ProjectProperties found, skipping further checks");
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkProjectProperties: encountered error while fetching projectProperties" +
                    " or projectProperties might be empty: " + e.getMessage(), e);
            projectPropertiesCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(projectPropertiesCheck.build());
        return project;
    }

    @Nullable
    private Team checkTeams(AzureDevopsClient client, String organization, String project,
                            PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder teamsCheckBuilder = PreflightCheckResult.builder()
                .name(LIST_TEAMS_API)
                .success(true);
        Team team = null;
        try {
            List<Team> teamResponse = client.getTeams(organization, project, 0, null);
            Optional<Team> teamOptional = teamResponse.stream().findFirst();
            if (teamOptional.isPresent()) {
                team = teamOptional.get();
                log.debug("checkTeams: Fetched teams :" + team);
            } else {
                log.warn("checkTeams: No teams present");
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkTeams: encountered error while fetching team might be empty: "
                    + e.getMessage(), e);
            teamsCheckBuilder.success(false).exception(e.getMessage());
        }
        parentChecks.check(teamsCheckBuilder.build());
        return team;
    }

    /**
     * validates the response from {@link AzureDevopsClient#getBuilds(String, String, Date, Date, String)}
     *
     * @param client  {@link AzureDevopsClient} with authentication interceptor
     * @param project for given project
     */
    private void checkBuilds(AzureDevopsClient client, String organization, String project,
                             PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder buildsCheck = PreflightCheckResult.builder()
                .name(LIST_BUILDS_API)
                .success(true);
        try {
            BuildResponse response = client.getBuilds(organization, project, null, null, "", null);
            if (response.getBuilds() == null)
                buildsCheck.success(false).error("response from " + LIST_BUILDS_API + " returned null result");
            else {
                if (!response.getBuilds().isEmpty()) {
                    log.debug("checkBuilds: Fetched build :" + response.getBuilds().get(0).getBuildNumber());
                } else {
                    log.debug("checkBuilds: Build are empty for project Id {}", project);
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkBuilds: encountered error while fetching build : " + e.getMessage(), e);
            buildsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(buildsCheck.build());
    }

    /**
     * validates the response from {@link AzureDevopsClient#getPipelines(String, String, String)}
     *
     * @param client  {@link AzureDevopsClient} with authentication interceptor
     * @param project for given project
     */
    private void checkPipelines(AzureDevopsClient client, String organization, String project,
                                PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder pipelinesCheck = PreflightCheckResult.builder()
                .name(LIST_PIPELINES_API)
                .success(true);
        try {
            PipelineResponse response = client.getPipelines(organization, project, "", null);
            if (response.getPipelines() == null)
                pipelinesCheck.success(false)
                        .error("response from " + LIST_PIPELINES_API + " returned null result");
            else {
                if (!response.getPipelines().isEmpty()) {
                    String pipelineName = response.getPipelines().get(0).getName();
                    int pipelineId = response.getPipelines().get(0).getId();
                    log.debug("checkPipelines: Fetched pipeline :" + pipelineName);
                    checkRuns(client, organization, project, pipelineId, parentChecks);
                } else {
                    log.debug("checkPipelines: Pipeline are empty for project Id {}", project);
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkPipelines: encountered error while fetching pipelines : " + e.getMessage(), e);
            pipelinesCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(pipelinesCheck.build());
    }

    private void checkRepositories(AzureDevopsClient client, String organization, String project,
                                   PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder repositoriesCheck = PreflightCheckResult.builder()
                .name(LIST_REPOSITORIES_API)
                .success(true);
        try {
            List<Repository> repositories = client.getRepositories(organization, project, null);
            if (CollectionUtils.isNotEmpty(repositories)) {
                log.debug("Fetched repositories of size: " + repositories.size());
                Repository repository = repositories.get(0);
                checkCommits(client, organization, project, repository, parentChecks);
                checkPullRequests(client, organization, project, repository, parentChecks);
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkRepositories: encountered error while fetching repository : " + e.getMessage(), e);
            repositoriesCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(repositoriesCheck.build());
    }

    /**
     * validates the response from {@link AzureDevopsClient#getCommits(String, String, String, Date, Date, int)}
     *
     * @param client  {@link AzureDevopsClient} with authentication interceptor
     * @param project for given project
     */
    private void checkCommits(AzureDevopsClient client, String organization, String project, Repository repository,
                              PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder commitsCheck = PreflightCheckResult.builder()
                .name(LIST_COMMITS_API)
                .success(true);
        try {
            List<Commit> commits = client.getCommits(organization, project, repository.getId(), null, null, 0, null);
            if (!commits.isEmpty()) {
                log.debug("checkCommits: Fetched commits of size " + commits.size());
                checkCommitChanges(client, organization, project, repository, commits.get(0), parentChecks);
            } else {
                log.debug("checkCommits: Commits are empty for project Id {}, Repository id {}", project, repository.getId());
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkCommits: encountered error while fetching commits : " + e.getMessage(), e);
            commitsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(commitsCheck.build());
    }

    private void checkCommitChanges(AzureDevopsClient client, String organization, String project, Repository repository,
                                    Commit commit, PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) throws AzureDevopsClientException {
        PreflightCheckResult.PreflightCheckResultBuilder commitChangesCheck = PreflightCheckResult.builder()
                .name(LIST_COMMITCHANGES_API)
                .success(true);
        try {
            List<Change> commitChanges = client.getChanges(organization, project, repository.getId(), commit.getCommitId(), 0, null);
            if (!commitChanges.isEmpty()) {
                log.debug("checkCommits: Fetched commits of size " + commitChanges.size());
            } else {
                log.debug("checkCommitChanges: commitChanges are empty for project Id {}, CommitId id {}", project, commit.getCommitId());
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkCommitChanges: encountered error while fetching commits : " + e.getMessage(), e);
            commitChangesCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(commitChangesCheck.build());
    }

    /**
     * validates the response from {@link AzureDevopsClient#getPullRequests(String, String, String, int)}
     *
     * @param client       {@link AzureDevopsClient} with authentication interceptor
     * @param project      for given project
     * @param parentChecks
     */
    private void checkPullRequests(AzureDevopsClient client, String organization, String project, Repository repository,
                                   PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder pullRequestCheck = PreflightCheckResult.builder()
                .name(LIST_PULLREQUESTS_API)
                .success(true);
        try {
            List<PullRequest> pullRequests = client.getPullRequests(organization, project, repository.getId(), 0, null);
            if (!pullRequests.isEmpty()) {
                log.debug("Fetched PullRequests of size " + pullRequests.size());
            } else {
                log.debug("checkPullRequests: PullRequests are empty for project Id {}, Repository id {}", project, repository.getId());
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkPullRequests: encountered error while fetching PullRequests : " + e.getMessage(), e);
            pullRequestCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(pullRequestCheck.build());
    }

    /**
     * validates the response from {@link AzureDevopsClient#getRuns(String, String, int)}
     *
     * @param client     {@link AzureDevopsClient} with authentication interceptor
     * @param project    for given project
     * @param pipelineId pipeline id
     */
    private void checkRuns(AzureDevopsClient client, String organization, String project, int pipelineId,
                           PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder pipelineRunsCheck = PreflightCheckResult.builder()
                .name(LIST_PIPELINE_RUNS_API)
                .success(true);
        try {
            RunResponse response = client.getRuns(organization, project, pipelineId, null);
            if (response == null) {
                pipelineRunsCheck.success(false)
                        .error("response from " + LIST_PIPELINE_RUNS_API + " returned null result");
            } else {
                if (!response.getRuns().isEmpty()) {
                    log.debug("checkRuns: Fetched runs :" + response.getRuns().get(0).getName());
                } else {
                    log.debug("checkRuns: Runs are empty for project Id {}", project);
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkRuns: encountered error while fetching runs: " + e.getMessage(), e);
            pipelineRunsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(pipelineRunsCheck.build());
    }

    private void checkWorkItems(AzureDevopsClient client, String organization, String project, List<String> ids,
                                PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder workItemsCheck = PreflightCheckResult.builder()
                .name(LIST_WORKITEMS_API)
                .success(true);
        try {
            List<WorkItem> response = client.getWorkItems(organization, project, ids, null);
            if (response == null) {
                workItemsCheck.success(false)
                        .error("response from " + LIST_WORKITEMS_API + " returned null result");
            } else {
                if (!response.isEmpty()) {
                    log.debug("checkWorkItems: Fetched workItems :" + response.get(0).getId()
                            + " for project : " + project + " and ids : " + ids);
                } else {
                    log.debug("checkWorkItems: WorkItems are empty for project : " + project + " and ids : " + ids);
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkWorkItems: encountered error while fetching workItems: " + e.getMessage(), e);
            workItemsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(workItemsCheck.build());
    }

    private void checkWorkItemQuery(AzureDevopsClient client, String organization, String project,
                                    String team, PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder workItemsQueryCheck = PreflightCheckResult.builder()
                .name(LIST_WIQL_API)
                .success(true);
        try {
            WorkItemQueryResult response = client.getWorkItemQuery(organization, project,
                    Date.from(Instant.now().minus(Duration.ofDays(90))), Date.from(Instant.now()), null);
            if (response == null) {
                workItemsQueryCheck.success(false)
                        .error("response from " + LIST_WORKITEMS_API + " returned null result");
            } else {
                if (!response.getWorkItems().isEmpty()) {
                    log.debug("checkWorkItems: Fetched workItemId :" + response.getWorkItems().get(0).getId()
                            + " for project : " + project);
                    List<String> workItemIds = response.getWorkItems().stream()
                            .map(WorkItemQueryResult.WorkItemReference::getId)
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    checkWorkItems(client, organization, project, workItemIds, parentChecks);
                } else {
                    log.debug("checkWorkItems: WorkItemIds are empty for project : " + project);
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("checkWorkItems: encountered error while fetching workItems: " + e.getMessage(), e);
            workItemsQueryCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(workItemsQueryCheck.build());
    }

    private void checkChangesets(AzureDevopsClient client, String organization,
                                 String project,
                                 PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder changesets = PreflightCheckResult.builder()
                .name("changesets")
                .success(true);
        try {
            List<ChangeSet> changeSets = client.getChangesets(organization, project, null, 0, null);
            if (changeSets == null) {
                changesets.success(false).error("response from " + CHANGESETS_API + " returned null ");
            } else {
                if (CollectionUtils.isNotEmpty(changeSets)) {
                    String changesetId = changeSets.get(0).getChangesetId().toString();
                    parentChecks.check(checkChangesetsChanges(client, organization, changesetId));
                    parentChecks.check(checkChangesetsWorkitems(client, organization, changesetId));
                }
            }
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch changesets for organization " + organization + e.getMessage(), e);
            changesets.success(false).exception(e.getMessage());
        }
        parentChecks.check(changesets.build());
    }

    private PreflightCheckResult checkChangesetsChanges(AzureDevopsClient client, String organization, String changesetId) {
        PreflightCheckResult.PreflightCheckResultBuilder changesetsChanges = PreflightCheckResult.builder()
                .name("changeset changes")
                .success(true);
        try {
            client.getChangesetChanges(organization, changesetId, 0, null);
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch changeset changes for id " + changesetId + ":" + e.getMessage(), e);
            changesetsChanges.success(false).exception(e.getMessage());
        }
        return changesetsChanges.build();
    }

    private PreflightCheckResult checkChangesetsWorkitems(AzureDevopsClient client, String organization, String changesetId) {
        PreflightCheckResult.PreflightCheckResultBuilder branches = PreflightCheckResult.builder()
                .name("changeset workitems")
                .success(true);
        try {
            client.getBranches(organization, changesetId, null);
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch changeset workitems for id " + changesetId + ":" + e.getMessage(), e);
            branches.success(false).exception(e.getMessage());
        }
        return branches.build();
    }

    private PreflightCheckResult checkBranches(AzureDevopsClient client, String organization, String project) {
        PreflightCheckResult.PreflightCheckResultBuilder branches = PreflightCheckResult.builder()
                .name("tfvc branches")
                .success(true);
        try {
            client.getBranches(organization, project, null);
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch tfvc branches : " + e.getMessage(), e);
            branches.success(false).exception(e.getMessage());
        }
        return branches.build();
    }

    private PreflightCheckResult checkLabels(AzureDevopsClient client, String organization, String project) {
        PreflightCheckResult.PreflightCheckResultBuilder labels = PreflightCheckResult.builder()
                .name("tfvc labels")
                .success(true);
        try {
            client.getLabels(organization, project, 0);
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch tfvc labels " + e.getMessage(), e);
            labels.success(false).exception(e.getMessage());
        }
        return labels.build();
    }

    private void checkIterations(AzureDevopsClient client, String organization, String project, String team,
                                 PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder iterations = PreflightCheckResult.builder()
                .name("iterations")
                .success(true);
        try {
            List<Iteration> response = client.getIterations(organization, project, team, false, null);
            if (Objects.isNull(response)) {
                iterations.success(false).error("response from " + LIST_ITERATIONS_API + " returned null result");
            }
        } catch (AzureDevopsClientException e) {
            log.error("Failed to fetch iterations : " + e.getMessage(), e);
            iterations.success(false).exception(e.getMessage());
        }
        parentChecks.check(iterations.build());
    }

}