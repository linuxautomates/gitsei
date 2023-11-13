package io.levelops.commons.databases.models.database;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdInstanceDetails.CiCdInstanceDetailsBuilder.class)
public class CiCdInstanceDetails {

    @JsonProperty("jenkins_version")
    String jenkinsVersion;

    @JsonProperty("plugin_version")
    String pluginVersion;

    @JsonProperty("config_updated_at")
    Instant configUpdatedAt;

    @JsonProperty("jenkins_instance_url")
    String instanceUrl;

    @JsonProperty("jenkins_instance_name")
    String instanceName;

}