package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClient;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClientFactory;
import io.levelops.integrations.checkmarx.models.*;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class CxScaPreflightCheck implements PreflightCheck {
    public static final String CXSCA = "cxsca";
    private final CxScaClientFactory clientFactory;

    @Autowired
    public CxScaPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = CxScaClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    @Override
    public String getIntegrationType() {
        return CXSCA;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        CxScaClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException e) {
            log.error("check: error creating client for tenant: " + tenantId + " , integration: "
                    + integration + e.getMessage(), e);
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }
        CxScaProject project = null;
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get projects")
                .success(true);
        try {
            project = client.getProjectById("1");
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        builder.check(check.build());
        if (project != null) {
            builder.check(checkRiskReport(client, project.getId()));
        }

        check = PreflightCheckResult.builder()
                .name("Get scans")
                .success(true);
        CxScaScan scan = null;
        try {
            scan = client.getScanById("1");
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        builder.check(check.build());
        if (scan != null) {
            builder.check(checkRiskReportVulnerabilities(client, scan.getScanId()));
            builder.check(checkRiskReportPackages(client, scan.getScanId()));
            builder.check(checkRiskReportLicenses(client, scan.getScanId()));
        }
        return builder.build();
    }

    private PreflightCheckResult checkRiskReport(CxScaClient client, String projectId) {
        PreflightCheckResult.PreflightCheckResultBuilder builder = PreflightCheckResult.builder()
                .name("Get Risk Report")
                .success(true);
        try {
            CxScaRiskReport report = client.getRiskReportSummaries(projectId, Integer.MAX_VALUE);
            if (report == null) {
                builder.success(false).error("Scan Api failed");
            }
        } catch (Exception e) {
            builder.success(false).exception(e.getMessage());
        }
        return builder.build();
    }

    private PreflightCheckResult checkRiskReportLicenses(CxScaClient client, String scanId) {

        PreflightCheckResult.PreflightCheckResultBuilder builder = PreflightCheckResult.builder()
                .name("Get Risk Report Licenses")
                .success(true);
        try {
            List<CxScaRiskReportLicense> report = client.getLicenseForRiskReport(scanId);
            if (report == null) {
                builder.success(false).error("Scan Api failed");
            }
        } catch (Exception e) {
            builder.success(false).exception(e.getMessage());
        }
        return builder.build();
    }

    private PreflightCheckResult checkRiskReportVulnerabilities(CxScaClient client, String scanId) {

        PreflightCheckResult.PreflightCheckResultBuilder builder = PreflightCheckResult.builder()
                .name("Get Risk Report Vulnerabilities")
                .success(true);
        try {
            List<CxScaRiskReportVulnerability> report = client.getVulnerabilitiesForRiskReport(scanId);
            if (report == null) {
                builder.success(false).error("Scan Api failed");
            }
        } catch (Exception e) {
            builder.success(false).exception(e.getMessage());
        }
        return builder.build();
    }

    private PreflightCheckResult checkRiskReportPackages(CxScaClient client, String scanId) {

        PreflightCheckResult.PreflightCheckResultBuilder builder = PreflightCheckResult.builder()
                .name("Get Risk Report Packages")
                .success(true);
        try {
            List<CxScaRiskReportPackage> report = client.getPackagesForRiskReport(scanId);
            if (report == null) {
                builder.success(false).error("Scan Api failed");
            }
        } catch (Exception e) {
            builder.success(false).exception(e.getMessage());
        }
        return builder.build();
    }

}
