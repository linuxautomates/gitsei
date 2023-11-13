package io.levelops.integrations.gitlab.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


public class GitlabClientTest {
    MockWebServer mockWebServer;
    GitlabClientFactory clientFactory;
    OkHttpClient client;
    ObjectMapper objectMapper;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new OkHttpClient();
        objectMapper = DefaultObjectMapper.get();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();;
    }

    @Test
    public void testSwallowExceptions() throws JsonProcessingException, GitlabClientException {
        GitlabClient gitlabClient = new GitlabClient(client, objectMapper, mockWebServer.url("/").toString(), 10, true, false, true);
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(500));
        var mrs = gitlabClient.streamMergeRequests("abc", new Date(), new Date(), 10).collect(Collectors.toList());
        var jobs = gitlabClient.streamJobs("a", "a", 10).collect(Collectors.toList());
        var issues = gitlabClient.streamIssues(new Date(), new Date(), 10).collect(Collectors.toList());
    }
}