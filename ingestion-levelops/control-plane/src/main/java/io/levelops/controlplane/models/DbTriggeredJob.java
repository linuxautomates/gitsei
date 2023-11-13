package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTriggeredJob.DbTriggeredJobBuilder.class)
public class DbTriggeredJob {

    @JsonProperty("job_id")
    String jobId;
    @JsonProperty("partial")
    Boolean partial;
    @JsonProperty("trigger_id")
    String triggerId;
    @JsonProperty("iteration_id")
    String iterationId;
    @JsonProperty("iteration_ts")
    Long iterationTs;
    @JsonProperty("created_at")
    Long createdAt;

}
