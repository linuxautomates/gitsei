package io.levelops.integrations.okta.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.okta.client.OktaClient;
import io.levelops.integrations.okta.client.OktaClientException;
import io.levelops.integrations.okta.client.OktaClientFactory;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaScanQuery;
import io.levelops.integrations.okta.models.PaginatedOktaResponse;
import io.levelops.integrations.okta.services.OktaEnrichmentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Okta's implementation of the {@link DataSource}. This class can be used to fetch data from Okta.
 */
@Log4j2
public class OktaGroupsDataSource implements DataSource<OktaGroup, OktaScanQuery> {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;

    private final OktaClientFactory clientFactory;
    private final OktaEnrichmentService enrichmentService;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link OktaClientFactory} for fetching the {@link OktaClient}
     * @param enrichmentService {@link OktaEnrichmentService} for enriching the groups
     */
    public OktaGroupsDataSource(OktaClientFactory clientFactory,
                                OktaEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<OktaGroup> fetchOne(OktaScanQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the groups from Okta based on {@link OktaScanQuery}.
     * It makes calls to Okta using the {@link OktaClient}. Uses {@link PaginationUtils#stream(String, Function)}
     * for fetching groups by the cursor returned.
     *
     * @param query {@link OktaScanQuery} used to fetch the groups
     * @return {@link Stream<Data<OktaGroup>>} containing all the fetched groups
     * @throws FetchException If any error occurs while fetching the groups
     */
    @Override
    public Stream<Data<OktaGroup>> fetchMany(OktaScanQuery query) throws FetchException {
        OktaClient oktaClient = clientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(oktaClient, query, cursor));
    }

    /**
     * Fetches {@link OktaGroup} using the {@code query} and {@code cursor}. It puts the cursor for the next page
     * in the returned {@link PaginationUtils.CursorPageData} for fetching the next page.
     * Returns {@code null} when {@code cursor} is {@code null} (denotes end of pages).
     *
     * @param client {@link OktaClient} to make calls to Okta
     * @param query  {@link OktaScanQuery} for fetching the groups
     * @param cursor {@link String} cursor for the next page, must be equal to
     *               {@link OktaGroupsDataSource#STARTING_CURSOR} for the first page
     * @return {@link PaginationUtils.CursorPageData} with the {@code cursor} for the next page
     */
    @Nullable
    private PaginationUtils.CursorPageData<Data<OktaGroup>> getPageData(OktaClient client,
                                                                        OktaScanQuery query,
                                                                        String cursor) {
        if (cursor == null)
            return null;
        OktaScanQuery cursoredQuery = STARTING_CURSOR.equals(cursor) ? query :
                OktaScanQuery.builder()
                        .integrationKey(query.getIntegrationKey())
                        .cursor(cursor)
                        .from(null)
                        .build();
        try {
            PaginatedOktaResponse<OktaGroup> response = client.getGroups(cursoredQuery);
            String nextCursor = response.getNextCursor();
            log.debug("getPageData: received next cursor for integration key: {} as {}",
                    query.getIntegrationKey(), nextCursor);
            List<OktaGroup> groups = enrichmentService.enrichGroups(client, query.getIntegrationKey(),
                    response.getValues(), client.isEnrichmentEnabled());
            return PaginationUtils.CursorPageData.<Data<OktaGroup>>builder()
                    .data(groups.stream()
                            .map(BasicData.mapper(OktaGroup.class))
                            .collect(Collectors.toList()))
                    .cursor(nextCursor)
                    .build();
        } catch (OktaClientException e) {
            log.error("getPageData: encountered Okta client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered Okta client exception for " + query.getIntegrationKey(), e);
        }
    }
}
