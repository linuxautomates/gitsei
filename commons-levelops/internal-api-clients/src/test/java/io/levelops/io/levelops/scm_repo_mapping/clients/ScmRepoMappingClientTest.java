package io.levelops.io.levelops.scm_repo_mapping.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.io.levelops.scm_repo_mapping.models.ScmRepoMappingResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;

public class ScmRepoMappingClientTest {
    @Test
    public void test202Response() throws IOException {
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        MockWebServer mockWebserver = new MockWebServer();
        ScmRepoMappingResponse response = ScmRepoMappingResponse.builder()
                .jobId("jobId-1")
                .build();
        mockWebserver.enqueue(new MockResponse().setResponseCode(202).setBody(objectMapper.writeValueAsString(response)));
        mockWebserver.start();
        HttpUrl baseUrl = mockWebserver.url("/v1/mock/");

        OkHttpClient okHttpClient = new OkHttpClient();
        ScmRepoMappingClient scmRepoMappingClient = new ScmRepoMappingClient(okHttpClient, objectMapper, baseUrl.toString());
        scmRepoMappingClient.getScmRepoMapping("sidofficial", "11");
    }
}
