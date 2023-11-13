package io.levelops.integrations.awsdevtools.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CBBuildTest {

    private static final String RESPONSE_FILE_NAME = "builds.json";

    @Test
    public void deSerialize() throws IOException {
        CBBuild response = DefaultObjectMapper.get()
                .readValue(CBBuild.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        CBBuild.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
