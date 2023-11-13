package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.blackduck.BlackDuckClient;
import io.levelops.integrations.blackduck.BlackDuckClientException;
import io.levelops.integrations.blackduck.BlackDuckClientFactory;
import io.levelops.integrations.blackduck.models.BlackDuckIssue;
import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckVersion;
import io.levelops.integrations.blackduck.utils.BlackDuckUtils;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class BlackDuckPreFlightCheck implements PreflightCheck {

    private static final String BLACKDUCK = "blackduck";
    private static final int RESPONSE_PAGE_SIZE = 1;

    private final BlackDuckClientFactory clientFactory;

    @Autowired
    public BlackDuckPreFlightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = BlackDuckClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link BlackDuckPreFlightCheck#BLACKDUCK}
     */
    @Override
    public String getIntegrationType() {
        return BLACKDUCK;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder parentChecks = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        BlackDuckClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, 1);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: " + integration
                    + e.getMessage(), e);
            return parentChecks
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        List<BlackDuckProject> blackDuckProjects = checkProjects(client, parentChecks);
        if (blackDuckProjects != null && !blackDuckProjects.isEmpty()) {
            List<BlackDuckVersion> blackDuckVersions = checkVersion(BlackDuckUtils.extractIdsFromProjects(blackDuckProjects), client, parentChecks);
            checkIssues(BlackDuckUtils.extractIdsFromProjects(blackDuckProjects), BlackDuckUtils.extractIdsFromVersions(blackDuckVersions), client, parentChecks);
        }
        return parentChecks.build();
    }

    private List<BlackDuckProject> checkProjects(BlackDuckClient client,
                                                 PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder projectsCheck = PreflightCheckResult.builder()
                .name("/project")
                .success(true);
        List<BlackDuckProject> projects = new ArrayList<>();
        try {
            projects = client.getProjects();
            if (CollectionUtils.isEmpty(projects)) {
                projectsCheck.success(true);
            }

        } catch (BlackDuckClientException e) {
            projectsCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(projectsCheck.build());
        return projects;
    }

    private List<BlackDuckVersion> checkVersion(List<String> projectIds, BlackDuckClient client,
                                                PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder versionChecks = PreflightCheckResult.builder()
                .name("/versions")
                .success(true);
        List<BlackDuckVersion> versions = new ArrayList<>();
        try {
            versions = client.getVersions(projectIds.stream().findAny().orElseThrow());
            if (CollectionUtils.isEmpty((versions))) {
                versionChecks.success(true);
            }

        } catch (BlackDuckClientException e) {
            versionChecks.success(false).exception(e.getMessage());
        }
        parentChecks.check(versionChecks.build());
        return versions;
    }

    private void checkIssues(List<String> projectIds, List<String> versionIds, BlackDuckClient client,
                             PreflightCheckResults.PreflightCheckResultsBuilder parentChecks) {
        PreflightCheckResult.PreflightCheckResultBuilder issuesCheck = PreflightCheckResult.builder()
                .name("/issues")
                .success(true);
        List<BlackDuckIssue> issues;
        try {
            issues = client.getIssues(projectIds.stream().findAny().orElseThrow(), versionIds.stream().findAny().orElseThrow(),0);
            if (CollectionUtils.isEmpty((issues))) {
                issuesCheck.success(true);
            }

        } catch (BlackDuckClientException e) {
            issuesCheck.success(false).exception(e.getMessage());
        }
        parentChecks.check(issuesCheck.build());
    }
}
