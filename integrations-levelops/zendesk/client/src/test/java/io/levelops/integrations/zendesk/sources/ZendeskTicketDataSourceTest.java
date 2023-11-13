package io.levelops.integrations.zendesk.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.zendesk.client.ZendeskClient;
import io.levelops.integrations.zendesk.client.ZendeskClientException;
import io.levelops.integrations.zendesk.client.ZendeskClientFactory;
import io.levelops.integrations.zendesk.models.*;
import io.levelops.integrations.zendesk.services.ZendeskTicketEnrichmentService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class ZendeskTicketDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    ZendeskTicketDataSource dataSource;

    @Before
    public void setup() throws ZendeskClientException {
        ZendeskClient client = Mockito.mock(ZendeskClient.class);
        ZendeskClientFactory clientFactory = Mockito.mock(ZendeskClientFactory.class);

        ZendeskTicketEnrichmentService enrichmentService = new ZendeskTicketEnrichmentService(1, 10);
        dataSource = new ZendeskTicketDataSource(clientFactory, enrichmentService);

        when(client.isEnrichmentEnabled()).thenReturn(false);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        String cursor = "cursor";
        when(client.getTickets(eq(ZendeskTicketQuery.builder().integrationKey(TEST_KEY).build())))
                .thenReturn(ExportTicketsResponse.builder()
                        .tickets(List.of(Ticket.builder().id(1L).build()))
                        .groups(Collections.emptyList())
                        .users(Collections.emptyList())
                        .afterCursor(cursor)
                        .build());
        when(client.getTickets(eq(ZendeskTicketQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(cursor)
                .build())))
                .thenReturn(ExportTicketsResponse.builder()
                        .tickets(List.of(Ticket.builder().id(1L).build()))
                        .groups(Collections.emptyList())
                        .users(Collections.emptyList())
                        .brands(Collections.emptyList())
                        .organizations(Collections.emptyList())
                        .metrics(Collections.emptyList())
                        .build());

        when(client.getJiraLinks(anyLong())).thenReturn(GetJiraLinkResponse.builder().build());
        when(client.getRequestAttributes(anyLong())).thenReturn(Ticket.RequestAttributes.builder().build());
        when(client.getRequestComments(anyLong())).thenReturn(RequestCommentResponse.builder().build());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(ZendeskTicketQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<Ticket>> tickets = dataSource.fetchMany(ZendeskTicketQuery.builder()
                .integrationKey(TEST_KEY)
                .cursor(null)
                .from(null)
                .build())
                .collect(Collectors.toList());
        assertThat(tickets).hasSize(2);
    }
}
