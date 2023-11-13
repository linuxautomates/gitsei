package io.levelops.integrations.testrails.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
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
import io.levelops.integrations.testrails.models.TestRun;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TestRails's implementation of the {@link DataSource}. This class can be used to fetch test run data
 * from TestRails.
 */
@Log4j2
public class TestRailsTestRunDataSource implements DataSource<TestRun, TestRailsQuery> {


    private final TestRailsClientFactory clientFactory;
    private final TestRailsEnrichmentService enrichmentService;

    private final Integer PAGE_LIMIT = 250;
    private final int BATCH_SIZE = 100;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link TestRailsClientFactory} for fetching the {@link TestRailsClient}
     * @param enrichmentService {@link TestRailsEnrichmentService} for enriching the test runs
     */
    public TestRailsTestRunDataSource(TestRailsClientFactory clientFactory,
                                      TestRailsEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<TestRun> fetchOne(TestRailsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the test runs from TestRails based on {@link TestRailsQuery}.
     * It makes calls to TestRails using the {@link TestRailsClient}.
     * Uses {@link PaginationUtils#stream(String, Function)} for fetching test runs by the cursor returned.
     *
     * @param query {@link TestRailsQuery} used to fetch the test runs
     * @return {@link Stream <Data<TestRun>>} containing all the fetched test runs for each project
     * @throws FetchException If any error occurs while fetching the test runs
     */
    @Override
    public Stream<Data<TestRun>> fetchMany(TestRailsQuery query) throws FetchException {
        TestRailsClient testRailsClient = clientFactory.get(query.getIntegrationKey());
        List<Project> projects = testRailsClient.getProjects()
                .collect(Collectors.toList());
        return projects.stream().flatMap(project -> PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getPageData(testRailsClient, query, offset, PAGE_LIMIT, project.getId());
            } catch (TestRailsClientException e) {
                log.warn("Encountered testrails client error for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered testrails client error for integration key: "
                        + query.getIntegrationKey(), e);
            }
        }));
    }

    /**
     * Fetches {@link TestRun} using the {@code query}.
     *
     * @param testRailsClient {@link TestRailsClient} to make calls to TestRails
     * @param query           {@link TestRailsQuery} for fetching the test runs
     *                        {@link TestRailsTestRunDataSource#PAGE_LIMIT} for the first page
     * @param offset          to be used for pagination purpose
     * @param pageLimit       to be used for pagination purpose
     * @param projectId       to be used to fetch test runs of project with this id
     * @return {@link List<Data<TestRun>>} containing all the fetched and enriched test run of a project
     * based on pagination.
     * @throws TestRailsClientException If any error occurs while fetching the test runs
     */
    private List<Data<TestRun>> getPageData(TestRailsClient testRailsClient, TestRailsQuery query,
                                            Integer offset, Integer pageLimit, Integer projectId) throws TestRailsClientException {
        List<TestRun> testRuns = testRailsClient.getTestRuns(projectId, offset, pageLimit);
        List<TestRun> enrichedTestRuns = enrichmentService.enrichTestRuns(testRailsClient,
                query.getIntegrationKey(), testRuns, projectId);
        return enrichedTestRuns
                .stream()
                .flatMap(testRun -> StreamUtils.partition(testRun.getTests().stream(), BATCH_SIZE)
                        .map(batch -> testRun.toBuilder().tests(batch).build()))
                .map(BasicData.mapper(TestRun.class))
                .collect(Collectors.toList());
    }
}
