package io.levelops.commons.databases.models.dora;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DoraTimeSeriesDTO.DoraTimeSeriesDTOBuilder.class)
public class DoraTimeSeriesDTO {

    @JsonProperty("day")
    List<TimeSeriesData> day;

    @JsonProperty("week")
    List<TimeSeriesData> week;

    @JsonProperty("month")
    List<TimeSeriesData> month;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TimeSeriesData.TimeSeriesDataBuilder.class)
    public static class TimeSeriesData {
        @JsonProperty("key")
        Long key;

        @JsonProperty("count")
        Integer count;

        @JsonProperty("additional_key")
        String additionalKey;

        @JsonProperty("stacks")
        List<Map<String, Object>> stacks;
    }
}
