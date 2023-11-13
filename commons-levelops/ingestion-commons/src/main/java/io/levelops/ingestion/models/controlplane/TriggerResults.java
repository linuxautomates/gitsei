package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TriggerResults.TriggerResultsBuilder.class)
public class TriggerResults {

    @JsonProperty("trigger_id")
    String triggerId;

    @JsonProperty("trigger_type")
    String triggerType;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("iteration_id")
    String iterationId;

    @JsonProperty("partial")
    Boolean partial;

    @JsonProperty("jobs")
    List<JobDTO> jobs;

    @JsonProperty("count")
    public int getCount() {
        return (jobs == null) ? 0 : jobs.size();
    }

    @JsonProperty("has_next")
    Boolean hasNext; // only for paginated calls

}