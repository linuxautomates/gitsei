package io.levelops.commons.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class TimestampDeserializerTest {

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
    public void testDeserialize() throws IOException {
        TimestampHolder timestampHolder = DefaultObjectMapper.get().readValue("{ \"timestamp\": 1573168088 }", TimestampHolder.class);
        assertThat(timestampHolder.timestamp).isEqualTo(new Timestamp(1573168088000L));
    }


    @Test
    public void testTimestampDeserialize() throws IOException {
        TimestampHolder timestampHolder = DefaultObjectMapper.get().readValue("{ \"timestamp\": \"2021-08-13T13:43:20.069+00:00\" }", TimestampHolder.class);
        assertThat(timestampHolder.timestamp).isEqualTo(Timestamp.from(Instant.parse("2021-08-13T13:43:20.069+00:00")));
    }

    @Test
    public void testTimestampZoneDeserialize() throws IOException {
        TimestampHolder timestampHolder = DefaultObjectMapper.get().readValue("{ \"timestamp\": \"2022-04-12T04:47:30.952Z\" }", TimestampHolder.class);
        assertThat(timestampHolder.timestamp).isEqualTo(Timestamp.from(Instant.parse("2022-04-12T04:47:30.952Z")));
    }

    @Test
    public void testDeserializeNegative() throws IOException {
        TimestampHolder timestampHolder = DefaultObjectMapper.get().readValue("{ \"timestamp\": -1573168088 }", TimestampHolder.class);
        assertThat(timestampHolder.timestamp).isEqualTo(new Timestamp(-1573168088000L));
    }
}
