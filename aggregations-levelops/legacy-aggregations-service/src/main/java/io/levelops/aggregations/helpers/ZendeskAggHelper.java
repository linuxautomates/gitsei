package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.zendesk.models.Ticket;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Helper class for performing zendesk aggregations
 */
@Log4j2
@Service
public class ZendeskAggHelper {

    private static final String TICKETS_DATA_TYPE = "tickets";

    private final JobDtoParser jobDtoParser;
    private final ZendeskTicketService ticketService;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public ZendeskAggHelper(JobDtoParser jobDtoParser,
                            ZendeskTicketService ticketService,
                            IntegrationTrackingService trackingService) {
        this.jobDtoParser = jobDtoParser;
        this.ticketService = ticketService;
        this.trackingService = trackingService;
    }

    /**
     * Performs aggregation on the {@code triggerResults} and stores the corresponding results in the db
     *
     * @param customer       {@link String} customer id
     * @param integrationId  {@link String} integration of the tickets
     * @param triggerResults {@link MultipleTriggerResults} on which the aggregation should be performed
     * @param currentTime    {@link Date} fetch time
     * @return true, if the aggregation was successful, false otherwise
     */
    public boolean setupZendeskTickets(String customer,
                                       String integrationId,
                                       List<IntegrationConfig.ConfigEntry> customFieldConfigs,
                                       MultipleTriggerResults triggerResults,
                                       List<DbZendeskField> customFieldProperties,
                                       Date currentTime) {
        Date truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        boolean result = jobDtoParser.applyToResults(customer, TICKETS_DATA_TYPE, Ticket.class,
                triggerResults.getTriggerResults().get(0),
                ticket -> {
                    try {
                        DbZendeskTicket dbTicket = DbZendeskTicket.fromTicket(ticket, integrationId, currentTime, customFieldConfigs, customFieldProperties);
                        DbZendeskTicket old = ticketService.get(customer,
                                dbTicket.getTicketId(),
                                Integer.parseInt(integrationId),
                                truncatedDate).orElse(null);
                        if (old == null || old.getTicketUpdatedAt().before(dbTicket.getTicketUpdatedAt()))
                            ticketService.insertAndReturnId(customer, dbTicket);
                    } catch (Exception e) {
                        log.error("setupZendeskTickets: error inserting ticket with id: "
                                + ticket.getId(), e);
                    }
                },
                List.of());
        if (result)
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(truncatedDate.toInstant().getEpochSecond())
                            .build());
        return result;
    }
}
