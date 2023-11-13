package io.levelops.integrations.salesforce.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.salesforce.client.SalesforceClient;
import io.levelops.integrations.salesforce.client.SalesforceClientException;
import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.SOQLJobResponse;
import io.levelops.integrations.salesforce.models.SalesforceEntity;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.SalesforcePaginatedResponse;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class provides a skeleton implementation of the {@link DataSource} to minimize the efforts required for
 * fetching entities from the SalesForce
 */
@Log4j2
public abstract class SalesforceDataSource<D extends SalesforceEntity> implements DataSource<D, SalesforceIngestionQuery> {

    private static final String STARTING_LOCATOR = null;
    public static final String END_LOCATOR = "null";

    private final SalesforceClientFactory clientFactory;

    protected SalesforceDataSource(SalesforceClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<D> fetchOne(SalesforceIngestionQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the entities from the SalesForce based on the {@link SalesforceIngestionQuery}.
     * It uses {@link PaginationUtils#stream(String, Function)} for fetching assets based on the locator header returned.
     * @param query {@link SalesforceIngestionQuery} used to fetch the entity
     * @return all the fetched entities
     * @throws FetchException If any error occurs while fetching the entities
     */
    @Override
    public Stream<Data<D>> fetchMany(SalesforceIngestionQuery query) throws FetchException {
        SalesforceClient salesForceClient = clientFactory.get(query.getIntegrationKey());
        try {
            SOQLJobResponse SOQLJobResponse = salesForceClient.createQueryJob("query", getSOQLStatement(query));
            if (!SOQLJobResponse.getState().equalsIgnoreCase("JobComplete")) {
                throw new FetchException("Query Job not completed within " +
                        "given time bound. Integration key: " + query.getIntegrationKey());
            }
            return PaginationUtils.stream(STARTING_LOCATOR, locator ->
                    getResults(salesForceClient, SOQLJobResponse.getId(), locator));
        } catch (InterruptedException e) {
            throw new FetchException("Encountered exception while fetching Job submit status", e);
        }
    }

    /**
     * Fetches the entities using {@link SalesforceClient}, {@param jobId} and {@param locator}.
     * It puts the cursor for the next page in the returned {@link PaginationUtils.CursorPageData}
     * for fetching the next page. Returns {@code null} when {@code cursor} is {@code null} (denotes end of pages).
     * @param salesForceClient {@link SalesforceClient} to make calls to salesforce.
     * @param jobId id of the query job created
     * @param locator address of the query job result set
     * @return {@link PaginationUtils.CursorPageData} with the {@code cursor} for the next page
     */
    protected PaginationUtils.CursorPageData<Data<D>> getResults(SalesforceClient salesForceClient, String jobId, String locator) {
        try {
            SalesforcePaginatedResponse<D> data = salesForceClient.getQueryResults(jobId, locator, getType());
            String nextLocator = data.getSalesForceLocator();
            if (nextLocator.equalsIgnoreCase(END_LOCATOR)) {
                nextLocator = null;
            }
            return PaginationUtils.CursorPageData.<Data<D>>builder()
                    .data(data.getRecords().stream().map(BasicData.mapper(getType())).collect(Collectors.toList()))
                    .cursor(nextLocator)
                    .build();
        } catch (SalesforceClientException | IOException e) {
            throw new RuntimeStreamException("Encountered salesforce exception", e);
        }
    }

    /**
     * SOQL statement for fetching desired entity
     * @param query SalesForce ingestion query
     */
    public abstract String getSOQLStatement(SalesforceIngestionQuery query);

    /**
     * target type of the object
     */
    public abstract Class<D> getType();
}
