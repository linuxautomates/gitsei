package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.SnykControllers;
import io.levelops.ingestion.agent.ingestion.SnykIterativeScanController;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.sources.SnykAllProjectsDataSource;
import io.levelops.integrations.snyk.sources.SnykDepGraphDataSource;
import io.levelops.integrations.snyk.sources.SnykIssueDataSource;
import io.levelops.integrations.snyk.sources.SnykOrgDataSource;
import io.levelops.integrations.snyk.sources.SnykProjectDataSource;
import io.levelops.integrations.snyk.sources.SnykProjectDataSource.SnykProjectQuery;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnykConfiguration {

    @Bean
    public SnykClientFactory snykClientFactory(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        return new SnykClientFactory(inventoryService, objectMapper, okHttpClient);
    }

    @Bean
    public SnykIterativeScanController snykIterativeScanController(
            IngestionEngine ingestionEngine,
            @Qualifier("onboardingInDays") int onboardingInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            SnykClientFactory snykClientFactory) {

        SnykOrgDataSource snykOrgDataSource = ingestionEngine.add("SnykOrgDataSource",
                new SnykOrgDataSource(snykClientFactory));

        IntegrationController<BaseIntegrationQuery> snykOrgsController = ingestionEngine.add("SnykOrgsController", SnykControllers.orgsController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(snykOrgDataSource)
                .build());

        SnykProjectDataSource snykProjectDataSource = ingestionEngine.add("SnykProjectDataSource",
                new SnykProjectDataSource(snykClientFactory));

        IntegrationController<SnykProjectQuery> snykProjectsController = ingestionEngine.add("SnykProjectsController", SnykControllers.projectsController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(snykProjectDataSource)
                .build());

        SnykAllProjectsDataSource snykAllProjectsDataSource = ingestionEngine.add("SnykAllProjectsDataSource",
                new SnykAllProjectsDataSource(snykClientFactory));

        IntegrationController<BaseIntegrationQuery> snykAllProjectsController = ingestionEngine.add("SnykAllProjectsController", SnykControllers.allProjectsController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(snykAllProjectsDataSource)
                .build());

        SnykIssueDataSource snykIssueDataSource = ingestionEngine.add("SnykIssueDataSource",
                new SnykIssueDataSource(snykClientFactory));

        IntegrationController<BaseIntegrationQuery> snykIssuesController = ingestionEngine.add("SnykIssuesController", SnykControllers.issuesController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(snykIssueDataSource)
                .build());

        SnykDepGraphDataSource snykDepGraphDataSource = ingestionEngine.add("SnykDepGraphDataSource",
                new SnykDepGraphDataSource(snykClientFactory));

        IntegrationController<BaseIntegrationQuery> snykDepGraphController = ingestionEngine.add("SnykDepGraphController", SnykControllers.depGraphController()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(snykDepGraphDataSource)
                .build());


        return ingestionEngine.add("SnykIterativeScanController", SnykIterativeScanController
                .builder()
                .objectMapper(objectMapper)
                .orgsController(snykOrgsController)
                .projectsController(snykAllProjectsController)
                .issuesController(snykIssuesController)
                .depGraphController(snykDepGraphController)
                .build());
    }

}
