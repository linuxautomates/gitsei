package io.levelops.workitems.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.NotificationRequestorType;
import io.levelops.commons.databases.models.database.QuestionnaireNotificationRequest;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

public class QuestionnaireNotificationClientIntegrationTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private QuestionnaireNotificationClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new QuestionnaireNotificationClient(okHttpClient, objectMapper, "http://localhost:8080");
    }

    @Test
    public void create() throws InternalApiClientException {
        QuestionnaireNotificationRequest request = QuestionnaireNotificationRequest.builder()
                .questionnaireId(UUID.fromString("36a3b38f-cc4a-4a57-8560-c4b6a3193d20"))
                .recipients(List.of("viraj@levelops.io", "slackbot-test-private-va"))
                .mode(NotificationMode.SLACK)
                .requestorType(NotificationRequestorType.SLACK_USER)
                .requestorId("US4JC7ZM5")
                .requestorName("viraj")
                .build();
        QuestionnaireNotificationClient.QueueResponse r = client.queueRequest("foo", request);
        System.out.println(r);
    }

}