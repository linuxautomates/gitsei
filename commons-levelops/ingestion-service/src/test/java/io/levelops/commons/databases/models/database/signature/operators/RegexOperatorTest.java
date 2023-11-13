package io.levelops.commons.databases.models.database.signature.operators;

import io.levelops.commons.databases.models.database.signature.SignatureOperator;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexOperatorTest {

    @Test
    public void deserial() throws IOException {
        String in = "{\"id\":\"var1\", \"field\":\"custom123\", \"type\":\"regex\"}";
        RegexOperator regexOperator = DefaultObjectMapper.get().readValue(in, RegexOperator.class);
        assertThat(regexOperator.getField()).isEqualTo("custom123");
        assertThat(regexOperator.getId()).isEqualTo("var1");
        assertThat(regexOperator.getType()).isEqualTo("regex");
    }

    @Test
    public void builder() {
        RegexOperator op = RegexOperator.builder()
                .id("var1")
                .field("abc")
                .build();
        assertThat(op.getType()).isEqualTo("regex");
    }

    @Test
    public void deserialList() throws IOException {
        String in = "{\"id\":\"var1\", \"field\":\"custom123\", \"type\":\"regex\"}";
        String list = "[" + in + "," + in + "]";
        List<? extends SignatureOperator> o = DefaultObjectMapper.get().readValue(list,
                DefaultObjectMapper.get().getTypeFactory().constructCollectionType(
                        List.class, SignatureOperator.class));
        DefaultObjectMapper.prettyPrint(o);
    }
}