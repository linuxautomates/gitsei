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
import io.levelops.integrations.okta.models.*;
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
public class OktaUsersDataSource implements DataSource<OktaUser, OktaScanQuery> {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;

    private final OktaClientFactory clientFactory;
    private final OktaEnrichmentService enrichmentService;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link OktaClientFactory} for fetching the {@link OktaClient}
     * @param enrichmentService {@link OktaEnrichmentService} for enriching the users
     */
    public OktaUsersDataSource(OktaClientFactory clientFactory,
                               OktaEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<OktaUser> fetchOne(OktaScanQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the users from Okta based on {@link OktaScanQuery}.
     * It makes calls to Okta using the {@link OktaClient}. Uses {@link PaginationUtils#stream(String, Function)}
     * for fetching groups by the cursor returned.
     *
     * @param query {@link OktaScanQuery} used to fetch the users
     * @return {@link Stream<Data<OktaUser>>} containing all the fetched users
     * @throws FetchException If any error occurs while fetching the users
     */
    @Override
    public Stream<Data<OktaUser>> fetchMany(OktaScanQuery query) throws FetchException {
        OktaClient oktaClient = clientFactory.get(query.getIntegrationKey());
        List<OktaLinkedObject> linkedObjects = oktaClient.getLinkedObjectDefinitions();
        List<OktaUserType> userTypes = oktaClient.getUserTypes();
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(oktaClient, query, linkedObjects, userTypes, cursor));
    }

    /**
     * Fetches {@link OktaUser} using the {@code query} and {@code cursor}. It puts the cursor for the next page
     * in the returned {@link PaginationUtils.CursorPageData} for fetching the next page.
     * Returns {@code null} when {@code cursor} is {@code null} (denotes end of pages).
     *
     * @param client {@link OktaClient} to make calls to Okta
     * @param query  {@link OktaScanQuery} for fetching the users
     * @param cursor {@link String} cursor for the next page, must be equal to
     *               {@link OktaUsersDataSource#STARTING_CURSOR} for the first page
     * @return {@link PaginationUtils.CursorPageData} with the {@code cursor} for the next page
     */
    @Nullable
    private PaginationUtils.CursorPageData<Data<OktaUser>> getPageData(OktaClient client,
                                                                       OktaScanQuery query,
                                                                       List<OktaLinkedObject> linkedObjects,
                                                                       List<OktaUserType> userTypes,
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
            PaginatedOktaResponse<OktaUser> response = client.getUsers(cursoredQuery);
            String nextCursor = response.getNextCursor();
            log.debug("getPageData: received next cursor for integration key: {} as {}",
                    query.getIntegrationKey(), nextCursor);
            List<OktaUser> users = enrichmentService.enrichUsers(client, query.getIntegrationKey(),
                    response.getValues(), linkedObjects, userTypes, client.isEnrichmentEnabled());
            return PaginationUtils.CursorPageData.<Data<OktaUser>>builder()
                    .data(users.stream()
                            .map(BasicData.mapper(OktaUser.class))
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
