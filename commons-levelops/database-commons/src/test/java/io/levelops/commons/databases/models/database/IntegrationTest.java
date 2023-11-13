package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class IntegrationTest {

    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        Integration integration = new Integration.IntegrationBuilder()
                .application(IntegrationType.PAGERDUTY.toString())
                .id("integ1")
                .name("PagerPuppy")
                .description("The puppy of pagers")
                .status("active")
                .url("https://api.pagerduty.com")
                .createdAt(0L)
                .updatedAt(1L)
                .build();
        String value = mapper.writeValueAsString(integration);
        Assertions.assertThat(value).isEqualTo(ResourceUtils.getResourceAsString("samples/database/integration.json"));
    }

    public void deserializeTest() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = DefaultObjectMapper.get().copy();
        Integration integration = mapper.readValue(ResourceUtils.getResourceAsString("samples/database/integration.json"), Integration.class);
        Integration expected = new Integration.IntegrationBuilder()
                .application(IntegrationType.PAGERDUTY.toString())
                .id("integ1")
                .name("PagerPuppy")
                .description("The puppy of pagers")
                .status("active")
                .url("https://api.pagerduty.com")
                .createdAt(0L)
                .updatedAt(1L)
                .build();
        Assertions.assertThat(integration).isEqualTo(expected);
    }
}