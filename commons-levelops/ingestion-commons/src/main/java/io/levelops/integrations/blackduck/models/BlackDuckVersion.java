package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckVersion.BlackDuckVersionBuilder.class)
public class BlackDuckVersion {
    @JsonProperty("versionName")
    String name;

    @JsonProperty("nickname")
    String versionNickName;

    @JsonProperty("description")
    String description;

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
    Date settingsUpdatedAt;

    @JsonProperty("securityRiskProfile")
    BlackDuckRiskCounts securityRiskProfile;

    @JsonProperty("licenseRiskProfile")
    BlackDuckRiskCounts licenseRiskProfile;

    @JsonProperty("operationalRiskProfile")
    BlackDuckRiskCounts operationalRiskProfile;

    @JsonProperty("policyStatus")
    String policyStatus;

    @JsonProperty("lastBomUpdateDate")
    Date lastBomUpdate;
}
