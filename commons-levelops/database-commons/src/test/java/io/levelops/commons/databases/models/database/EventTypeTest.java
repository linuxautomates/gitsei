package io.levelops.commons.databases.models.database;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class EventTypeTest {

    @Test
    public void serializationTest() throws IOException {
        var eventType = EventType.builder()
            .component(Component.builder().id(UUID.randomUUID()).name("jira").type(ComponentType.INTEGRATION).subComponents(List.of()).build())
            .description("description")
            .type(EventType.JIRA_ISSUE_CREATED.toString())
            .data(Map.of("test", KvField.builder().key("test").type("text").build()))
            .build();
        var content = DefaultObjectMapper.get().writeValueAsString(eventType);
        assertThat(content).isNotNull();
    }

    @Test
    public void deserializationTest() throws IOException {
        var content = ResourceUtils.getResourceAsObject("samples/database/event_type.json", EventType.class);
        assertThat(content).isNotNull();
        assertThat(content.getComponent().getName()).isEqualTo("jira");
        assertThat(content.getComponent().getType()).isEqualTo(ComponentType.INTEGRATION);
    }

    @Test
    public void fromStringTest() throws IOException {
        assertThat(EventType.fromString("praetorian_report_created")).isEqualTo(EventType.PRAETORIAN_REPORT_CREATED);
    }

    @Test
    public void testGetIcon() {
        assertThat(EventType.getIcon(TriggerType.MANUAL, null)).isEqualTo("levelops");
        assertThat(EventType.getIcon(TriggerType.SCHEDULED, null)).isEqualTo("clock-circle");
        assertThat(EventType.getIcon(TriggerType.COMPONENT_EVENT, null)).isEqualTo("levelops");
        assertThat(EventType.getIcon(TriggerType.COMPONENT_EVENT, EventType.ASSESSMENT_CREATED)).isEqualTo("levelops");
        assertThat(EventType.getIcon(TriggerType.COMPONENT_EVENT, EventType.JENKINS_CONFIG_CREATED)).isEqualTo("jenkins");
        assertThat(EventType.getIcon(TriggerType.COMPONENT_EVENT, EventType.MS_TMT_REPORT_CREATED)).isEqualTo("microsoft");
    }
}