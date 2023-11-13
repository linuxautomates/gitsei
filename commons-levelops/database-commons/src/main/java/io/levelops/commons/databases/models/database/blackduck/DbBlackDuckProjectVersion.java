package io.levelops.commons.databases.models.database.blackduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.blackduck.models.BlackDuckRiskCounts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class DbBlackDuckProjectVersion {
    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("project_name")
    private String name;

    @JsonProperty("project_description")
    private String projectDescription;

    @JsonProperty("project_attributes")
    DbBlackDuckProjectAttributes attributes;

    @JsonProperty("proj_created_at")
    private Date projCreatedAt;

    @JsonProperty("proj_updated_at")
    private Date projUpdatedAt;

    @JsonProperty("version_name")
    private String versionName;

    @JsonProperty("version_nickname")
    String versionNickName;

    @JsonProperty("version_released_on")
    Date releaseDate;

    @JsonProperty("source")
    private String source;

    @JsonProperty("version_attributes")
    private DbBlackDuckVersionAttributes versionAttributes;

    @JsonProperty("security_risk_profile")
    BlackDuckRiskCounts securityRiskProfile;

    @JsonProperty("license_risk_profile")
    BlackDuckRiskCounts licenseRiskProfile;

    @JsonProperty("operational_risk_profile")
    BlackDuckRiskCounts operationalRiskProfile;

    @JsonProperty("distribution")
    String distribution;

    @JsonProperty("setting_updated_at")
    Date settingUpdatedAt;

    @JsonProperty("version_created_at")
    Date versionCreatedAt;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;
}
