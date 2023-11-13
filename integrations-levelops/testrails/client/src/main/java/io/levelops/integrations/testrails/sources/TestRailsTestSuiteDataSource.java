package io.levelops.integrations.testrails.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.TestRailsTestSuite;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TestRails's implementation of the {@link DataSource}. This class can be used to fetch test suites with testcases data
 * from TestRails.
 */
@Log4j2
public class TestRailsTestSuiteDataSource implements DataSource<TestRailsTestSuite, TestRailsQuery> {

    private final TestRailsClientFactory clientFactory;
    private final TestRailsEnrichmentService enrichmentService;
    private final Integer BATCH_SIZE = 100;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link TestRailsClientFactory} for fetching the {@link TestRailsClient}
     * @param enrichmentService {@link TestRailsEnrichmentService} for enriching the testcases for test suites
     */
    public TestRailsTestSuiteDataSource(TestRailsClientFactory clientFactory,
                                        TestRailsEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<TestRailsTestSuite> fetchOne(TestRailsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the testcases from TestRails based on {@link TestRailsQuery}.
     * It makes calls to TestRails using the {@link TestRailsClient}.
     *
     * @param query {@link TestRailsQuery} used to fetch the testcases
     * @return {@link Stream<Data<TestRailsTestSuite>>} containing all the fetched test suites with testcases
     * @throws FetchException If any error occurs while fetching the test suites
     */
    @Override
    public Stream<Data<TestRailsTestSuite>> fetchMany(TestRailsQuery query) throws FetchException {
        TestRailsClient testRailsClient = clientFactory.get(query.getIntegrationKey());
        List<Project> projects = testRailsClient.getProjects()
                .collect(Collectors.toList());
        return projects.stream().flatMap(project -> {
            try {
                return getPageData(testRailsClient, query, project.getId());
            } catch (TestRailsClientException e) {
                throw new RuntimeStreamException("Encountered testrails client error while fetching test suites with testcase for integration key: "
                        + query.getIntegrationKey() + " project id: " + project.getId(), e);
            }
        });
    }

    /**
     * Fetches {@link TestRailsTestSuite} using the {@code query}.
     *
     * @param testRailsClient {@link TestRailsClient} to make calls to TestRails
     * @param query           {@link TestRailsQuery} for fetching the test suites
     * @param projectId           {@link Integer} project id for which test suites are fetched
     * @return {@link List<Data<TestRailsTestSuite>>} containing all the fetched and enriched testcase based on pagination.
     * @throws TestRailsClientException If any error occurs while fetching the test suites
     */
    private Stream<Data<TestRailsTestSuite>> getPageData(TestRailsClient testRailsClient, TestRailsQuery query, Integer projectId) throws TestRailsClientException {
        List<TestRailsTestSuite> testSuites = testRailsClient.getTestSuites(projectId);
        List<TestRailsTestSuite> enrichTestSuites = enrichmentService.enrichTestSuites(testRailsClient,
                query.getIntegrationKey(), testSuites, projectId);
        return enrichTestSuites
                .stream()
                .flatMap(testSuite -> StreamUtils.partition(testSuite.getTestCases().stream(), BATCH_SIZE)
                        .map(batch -> testSuite.toBuilder().testCases(batch).build()))
                .map(BasicData.mapper(TestRailsTestSuite.class));
    }
}
