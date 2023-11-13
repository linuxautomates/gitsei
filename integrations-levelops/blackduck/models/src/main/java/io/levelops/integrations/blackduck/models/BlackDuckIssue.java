package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckIssue.BlackDuckIssueBuilder.class)
@JsonIgnoreProperties(value={ "_meta" }, allowSetters= true)
public class BlackDuckIssue {

    @JsonProperty("componentName")
    String componentName;

    @JsonProperty("componentVersionName")
    String componentVersionName;

    @JsonProperty("componentVersionOriginName")
    String componentVersionOriginName;

    @JsonProperty("componentVersionOriginId")
    String componentVersionOriginId;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckLicence.BlackDuckLicenceBuilder.class)
    public static class BlackDuckLicence {
        @JsonProperty("type")
        String licenceType;

        @JsonProperty("licenses")
        List<BlackDuckLicenceInfo> blackDuckLicenceInfo;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckLicenceInfo.BlackDuckLicenceInfoBuilder.class)
    public static class BlackDuckLicenceInfo {

        @JsonProperty("licenses")
        String licenceName;

        @JsonProperty("ownership")
        String licenceOwnership;

        @JsonProperty("codeSharing")
        String codeSharing;

        @JsonProperty("licenseDisplay")
        String licenseDisplay;
    }

    @JsonProperty("vulnerabilityWithRemediation")
    BlackDuckVulnerability blackDuckVulnerability;

    @JsonProperty("_meta")
    BlackDuckMetadata blackDuckMetadata;
}
