package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class PagerDutyUserTest {
    @Test
    public void deserializationTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy()
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/user.json");
        PagerDutyUser object = mapper.readValue(json, PagerDutyUser.class);
        Assert.assertEquals("ivan.leon.it@gmail.com", object.getEmail());
        Assert.assertEquals("Jose Leon", object.getName());
        Assert.assertEquals("P5OQRZ5", object.getId());
        Assert.assertEquals("America/Los_Angeles", object.getTimeZone());
        Assert.assertEquals("user", object.getType());
        Assert.assertEquals("owner", object.getRole());
    }

    @Test
    public void serializeTest() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .setSerializationInclusion(Include.NON_ABSENT);
        var d = mapper.writeValueAsString(
            PagerDutyUser.builder()
                .id("P2LYZHY")
                .name("Test User")
                .email("test.user@test.com")
                .role("owner")
                .timeZone("America/Los_Angeles")
                .type("user")
                .build());
        Assert.assertNotNull(d);
        Assert.assertEquals(ResourceUtils.getResourceAsString("integrations/pagerduty/user_agg_row.json"), d);
    }
}