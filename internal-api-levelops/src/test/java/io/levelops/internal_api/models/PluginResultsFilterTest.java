package io.levelops.internal_api.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginResultsFilterTest {

    @Test
    public void serial() throws JsonProcessingException {
        PluginResultsFilter filter = PluginResultsFilter.builder()
                .createdAt(PluginResultsFilter.CreatedAtFilter.builder()
                        .from(Date.from(Instant.parse("2020-02-24T01:02:03Z")))
                        .to(Date.from(Instant.parse("2020-02-24T01:02:03Z")))
                        .build())
                .build();
        String s = DefaultObjectMapper.get().writeValueAsString(filter);
        System.out.println(s);
        assertThat(s).isEqualTo("{\"created_at\":{\"from\":1582506123000,\"to\":1582506123000}}");

        String input = "{\"created_at\":{\"from\":\"2020-02-24T01:02:03Z\"}}";
        PluginResultsFilter out = DefaultObjectMapper.get().readValue(input, PluginResultsFilter.class);
        System.out.println(out);
        assertThat(out.getCreatedAt().getFrom()).isEqualTo("2020-02-24T01:02:03Z");
    }

    @Test
    public void serial2() throws JsonProcessingException {
        String input = "{}";
        PluginResultsFilter out = DefaultObjectMapper.get().readValue(input, PluginResultsFilter.class);
        assertThat(out.getCreatedAt()).isNotNull();
    }
}