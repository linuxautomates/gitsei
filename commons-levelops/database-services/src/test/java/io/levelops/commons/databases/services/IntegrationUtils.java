package io.levelops.commons.databases.services;



import io.levelops.commons.databases.models.database.Integration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IntegrationUtils {
    public static Integration createIntegration(IntegrationService integrationService, String company, int i, String application) throws SQLException {
        Integration integration = Integration.builder()
                .name("name" + i)
                .url("http")
                .status("good")
                .application(application)
                .description("desc")
                .satellite(true)
                .build();
        String integrationId = integrationService.insert(company, integration);
        return integration.toBuilder().id(integrationId).build();
    }

    public static Integration createIntegration(IntegrationService integrationService, String company, int i) throws SQLException {
        return createIntegration(integrationService, company, i, "jira");
    }

    public static List<Integration> createIntegrations(IntegrationService integrationService, String company, int n) throws SQLException {
        List<Integration> integrations = new ArrayList<>();
        for (int i =0; i<n; i++) {
            Integration integration = IntegrationUtils.createIntegration(integrationService, company, i);
            integrations.add(integration);
        }
        return integrations;
    }
}
