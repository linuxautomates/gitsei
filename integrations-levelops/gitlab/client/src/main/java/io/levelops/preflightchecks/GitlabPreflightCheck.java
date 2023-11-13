package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.functional.IngestionResult;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabBranch;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabMilestone;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.models.ScmRepository;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link PreflightCheck} for Gitlab integration
 */
@Log4j2
@Component
public class GitlabPreflightCheck implements PreflightCheck, ScmRepositoryService {

    public static final int PAGE = 1;
    private static final String GITLAB = "gitlab";
    private static final int RESPONSE_PAGE_SIZE = 1;
    private static final int PAGE_SIZE = 20; // With page size 10, gitlab returns internal server error
    private final GitlabClientFactory clientFactory;

    @Autowired
    public GitlabPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient, InventoryService inventoryService) {
        clientFactory = GitlabClientFactory.builder()
                .inventoryService(inventoryService)
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    @Override
    public String getIntegrationType() {
        return GITLAB;
    }


    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        GitlabClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, 1, true);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: "
                    + integration + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        GitlabProject project = null;
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get projects")
                .success(true);
        try {
            List<GitlabProject> gitlabProjects = client.getProjects(PAGE, PAGE_SIZE);
            if (gitlabProjects.size() != 0) {
                project = gitlabProjects.get(0);
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        builder.check(check.build());
        if (project != null) {
            builder.check(checkBranches(client, project));
            builder.check(checkCommits(client, project));
            builder.check(checkMergeRequests(client, project));
            builder.check(checkUsers(client, project));
            builder.check(checkMilestones(client, project));
            builder.check(checkPipeline(client, builder, project));
        }
        builder.check(checkIssues(client));
        builder.check(checkGroups(client));
        return builder.build();
    }

    private PreflightCheckResult checkBranches(GitlabClient client, GitlabProject project) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get branches")
                .success(true);
        try {
            List<GitlabBranch> branches = client.getBranches(project.getId(), PAGE, PAGE_SIZE);
            if (branches == null) {
                check.success(false).error("Get branches did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkCommits(GitlabClient client, GitlabProject project) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get commits")
                .success(true);
        Date untilDate = Date.from(new Date().toInstant());
        Date sinceDate = Date.from(new Date().toInstant().minus(1, ChronoUnit.MINUTES));
        try {
            List<GitlabCommit> commits = client.getCommits(project.getId(), sinceDate, untilDate, PAGE, PAGE_SIZE);
            if (commits == null) {
                check.success(false).error("Get commits did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkMergeRequests(GitlabClient client, GitlabProject project) {

        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get Merge Requests")
                .success(true);
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(1, ChronoUnit.MINUTES));
        try {
            List<GitlabMergeRequest> mergeRequests = client.getMergeRequests(
                    project.getId(), updatedAfter, updatedBefore, PAGE, PAGE_SIZE);
            if (mergeRequests == null) {
                check.success(false).error("Get Merge Requests did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkIssues(GitlabClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get issues")
                .success(true);
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(1, ChronoUnit.MINUTES));
        try {
            List<GitlabIssue> issues = client.getIssues(updatedAfter, updatedBefore, PAGE, PAGE_SIZE);
            if (issues == null) {
                check.success(false).error("Get issues did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkGroups(GitlabClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get groups")
                .success(true);
        try {
            IngestionResult<GitlabGroup> result = client.getGroups(PAGE, PAGE_SIZE);
            if (result == null || result.getData() == null) {
                check.success(false).error("Get groups did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkUsers(GitlabClient client, GitlabProject project) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get Users")
                .success(true);
        Date to = Date.from(new Date().toInstant());
        Date from = Date.from(new Date().toInstant().minus(1, ChronoUnit.MINUTES));
        try {
            //anything less than 12 may result in a timeout
            List<GitlabUser> users = client.getUsers(project.getId(), from, to, PAGE, 100);
            if (users == null) {
                check.success(false).error("Get users did not return any data");
            }
        } catch (Exception e) {
            log.warn("Failed to run preflight check", e);
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkMilestones(GitlabClient client, GitlabProject project) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get Milestones")
                .success(true);
        try {
            List<GitlabMilestone> milestones = client.getMilestones(project.getId(), PAGE, PAGE_SIZE);
            if (milestones == null) {
                check.success(false).error("Get Milestones did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkPipeline(GitlabClient client, PreflightCheckResults.PreflightCheckResultsBuilder builder, GitlabProject project) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get pipelines")
                .success(true);
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(1, ChronoUnit.MINUTES));
        try {
            List<GitlabPipeline> pipelines = new ArrayList<>();
            if ((project.getBuildsAccessLevel() != null && !"disabled".equals(project.getBuildsAccessLevel())) || project.isJobsEnabled()) {
                pipelines = client.getProjectPipelines(project.getId(), updatedAfter,
                        updatedBefore, PAGE, PAGE_SIZE);
            } else {
                check.success(true).warning("Pipelines are disabled for the project " + project.getId());
            }
            if (CollectionUtils.isNotEmpty(pipelines)) {
                GitlabPipeline detailedPipeline = client.getProjectPipeline(project.getId(), pipelines.get(0).getPipelineId());
                if (detailedPipeline == null) {
                    check.success(false).error("Get detailed pipeline did not return any data ");
                } else {
                    builder.check(checkPipelineVariable(client, project, detailedPipeline.getPipelineId()));
                    builder.check(checkPipelineTestReport(client, project, detailedPipeline.getPipelineId()));
                }
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkPipelineVariable(GitlabClient client, GitlabProject project, String pipelineId) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get pipeline variables")
                .success(true);
        try {
            client.getPipelineTestReport(project.getId(), pipelineId);
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkPipelineTestReport(GitlabClient client, GitlabProject project, String pipelineId) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get pipeline test reports")
                .success(true);
        try {
            client.getPipelineTestReport(project.getId(), pipelineId);
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    @Override
    public int getTotalRepositoriesCount(String company, Integration integration, String repoName, String projectKey) throws GitlabClientException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GitlabClient client = clientFactory.get(integrationKey, true);

        if (StringUtils.isNotEmpty(repoName)) {
            return (int) client.streamProjectsByName(repoName).count();
        }

        return (int) client.streamProjects(true).count();
    }

    @Override
    public List<ScmRepository> getScmRepositories(String company, Integration integration, List<String> filterRepos, int pageNumber, int pageSize) throws GitlabClientException {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GitlabClient client = clientFactory.get(integrationKey, true);
        List<GitlabProject> projectList = client.getProjects(pageNumber, pageSize);

        if (CollectionUtils.isEmpty(projectList)) {
            return List.of();
        }

        return projectList.stream()
                .filter(i -> CollectionUtils.isEmpty(filterRepos) || filterRepos.contains(i.getName()))
                .map(gitlabProject -> ScmRepository.builder()
                        .name(gitlabProject.getName())
                        .description(gitlabProject.getDescription())
                        .url(gitlabProject.getWebUrl())
                        .updatedAt(gitlabProject.getLastActivityAt().toInstant().getEpochSecond())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ScmRepository> searchScmRepository(String company, Integration integration, String repoName, String projectKey, int pageNumber, int pageSize) throws Exception {

        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(company)
                .integrationId(integration.getId())
                .build();

        GitlabClient client = clientFactory.get(integrationKey, true);
        List<GitlabProject> projectList = client.getProjectByName(repoName, pageNumber, pageSize);

        if (CollectionUtils.isEmpty(projectList)) {
            return List.of();
        }

        List<ScmRepository> repoList = Lists.newArrayList();
        projectList.forEach(gitlabProject -> {
            ScmRepository repo = ScmRepository.builder()
                    .name(gitlabProject.getName())
                    .description(gitlabProject.getName())
                    .url(gitlabProject.getWebUrl())
                    .updatedAt(gitlabProject.getLastActivityAt().toInstant().getEpochSecond())
                    .build();
            repoList.add(repo);
        });

        return repoList;
    }
}
