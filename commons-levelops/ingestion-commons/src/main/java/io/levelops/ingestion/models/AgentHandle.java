package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AgentHandle.AgentHandleBuilder.class)
public class AgentHandle {

    @JsonProperty("agent_id")
    String agentId;

    @JsonProperty("agent_type")
    String agentType;

    @JsonProperty("controller_names")
    Set<String> controllerNames;

    @JsonProperty("tenant_id")
    String tenantId; // for satellites

    @JsonProperty("integration_ids")
    List<String> integrationIds; // for satellites

    @JsonProperty("telemetry")
    Map<String, Object> telemetry; // contains telemetry data (like build version, etc.)

}