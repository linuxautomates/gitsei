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

public class PagerDutyUsersPageTest {

    @Test
    public void test() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        URL json = getClass().getClassLoader().getResource("integrations/pagerduty/users.json");
        PagerDutyUsersPage object = mapper.readValue(json, PagerDutyUsersPage.class);
        Assert.assertEquals(25, object.getLimit());
        Assert.assertEquals(0, object.getOffset());
        Assert.assertEquals(3, object.getTotal());
        Assert.assertFalse(object.isMore());
        Assert.assertEquals(object.getTotal(), object.getOffset() + object.getItems().size());
    }
}