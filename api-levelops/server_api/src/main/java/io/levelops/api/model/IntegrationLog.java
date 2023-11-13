package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.etl.models.EtlLogDTO;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IntegrationLog.IntegrationLogBuilder.class)
public class IntegrationLog {
    @JsonProperty("ingestion_log")
    IngestionLogDTO ingestionLogDTO;

    @Nullable
    @JsonProperty("etl_log")
    EtlLogDTO etlLogDTO;
}
