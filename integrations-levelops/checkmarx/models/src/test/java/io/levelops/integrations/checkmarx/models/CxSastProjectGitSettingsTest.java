package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CxSastProjectGitSettingsTest {
    private static final String RESPONSE_FILE_NAME = "git-settings-response.json";

    @Test
    public void deSerialize() throws IOException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        VCSSettings.GitSettings response = mapper.readValue(CxSastProjectGitSettingsTest.class.getClassLoader()
                .getResourceAsStream(RESPONSE_FILE_NAME), VCSSettings.GitSettings.class);
        assertThat(response).isNotNull();
    }
}
