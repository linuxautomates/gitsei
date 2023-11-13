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
import io.levelops.integrations.zendesk.models.ListTicketMetricEventResponse;
import io.levelops.integrations.zendesk.models.TicketMetricEvent;
import io.levelops.integrations.zendesk.models.ZendeskTicketQuery;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.firstNonNull;

@Log4j2
public class ZendeskMetricEventDataSource implements DataSource<TicketMetricEvent, ZendeskTicketQuery> {

    private static final String STARTING_CURSOR = "0";

    private final ZendeskClientFactory clientFactory;

    public ZendeskMetricEventDataSource(ZendeskClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<TicketMetricEvent> fetchOne(ZendeskTicketQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<TicketMetricEvent>> fetchMany(ZendeskTicketQuery query) throws FetchException {
        ZendeskClient client = clientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(STARTING_CURSOR, cursor -> getPageData(query, client, cursor));
    }

    private PaginationUtils.CursorPageData<Data<TicketMetricEvent>> getPageData(ZendeskTicketQuery query,
                                                                                ZendeskClient client, String cursor) {
        long startTime = STARTING_CURSOR.equals(cursor) ? firstNonNull(query.getFrom(), new Date(0)).getTime() / 1000
                : Long.parseLong(cursor);
        try {
            ListTicketMetricEventResponse response = client.getTicketMetricEvents(startTime);
            return PaginationUtils.CursorPageData.<Data<TicketMetricEvent>>builder()
                    .data(response.getTicketMetricEvents().stream()
                            .map(BasicData.mapper(TicketMetricEvent.class))
                            .collect(Collectors.toList()))
                    .cursor(String.valueOf(response.getEndTime()))
                    .build();
        } catch (ZendeskClientException e) {
            log.error("getPageData: encountered Zendesk client error for integration key: "
                    + query.getIntegrationKey() + " as : " + e.getMessage(), e);
            throw new RuntimeStreamException("Zendesk client exception for " + query.getIntegrationKey(), e);
        }
    }
}
