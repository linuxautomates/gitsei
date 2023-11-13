package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Information on job to be executed by agents.
 *
 * Note: this data goes to the client side
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JobContext.JobContextBuilder.class)
public class JobContext {
    @JsonProperty("job_id")
    String jobId;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("intermediate_state")
    Map<String, Object> intermediateState;

    @JsonProperty("attempt_count")
    long attemptCount;
}
