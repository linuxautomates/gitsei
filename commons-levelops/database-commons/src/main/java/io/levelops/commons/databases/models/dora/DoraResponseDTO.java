package io.levelops.commons.databases.models.dora;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DoraResponseDTO.DoraResponseDTOBuilder.class)
public class DoraResponseDTO {

    @JsonProperty("time_series")
    DoraTimeSeriesDTO timeSeries;

    @JsonProperty("stats")
    DoraSingleStateDTO stats;


}
