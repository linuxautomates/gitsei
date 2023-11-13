package io.levelops.ingestion.integrations.zendesk.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.zendesk.models.TicketRequest;
import io.levelops.integrations.zendesk.models.TicketIngestionQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ZendeskIngestionServiceTest {

    private static final String TEST_ID = "1";
    private static final long TEST_TICKET_ID = 1L;

    private ZendeskIngestionService ingestionService;

    @Before
    public void setup() throws IngestionServiceException, InventoryException {
        ControlPlaneService controlPlaneService = Mockito.mock(ControlPlaneService.class);
        when(controlPlaneService.submitJob(any(CreateJobRequest.class))).thenAnswer((Answer<SubmitJobResponse>) invocation -> {
            final CreateJobRequest jobRequest = invocation.getArgument(0, CreateJobRequest.class);
            if (jobRequest.getQuery() instanceof TicketIngestionQuery) {
                return SubmitJobResponse.builder()
                        .jobId(TEST_ID)
                        .build();
            } else {
                throw new IllegalArgumentException("must be an instance " + TicketIngestionQuery.class.getName());
            }
        });
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        when(inventoryService.getIntegration(any(IntegrationKey.class))).thenReturn(Integration.builder()
                .build());
        ingestionService = new ZendeskIngestionService(controlPlaneService, inventoryService);
    }

    @Test
    public void testIntegrationType() {
        assertThat(ingestionService.getIntegrationType()).isEqualByComparingTo(IntegrationType.ZENDESK);
    }

    @Test
    public void testInsertTicket() throws IngestionServiceException {
        final SubmitJobResponse job = ingestionService.insertTicket(EMPTY, EMPTY, TicketRequest.builder().build(),
                null);
        assertThat(job).isNotNull();
        assertThat(job.getJobId()).isEqualTo(TEST_ID);
    }

    @Test
    public void testUpdateTicket() throws IngestionServiceException {
        final SubmitJobResponse job = ingestionService.updateTicket(EMPTY, EMPTY, TEST_TICKET_ID,
                TicketRequest.builder().build(), null);
        assertThat(job).isNotNull();
        assertThat(job.getJobId()).isEqualTo(TEST_ID);
    }
}
