package io.levelops.commons.databases.models.database;

import io.levelops.commons.databases.models.database.MessageTemplate.TemplateType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class MessageTemplateTest {
    @Test
    public void serializationTest() throws IOException {
        var template = ResourceUtils.getResourceAsString("samples/database/message_template.json");
        Assertions.assertThat(template).isNotNull();
        Assertions.assertThat(template.replaceAll("\\s", "")).isEqualTo(DefaultObjectMapper.get().writeValueAsString(MessageTemplate.builder().eventType(EventType.ALL).createdAt(1579221540l).name("email_template").botName("").id("2").message("Please_fill_out").emailSubject("my_test").type(TemplateType.EMAIL).build()));
    }

    @Test
    public void deserializationTest() throws IOException {
        var template = ResourceUtils.getResourceAsObject("samples/database/message_template.json", MessageTemplate.class);
        Assertions.assertThat(template).isNotNull();
        Assertions.assertThat(template.getEventType()).isEqualTo(EventType.ALL);
    }
}