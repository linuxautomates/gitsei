package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class ZendeskIngestionController implements DataController<TicketIngestionQuery> {

    private static final String USER_EMAIL = "user_email";

    private final ObjectMapper objectMapper;
    private final ZendeskClientFactory clientFactory;

    @Builder
    public ZendeskIngestionController(ObjectMapper objectMapper, ZendeskClientFactory clientFactory) {
        this.objectMapper = objectMapper;
        this.clientFactory = clientFactory;
    }

    @Override
    public TicketIngestionQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        TicketIngestionQuery query = objectMapper.convertValue(arg, TicketIngestionQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, TicketIngestionQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        IntegrationKey integrationKey = query.getIntegrationKey();
        ZendeskClient client = clientFactory.get(integrationKey);
        Ticket ticket;
        if (query.isUpdate()) {
            if (query.getId() == null) {
                log.error("ingest: ticket id cannot be null with update ticket: {}", query);
                throw new IngestException("ticket id cannot be null with update ticket: " + query);
            }
            TicketRequestBody requestBody = getTicketRequestBody(query, client);
            log.debug("ingest: updating ticket for {} with id {}", integrationKey, query.getId());
            WriteTicketResponse response = client.updateTicket(query.getId(), requestBody);
            ticket = response.getTicket();
            log.debug("ingest: updated ticket: {}", ticket);
        } else {
            log.debug("ingest: creating new ticket for {}", integrationKey);
            TicketRequestBody requestBody = getTicketRequestBody(query, client);
            WriteTicketResponse response = client.createTicket(requestBody);
            ticket = response.getTicket();
            log.debug("ingest: created ticket: {}", ticket);
        }
        log.info("ingest: completed ingest job successfully for {}", integrationKey);
        return ZendeskWriteTicketResult.builder()
                .success(true)
                .ticket(ticket)
                .build();
    }

    private TicketRequestBody getTicketRequestBody(TicketIngestionQuery query,
                                                   ZendeskClient client) {
        TicketRequest request = query.getTicketRequest();
        String assignee = request.getAssignee();
        Ticket.TicketBuilder ticketBuilder = Ticket.builder();
        if (assignee != null) {
            if (request.isGroup()) {
                ticketBuilder.groupId(client.getGroupIdByName(assignee));
            } else {
                ticketBuilder.assigneeId(client.getUserIdByEmail(assignee));
            }
        }
        if (request.getRequesterEmail() != null) {
            ticketBuilder.requesterId(client.getUserIdByEmail(request.getRequesterEmail()));
        }
        List<String> followerEmails = request.getFollowerEmails();
        if (followerEmails != null && !followerEmails.isEmpty()) {
            List<Map<String, String>> followers = new ArrayList<>();
            for (String followerEmail : followerEmails) {
                followers.add(Map.of(USER_EMAIL, followerEmail));
            }
            ticketBuilder.followers(followers);
        }
        return TicketRequestBody.builder()
                .ticket(ticketBuilder
                        .subject(request.getSubject())
                        .type(request.getType())
                        .priority(request.getPriority())
                        .status(request.getStatus())
                        .dueAt(request.getDueDate())
                        .build())
                .comment(request.getComment())
                .build();
    }
}
