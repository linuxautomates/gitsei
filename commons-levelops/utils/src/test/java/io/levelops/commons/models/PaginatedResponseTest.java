package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PaginatedResponseTest {

    @Test
    public void testHasNext() {
        assertThat(PaginatedResponse.of(1, 3, List.of("a", "b", "c")).getMetadata().getHasNext()).isTrue();
        assertThat(PaginatedResponse.of(1, 10, List.of("a", "b", "c")).getMetadata().getHasNext()).isFalse();
    }

    @Test
    public void serial() throws JsonProcessingException {
        PaginatedResponse<String> r = PaginatedResponse.of(1, 3, List.of("a", "b", "c"));
        String json = DefaultObjectMapper.get().writeValueAsString(r);
        assertThat(json).isEqualTo("{\"records\":[\"a\",\"b\",\"c\"],\"count\":3,\"_metadata\":{\"page_size\":3,\"page\":1,\"next_page\":2,\"has_next\":true}}");
    }

    @Test
    public void deserial() throws IOException {

        String json = "{\"records\":[\"a\",\"b\",\"c\"],\"count\":3,\"_metadata\":{\"page_size\":3,\"page\":1,\"next_page\":2,\"has_next\":true}}";
        PaginatedResponse<String> r = DefaultObjectMapper.get().readValue(json, PaginatedResponse.class);
        assertThat(r.getResponse().getRecords()).containsExactly("a", "b", "c");
        assertThat(r.getMetadata().getPage()).isEqualTo(1);
        assertThat(r.getMetadata().getPageSize()).isEqualTo(3);
        assertThat(r.getMetadata().getHasNext()).isTrue();
    }

}