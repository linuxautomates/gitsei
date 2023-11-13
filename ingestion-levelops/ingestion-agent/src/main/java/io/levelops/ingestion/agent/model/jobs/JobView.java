package io.levelops.ingestion.agent.model.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.engine.models.JobConverter;
import io.levelops.ingestion.models.Job;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.time.Instant;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@Log4j2
public class JobView {

    // TODO get rid of this view

    @JsonUnwrapped
    Job job;

    @JsonProperty("elapsed_ms")
    Long elapsedMs;

    public static JobView fromEngineJob(IngestionEngine.EngineJob job) {
        return JobView.builder()
                .job(JobConverter.fromEngineJob(job))
                .elapsedMs(Duration.between(
                        job.getCreatedAt().toInstant(),
                        (job.getDoneAt() == null ? Instant.now() : job.getDoneAt().toInstant()))
                        .toMillis())
                .build();
    }

}
