package io.levelops.commons.databases.models.database;

import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class TriggerSchemaTest {

    @Test
    public void test() throws IOException {
        var content = ResourceUtils.getResourceAsObject("samples/database/trigger_schema.json", TriggerSchema.class);
        Assertions.assertThat(content).isNotNull();
    }
}