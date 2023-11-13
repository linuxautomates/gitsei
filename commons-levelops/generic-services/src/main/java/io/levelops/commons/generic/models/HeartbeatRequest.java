package io.levelops.commons.generic.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.CiCdInstanceDetails;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HeartbeatRequest.HeartbeatRequestBuilder.class)
public class HeartbeatRequest {

    @JsonProperty("instance_id")
    String instanceId;

    @JsonProperty("timestamp")
    Long timestamp;

    @JsonProperty("details")
    CiCdInstanceDetails ciCdInstanceDetails;

}
