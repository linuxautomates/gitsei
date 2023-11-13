package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaScan.CxScaScanBuilder.class)
public class CxScaScan {

    @JsonProperty("projectId")
    String projectId;

    @JsonProperty("scanId")
    String scanId;

    @JsonProperty("status")
    CxScaScanStatus status;

    @JsonProperty("origin")
    String origin;

    @JsonProperty("licenses")
    List<CxScaRiskReportLicense> licenses;

    @JsonProperty("packages")
    List<CxScaRiskReportPackage> packages;

    @JsonProperty("vulnerabilities")
    List<CxScaRiskReportVulnerability> vulnerabilities;

}
