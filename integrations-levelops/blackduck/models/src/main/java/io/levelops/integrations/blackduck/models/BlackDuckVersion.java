package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckVersion.BlackDuckVersionBuilder.class)
@JsonIgnoreProperties(value={ "_meta" }, allowSetters= true)
public class BlackDuckVersion {

    @JsonProperty("versionName")
    String versionName;

    @JsonProperty("nickname")
    String versionNickName;

    @JsonProperty("releasedOn")
    Date releaseDate;

    @JsonProperty("createdAt")
    Date versionCreatedAt;

    @JsonProperty("source")
    String source;

    @JsonProperty("phase")
    String phase;

    @JsonProperty("distribution")
    String distribution;

    @JsonProperty("settingUpdatedAt")
    Date settingUpdatedAt;

    @JsonProperty("securityRiskProfile")
    BlackDuckRiskCounts securityRiskProfile;

    @JsonProperty("licenseRiskProfile")
    BlackDuckRiskCounts licenseRiskProfile;

    @JsonProperty("operationalRiskProfile")
    BlackDuckRiskCounts operationalRiskProfile;

    @JsonProperty("policyStatus")
    String policyStatus;

    @JsonProperty("licence")
    BlackDuckLicence blackDuckLicence;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckVersion.BlackDuckLicence.BlackDuckLicenceBuilder.class)
    public static class BlackDuckLicence {
        @JsonProperty("type")
        String licenceType;
        @JsonProperty("licenses")
        List<BlackDuckVersion.BlackDuckLicenceInfo> blackDuckLicenceInfo;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckVersion.BlackDuckLicenceInfo.BlackDuckLicenceInfoBuilder.class)
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

    @JsonProperty("lastBomUpdateDate")
    Date lastBomUpdateDate;

    @JsonProperty("bomCount")
    Integer bomCount;

    @JsonProperty("_meta")
    BlackDuckMetadata blackDuckMetadata;
}
