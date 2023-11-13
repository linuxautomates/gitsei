package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTriggerSettings.DbTriggerSettingsBuilder.class)
public class DbTriggerSettings {

    @JsonProperty("backpressure_threshold")
    Integer backpressureThreshold;

    @JsonProperty("backward_scan_subjob_span_in_minutes")
    Long backwardScanSubjobSpanInMinutes;

    @JsonProperty("onboarding_span_in_days")
    Long onboardingSpanInDays;

    /**
     * To override default page size on a data source basis.
     * For example: {@code {jira_issues: 20} } sets the page size to 20 for Jira Issues.
     */
    @JsonProperty("data_source_page_sizes")
    Map<String, Integer> dataSourcePageSizes;

}
