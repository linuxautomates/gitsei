package io.levelops.aggregations_shared.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Log4j2
public class JobInstanceIdTest {
    @Test
    public void serializeTest() throws JsonProcessingException {
        var uuid = UUID.randomUUID();
        var j = JobInstanceId.builder()
                .jobDefinitionId(uuid)
                .instanceId(1)
                .build();
        var o = DefaultObjectMapper.get();
        String expected = String.format("{\"definition_id\":\"%s\",\"instance_id\":1}", uuid);
        assertThat(expected).isEqualTo(o.writeValueAsString(j));
    }

    @Test
    public void deserializeTest() throws JsonProcessingException {
        var uuid = UUID.randomUUID();
        String serialized = String.format("{\"definition_id\":\"%s\",\"instance_id\":1}", uuid);
        var o = DefaultObjectMapper.get();
        var deserialized = o.readValue(serialized, JobInstanceId.class);
        assertThat(deserialized.getInstanceId()).isEqualTo(1);
        assertThat(deserialized.getJobDefinitionId()).isEqualTo(uuid);
        log.info("deserialized {}", deserialized);
    }
}