package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJob.DbJobBuilder.class)
public class DbJob {

    // NB: update JobDTO model when making changes here
    // NB2: see JobDTO for documentation

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
    @JsonProperty("reserved")
    Boolean reserved;
    @JsonProperty("tags")
    Set<String> tags;
    @JsonProperty("level")
    Integer level;
    @JsonProperty("parent_id")
    String parentId;
    @JsonProperty("attempt_count")
    Integer attemptCount;
    @JsonProperty("attempt_max")
    Integer attemptMax;
    @JsonProperty("query")
    Map<String, Object> query;
    @JsonProperty("callback_url")
    String callbackUrl;
    @JsonProperty("controller_name")
    String controllerName;
    @JsonProperty("created_at")
    Long createdAt;
    @JsonProperty("status_changed_at")
    Long statusChangedAt;
    @JsonProperty("result")
    Map<String, Object> result;
    @JsonProperty("intermediate_state")
    Map<String, Object> intermediateState;
    @JsonProperty("error")
    Map<String, Object> error; // critical error
    @JsonProperty("failures")
    List<IngestionFailure> ingestionFailures; // warnings
}
