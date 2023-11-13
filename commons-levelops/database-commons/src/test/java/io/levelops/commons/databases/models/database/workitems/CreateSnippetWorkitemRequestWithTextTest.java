package io.levelops.commons.databases.models.database.workitems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CreateSnippetWorkitemRequestWithTextTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        CreateSnippetWorkitemRequestWithText expected = CreateSnippetWorkitemRequestWithText.builder()
                .url("https://jenkins.dev.levelops.io/view/Pipelines/job/maven-test-1/42/console")
                .title("See logs snippets from jenkins.dev.levelops.io")
                .requestor("viraj@levelops.io")
                .recipients(List.of("slackbot-test-private-va-4-pub", "viraj@levelops.io"))
                .mode(NotificationMode.SLACK)
                .message("Could you please look into this error?")
                .snippet("line 1 \n line 2 \n line 3")
                .build();

        String serialized = MAPPER.writeValueAsString(expected);
        CreateSnippetWorkitemRequestWithText actual = MAPPER.readValue(serialized, CreateSnippetWorkitemRequestWithText.class);
        Assert.assertEquals(expected, actual);
    }
}