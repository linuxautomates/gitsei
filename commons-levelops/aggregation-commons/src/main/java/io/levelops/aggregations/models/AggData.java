package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public abstract class AggData {
    @JsonProperty("_levelops_agg_version")
    protected String aggVersion;
    @JsonProperty("_levelops_ingestion_data")
    protected MultipleTriggerResults results;
}
