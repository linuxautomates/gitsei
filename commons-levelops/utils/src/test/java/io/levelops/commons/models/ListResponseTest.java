package io.levelops.commons.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ListResponseTest {

    private final ObjectMapper m = DefaultObjectMapper.get();

    @Test
    public void testDeserialize() throws IOException {
        ListResponse<String> output = m.readValue("{ \"count\":-999, \"records\": [ \"a\", \"b\" ]}",
                m.getTypeFactory().constructParametricType(ListResponse.class, String.class));

        assertThat(output.getCount()).isEqualTo(2);
        assertThat(output.getRecords()).containsExactly("a", "b");
    }

}