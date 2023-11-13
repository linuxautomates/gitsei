package io.levelops.integrations.testrails.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

/**
 * TestRails's implementation of the {@link DataSource}. This class can be used to fetch custom case fields
 * from TestRails.
 */
@Log4j2
public class TestRailsCaseFieldDataSource implements DataSource<CaseField, TestRailsQuery> {
    private final TestRailsClientFactory clientFactory;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link TestRailsClientFactory} for fetching the {@link TestRailsClient}
     */
    public TestRailsCaseFieldDataSource(TestRailsClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<CaseField> fetchOne(TestRailsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the case fields from TestRails based on {@link TestRailsQuery}.
     * It makes calls to TestRails using the {@link TestRailsClient}.
     *
     * @param query {@link TestRailsQuery} used to fetch the projects
     * @return {@link Stream <Data<CaseField>>} containing all the fetched case fields
     * @throws FetchException If any error occurs while fetching the case fields
     */
    @Override
    public Stream<Data<CaseField>> fetchMany(TestRailsQuery query) throws FetchException {
        TestRailsClient testRailsClient = clientFactory.get(query.getIntegrationKey());
        try {
            return testRailsClient.getCaseFields().stream().map(BasicData.mapper(CaseField.class));
        } catch (TestRailsClientException e) {
            throw new RuntimeStreamException("Encountered testrails client error while fetching case fields for integration key: "
                    + query.getIntegrationKey(), e);
        }
    }
}
