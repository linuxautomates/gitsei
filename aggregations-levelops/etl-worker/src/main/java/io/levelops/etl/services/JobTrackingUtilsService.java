package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Log4j2
@Service
public class JobTrackingUtilsService {
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final String workerId;

    public JobTrackingUtilsService(
            JobInstanceDatabaseService jobInstanceDatabaseService,
            @Qualifier("workerId") String workerId) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.workerId = workerId;
    }

    public Boolean updateJobInstanceStatus(JobInstanceId jobInstanceId, JobStatus jobStatus) throws JsonProcessingException {
        log.info("Switching job status to {} for worker id: {}, job instance id: {}", jobStatus, workerId, jobInstanceId);
        DbJobInstanceUpdate update = DbJobInstanceUpdate.builder()
                .jobStatus(jobStatus)
                .workerIdCondition(workerId)
                .build();
        return jobInstanceDatabaseService.update(jobInstanceId, update);
    }

    public Boolean updateJobInstanceToPending(JobInstanceId jobInstanceId) throws JsonProcessingException {
        DbJobInstanceUpdate update = DbJobInstanceUpdate.builder()
                .jobStatus(JobStatus.PENDING)
                .workerIdCondition(workerId)
                .incrementAttemptCount(true)
                .startTime(Instant.now())
                .heartbeat(Instant.now())
                .build();
        return jobInstanceDatabaseService.update(jobInstanceId, update);
    }
}
