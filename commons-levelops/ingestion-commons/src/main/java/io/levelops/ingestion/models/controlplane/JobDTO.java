package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.functional.IngestionFailure;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JobDTO.JobDTOBuilder.class)
public class JobDTO {

    // DTO for ControlPlane's DbJob and DbTriggeredJob

    @JsonProperty("id")
    String id;

    @JsonProperty("agent_id")
    String agentId;

    @JsonProperty("status")
    JobStatus status;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    /**
     * Jobs marked as reserved belong to integrations that must be ingested by a dedicated agent,
     * and therefore should be not be pulled by generic agents.
     * To ingest reserved jobs, dedicated agents will have to explicitly specify which tenant and integration ids they want.
     */
    @JsonProperty("reserved")
    Boolean reserved;

    @JsonProperty("tags")
    Set<String> tags;

    /**
     * Top-level jobs start with 0 (or null) while sub-jobs are one level deeper than their parent.
     * If level > 0, then parent_id will indicate which parent job spawned the current job.
     */
    @JsonProperty("level")
    Integer level;

    @JsonProperty("parent_id")
    String parentId;

    /**
     * When attempt_count reached attempt_max, the control plane will stop retrying.
     */
    @JsonProperty("attempt_count")
    Integer attemptCount;

    @JsonProperty("attempt_max")
    Integer attemptMax;

    @JsonProperty("query")
    Object query;

    @JsonProperty("callback_url")
    String callbackUrl;

    /**
     * Unique name of the Ingestion Controller that will execute this job.
     * Only agents that have declared having this controller will be able to pull this job.
     */
    @JsonProperty("controller_name")
    String controllerName;

    @JsonProperty("created_at")
    Long createdAt;

    /**
     * When the status of this job changed last.
     */
    @JsonProperty("status_changed_at")
    Long statusChangedAt;

    /**
     * Result of the job if status is successful; or error data in case of failure
     */
    @JsonProperty("result")
    Map<String, Object> result;

    @JsonProperty("intermediate_state")
    Map<String, Object> intermediateState;

    @JsonProperty("error")
    Map<String, Object> error; // critical error

    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures; // warnings

    // region TriggeredJob fields

    @JsonProperty("trigger_id")
    String triggerId;

    /**
     * If False, the job is a "full scan"; if True, the job is "partial", i.e. it contains a subset of data for the time delta it was fetching.
     */
    @JsonProperty("partial")
    Boolean partial;

    /**
     * Id of iteration that triggered this job. If null, this is not a triggered job.
     */
    @JsonProperty("iteration_id")
    String iterationId;

    @JsonProperty("iteration_ts")
    Long iterationTs;

    //endregion
}
