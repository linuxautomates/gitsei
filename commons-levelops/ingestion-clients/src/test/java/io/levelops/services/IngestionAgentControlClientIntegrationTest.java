package io.levelops.services;

import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.oauth.GenericTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.AgentHandle;
import okhttp3.OkHttpClient;
import org.junit.Test;


public class IngestionAgentControlClientIntegrationTest {

    @Test
    public void name() throws IngestionAgentControlClient.ControlException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .addInterceptor(new GenericTokenInterceptor(ClientConstants.AUTHORIZATION, ClientConstants.APIKEY_, StaticOauthTokenProvider.builder()
                        .token(System.getenv("API_KEY"))
                        .build()))
                .build();
        IngestionAgentControlClient ingestionAgentControlClient = new IngestionAgentControlClient(client, DefaultObjectMapper.get(),
                "https://testapi1.levelops.io/v1/ingestion");

        ingestionAgentControlClient.registerAgent(AgentHandle.builder()
                .build());
    }
}