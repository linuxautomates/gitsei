package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.integrations.zendesk.models.Field;
import io.levelops.integrations.zendesk.models.Ticket;
import io.levelops.integrations.zendesk.models.TicketMetricEvent;
import io.levelops.integrations.zendesk.models.ZendeskTicketQuery;
import io.levelops.integrations.zendesk.sources.ZendeskFieldDataSource;
import io.levelops.integrations.zendesk.sources.ZendeskMetricEventDataSource;
import io.levelops.integrations.zendesk.sources.ZendeskTicketDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Zendesk's implementation of the {@link DataController}
 */
@Log4j2
public class ZendeskController implements DataController<ZendeskTicketQuery> {

    private static final String DATATYPE_TICKETS = "tickets";
    private static final String INTEGRATION_TYPE = "zendesk";
    private static final String DATATYPE_TICKET_METRIC_EVENTS = "ticket-metric-events";
    private static final String DATATYPE_FIELDS = "fields";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<Ticket, ZendeskTicketQuery> ticketPaginationStrategy;
    private final PaginationStrategy<TicketMetricEvent, ZendeskTicketQuery> eventPaginationStrategy;
    private final PaginationStrategy<Field, ZendeskTicketQuery> fieldPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public ZendeskController(ObjectMapper objectMapper, ZendeskTicketDataSource ticketDataSource,
                             ZendeskMetricEventDataSource metricEventDataSource,
                             ZendeskFieldDataSource fieldDataSource,
                             StorageDataSink storageDataSink, int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.ticketPaginationStrategy = StreamedPaginationStrategy.<Ticket, ZendeskTicketQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_TICKETS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(ticketDataSource)
                .skipEmptyResults(true)
                .build();
        this.eventPaginationStrategy = StreamedPaginationStrategy.<TicketMetricEvent, ZendeskTicketQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_TICKET_METRIC_EVENTS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(metricEventDataSource)
                .skipEmptyResults(true)
                .build();
        this.fieldPaginationStrategy = SinglePageStrategy.<Field, ZendeskTicketQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_FIELDS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(fieldDataSource)
                .skipEmptyResults(true)
                .build();
    }

    /**
     * parses the {@code arg0}
     *
     * @param arg {@link Object} corresponding to the required {@link ZendeskTicketQuery}
     * @return {@link ZendeskTicketQuery} for the job
     */
    @Override
    public ZendeskTicketQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        ZendeskTicketQuery query = objectMapper.convertValue(arg, ZendeskTicketQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    /**
     * Ingests the data for {@code jobId} with the {@code query}. It calls the {@link ZendeskTicketDataSource} for
     * fetching the tickets.
     *
     * @param jobId {@link String} id of the job for which the data needs to be ingested
     * @param query {@link ZendeskTicketQuery} describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, ZendeskTicketQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        ZendeskTicketQuery ticketQuery;
        if (query.getFrom() == null && query.getCursor() == null) {
            ticketQuery = ZendeskTicketQuery.builder()
                    .from(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                    .integrationKey(query.getIntegrationKey())
                    .build();
        } else {
            ticketQuery = query;
        }
        StorageResult ticketStorageResult = ticketPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(),
                ticketQuery);
        StorageResult metricEventStorageResult = eventPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), query);
        ControllerIngestionResult fieldStorageResult;
        if(ticketStorageResult.getCount() > 0) {
            fieldStorageResult = fieldPaginationStrategy.ingestAllPages(jobContext,
                    query.getIntegrationKey(), query);
        } else {
            fieldStorageResult = new EmptyIngestionResult();
        }
        return new ControllerIngestionResultList(ticketStorageResult, metricEventStorageResult, fieldStorageResult);
    }
}