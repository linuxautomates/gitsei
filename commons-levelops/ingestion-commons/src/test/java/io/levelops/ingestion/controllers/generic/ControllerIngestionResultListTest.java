package io.levelops.ingestion.controllers.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ControllerIngestionResultListTest {

    @Test
    public void testSerialize() throws JsonProcessingException {
        String output = DefaultObjectMapper.get().writeValueAsString(new ControllerIngestionResultList("test", List.of()));
        Map<String, Object> o = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(), "", output);
        assertThat(o.get("merge_strategy")).isEqualTo("test");
    }
}