package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.integrations.snyk.client.SnykClient;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.api.SnykApiListOrgsResponse;
import io.levelops.models.PreflightCheckResult;
import io.levelops.models.PreflightCheckResults;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class SnykPreflightCheck implements PreflightCheck  {
    @Getter
    private final String integrationType = "snyk";
    private final SnykClientFactory clientFactory;

    public SnykPreflightCheck(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = SnykClientFactory.builder()
                .objectMapper(objectMapper).okHttpClient(okHttpClient)
                .build();
    }

    @Override
    public PreflightCheckResults check(String tenantId, Integration integration, Token token) {
        PreflightCheckResults.PreflightCheckResultsBuilder builder = PreflightCheckResults.builder()
                .tenantId(tenantId)
                .integration(integration)
                .allChecksMustPass();

        SnykClient client;
        try {
            client = clientFactory.buildFromToken(tenantId, integration, token);
        } catch (InventoryException | NullPointerException e) {
            return builder
                    .success(false)
                    .exception(e.getMessage())
                    .build();
        }

        builder.check(checkOrgs(client));

        return builder.build();
    }

    private PreflightCheckResult checkOrgs(SnykClient client) {
        PreflightCheckResult.PreflightCheckResultBuilder check = PreflightCheckResult.builder()
                .name("Get orgs")
                .success(true);
        try {
            SnykApiListOrgsResponse snykApiListOrgsResponse = client.getOrgs();
            if((snykApiListOrgsResponse== null) || (CollectionUtils.isEmpty(snykApiListOrgsResponse.getOrgs()))){
                check.success(false).error("Get orgs did not return any data");
            }
        } catch (Exception e) {
            check.success(false).exception(e.getMessage());
        }
        return check.build();
    }
}
