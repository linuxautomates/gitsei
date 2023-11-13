package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class TenantSCMSettingsTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testDeserialize() throws JsonProcessingException {
        String data = "{ \"code_change_size_unit\": \"lines\", \"code_change_size_small\": 1000, \"code_change_size_medium\": 1000, \"comment_density_small\": 2000, \"comment_density_medium\": 5400 }";
        TenantSCMSettings expected = TenantSCMSettings.builder()
                .codeChangeSizeUnit("lines")
                .codeChangeSizeSmall(1000).codeChangeSizeMedium(1000).commentDensitySmall(2000).commentDensityMedium(5400)
                .build();
        TenantSCMSettings tenantSCMSettings = MAPPER.readValue(data, TenantSCMSettings.class);
        Assert.assertEquals(expected, tenantSCMSettings);
    }
}