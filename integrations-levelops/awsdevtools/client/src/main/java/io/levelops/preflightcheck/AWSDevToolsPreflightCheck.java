package io.levelops.preflightcheck;

import com.amazonaws.services.codebuild.model.*;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import io.levelops.preflightchecks.PreflightCheck;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link PreflightCheck} for AWSDevTools integration
 */
@Log4j2
@Component
public class AWSDevToolsPreflightCheck implements PreflightCheck {

    private static final String AWS_DEV_TOOLS = "awsdevtools";
    private static final int RESPONSE_PAGE_SIZE = 1;

    private final AWSDevToolsClientFactory clientFactory;

    public AWSDevToolsPreflightCheck() {
        this.clientFactory = AWSDevToolsClientFactory.builder()
                .pageSize(RESPONSE_PAGE_SIZE)
                .build();
    }

    /**
     * returns the integration type
     *
     * @return {@link String} returns {@link AWSDevToolsPreflightCheck#AWS_DEV_TOOLS}
     */
    @Override
    public String getIntegrationType() {
        return AWS_DEV_TOOLS;
    }

    /**
     * checks the validity of {@link Integration} and {@link Token} by calling list of projects, builds, build batches,
     * reports and report groups. Validates successful response.
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
        Map<String, Object> integrationMetadata = integration.getMetadata();
        if (integrationMetadata == null || !integrationMetadata.containsKey("regions") || integrationMetadata.get("regions") == null) {
            throw new InvalidInputException("regions cannot be null");
        }
        String region = Splitter.on(",").omitEmptyStrings().trimResults().splitToList((String) integrationMetadata.get("regions"))
                .stream().findFirst().orElseThrow();
        AWSDevToolsClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token, region);
        } catch (InventoryException e) {
            log.error("check: error creating client for awsdevtools: " + tenantId + " , integration: "
                    + integration + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        builder.check(checkProjects(client));
        builder.check(checkBuilds(client));
        builder.check(checkBuildBatches(client));
        builder.check(checkReports(client));
        builder.check(checkReportGroups(client));
        return builder.build();
    }

    /**
     * validates the response from {@link AWSDevToolsClient#listProjects(String)},
     * {@link AWSDevToolsClient#getProjects(AWSDevToolsQuery, List)}
     *
     * @param client {@link AWSDevToolsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkProjects(AWSDevToolsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/projects")
                .success(true);
        try {
            ListProjectsResult listProjectsResult = client.listProjects(null);
            if (listProjectsResult == null || listProjectsResult.getProjects() == null) {
                checkResultBuilder.success(false).error("response from /projects returned null result");
            } else {
                List<Project> response = client.getProjects(AWSDevToolsQuery.builder()
                        .from(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                        .to(Date.from(Instant.now()))
                        .build(), listProjectsResult.getProjects());
                if (response == null)
                    checkResultBuilder.success(false).error("response from /projects returned null result");
            }
        } catch (Exception e) {
            log.error("checkTicket: encountered error while fetching projects: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link AWSDevToolsClient#listBuilds(String)},
     * {@link AWSDevToolsClient#getBuilds(AWSDevToolsQuery, List)}
     *
     * @param client {@link AWSDevToolsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuilds(AWSDevToolsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/builds")
                .success(true);
        try {
            ListBuildsResult listBuildsResult = client.listBuilds(null);
            if (listBuildsResult == null || listBuildsResult.getIds() == null) {
                checkResultBuilder.success(false).error("response from /builds returned null result");
            } else {
                List<Build> response = client.getBuilds(AWSDevToolsQuery.builder()
                        .from(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                        .to(Date.from(Instant.now()))
                        .build(), listBuildsResult.getIds());
                if (response == null)
                    checkResultBuilder.success(false).error("response from /builds returned null result");
            }
        } catch (Exception e) {
            log.error("checkTicket: encountered error while fetching builds: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link AWSDevToolsClient#listBuildBatches(String)},
     * {@link AWSDevToolsClient#getBuildBatches(AWSDevToolsQuery, List)}
     *
     * @param client {@link AWSDevToolsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkBuildBatches(AWSDevToolsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/buildBatches")
                .success(true);
        try {
            ListBuildBatchesResult listBuildBatchesResult = client.listBuildBatches(null);
            if (listBuildBatchesResult == null || listBuildBatchesResult.getIds() == null) {
                checkResultBuilder.success(false).error("response from /buildBatches returned null result");
            } else {
                List<BuildBatch> response = client.getBuildBatches(AWSDevToolsQuery.builder()
                        .from(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)))
                        .to(Date.from(Instant.now()))
                        .build(), listBuildBatchesResult.getIds());
                if (response == null)
                    checkResultBuilder.success(false).error("response from /buildBatches returned null result");
            }
        } catch (Exception e) {
            log.error("checkTicket: encountered error while fetching buildBatches: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link AWSDevToolsClient#listReports(String)},
     * {@link AWSDevToolsClient#getReports(List)}
     *
     * @param client {@link AWSDevToolsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkReports(AWSDevToolsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/reports")
                .success(true);
        try {
            ListReportsResult listReportsResult = client.listReports(null);
            if (listReportsResult == null || listReportsResult.getReports() == null) {
                checkResultBuilder.success(false).error("response from /reports returned null result");
            } else {
                List<Report> response = client.getReports(listReportsResult.getReports());
                if (response == null)
                    checkResultBuilder.success(false).error("response from /reports returned null result");
            }
        } catch (Exception e) {
            log.error("checkTicket: encountered error while fetching reports: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }

    /**
     * validates the response from {@link AWSDevToolsClient#listReportGroups(String)},
     * {@link AWSDevToolsClient#getReportGroups(List)}
     *
     * @param client {@link AWSDevToolsClient} with authentication interceptor
     * @return {@link PreflightCheckResult} for the validation
     */
    private PreflightCheckResult checkReportGroups(AWSDevToolsClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder checkResultBuilder = PreflightCheckResult.builder()
                .name("/reportGroups")
                .success(true);
        try {
            ListReportGroupsResult listReportGroupsResult = client.listReportGroups(null);
            if (listReportGroupsResult == null || listReportGroupsResult.getReportGroups() == null) {
                checkResultBuilder.success(false).error("response from /reportGroups returned null result");
            } else {
                List<ReportGroup> response = client.getReportGroups(listReportGroupsResult.getReportGroups());
                if (response == null)
                    checkResultBuilder.success(false).error("response from /reportGroups returned null result");
            }
        } catch (Exception e) {
            log.error("checkTicket: encountered error while fetching reportGroups: " + e.getMessage(), e);
            checkResultBuilder.success(false).exception(e.getMessage());
        }
        return checkResultBuilder.build();
    }
}
