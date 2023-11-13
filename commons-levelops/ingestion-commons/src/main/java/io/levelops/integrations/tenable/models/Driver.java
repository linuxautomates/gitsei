package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Driver.DriverBuilder.class)
public class Driver {

    @JsonProperty("age_of_vuln")
    AgeOfVuln ageOfVuln;

    @JsonProperty("exploit_code_maturity")
    String exploitCodeMaturity;

    @JsonProperty("cvss3_impact_score")
    Integer cvss3ImpactScore;

    @JsonProperty("cvss_impact_score_predicted")
    Boolean cvssImpactScorePredicted;

    @JsonProperty("threat_intensity_last28")
    String threatIntensityLast28;

    @JsonProperty("threat_recency")
    ThreatRecency threatRecency;

    @JsonProperty("threat_sources_last28")
    List<String> threatSourcesLast28;

    @JsonProperty("product_coverage")
    String productCoverage;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AgeOfVuln.AgeOfVulnBuilder.class)
    public static class AgeOfVuln {
        @JsonProperty("lower_bound")
        Integer lowerBound;

        @JsonProperty("upper_bound")
        Integer upperBound;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ThreatRecency.ThreatRecencyBuilder.class)
    public static class ThreatRecency {
        @JsonProperty("lower_bound")
        Integer lowerBound;

        @JsonProperty("upper_bound")
        Integer upperBound;
    }
}
