package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultObjectMapperTest {

    @Test
    public void serialInstantAndDate() {
        Instant instant = Instant.ofEpochMilli(1586407064844L);
        String a = DefaultObjectMapper.writeAsPrettyJson(instant);
        String b = DefaultObjectMapper.writeAsPrettyJson(Date.from(instant));
        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo("1586407064844");
    }

    @Test
    public void deserialInstantAndDate() throws JsonProcessingException {
        String input = "1586407064844";
        Instant a = DefaultObjectMapper.get().readValue(input, Instant.class);
        Date b = DefaultObjectMapper.get().readValue(input, Date.class);
        assertThat(a).isEqualTo(Instant.ofEpochMilli(1586407064844L));
        assertThat(a).isEqualTo(b.toInstant());
        assertThat(Date.from(a)).isEqualTo(b);
    }
}
