package io.levelops.integrations.testrails.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.services.TestRailsEnrichmentService;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TestRails's implementation of the {@link DataSource}. This class can be used to fetch project data
 * from TestRails.
 */
@Log4j2
public class TestRailsProjectDataSource implements DataSource<Project, TestRailsQuery> {

    private final TestRailsClientFactory clientFactory;
    private final TestRailsEnrichmentService enrichmentService;

    private final Integer PAGE_LIMIT = 100;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link TestRailsClientFactory} for fetching the {@link TestRailsClient}
     * @param enrichmentService {@link TestRailsEnrichmentService} for enriching the projects
     */
    public TestRailsProjectDataSource(TestRailsClientFactory clientFactory,
                                      TestRailsEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<Project> fetchOne(TestRailsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the projects from TestRails based on {@link TestRailsQuery}.
     * It makes calls to TestRails using the {@link TestRailsClient}.
     *
     * @param query {@link TestRailsQuery} used to fetch the projects
     * @return {@link Stream<Data<Project>>} containing all the fetched projects
     * @throws FetchException If any error occurs while fetching the projects
     */
    @Override
    public Stream<Data<Project>> fetchMany(TestRailsQuery query) throws FetchException {
        TestRailsClient testRailsClient = clientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getPageData(testRailsClient, query, offset, PAGE_LIMIT);
            } catch (TestRailsClientException e) {
                log.warn("Encountered testrails client error for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered testrails client error for integration key: "
                        + query.getIntegrationKey(), e);
            }
        });
    }

    /**
     * Fetches {@link Project} using the {@code query}.
     *
     * @param testRailsClient {@link TestRailsClient} to make calls to TestRails
     * @param query           {@link TestRailsQuery} for fetching the test plans
     *                        {@link TestRailsProjectDataSource#PAGE_LIMIT} for the first page
     * @param offset          to be used for pagination purpose
     * @param pageLimit      to be used for pagination purpose
     * @return {@link List<Data<Project>>} containing all the fetched and enriched project based on pagination.
     * @throws TestRailsClientException If any error occurs while fetching the projects
     */
    private List<Data<Project>> getPageData(TestRailsClient testRailsClient, TestRailsQuery query,
                                            Integer offset, Integer pageLimit) throws TestRailsClientException {
        List<Project> projects = testRailsClient.getProjects(offset, pageLimit);
        List<Project> enrichedProjects = enrichmentService.enrichProjects(testRailsClient,
                query.getIntegrationKey(), projects);
        return enrichedProjects
                .stream()
                .map(BasicData.mapper(Project.class))
                .collect(Collectors.toList());
    }
}
