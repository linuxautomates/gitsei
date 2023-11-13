package io.levelops.plugins.clients;

import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.plugins.models.StoredPluginResultDTO;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.plugins.clients.PluginResultsClient.PluginResultsClientException;

public class PluginResultsClientIntegrationTest {

    private PluginResultsClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new PluginResultsClient(okHttpClient, DefaultObjectMapper.get(), "http://localhost:8080");
    }

    @Test
    public void create() throws PluginResultsClientException {
        String r = client.createPluginResult("foo", PluginResultDTO.builder()
                .tool("jenkins_config")
                .pluginClass("monitoring")
                .productIds(Collections.singletonList("1"))
                .version("1")
                .successful(true)
                .metadata(Map.of())
                .labels(Map.of("env", List.of("prod", "us1")))
                .results(Map.of("data", "yes"))
                .build());
        System.out.println(r);
    }


    @Test
    public void multipart() throws PluginResultsClientException, IOException {
        String r = client.createPluginResultMultipart("foo",
                new MockMultipartFile("random1", "jsonOriginal", "application/json", DefaultObjectMapper.get().writeValueAsString(
                        PluginResultDTO.builder()
                                .pluginClass("monitoring")
                                .tool("jenkins_config")
                                .productIds(Collections.singletonList("71"))
                                .version("1")
                                .successful(true)
                                .metadata(Map.of())
                                .labels(Map.of("env", List.of("prod", "us1")))
                                .results(Map.of("data", "yes"))
                                .build()).getBytes()),
                new MockMultipartFile("random", "original", "plain/text", "some more data".getBytes()));
        System.out.println(r);
    }

    @Test
    public void testSubmitPluginResultWithPreProcessing() throws PluginResultsClientException, IOException, URISyntaxException {
        File zipFileResource = new File(this.getClass().getClassLoader().getResource("data-3.zip").toURI());

        String r = client.submitPluginResultWithPreProcessing("foo",
                new MockMultipartFile("json", "json", "application/json", DefaultObjectMapper.get().writeValueAsString(
                        PluginResultDTO.builder()
                                .pluginClass("monitoring")
                                .tool("jenkins_config")
                                .productIds(Collections.singletonList("71"))
                                .version("1")
                                .successful(true)
                                .metadata(Map.of())
                                .labels(Map.of("env", List.of("prod", "us1")))
                                .results(Map.of("data", "yes"))
                                .build()).getBytes()),
                new MockMultipartFile("result", "result", "application/octet-stream", Files.readAllBytes(zipFileResource.toPath())));
        System.out.println(r);
    }

    @Test
    public void createStoredPluginResult() throws PluginResultsClientException {
        PluginResultDTO pluginResultDTO = PluginResultDTO.builder()
                .tool("jenkins_config")
                .pluginClass("monitoring")
                .productIds(Collections.singletonList("1"))
                .version("1")
                .successful(true)
                .metadata(Map.of())
                .labels(Map.of("env", List.of("prod", "us1")))
                .results(Map.of("data", "yes"))
                .build();
        StoredPluginResultDTO storedPluginResultDTO = StoredPluginResultDTO.builder()
                .pluginResult(pluginResultDTO).resultId(UUID.fromString("d7ccc7a3-ae6c-4398-9bdd-372be7d5735e"))
                .pluginResultStoragePath("cicd-job-run-stage-step-logs/tenant-foo/2020/09/02/d7ccc7a3-ae6c-4398-9bdd-372be7d5735e")
                .build();

        String r = client.createStoredPluginResult("foo", storedPluginResultDTO);
        System.out.println(r);
    }

    @Test
    public void list() throws PluginResultsClientException {
        PaginatedResponse<PluginResultDTO> r = client.list("foo", DefaultListRequest.builder().build());
        DefaultObjectMapper.prettyPrint(r);
    }

    @Test
    public void testGetOldestJobRunStartTimeById() throws PluginResultsClientException {
        Map<String, Object> map = client.getOldestJobRunStartTimeById("foo", "6b9b4179-f770-4333-ab5d-8d21d335dcaa");
        Integer oldestJobRunStartTime = (Integer) map.get("start_time");
        Assert.assertNotNull(oldestJobRunStartTime);
        Assert.assertEquals(1603959024L, oldestJobRunStartTime.longValue());
    }
}