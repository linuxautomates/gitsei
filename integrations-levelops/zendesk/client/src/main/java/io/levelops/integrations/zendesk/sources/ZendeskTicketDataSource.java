package io.levelops.integrations.zendesk.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.models.ExportTicketsResponse;
import io.levelops.integrations.zendesk.models.Ticket;
import io.levelops.integrations.zendesk.models.ZendeskTicketQuery;
import io.levelops.integrations.zendesk.services.ZendeskTicketEnrichmentService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

/**
 * Zendesk's implementation of the {@link DataSource}. This class can be used to fetch data from Zendesk.
 */
@Log4j2
public class ZendeskTicketDataSource implements DataSource<Ticket, ZendeskTicketQuery> {

    private static final String STARTING_CURSOR = StringUtils.EMPTY;

    private final ZendeskClientFactory clientFactory;
    private final ZendeskTicketEnrichmentService enrichmentService;

    /**
     * all arg constructor
     *
     * @param clientFactory     {@link ZendeskClientFactory} for fetching the {@link ZendeskClient}
     * @param enrichmentService {@link ZendeskTicketEnrichmentService} for enriching the tickets
     */
    public ZendeskTicketDataSource(ZendeskClientFactory clientFactory,
                                   ZendeskTicketEnrichmentService enrichmentService) {
        this.clientFactory = clientFactory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<Ticket> fetchOne(ZendeskTicketQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the tickets from Zendesk based on {@link ZendeskTicketQuery}.
     * It makes calls to Zendesk using the {@link ZendeskClient}. Uses {@link PaginationUtils#stream(String, Function)}
     * for fetching tickets by the cursor returned.
     *
     * @param query {@link ZendeskTicketQuery} used to fetch the tickets
     * @return {@link Stream<Data<Ticket>>} containing all the fetched tickets
     * @throws FetchException If any error occurs while fetching the tickets
     */
    @Override
    public Stream<Data<Ticket>> fetchMany(ZendeskTicketQuery query) throws FetchException {
        ZendeskClient zendeskClient = clientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(zendeskClient, query, cursor));
    }

    /**
     * Fetches {@link Ticket} using the {@code query} and {@code cursor}. It puts the cursor for the next page
     * in the returned {@link PaginationUtils.CursorPageData} for fetching the next page.
     * Returns {@code null} when {@code cursor} is {@code null} (denotes end of pages).
     *
     * @param client {@link ZendeskClient} to make calls to zendesk
     * @param query  {@link ZendeskTicketQuery} for fetching the tickets
     * @param cursor {@link String} cursor for the next page, must be equal to
     *               {@link ZendeskTicketDataSource#STARTING_CURSOR} for the first page
     * @return {@link PaginationUtils.CursorPageData} with the {@code cursor} for the next page
     */
    @Nullable
    private PaginationUtils.CursorPageData<Data<Ticket>> getPageData(ZendeskClient client,
                                                                     ZendeskTicketQuery query,
                                                                     String cursor) {
        if (cursor == null)
            return null;
        ZendeskTicketQuery cursoredQuery = STARTING_CURSOR.equals(cursor) ? query :
                ZendeskTicketQuery.builder()
                        .integrationKey(query.getIntegrationKey())
                        .cursor(cursor)
                        .from(null)
                        .build();
        try {
            ExportTicketsResponse response = client.getTickets(cursoredQuery);
            String nextCursor = response.getAfterCursor();
            log.debug("getPageData: received next cursor for integration key: {} as {}",
                    query.getIntegrationKey(), nextCursor);
            List<Ticket> tickets = enrichmentService.enrichTickets(client, query.getIntegrationKey(),
                    response.getTickets(), emptyIfNull(response.getUsers()), emptyIfNull(response.getGroups()),
                    emptyIfNull(response.getBrands()), emptyIfNull(response.getOrganizations()),
                    emptyIfNull(response.getMetrics()), client.isEnrichmentEnabled(), client.isJiralinksEnabled());
            return PaginationUtils.CursorPageData.<Data<Ticket>>builder()
                    .data(tickets.stream()
                            .map(BasicData.mapper(Ticket.class))
                            .collect(Collectors.toList()))
                    .cursor(nextCursor)
                    .build();
        } catch (ZendeskClientException e) {
            log.error("getPageData: encountered Zendesk client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("encountered Zendesk client exception for " + query.getIntegrationKey(), e);
        }
    }
}
