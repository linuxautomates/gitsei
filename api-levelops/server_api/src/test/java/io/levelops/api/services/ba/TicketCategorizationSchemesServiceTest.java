package io.levelops.api.services.ba;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.web.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TicketCategorizationSchemesServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void test() throws IOException {
        TicketCategorizationScheme s1 = MAPPER.readValue(ResourceUtils.getResourceAsString("ba/ba_profile_1.json"), TicketCategorizationScheme.class);
        try {
            TicketCategorizationSchemesService.validateProfile(s1);
        } catch (BadRequestException e) {
            Assert.fail("Not expected!");
        }
        TicketCategorizationScheme s2 = MAPPER.readValue(ResourceUtils.getResourceAsString("ba/ba_profile_2.json"), TicketCategorizationScheme.class);
        try {
            TicketCategorizationSchemesService.validateProfile(s2);
        } catch (BadRequestException e) {
            Assert.fail("Not expected!");
        }
        TicketCategorizationScheme s3 = MAPPER.readValue(ResourceUtils.getResourceAsString("ba/ba_profile_3.json"), TicketCategorizationScheme.class);
        try {
            TicketCategorizationSchemesService.validateProfile(s3);
        } catch (BadRequestException e) {
            Assert.fail("Not expected!");
        }
        TicketCategorizationScheme s4 = MAPPER.readValue(ResourceUtils.getResourceAsString("ba/ba_profile_4.json"), TicketCategorizationScheme.class);
        try {
            TicketCategorizationSchemesService.validateProfile(s4);
        } catch (BadRequestException e) {
            Assert.fail("Not expected!");
        }
        TicketCategorizationScheme s5 = MAPPER.readValue(ResourceUtils.getResourceAsString("ba/ba_profile_5.json"), TicketCategorizationScheme.class);
        try {
            TicketCategorizationSchemesService.validateProfile(s5);
            Assert.fail("Not expected!");
        } catch (BadRequestException e) {
            Assert.assertTrue(e.getMessage().startsWith("Duplicate categories found for indexes : "));
        }
    }
}