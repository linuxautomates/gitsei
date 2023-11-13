package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.Project;
import io.levelops.integrations.sonarqube.models.PullRequestResponse;
import io.levelops.integrations.sonarqube.models.IssueResponse;
import io.levelops.integrations.sonarqube.models.ProjectAnalysesResponse;
import io.levelops.integrations.sonarqube.models.ProjectBranchResponse;
import io.levelops.integrations.sonarqube.models.QualityGateResponse;
import io.levelops.integrations.sonarqube.models.Group;
import io.levelops.integrations.sonarqube.models.UserGroupResponse;
import io.levelops.integrations.sonarqube.models.UserResponse;
import io.levelops.integrations.sonarqube.models.ProjectResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static io.levelops.integrations.sonarqube.client.SonarQubeClient.*;

@Log4j2
@Component
public class SonarQubePreflightCheck implements PreflightCheck {

    private static final String SONARQUBE = "sonarqube";

    private static final int RESPONSE_PAGE_SIZE = 20;

    private final SonarQubeClientFactory clientFactory;

    @Autowired
    public SonarQubePreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = SonarQubeClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link SonarQubePreflightCheck#SONARQUBE}
     */
    @Override
    public String getIntegrationType() {
        return SONARQUBE;
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
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        Boolean usePrivilegedAPIs = integration.getMetadata() != null && (boolean) integration.getMetadata().getOrDefault("use_privileged_APIs", false);
        SonarQubeClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, 1);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkQualityGates(client));
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(LIST_USER_GROUP_API_PATH)
                .success(true);
        String groupName = null;
        try {
            if (usePrivilegedAPIs) {
                UserGroupResponse response = client.getUserGroups(1);
                if (response == null || response.getGroups() == null)
                    checkResultBuilder.success(false).error("response from LIST_USER_GROUP_API_PATH returned null result");
                else {
                    groupName = response.getGroups().stream()
                            .findFirst()
                            .map(Group::getName)
                            .orElse(null);
                }
            }
        } catch (SonarQubeClientException e) {
            log.error("checkUserGroups: encountered error while fetching UserGroups: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        builder.check(checkResultBuilder.build());
        if (usePrivilegedAPIs)
            builder.check(checkUsers(client, groupName));
        builder.check(checkProject(client, usePrivilegedAPIs));
        return builder.build();
    }

    /**
     * validates the response from {@link SonarQubeClient#getQualityGates()}
     *
     * @param client {@link SonarQubeClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkQualityGates(SonarQubeClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(LIST_QUALITY_GATES_API_PATH)
                .success(true);
        try {
            QualityGateResponse response = client.getQualityGates();
            if (response == null || response.getQualitygates() == null)
                checkResultBuilder.success(false).error("response from LIST_QUALITY_GATES_API_PATH returned null result");
        } catch (SonarQubeClientException e) {
            log.error("checkQualityGates: encountered error while fetching QualityGates: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link SonarQubeClient#getUsers(String, int)}
     *
     * @param client {@link SonarQubeClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkUsers(SonarQubeClient client, String groupName) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(LIST_USERS_API_PATH)
                .success(true);
        try {
            UserResponse response = client.getUsers(groupName, 1);
            if (response == null || response.getUsers() == null)
                checkResultBuilder.success(false).error("response from LIST_USERS_API_PATH returned null result");
        } catch (SonarQubeClientException e) {
            log.error("checkUsers: encountered error while fetching users: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from
     *
     * @param client {@link SonarQubeClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkProject(SonarQubeClient client, Boolean usePrivilegedAPIs) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(SEARCH_PROJECTS_API_PATH)
                .success(true);
        try {
            ProjectResponse projectResponse = client.getProjects(usePrivilegedAPIs, 1);
            if (!projectResponse.getProjects().isEmpty()) {
                Project project = projectResponse.getProjects().get(0);
                checkProjectAnalyses(client, project);
                checkProjectBranches(client, project);
                checkPullRequest(client, project);
            }
        } catch (SonarQubeClientException e) {
            log.error("checkProject: encountered error while fetching project: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link SonarQubeClient#getProjectAnalyses(String, Date, Date, int)}
     *
     * @param client  {@link SonarQubeClient} with authentication interceptor
     * @param project for given project
     */
    private void checkProjectAnalyses(SonarQubeClient client, Project project) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(SEARCH_PROJECT_ANALYSES_API_PATH)
                .success(true);
        try {
            ProjectAnalysesResponse response = client.getProjectAnalyses(project.getKey(), null, null, 1);
            project.toBuilder().analyses(response.getAnalyses()).build();
            if (response.getAnalyses() == null)
                checkResultBuilder.success(false).error("response from SEARCH_PROJECT_ANALYSES_API_PATH returned null result");
        } catch (SonarQubeClientException e) {
            log.error("checkProjectAnalyses: encountered error while fetching project analyses: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
    }

    /**
     * validates the response from {@link SonarQubeClient#getProjectBranches(String)}
     *
     * @param client  {@link SonarQubeClient} with authentication interceptor
     * @param project for given project
     */
    private void checkProjectBranches(SonarQubeClient client, Project project) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(LIST_PROJECT_BRANCHES_API_PATH)
                .success(true);
        try {
            ProjectBranchResponse response = client.getProjectBranches(project.getKey());
            project.toBuilder().branches(response.getBranches()).build();
            if (response.getBranches() == null)
                checkResultBuilder.success(false).error("response from LIST_PROJECT_BRANCHES_API_PATH returned null result");
        } catch (SonarQubeClientException e) {
            log.error("checkProjectBranches: encountered error while fetching project branches: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
    }

    /**
     * validates the response from {@link SonarQubeClient#getPullRequests(String)}
     *
     * @param client  {@link SonarQubeClient} with authentication interceptor
     * @param project for given project
     */
    private void checkPullRequest(SonarQubeClient client, Project project) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(LIST_PULL_REQUEST_API_PATH)
                .success(true);
        try {
            PullRequestResponse response = client.getPullRequests(project.getKey());
            if (!response.getPullRequests().isEmpty()) {
                String pullRequestId = response.getPullRequests().get(0).getKey();
                checkIssues(client, project, response, pullRequestId);
                project.toBuilder().pullRequests(response.getPullRequests()).build();
            }
        } catch (SonarQubeClientException e) {
            log.error("checkPullRequest: encountered error while fetching pull requests: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
    }

    /**
     * validates the response from {@link SonarQubeClient#getIssues(String, String, Date, Date, String, String, String, String, int)}
     *
     * @param client      {@link SonarQubeClient} with authentication interceptor
     * @param project     for given project
     * @param pullRequest for given pull request id
     */
    private void checkIssues(SonarQubeClient client, Project project, PullRequestResponse pullRequestResponse, String pullRequest) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name(SEARCH_ISSUES_API_PATH)
                .success(true);
        try {
            IssueResponse response = client.getIssues(project.getKey(), pullRequest, null, null, null, null, null, null, 1);
            pullRequestResponse.getPullRequests().get(0).toBuilder().issues(response.getIssues()).build();
            if (response.getIssues() == null)
                checkResultBuilder.success(false).error("response from SEARCH_ISSUES_API_PATH returned null result");
        } catch (SonarQubeClientException e) {
            log.error("checkIssues: encountered error while fetching issues: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
    }
}
