package io.levelops.ingestion.agent.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.io.RollingOutputStream;
import io.levelops.ingestion.agent.control.AgentControlService;
import io.levelops.ingestion.agent.controllers.JobController;
import io.levelops.ingestion.agent.model.AgentType;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.services.IngestionAgentControlClient;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class ControlConfig {

    @Bean
    public IngestionAgentControlClient agentControlClient(SatelliteConfigFileProperties configProperties,
                                                          ObjectMapper objectMapper,
                                                          @Qualifier("proxyOkHttpClient") OkHttpClient okHttpClient) {
        // base path depends on the endpoint (control plane or server api) - satellite uses server api
        String controlPlaneBasePath = configProperties.getSatellite().getUrl() == null ? null :
                HttpUrl.parse(configProperties.getSatellite().getUrl()).newBuilder()
                        .addPathSegment("v1")
                        .addPathSegment("ingestion")
                        .build()
                        .toString();

        /* for testing purposes: use control-plane directly...
        controlPlaneBasePath = configProperties.getLevelOpsUrl()  == null ? null :
                HttpUrl.parse( configProperties.getLevelOpsUrl() ).newBuilder()
                        .addPathSegment("control-plane")
                        .addPathSegment("v1")
                        .build()
                        .toString();
         */

        // authenticate client
        OkHttpClient client = okHttpClient.newBuilder()
                .addInterceptor(new GenericTokenInterceptor(ClientConstants.AUTHORIZATION, ClientConstants.APIKEY_, StaticOauthTokenProvider.builder()
                        .token(configProperties.getSatellite().getApiKey())
                        .build()))
                .build();

        log.info("Initializing AgentControlClient with url='{}'", controlPlaneBasePath);
        return IngestionAgentControlClient.builder()
                .objectMapper(objectMapper)
                .okHttpClient(client)
                .controlPlaneUrl(controlPlaneBasePath)
                .build();
    }

    @Bean
    public AgentControlService agentControlService(IngestionAgentControlClient controlClient,
                                                   @Qualifier("agentId") String agentId,
                                                   AgentType agentType,
                                                   @Qualifier("agentVersion") String agentVersion,
                                                   RollingOutputStream rollingLog,
                                                   IngestionEngine ingestionEngine,
                                                   JobController jobController,
                                                   SatelliteConfigFileProperties configProperties) {
        return AgentControlService.builder()
                .controlClient(controlClient)
                .agentId(agentId)
                .agentType(agentType.getValue())
                .agentVersion(agentVersion)
                .rollingLog(rollingLog)
                .tenantId(configProperties.getSatellite().getTenant())
                .integrationIds(configProperties.getIntegrationIds())
                .ingestionEngine(ingestionEngine)
                .jobController(jobController)
                .enableScheduling(true)
                .schedulingIntervalInSec(configProperties.getSatellite().getSchedulingInterval())
                .autoClearJobs(true)
                .reservedJobsFilter(true) // this will anyways be enforced to be True by the Levelops API (to prevent cross-contamination)
                .build();
    }
}
