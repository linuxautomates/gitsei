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
public class DbBlackDuckVersion {

    @JsonProperty("id")
    private String id;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("name")
    private String versionName;

    @JsonProperty("nickname")
    String versionNickName;

    @JsonProperty("description")
    String versionDescription;

    @JsonProperty("releasedOn")
    Date releaseDate;

    @JsonProperty("source")
    private String source;

    @JsonProperty("attributes")
    private DbBlackDuckVersionAttributes versionAttributes;

    @JsonProperty("securityRiskProfile")
    BlackDuckRiskCounts securityRiskProfile;

    @JsonProperty("licenseRiskProfile")
    BlackDuckRiskCounts licenseRiskProfile;

    @JsonProperty("operationalRiskProfile")
    BlackDuckRiskCounts operationalRiskProfile;

    @JsonProperty("distribution")
    String distribution;

    @JsonProperty("settingUpdatedAt")
    Date settingUpdatedAt;

    @JsonProperty("createdAt")
    Date versionCreatedAt;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

}
