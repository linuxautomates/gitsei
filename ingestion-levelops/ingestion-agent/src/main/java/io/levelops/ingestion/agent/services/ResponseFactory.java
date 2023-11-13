package io.levelops.ingestion.agent.services;

import io.levelops.commons.models.AgentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ResponseFactory {

    private String agentId;

    @Autowired
    public ResponseFactory(@Qualifier("agentId") String agentId) {
        this.agentId = agentId;
    }

    public <T> AgentResponse<T> build(T response) {
        return AgentResponse.<T>builder()
                .agentId(agentId)
                .response(response)
                .build();
    }
}
