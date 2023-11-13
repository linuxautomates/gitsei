package io.levelops.controlplane.controllers;


import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.EnableHistoricalTriggerRequest;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.runnables.GithubTrigger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class TriggerControllerTest {

    @Mock
    TriggerDatabaseService triggerDatabaseService;

    @Mock
    TriggeredJobService triggeredJobDatabaseService;

    private TriggerController triggerController;
    private MockMvc mvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        triggerController = new TriggerController(
                DefaultObjectMapper.get(), triggerDatabaseService, triggeredJobDatabaseService);
        mvc = MockMvcBuilders
                .standaloneSetup(triggerController)
                .build();
    }

    @Test
    public void testEnableHistoricalStrategy() throws Exception {
        when(triggerDatabaseService.getTriggerById("1")).thenReturn(
                Optional.ofNullable(DbTrigger.builder()
                        .type("github")
                        .tenantId("sid")
                        .metadata(GithubTrigger.GithubTriggerMetadata.builder()
                                .build())
                        .integrationId("1")
                        .build())
        );
        when(triggerDatabaseService.updateTriggerMetadata(any(), any())).thenReturn(true);
        EnableHistoricalTriggerRequest request = EnableHistoricalTriggerRequest.builder()
                .historicalSpanInDays(12L)
                .historicalSuccessiveBackwardScanCount(3)
                .build();
        mvc.perform(MockMvcRequestBuilders.put("/v1/triggers/1/enableHistoricalTrigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DefaultObjectMapper.get().writeValueAsString(request))
                )
                .andExpect(status().is(HttpStatus.OK.value()));
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(triggerDatabaseService).updateTriggerMetadata(eq("1"), argCaptor.capture());
        GithubTrigger.GithubTriggerMetadata capturedMetadata = (GithubTrigger.GithubTriggerMetadata) argCaptor.getValue();
        assertThat(capturedMetadata.getShouldStartFetchingHistory()).isTrue();
        assertThat(capturedMetadata.getHistoricalSpanInDays()).isEqualTo(12L);
        assertThat(capturedMetadata.getHistoricalSuccesiveBackwardScanCount()).isEqualTo(3);
    }
}