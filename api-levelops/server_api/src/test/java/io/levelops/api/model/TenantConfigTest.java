package io.levelops.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import java.io.IOException;
import java.util.Map;

public class TenantConfigTest {
    ObjectMapper mapper = DefaultObjectMapper.get().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    
    @Test
    public void test() throws IOException{
        var config1 = TenantConfigDTO.builder()
            .id("1")
            .name("name1")
            .value("test")
            .build();
        var val1 = mapper.writeValueAsString(config1);
        Assertions.assertThat(val1).isEqualTo(ResourceUtils.getResourceAsString("model/tenantConfigDTO1.json"));

        var config2 = TenantConfigDTO.builder()
            .id("2")
            .name("name2")
            .value(Map.of("size", 0, "text", "ok"))
            .build();
        var val2 = mapper.writeValueAsString(config2);
        Assertions.assertThat(val2).isEqualTo(ResourceUtils.getResourceAsString("model/tenantConfigDTO2.json"));
    }
    
}
