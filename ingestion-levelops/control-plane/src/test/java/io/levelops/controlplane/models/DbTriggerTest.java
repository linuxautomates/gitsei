package io.levelops.controlplane.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class DbTriggerTest {

    @Test
    public void testDeserialize() throws IOException {
        String input = "{\"iteration_ts\":1573240077, \"created_at\": 1573162249}";
        DbTrigger o = DefaultObjectMapper.get().readValue(input, DbTrigger.class);
        System.out.println(o);
        assertThat(o.getIterationTs()).isEqualTo(1573240077L);
    }
}