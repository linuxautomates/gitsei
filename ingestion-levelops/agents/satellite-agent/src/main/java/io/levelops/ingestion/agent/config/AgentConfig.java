package io.levelops.ingestion.agent.config;

import io.levelops.commons.io.RollingOutputStream;
import io.levelops.ingestion.agent.SatelliteAgentApplication;
import io.levelops.ingestion.agent.model.AgentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AgentConfig {

    @Bean
    @Primary
    public AgentType agentType() {
        return new AgentType("satellite");
    }

    @Bean
    public RollingOutputStream rollingLog() {
        return SatelliteAgentApplication.rollingLog;
    }
}
