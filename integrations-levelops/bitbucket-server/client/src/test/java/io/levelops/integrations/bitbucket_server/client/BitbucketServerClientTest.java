package io.levelops.integrations.bitbucket_server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketServerClientTest {
    private MockWebServer mockWebServer;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @After
    public void cleanup() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void test404FetchPrCommits() throws IOException, BitbucketServerClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        BitbucketServerClient bitbucketServerClient = new BitbucketServerClient(okHttpClient, objectMapper, mockWebServer.url("/").toString(), 10, true);
        var s = bitbucketServerClient.streamPrCommits("project", "repo", 1).collect(Collectors.toList());
        assertThat(s).hasSize(0);
    }


}