package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.commons.xml.DefaultXmlMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastResultTest {

    @Test
    public void testConvertToJson() throws IOException {
        String input = ResourceUtils.getResourceAsString("input.xml");
        CxXmlResults cxXmlResults = DefaultXmlMapper.getXmlMapper().readValue(input, CxXmlResults.class);
        String output = DefaultObjectMapper.get().copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .setSerializationInclusion(Include.NON_NULL)
                .setSerializationInclusion(Include.NON_ABSENT)
                .writeValueAsString(cxXmlResults);
        assertThat(output).isNotNull();
    }
}
