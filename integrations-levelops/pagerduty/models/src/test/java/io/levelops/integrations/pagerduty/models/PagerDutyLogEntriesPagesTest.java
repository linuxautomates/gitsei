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

public class PagerDutyLogEntriesPagesTest {

    @Test
    public void serializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/log_entries.json");
        PagerDutyLogEntriesPage
     object = mapper.readValue(json, PagerDutyLogEntriesPage
    .class);
        Assert.assertEquals(77, object.getLimit());
        Assert.assertEquals(8, object.getOffset());
        Assert.assertEquals(17, object.getTotal());
        Assert.assertFalse(object.isMore());
        Assert.assertEquals(object.getTotal(), object.getItems().size());
    }

}