package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClient;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class CxSastPreflightCheck implements PreflightCheck {

    public static final String CXSAST = "cxsast";
    private final CxSastClientFactory clientFactory;

    @Autowired
    public CxSastPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        clientFactory = CxSastClientFactory.builder()
                .okHttpClient(okHttpClient)
                .objectMapper(objectMapper)
                .build();
    }

    @Override
    public String getIntegrationType() {
        return CXSAST;
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .allChecksMustPass()
                .tenantId(tenantId)
                .integration(integration);
        CxSastClient client;
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
        builder.check(checkProject(client));
        builder.check(checkScans(client));
        return builder.build();
    }

    private PreflightCheckResult checkProject(CxSastClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get projects")
                .success(true);
        try {
            List<CxSastProject> projects = client.getProjects();
            if (projects == null) {
                check.success(false).error("Project Api failed");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

    private PreflightCheckResult checkScans(CxSastClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get Scans")
                .success(true);
        try {
            List<CxSastScan> scans = client.getScans();
            if (scans == null) {
                check.success(false).error("Scan Api failed");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }

}
