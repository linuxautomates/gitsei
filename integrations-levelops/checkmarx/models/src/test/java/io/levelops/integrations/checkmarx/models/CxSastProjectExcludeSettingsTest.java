package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastProjectExcludeSettingsTest {
    private static final String RESPONSE_FILE_NAME = "exclude-settings-response.json";

    @Test
    public void deSerialize() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        VCSSettings.ExcludeSettings response = mapper.readValue(CxSastProjectExcludeSettingsTest.class.getClassLoader()
                .getResourceAsStream(RESPONSE_FILE_NAME), VCSSettings.ExcludeSettings.class);
        assertThat(response).isNotNull();
    }
}
