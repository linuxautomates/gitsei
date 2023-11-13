package io.levelops.ingestion.services;

import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// Copied from Runbook service. Original author: @maxime
public class ControlPlaneJobServiceTest {
    @Mock
    ControlPlaneService controlPlaneService;

    ControlPlaneJobService controlPlaneJobService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        controlPlaneJobService = new ControlPlaneJobService(controlPlaneService, 10);
    }

    @Test
    public void testSuccess() throws IngestionServiceException {
        when(controlPlaneService.getJob(eq("job123"))).thenReturn(
                JobDTO.builder().status(JobStatus.SUCCESS).build());
        Optional<JobDTO> job = controlPlaneJobService.getJobIfComplete("job123");
        assertThat(job).isPresent();
        assertThat(job.get().getStatus()).isEqualTo(JobStatus.SUCCESS);
    }


    @Test
    public void testPending() throws IngestionServiceException {
        when(controlPlaneService.getJob(eq("job123"))).thenReturn(
                JobDTO.builder().status(JobStatus.PENDING).build());
        Optional<JobDTO> job = controlPlaneJobService.getJobIfComplete("job123");
        assertThat(job).isEmpty();
    }

    @Test
    public void testRetryable() throws IngestionServiceException {
        when(controlPlaneService.getJob(eq("job123"))).thenReturn(
                JobDTO.builder().status(JobStatus.FAILURE).attemptCount(1).build());
        Optional<JobDTO> job = controlPlaneJobService.getJobIfComplete("job123");
        assertThat(job).isEmpty();
    }

    @Test
    public void testNotRetryable() throws IngestionServiceException {
        when(controlPlaneService.getJob(eq("job123"))).thenReturn(
                JobDTO.builder().status(JobStatus.FAILURE).attemptCount(10).build());
        Optional<JobDTO> job = controlPlaneJobService.getJobIfComplete("job123");
        assertThat(job).isPresent();
        assertThat(job.get().getStatus()).isEqualTo(JobStatus.FAILURE);
    }

    @Test
    public void testTimeout() throws IngestionServiceException {
        when(controlPlaneService.getJob(eq("job123"))).thenReturn(
                JobDTO.builder().status(JobStatus.FAILURE).build());
        assertThatThrownBy(() -> controlPlaneJobService.getJobIfCompleteWithTimeout("job123", Instant.MIN, 10)).isInstanceOf(TimeoutException.class);
    }
}