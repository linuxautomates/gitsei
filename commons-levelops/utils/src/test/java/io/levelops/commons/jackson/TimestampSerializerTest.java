package io.levelops.commons.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import java.io.IOException;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

public class TimestampSerializerTest {

    static class TimestampHolder {
        @JsonProperty("timestamp")
        Timestamp timestamp;
    }

    @Test
    public void testDeserializeNull() throws IOException {
        TimestampHolder timestampHolder = DefaultObjectMapper.get().readValue("{}", TimestampHolder.class);
        assertThat(timestampHolder.timestamp).isNull();
    }

    @Test(expected = IOException.class)
    public void testDeserializeInvalid() throws IOException {
        DefaultObjectMapper.get().readValue("{ \"timestamp\": \"abc\" }", TimestampHolder.class);
    }

    @Test
    public void testSerialize() throws IOException {
        TimestampHolder timestampHolder1 = new TimestampHolder();
        timestampHolder1.timestamp = new Timestamp(1573168088000L);
        String out = DefaultObjectMapper.get().writeValueAsString(timestampHolder1);
        assertThat(out).isEqualTo("{\"timestamp\":1573168088}");
    }

    @Test
    public void testSerializeNull() throws IOException {
        TimestampHolder timestampHolder1 = new TimestampHolder();
        timestampHolder1.timestamp = null;
        String out = DefaultObjectMapper.get().writeValueAsString(timestampHolder1);
        assertThat(out).isEqualTo("{}");
    }
}
