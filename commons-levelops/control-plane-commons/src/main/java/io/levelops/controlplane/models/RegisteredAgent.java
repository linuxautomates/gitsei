package io.levelops.controlplane.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RegisteredAgent.RegisteredAgentBuilder.class)
public class RegisteredAgent {
    @JsonProperty("agent_id")
    String agentId;
    @JsonProperty("agent_type")
    String agentType;
    @JsonProperty("controller_names")
    List<String> controllerNames;
    @JsonProperty("tenant_id")
    String tenantId;
    @JsonProperty("integration_ids")
    List<String> integrationIds;
    @JsonProperty("last_heartbeat")
    Long lastHeartbeat;
    @JsonProperty("last_heartbeat_since")
    Long lastHeartbeatSince;
}