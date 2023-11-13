package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KvFieldTest {

    @Test
    public void serial() throws JsonProcessingException {

        KvField kvField = DefaultObjectMapper.get().readValue("{\"key\":\"abc\"}", KvField.class);
        DefaultObjectMapper.prettyPrint(kvField);
        assertThat(kvField.getKey()).isEqualTo("abc");
        assertThat(kvField.getDisplayName()).isEqualTo("abc");

        kvField = DefaultObjectMapper.get().readValue("{\"key\":\"abc\",\"display_name\":\"XYZ\"}", KvField.class);
        DefaultObjectMapper.prettyPrint(kvField);
        assertThat(kvField.getKey()).isEqualTo("abc");
        assertThat(kvField.getDisplayName()).isEqualTo("XYZ");

    }
}