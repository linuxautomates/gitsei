package io.levelops.ingestion.agent.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.agent.control.AgentControlService;
import io.levelops.ingestion.agent.controllers.JobController;
import io.levelops.ingestion.agent.model.AgentType;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.services.IngestionAgentControlClient;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class ControlConfig {

    @Value("${CONTROL_PLANE_URL:}")
    private String controlPlaneUrl;

    @Value("${SCHEDULING_INTERVAL:30}")
    private Long schedulingInterval;

    @Value("${AUTO_CLEAR_JOBS:true}")
    private Boolean autoClearJobs;

    @Bean
    public IngestionAgentControlClient agentControlClient(ObjectMapper objectMapper,
                                                 OkHttpClient okHttpClient) {
        // base path depends on the endpoint (control plane or server api) - default-agent uses control plane directly
        String controlPlaneBasePath = controlPlaneUrl == null ? null :
                HttpUrl.parse(controlPlaneUrl).newBuilder()
                        .addPathSegment("control-plane")
                        .addPathSegment("v1")
                        .build()
                        .toString();

        log.info("Initializing AgentControlClient with url='{}'", controlPlaneUrl);
        return IngestionAgentControlClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(okHttpClient)
                .controlPlaneUrl(controlPlaneBasePath) // nullable
                .build();
    }

    @Bean
    public AgentControlService agentControlService(IngestionAgentControlClient controlClient,
                                                   @Qualifier("agentId") String agentId,
                                                   AgentType agentType,
                                                   @Qualifier("agentVersion") String agentVersion,
                                                   IngestionEngine ingestionEngine,
                                                   JobController jobController) {
        return AgentControlService.builder()
                .controlClient(controlClient)
                .agentId(agentId)
                .agentType(agentType.getValue())
                .agentVersion(agentVersion)
                .ingestionEngine(ingestionEngine)
                .jobController(jobController)
                .enableScheduling(StringUtils.isNotEmpty(controlPlaneUrl))
                .schedulingIntervalInSec(schedulingInterval)
                .autoClearJobs(autoClearJobs) // only matters if scheduling is enabled
                .reservedJobsFilter(false) // make sure we don't pull jobs that are reserved for dedicated agents (satellite, ...)
                .build();
    }
}
