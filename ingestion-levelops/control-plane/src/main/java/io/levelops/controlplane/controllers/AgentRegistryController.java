package io.levelops.controlplane.controllers;

import io.levelops.commons.models.ListResponse;
import io.levelops.controlplane.discovery.AgentRegistryService;
import io.levelops.controlplane.discovery.RegisteredAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/agents")
public class AgentRegistryController {

    private final AgentRegistryService agentRegistryService;

    @Autowired
    public AgentRegistryController(AgentRegistryService agentRegistryService) {
        this.agentRegistryService = agentRegistryService;
    }

    @GetMapping
    public ListResponse<RegisteredAgent> listAgents() {
        return ListResponse.of(agentRegistryService.getAllAgents());
    }

    @DeleteMapping
    public void clearAgents() {
        agentRegistryService.clearAll();
    }
}
