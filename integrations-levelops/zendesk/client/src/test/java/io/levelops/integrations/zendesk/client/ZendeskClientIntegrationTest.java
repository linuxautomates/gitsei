package io.levelops.integrations.zendesk.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.zendesk.models.*;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ZendeskClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "zendesk1";
    private static final String APPLICATION = "zendesk";
    private static final String ZENDESK_URL = System.getenv("ZENDESK_URL");
    private static final String ZENDESK_USERNAME = System.getenv("ZENDESK_USERNAME");
    private static final String ZENDESK_API_KEY = System.getenv("ZENDESK_API_KEY");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();
    private static final long TEST_TICKET_ID = 1;

    private ZendeskClientFactory clientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, ZENDESK_URL, Collections.emptyMap(), ZENDESK_USERNAME, ZENDESK_API_KEY)
                .build());
        clientFactory = ZendeskClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void tickets() throws ZendeskClientException {
        ExportTicketsResponse response = clientFactory.get(TEST_INTEGRATION_KEY).getTickets(ZendeskTicketQuery.builder()
                .from(Date.from(new Date().toInstant().minus(5, ChronoUnit.MINUTES)))
                .build());
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getTickets()).isNotNull();
    }

    @Test
    public void fields() throws ZendeskClientException {
        ListTicketFieldsResponse response = clientFactory.get(TEST_INTEGRATION_KEY).getTicketFields();
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getFields()).isNotNull();
    }

    @Test
    public void jiraLinks() throws ZendeskClientException {
        GetJiraLinkResponse response = clientFactory.get(TEST_INTEGRATION_KEY).getJiraLinks(TEST_TICKET_ID);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void requestAttributes() throws ZendeskClientException {
        Ticket.RequestAttributes requestAttributes = clientFactory.get(TEST_INTEGRATION_KEY).getRequestAttributes(TEST_TICKET_ID);
        DefaultObjectMapper.prettyPrint(requestAttributes);
    }

    @Test
    public void requestComments() throws ZendeskClientException {
        RequestCommentResponse comments = clientFactory.get(TEST_INTEGRATION_KEY).getRequestComments(TEST_TICKET_ID);
        DefaultObjectMapper.prettyPrint(comments);
    }

}
