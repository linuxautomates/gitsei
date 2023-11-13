package io.levelops.commons.databases.models.database;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TriggerTypeTest {
    @Test
    public void test(){
        Assertions.assertThat(TriggerType.fromString("type")).as("").isEqualTo(TriggerType.UNKNOWN);
        Assertions.assertThat(TriggerType.fromString("scheduled")).as("").isEqualTo(TriggerType.SCHEDULED);
        Assertions.assertThat(TriggerType.fromString("manual")).as("").isEqualTo(TriggerType.MANUAL);
        Assertions.assertThat(TriggerType.fromString("component_event")).as("").isEqualTo(TriggerType.COMPONENT_EVENT);
        Assertions.assertThat(TriggerType.fromString("SCHEDULED")).as("").isEqualTo(TriggerType.SCHEDULED);
        Assertions.assertThat(TriggerType.fromString("MANUAL")).as("").isEqualTo(TriggerType.MANUAL);
        Assertions.assertThat(TriggerType.fromString("COMPONENT_EVENT")).as("").isEqualTo(TriggerType.COMPONENT_EVENT);
    }
}