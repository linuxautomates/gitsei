package io.levelops.commons.generic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HeartbeatResponse.HeartbeatResponseBuilder.class)
public class HeartbeatResponse {

    @JsonProperty("success")
    boolean success;

    @JsonProperty("server_version")
    String serverVersion;

    @JsonProperty("configuration")
    CiCdInstanceConfig configuration;
}
