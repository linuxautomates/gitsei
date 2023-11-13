package io.levelops.events.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventsRESTClientTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerializationDeserializtion() throws JsonProcessingException {
        List<String> runIds = List.of("r1", "r2", "r3", "r4");
        Map<String, List<String>> result = Map.of("run_ids", runIds);
        String serialized = MAPPER.writeValueAsString(result);
        EventsRESTClient.ProcessTriggerEventsResponse processTriggerEventsResponse = MAPPER.readValue(serialized, EventsRESTClient.ProcessTriggerEventsResponse.class);
        Assert.assertEquals(runIds.stream().collect(Collectors.toSet()), processTriggerEventsResponse.getRunIds().stream().collect(Collectors.toSet()));

        EventsRESTClient.ProcessTriggerEventsResponse builtObject = EventsRESTClient.ProcessTriggerEventsResponse.builder()
                .runIds(runIds).build();
        serialized = MAPPER.writeValueAsString(builtObject);
        processTriggerEventsResponse = MAPPER.readValue(serialized, EventsRESTClient.ProcessTriggerEventsResponse.class);
        Assert.assertEquals(runIds.stream().collect(Collectors.toSet()), processTriggerEventsResponse.getRunIds().stream().collect(Collectors.toSet()));
    }
}