package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SubmitJobResponse.SubmitJobResponseBuilder.class)
public class SubmitJobResponse {

    @JsonProperty("job_id")
    String jobId;

    @JsonProperty
    JobStatus status;
}
