package io.levelops.commons.models;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ComponentTypeTest {
    @Test
    public void test(){
        Assertions.assertThat(ComponentType.fromString("tickets")).isEqualTo(ComponentType.SMART_TICKET);
        Assertions.assertThat(ComponentType.fromString("Tickets")).isEqualTo(ComponentType.SMART_TICKET);
        Assertions.assertThat(ComponentType.fromString("smart_Ticket")).isEqualTo(ComponentType.SMART_TICKET);
        Assertions.assertThat(ComponentType.fromString("assessment")).isEqualTo(ComponentType.ASSESSMENT);
        Assertions.assertThat(ComponentType.fromString("Assessment")).isEqualTo(ComponentType.ASSESSMENT);
        Assertions.assertThat(ComponentType.fromString("quiz")).isEqualTo(ComponentType.ASSESSMENT);
        Assertions.assertThat(ComponentType.fromString("plugins")).isEqualTo(ComponentType.PLUGIN_RESULT);
        Assertions.assertThat(ComponentType.fromString("plugin_result")).isEqualTo(ComponentType.PLUGIN_RESULT);
        Assertions.assertThat(ComponentType.fromString("PLUGIN_RESULT")).isEqualTo(ComponentType.PLUGIN_RESULT);
        Assertions.assertThat(ComponentType.fromString("integration")).isEqualTo(ComponentType.INTEGRATION);
        Assertions.assertThat(ComponentType.fromString("Integration")).isEqualTo(ComponentType.INTEGRATION);
        Assertions.assertThat(ComponentType.fromString("")).isEqualTo(ComponentType.NONE);
    }
}