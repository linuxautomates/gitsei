package io.levelops.workitems.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequest;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public class WorkItemsRESTClientIntegrationTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private WorkItemsRESTClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new WorkItemsRESTClient(okHttpClient, objectMapper, "http://localhost:8080");
    }

    @Test
    public void createSnippetWorkItemMultipart() throws IOException {
        String snippetText = ResourceUtils.getResourceAsString("workitems/large_snippet_data.txt");
        String snippetText1 = "temp data";

        CreateSnippetWorkitemRequest request = CreateSnippetWorkitemRequest.builder()
                .title("Snippet from jenkins.dev.levelops.io")
                .url("https://jenkins.dev.levelops.io/view/Pipelines/job/maven-test-1/42/console")
                .requestor("viraj@levelops.io")
                .mode(NotificationMode.SLACK)
                .recipients(List.of("viraj@levelops.io", "slackbot-test-private-va-4-pub"))
                .build();

        MultipartFile jsonFile = new MockMultipartFile("json", "json", "application/json", objectMapper.writeValueAsBytes(request));
        MultipartFile snippetFile = new MockMultipartFile("file", "Really really really loooooooooooooooooooong name.txt", "application/json", snippetText.getBytes());
        WorkItem r = client.createSnippetWorkItemMultipart("foo", jsonFile, snippetFile);
        System.out.println(r);
    }

    @Test
    public void createSnippetWorkItem() throws IOException {
        String snippetText = ResourceUtils.getResourceAsString("workitems/large_snippet_data.txt");
        String snippetText1 = "temp data";

        CreateSnippetWorkitemRequestWithText request = CreateSnippetWorkitemRequestWithText.builder()
                .title("Snippet from jenkins.dev.levelops.io")
                .url("https://jenkins.dev.levelops.io/view/Pipelines/job/maven-test-1/42/console")
                .requestor("viraj@levelops.io")
                .mode(NotificationMode.SLACK)
                .recipients(List.of("viraj@levelops.io", "slackbot-test-private-va-4-pub"))
                .snippet(snippetText)
                .build();
        WorkItem r = client.createSnippetWorkItem("foo", request);
        System.out.println(r);
    }
}