package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunbookRunsStatusCheckHandlerTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserializeRequest() throws JsonProcessingException {
        List<String> runIds = List.of("r1", "r2","r3");
        Map<String, Object> expected = Map.of("run_ids", runIds);
        String serialized = MAPPER.writeValueAsString(expected);

        RunbookRunsStatusCheckHandler.RunbookRunsStatusCheckRequest request = MAPPER.readValue(serialized, RunbookRunsStatusCheckHandler.RunbookRunsStatusCheckRequest.class);
        List<String> actualRunIds = request.getRunIds();
        Assert.assertEquals(runIds.stream().collect(Collectors.toSet()), actualRunIds.stream().collect(Collectors.toSet()));
    }
}