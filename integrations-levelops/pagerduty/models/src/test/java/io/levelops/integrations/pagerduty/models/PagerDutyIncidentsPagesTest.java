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

public class PagerDutyIncidentsPagesTest {

    @Test
    public void serializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/incidents.json");
        PagerDutyIncidentsPage object = mapper.readValue(json, PagerDutyIncidentsPage.class);
        Assert.assertEquals(25, object.getLimit());
        Assert.assertEquals(2, object.getOffset());
        Assert.assertEquals(4, object.getTotal());
        Assert.assertFalse(object.isMore());
        Assert.assertEquals(object.getTotal(), object.getItems().size());
    }

}