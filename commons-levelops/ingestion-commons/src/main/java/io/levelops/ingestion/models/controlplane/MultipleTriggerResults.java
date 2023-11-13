package io.levelops.ingestion.models.controlplane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonDeserialize(builder = MultipleTriggerResults.MultipleTriggerResultsBuilder.class)
public class MultipleTriggerResults {

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("tenant_id")
    String tenantId;

    @JsonProperty("partial")
    Boolean partial;

    @JsonProperty("trigger_results")
    List<TriggerResults> triggerResults;
}