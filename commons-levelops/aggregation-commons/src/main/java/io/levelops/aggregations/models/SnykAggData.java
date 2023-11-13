package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.snyk.SnykAggForSeverity;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = SnykAggData.SnykAggDataBuilderImpl.class)
public class SnykAggData extends AggData {
    @JsonProperty("agg_by_severity")
    @Default
    private Map<String, SnykAggForSeverity> aggBySeverity = new HashMap<>();

    @JsonProperty("agg_by_scm_url_by_severity")
    @Default
    private Map<String, Map<String, SnykAggForSeverity>> aggBySCMUrlBySeverity = new HashMap<>();

    @JsonProperty("agg_by_scm_repo_name_partial_by_severity")
    @Default
    private Map<String, Map<String, SnykAggForSeverity>> aggBySCMRepoNamePartialBySeverity = new HashMap<>();

    @JsonProperty("suppressed_issues")
    @Default
    private List<SnykVulnerability> suppressedIssues = new ArrayList<>();

    @JsonProperty("new_vulns") //new issues since last day's agg run
    @Default
    private List<SnykVulnerability> newVulns = new ArrayList<>();

    @JsonProperty("new_vulns_count") //new issues since last day's agg run
    @Default
    private Integer newVulnsCount = 0;

    @JsonProperty("total_vuln_count")
    @Default
    private Integer vulnerabilityCount = 0;

    @JsonProperty("agg_by_severity_time_series")
    @Default
    private Map<Long, Map<String, SnykAggForSeverity>> aggBySeverityTimeSeries = new HashMap<>();

    @JsonPOJOBuilder(withPrefix = "")
    static final class SnykAggDataBuilderImpl extends SnykAggDataBuilder<SnykAggData, SnykAggDataBuilderImpl> {
    }
}
