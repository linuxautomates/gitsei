package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CreateJobRequest {

    /**
     * Name of the DataController the job will be sent to.
     * Either id or name need to be specified.
     */
    @JsonProperty("controller_name")
    private String controllerName;

    @JsonProperty("query")
    private Object query; // TODO revisit model

    /**
     * Optional. If specified, the results will be posted to this url when the job is completed.
     * @deprecated doesn't work for satellites!
     */
    @Deprecated
    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("integration_id")
    private String integrationId;

    /**
     * Jobs marked as reserved belong to integrations that must be ingested by a dedicated agent,
     * and therefore should be not be pulled by generic agents.
     * To ingest reserved jobs, dedicated agents will have to explicitly specify which tenant and integration ids they want.
     */
    @JsonProperty("reserved")
    private Boolean reserved;

    @JsonProperty("tags")
    private Set<String> tags;

    /**
     * UUID.
     * <p>
     * Leave blank and a new job Id will be assigned automatically.
     */
    @JsonProperty("job_id")
    private String jobId;

    /**
     * Used to resume a failed job from a given intermediate state.
     */
    @JsonProperty("intermediate_state")
    Map<String, Object> intermediateState;

    @JsonProperty("attempt_count")
    long attemptCount;

    // region triggered jobs (fields for tracing purposes)
    @JsonProperty("trigger_id")
    private String triggerId;

    @JsonProperty("iteration_id")
    private String iterationId;
    //endregion
}
