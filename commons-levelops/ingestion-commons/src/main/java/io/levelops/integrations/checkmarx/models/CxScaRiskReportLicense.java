package io.levelops.integrations.checkmarx.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaRiskReportLicense.CxScaRiskReportLicenseBuilder.class)
public class CxScaRiskReportLicense {

    @JsonProperty("id")
    String id;

    @JsonProperty("referenceType")
    String referenceType;

    @JsonProperty("reference")
    String reference;

    @JsonProperty("royaltyFree")
    String royaltyFree;

    @JsonProperty("copyrightRiskScore")
    Integer copyrightRiskScore;

    @JsonProperty("riskLevel")
    String riskLevel;

    @JsonProperty("linking")
    String linking;

    @JsonProperty("copyLeft")
    String copyLeft;

    @JsonProperty("patentRiskScore")
    Integer patentRiskScore;

    @JsonProperty("name")
    String name;

    @JsonProperty("url")
    String url;
}
