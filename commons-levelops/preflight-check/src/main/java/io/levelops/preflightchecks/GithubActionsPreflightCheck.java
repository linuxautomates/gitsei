package io.levelops.preflightchecks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.models.IntegrationType;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class GithubActionsPreflightCheck extends GithubPreflightCheck{

    @Autowired
    public GithubActionsPreflightCheck(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        super(inventoryService, objectMapper, okHttpClient);
    }

    @Override
    public String getIntegrationType() {
        return IntegrationType.GITHUB_ACTIONS.toString();
    }
}
