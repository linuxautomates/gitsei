package io.levelops.ingestion.agent.config;

import io.levelops.ingestion.agent.model.AgentType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@Log4j2
public class BaseAgentConfig {

    @Bean("agentId")
    public String agentId() {
        String agentId = UUID.randomUUID().toString();
        log.info("agent_id={}", agentId);
        return agentId;
    }

    @Bean("defaultAgentType")
    public AgentType agentType() {
        return new AgentType("default");
    }

    @Bean("agentVersion")
    public String agentVersion(@Value("${BUILD_VERSION:Unknown}") String buildVersion) {
        log.info("Version: {}", buildVersion);
        log.info("Memory: free={}MB, total={}MB, max={}MB", Runtime.getRuntime().freeMemory() / 1024 / 1024, Runtime.getRuntime().totalMemory() / 1024 / 1024, Runtime.getRuntime().maxMemory() / 1024 / 1024);
        return buildVersion;
    }

}
