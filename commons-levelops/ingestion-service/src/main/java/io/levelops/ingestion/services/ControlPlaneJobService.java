package io.levelops.ingestion.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.dates.DateUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

// Copied from Runbook service. Original author: @maxime
public class ControlPlaneJobService {
    private final ControlPlaneService controlPlaneService;
    private final int defaultAttemptMax; // THIS MUST MATCH CONTROL PLANE'S VALUE!

    public ControlPlaneJobService(ControlPlaneService controlPlaneService,
                                  int defaultAttemptMax) {
        this.controlPlaneService = controlPlaneService;
        this.defaultAttemptMax = defaultAttemptMax;
    }

    public Optional<JobDTO> getJobIfCompleteWithTimeout(String jobId, Instant startedAt, int customTimeoutSeconds) throws IngestionServiceException, TimeoutException {
        Optional<JobDTO> job = getJobIfComplete(jobId);
        if (job.isEmpty()) {
            if (DateUtils.isLongerThan(startedAt, Instant.now(), customTimeoutSeconds, ChronoUnit.SECONDS)) {
                throw new TimeoutException("Job did not complete in time");
            }
        }
        return job;
    }

    public Optional<JobDTO> getJobIfComplete(String jobId) throws IngestionServiceException {
        JobDTO job;
        try {
            job = controlPlaneService.getJob(jobId);
        } catch (IngestionServiceException e) {
            throw new IngestionServiceException("Failed to get job with id=" + jobId, e);
        }
        JobStatus status = MoreObjects.firstNonNull(job.getStatus(), JobStatus.UNASSIGNED);
        switch (status) {
            case FAILURE:
                if (isJobRetryable(job)) {
                    break;
                }
            case ABORTED:
            case CANCELED:
            case SUCCESS:
                return Optional.of(job);
        }

        // job not complete yet
        return Optional.empty();
    }

    private boolean isJobRetryable(JobDTO job) {
        if (JobStatus.FAILURE != job.getStatus()) {
            // only failed jobs are retry-able
            return false;
        }
        int attemptCount = ObjectUtils.defaultIfNull(job.getAttemptCount(), 0);
        int attemptMax = ObjectUtils.defaultIfNull(job.getAttemptMax(), defaultAttemptMax);
        attemptMax = (attemptMax == 0) ? defaultAttemptMax : attemptMax;
        return attemptMax > 0 && attemptCount < attemptMax;
    }
}
