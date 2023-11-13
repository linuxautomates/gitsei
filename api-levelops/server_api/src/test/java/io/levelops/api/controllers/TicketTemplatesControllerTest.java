package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TicketTemplatesControllerTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialization() throws IOException {
        String data = ResourceUtils.getResourceAsString("ticket_templates/ticket_template_create.json");
        TicketTemplate tt = MAPPER.readValue(data, TicketTemplate.class);
        Assert.assertNotNull(tt);
    }
}