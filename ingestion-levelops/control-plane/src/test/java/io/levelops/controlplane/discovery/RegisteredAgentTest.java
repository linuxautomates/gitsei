package io.levelops.controlplane.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.AgentHandle;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisteredAgentTest {

    @Test
    public void serial() throws JsonProcessingException {
        RegisteredAgent in = RegisteredAgent.builder()
                .lastHeartbeat(null)
                .agentHandle(AgentHandle.builder()
                        .agentId("123")
                        .controllerNames(Set.of("a", "b"))
                        .agentType("type")
                        .tenantId("coke")
                        .integrationIds(List.of("42"))
                        .telemetry(Map.of("log", "ok\nok\n"))
                        .build())
                .build();
        String s = DefaultObjectMapper.get().writeValueAsString(in);
        System.out.println(s);

        RegisteredAgent out = DefaultObjectMapper.get().readValue(s, RegisteredAgent.class);
        assertThat(out).isEqualTo(in);
    }

}