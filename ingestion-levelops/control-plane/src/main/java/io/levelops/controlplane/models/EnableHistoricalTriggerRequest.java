package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EnableHistoricalTriggerRequest.EnableHistoricalTriggerRequestBuilder.class)
public class EnableHistoricalTriggerRequest {
    @JsonProperty("historical_span_in_days")
    Long historicalSpanInDays;

    @JsonProperty("historical_sub_job_span_in_min")
    Long historicalSubJobSpanInMin;

    @JsonProperty("historical_successive_backward_scan_count")
    Integer historicalSuccessiveBackwardScanCount;
}