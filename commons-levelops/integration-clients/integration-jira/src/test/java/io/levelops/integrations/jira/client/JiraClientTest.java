package io.levelops.integrations.jira.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraClientTest {
    MockWebServer server;
    JiraClient client;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        client = new JiraClient(
                okHttpClient,
                objectMapper,
                () -> server.url("/").toString(),
                true,
                "1.0",
                "1.0",
                true,
                List.of(),
                Integration.Authentication.ATLASSIAN_CONNECT_JWT);
    }


    @After
    public void cleanup() throws IOException {
        server.shutdown();
    }

    @Test
    public void testBulkEmail() throws JiraClientException, InterruptedException {
        server.enqueue(new MockResponse().setBody("[{\"accountId\":\"620ea8f807f51e006944affb\",\"email\":\"thomas.grasman@harness.io\"},{\"accountId\":\"6079c175994473006837cb70\",\"email\":\"andrew.spangler@harness.io\"}]"));
        var emails = client.getUserEmailBulk(List.of("1", "2"));
        assertThat(emails.size()).isEqualTo(2);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getRequestUrl().queryParameterValues("accountId")).containsExactly("1", "2");
    }

    @Test
    public void testBulkEmailLimit() throws JiraClientException, InterruptedException {
        server.enqueue(new MockResponse().setBody("[{\"accountId\":\"620ea8f807f51e006944affb\",\"email\":\"thomas.grasman@harness.io\"},{\"accountId\":\"6079c175994473006837cb70\",\"email\":\"andrew.spangler@harness.io\"}]"));
        server.enqueue(new MockResponse().setBody("[{\"accountId\":\"620ea8f807f51e006944affb\",\"email\":\"thomas.grasman@harness.io\"},{\"accountId\":\"6079c175994473006837cb70\",\"email\":\"andrew.spangler@harness.io\"}]"));
        List<String> accountIds = IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(java.util.stream.Collectors.toList());
        var emails = client.getUserEmailBulk(accountIds);
        assertThat(emails.size()).isEqualTo(4);
        RecordedRequest request1 = server.takeRequest();
        RecordedRequest request2 = server.takeRequest();
        assertThat(request1.getRequestUrl().queryParameterValues("accountId")).containsExactlyElementsOf(accountIds.subList(0, 90));
        assertThat(request2.getRequestUrl().queryParameterValues("accountId")).containsExactlyElementsOf(accountIds.subList(90, 100));
    }
}