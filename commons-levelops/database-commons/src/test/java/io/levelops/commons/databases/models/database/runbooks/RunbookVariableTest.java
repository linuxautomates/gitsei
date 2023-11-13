package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RunbookVariableTest {

    @Test
    public void testSerial() throws JsonProcessingException {

        String input = "{ \"content_type\" : \"string\" }";
        RunbookVariable runbookVariable = DefaultObjectMapper.get().readValue(input, RunbookVariable.class);
        DefaultObjectMapper.prettyPrint(runbookVariable);
        assertThat(runbookVariable.getContentType().toString()).isEqualTo("string");
        String out = DefaultObjectMapper.get().writeValueAsString(runbookVariable);
        assertThat(out).isEqualTo("{\"content_type\":\"string\"}");

        input = "{ \"content_type\" : \"integration/jira/issues\" }";
        runbookVariable = DefaultObjectMapper.get().readValue(input, RunbookVariable.class);
        assertThat(runbookVariable.getContentType().toString()).isEqualTo("integration/jira/issues");
        out = DefaultObjectMapper.get().writeValueAsString(runbookVariable);
        assertThat(out).isEqualTo("{\"content_type\":\"integration/jira/issues\"}");

    }
}