package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class PagerDutyLogEntryTest {
    @Test
    public void deserializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/log_entry.json");
        PagerDutyLogEntry object = mapper.readValue(json, PagerDutyLogEntry.class);
        Assert.assertEquals(1580261734L, object.getCreatedAt().toInstant().getEpochSecond());
        Assert.assertNotNull(object.getIncidentDate());
        Assert.assertEquals(1580261734L, object.getIncidentDate().longValue());
        Assert.assertEquals("RNM942EACWPQSLCPWNWCGTI1QO", object.getId());
        Assert.assertNotNull(object.getIncident().getAcknowledgements());
        Assert.assertEquals(1, object.getIncident().getAcknowledgements().size());
        System.out.println("Acks: " + object.getIncident().getAcknowledgements());
        // Assert.assertEquals("", object.getUrgency());
        // Assert.assertEquals("", object.getPriority());
    }
}