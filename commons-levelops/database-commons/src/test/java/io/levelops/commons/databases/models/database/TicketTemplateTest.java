package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TicketTemplateTest {
    @Test
    public void serializeTest() throws JsonProcessingException {
        var string = DefaultObjectMapper.get().writeValueAsString(TicketTemplate.builder().notifyBy(Map.of(EventType.ALL, List.of("EMAIL"))).build());
        Assertions.assertThat(string).isNotBlank();
    }

    @Test
    public void test() throws IOException {
        var r = ResourceUtils.getResourceAsObject("samples/database/ticket_template.json", TicketTemplate.class);
        Assertions.assertThat(r).isNotNull();
        Assertions.assertThat(r.getNotifyBy()).containsExactlyInAnyOrderEntriesOf(Map.of(EventType.ALL, List.of("EMAIL", "SLACK")));
        Assertions.assertThat(r.getMessageTemplateIds()).containsExactlyInAnyOrder("1", "2");
    }
}