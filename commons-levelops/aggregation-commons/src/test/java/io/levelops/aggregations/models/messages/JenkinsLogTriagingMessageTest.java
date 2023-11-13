package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class JenkinsLogTriagingMessageTest {

    private static final String company = "test";

    @Test
    public void serializationTest() throws JsonProcessingException {
        var message = JenkinsLogTriagingMessage.builder()
            .company(company)
            .jobName("jobName")
            .jobStatus("FAILED")
            .instanceId(UUID.fromString("8859f254-84e0-4f7f-8898-188476edd145"))
            .instanceName("Instance Name")
            .jobId(UUID.fromString("8c4b4a1d-cf5f-4043-b469-6117eab301df"))
            .jobRunId(UUID.fromString("75250155-fe80-4733-b82b-617b469f94a1"))
            .stageId(UUID.fromString("2062411b-9932-4587-9b61-7c4f1e221d5e"))
            .logLocation("logLocation")
            .logBucket("logBucket")
            .build();

        var messageText = DefaultObjectMapper.get().writeValueAsString(message);
        System.out.println("message: " + messageText);

        Assertions.assertThat(messageText).isNotBlank();
    }

    @Test
    public void deserializationTest() throws IOException {
        var message = ResourceUtils.getResourceAsObject("models/jenkins/analyze_message.json", JenkinsLogTriagingMessage.class);

        var ref = JenkinsLogTriagingMessage.builder()
            .company(company)
            .jobName("jobName")
            .jobStatus("FAILED")
            .instanceId(UUID.fromString("8859f254-84e0-4f7f-8898-188476edd145"))
            .instanceName("Instance Name")
            .jobId(UUID.fromString("8c4b4a1d-cf5f-4043-b469-6117eab301df"))
            .jobRunId(UUID.fromString("75250155-fe80-4733-b82b-617b469f94a1"))
            .stageId(UUID.fromString("2062411b-9932-4587-9b61-7c4f1e221d5e"))
            .logLocation("logLocation")
            .build();
        
        Assertions.assertThat(message).isEqualTo(ref);
    }
}