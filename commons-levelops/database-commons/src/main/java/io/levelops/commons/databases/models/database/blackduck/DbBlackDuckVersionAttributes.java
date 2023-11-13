package io.levelops.commons.databases.models.database.blackduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbBlackDuckVersionAttributes.DbBlackDuckVersionAttributesBuilder.class)
public class DbBlackDuckVersionAttributes {

    @JsonProperty("nickname")
    String versionNickName;

    @JsonProperty("phase")
    String phase;

    @JsonProperty("distribution")
    String distribution;

    @JsonProperty("settingUpdatedAt")
    Date settingsUpdatedAt;

    @JsonProperty("policyStatus")
    String policyStatus;

    @JsonProperty("lastBomUpdateDate")
    Date lastBomUpdate;
}
