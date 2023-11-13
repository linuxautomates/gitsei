package io.levelops.commons.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultXmlMapperTest {

    @Test
    public void name() throws IOException {
        String input = ResourceUtils.getResourceAsString("xml/input.xml");
        Pojo o = DefaultXmlMapper.getXmlMapper().readValue(input, Pojo.class);
        String output = DefaultObjectMapper.get().writeValueAsString(o);
        System.out.println(output);
        assertThat(output).isEqualTo(ResourceUtils.getResourceAsString("xml/output.json"));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Pojo {
        @JsonProperty("Attr1") // input / output
        String attribute1;

        @JsonProperty("attribute2") // JSON output
        @JacksonXmlProperty(localName = "Attr2") // XML input
        String attribute2;

        @JsonProperty("children") // json output list
        @JacksonXmlProperty(localName = "Child") // xml input item
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Child> children;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Child {
        @JsonProperty("Value")
        String value;
    }


}