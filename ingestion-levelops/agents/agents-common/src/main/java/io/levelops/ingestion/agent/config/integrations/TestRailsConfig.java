package io.levelops.ingestion.agent.config.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.agent.ingestion.TestRailsController;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import io.levelops.integrations.testrails.sources.TestRailsCaseFieldDataSource;
import io.levelops.integrations.testrails.sources.TestRailsProjectDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestSuiteDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestPlanDataSource;
import io.levelops.integrations.testrails.sources.TestRailsTestRunDataSource;
import io.levelops.integrations.testrails.sources.TestRailsUserDataSource;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
* Configuration class for TestRails integration. Adds the framework entities into the ingestion engine
*/
@Configuration
public class TestRailsConfig {

    private static final String TESTRAILS_CONTROLLER = "TestRailsController";
    private static final String TESTRAILS_USER_DATA_SOURCE = "TestRailsUserDataSource";
    private static final String TESTRAILS_PROJECT_DATA_SOURCE = "TestRailsProjectDataSource";
    private static final String TESTRAILS_TEST_PLAN_DATA_SOURCE = "TestRailsTestPlanDataSource";
    private static final String TESTRAILS_TEST_RUN_DATA_SOURCE = "TestRailsTestRunDataSource";
    private static final String TESTRAILS_CASE_FIELD_DATA_SOURCE = "TestRailsCaseFieldDataSource";
    private static final String TESTRAILS_TEST_SUITE_DATA_SOURCE = "TestRailsTestSuiteDataSource";


    @Bean
    public TestRailsClientFactory testRailsClientFactory(InventoryService inventoryService, ObjectMapper objectMapper,
                                                         OkHttpClient okHttpClient,
                                                         @Value("${testrails_response_page_size:200}") int pageSize) {
        return TestRailsClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .pageSize(pageSize)
                .build();
    }

    @Bean
    public TestRailsEnrichmentService enrichmentService(@Value("${testrails_fork_threshold:32}") int forkThreshold,
                                                        @Value("${testrails_thread_count:8}") int threadCount) {
        return new TestRailsEnrichmentService(threadCount, forkThreshold);
    }

    @Bean
    public TestRailsUserDataSource testRailsUserDataSource(IngestionEngine ingestionEngine,
                                                           TestRailsClientFactory clientFactory) {
        return ingestionEngine.add(TESTRAILS_USER_DATA_SOURCE, new TestRailsUserDataSource(clientFactory));
    }

    @Bean
    public TestRailsProjectDataSource testRailsProjectDataSource(IngestionEngine ingestionEngine,
                                                                 TestRailsClientFactory testRailsClientFactory,
                                                                 TestRailsEnrichmentService enrichmentService) {
        return ingestionEngine.add(TESTRAILS_PROJECT_DATA_SOURCE,
                new TestRailsProjectDataSource(testRailsClientFactory, enrichmentService));
    }

    @Bean
    public TestRailsTestPlanDataSource testRailsTestPlanDataSource(IngestionEngine ingestionEngine,
                                                               TestRailsClientFactory clientFactory,
                                                                   TestRailsEnrichmentService enrichmentService) {
        return ingestionEngine.add(TESTRAILS_TEST_PLAN_DATA_SOURCE,
                new TestRailsTestPlanDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public TestRailsTestRunDataSource testRailsTestRunDataSource(IngestionEngine ingestionEngine,
                                                                 TestRailsClientFactory clientFactory,
                                                                 TestRailsEnrichmentService enrichmentService) {
        return ingestionEngine.add(TESTRAILS_TEST_RUN_DATA_SOURCE,
                new TestRailsTestRunDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public TestRailsCaseFieldDataSource testRailsCaseFieldDataSource(IngestionEngine ingestionEngine,
                                                                 TestRailsClientFactory clientFactory) {
        return ingestionEngine.add(TESTRAILS_CASE_FIELD_DATA_SOURCE,
                new TestRailsCaseFieldDataSource(clientFactory));
    }

    @Bean
    public TestRailsTestSuiteDataSource testRailsTestSuiteDataSource(IngestionEngine ingestionEngine,
                                                                   TestRailsClientFactory clientFactory,
                                                                   TestRailsEnrichmentService enrichmentService) {
        return ingestionEngine.add(TESTRAILS_TEST_SUITE_DATA_SOURCE,
                new TestRailsTestSuiteDataSource(clientFactory, enrichmentService));
    }

    @Bean
    public TestRailsController testRailsController(IngestionEngine ingestionEngine,
                                                   TestRailsProjectDataSource projectDataSource,
                                                   TestRailsUserDataSource userDataSource,
                                                   TestRailsTestPlanDataSource testPlanDataSource,
                                                   TestRailsTestRunDataSource testRunDataSource,
                                                   TestRailsCaseFieldDataSource caseFieldDataSource,
                                                   TestRailsTestSuiteDataSource testSuiteDataSource,
                                                   ObjectMapper objectMapper, StorageDataSink storageDataSink,
                                                   @Qualifier("onboardingInDays") int onboardingInDays) {
        return ingestionEngine.add(TESTRAILS_CONTROLLER, TestRailsController.builder()
                .projectDataSource(projectDataSource)
                .userDataSource(userDataSource)
                .testPlanDataSource(testPlanDataSource)
                .testRunDataSource(testRunDataSource)
                .caseFieldDataSource(caseFieldDataSource)
                .testSuiteDataSource(testSuiteDataSource)
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .onboardingScanInDays(onboardingInDays)
                .build());
    }
}
