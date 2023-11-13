package io.levelops.commons.databases.models.database.runbooks;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class RunbookRunningNodeTest {

    Instant i = Instant.parse("2020-04-09T01:37:51.173Z");

    @Test
    public void serial() {
        String output = DefaultObjectMapper.writeAsPrettyJson(RunbookRunningNode.builder()
                .createdAt(i)
                .stateChangedAt(i)
                    .build());
        System.out.println(output);
        assertThat(output).contains("\"state_changed_at\" : 1586396271173");
        assertThat(output).contains("\"created_at\" : 1586396271173");
    }

    @Test
    public void deserial() throws IOException {
        RunbookRunningNode runbookRunningNode = DefaultObjectMapper.get().readValue("{\n" +
                "  \"state_changed_at\" : 1586396271173,\n" +
                "  \"created_at\" : 1586396271173\n" +
                "}", RunbookRunningNode.class);
        assertThat(runbookRunningNode.getCreatedAt()).isEqualTo(i);
        assertThat(runbookRunningNode.getStateChangedAt()).isEqualTo(i);
    }
}