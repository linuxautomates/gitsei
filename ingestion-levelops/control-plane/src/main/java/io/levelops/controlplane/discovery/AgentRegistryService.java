package io.levelops.controlplane.discovery;

import io.levelops.ingestion.models.AgentHandle;
import java.util.List;
import java.util.Optional;

public interface AgentRegistryService {

    void registerAgent(AgentHandle agentHandle);

    Optional<RegisteredAgent> refreshHeartbeatAndTelemetry(AgentHandle agentHandle);

    List<RegisteredAgent> getAllAgents();

    Optional<RegisteredAgent> getAgentById(String agentId);

    /**
     * Get a list of active agents by looking up with the controller name
     * 
     * @param controllerName
     * @return
     */
    List<RegisteredAgent> getAgentsByControllerName(String controllerName);

    void clearAll();
}
