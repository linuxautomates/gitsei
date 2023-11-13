package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

public class PagerDutyIncidentTest {
    @Test
    public void deserializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/incident.json");
        PagerDutyIncident object = mapper.readValue(json, PagerDutyIncident.class);
        Assert.assertEquals(1581121867L, object.getCreatedAt().toInstant().getEpochSecond());
        Assert.assertEquals(1581121867L, object.getLastStatusChangeAt().toInstant().getEpochSecond());
        Assert.assertEquals(1581121867L, object.getUpdatedAt().longValue());
        Assert.assertEquals("PR20TFI", object.getId());
        Assert.assertEquals(PagerDutyIncident.Urgency.high, object.getUrgency());
        Assert.assertNotNull(object.getPriority());
        Assert.assertEquals("P0", object.getPriority().getSummary());
        Assertions.assertThat(object.getAcknowledgements()).containsExactlyInAnyOrder(PagerDutyIncident.Acknowledgement.builder()
            .at("2021-02-11T15:47:35Z")
            .acknowledger(PagerDutyIncident.Acknowledger.builder()
                .id("P5OQRZ5")
                .name("Jose Leon")
                .type("user_reference")
                .build())
            .build());
    }
}