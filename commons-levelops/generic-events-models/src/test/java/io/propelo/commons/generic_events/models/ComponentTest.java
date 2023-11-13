package io.propelo.commons.generic_events.models;

import org.junit.Assert;
import org.junit.Test;

public class ComponentTest {
    @Test
    public void testSerialize() {
        for(Component c : Component.values()) {
            Assert.assertEquals(c, Component.fromString(c.toString()));
        }
    }
}