package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaRiskReport.CxScaRiskReportBuilder.class)
public class CxScaRiskReport {

    @JsonProperty("projectId")
    String projectId;

    @JsonProperty("riskReportId")
    String riskReportId;

    @JsonProperty("highVulnerabilityCount")
    Integer highVulnerabilityCount;

    @JsonProperty("lowVulnerabilityCount")
    Integer lowVulnerabilityCount;

    @JsonProperty("totalPackages")
    Integer totalPackages;

    @JsonProperty("directPackages")
    Integer directPackages;

    @JsonProperty("riskScore")
    Double riskScore;

    @JsonProperty("totalOutdatedPackages")
    Integer totalOutdatedPackages;
}
