package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketFieldTest {

    @Test
    public void name() throws JsonProcessingException {
        TicketField ticketField = DefaultObjectMapper.get().readValue("{ \"id\" : 123 , \"key\" : \"abc\" }", TicketField.class);
        assertThat(ticketField.getId()).isEqualTo(123L);
        assertThat(ticketField.getField().getKey()).isEqualTo("abc");
    }
}