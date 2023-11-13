package io.levelops.integrations.zendesk.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.models.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class ZendeskClientEnrichmentServiceTest {

    private ZendeskClient zendeskClient;
    private ZendeskTicketEnrichmentService enrichmentService;

    @Before
    public void setup() throws ZendeskClientException {
        zendeskClient = Mockito.mock(ZendeskClient.class);
        enrichmentService = new ZendeskTicketEnrichmentService(2, 10);
        when(zendeskClient.getJiraLinks(anyLong()))
                .thenReturn(GetJiraLinkResponse.builder().links(List.of(JiraLink.builder().build())).build());
        when(zendeskClient.getRequestComments(anyLong()))
                .thenReturn(RequestCommentResponse.builder().comments(List.of(RequestComment.builder().build()))
                        .build());
        when(zendeskClient.getRequestAttributes(anyLong()))
                .thenReturn(Ticket.RequestAttributes.builder().canBeSolvedByMe(true).build());
    }

    @Test
    public void enrich() {
        List<Ticket> tickets = enrichmentService.enrichTickets(zendeskClient,
                IntegrationKey.builder().build(), List.of(Ticket.builder().id(1L).build()), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                true, true);
        assertThat(tickets).isNotNull();
        assertThat(tickets).hasSize(1);
        Ticket ticket = tickets.get(0);
        assertThat(ticket.getJiraLinks()).hasSize(1);
        assertThat(ticket.getRequestAttributes()).isNotNull();
        assertThat(ticket.getRequestAttributes().getCanBeSolvedByMe()).isTrue();
        assertThat(ticket.getRequestComments()).hasSize(1);
    }
}
