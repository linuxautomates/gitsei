package io.levelops.commons.generic.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GenericRequestsClientIntegrationTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private GenericRequestsClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new GenericRequestsClient(okHttpClient, objectMapper, "http://localhost:8080");
    }

    @Test
    public void create() throws GenericRequestsClient.GenericRequestsClientException, JsonProcessingException {
        Map<String,Object> data = new HashMap<>();
        data.put("repo_url", "https://github.com/testadmin1-levelops/openapi-generator.git");

        String payload = objectMapper.writeValueAsString(data);

        GenericRequest genericRequest = GenericRequest.builder()
                .requestType("JenkinsPluginJobRunClearanceRequest")
                .payload(payload)
                .build();

        GenericResponse r = client.create("foo", genericRequest);
        System.out.println(r);
    }

    @Test
    public void createMultipart() throws GenericRequestsClient.GenericRequestsClientException, IOException {
        Map<String,Object> data = new HashMap<>();
        data.put("repo_url", "https://github.com/testadmin1-levelops/openapi-generator.git");

        String payload = objectMapper.writeValueAsString(data);

        GenericRequest genericRequest = GenericRequest.builder()
                .requestType("JenkinsPluginJobRunClearanceRequest")
                .payload(payload)
                .build();

        MultipartFile jsonFile = new MockMultipartFile("json", "json", "application/json", objectMapper.writeValueAsBytes(genericRequest));
        MultipartFile zipFile = new MockMultipartFile("file", "file", "application/json", "temp data".getBytes());
        GenericResponse r = client.createMultipart("foo", jsonFile, zipFile);
        System.out.println(r);
    }
}