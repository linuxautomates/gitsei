package io.levelops.integrations.awsdevtools.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CBProjectTest {

    private static final String RESPONSE_FILE_NAME = "projects.json";

    @Test
    public void deSerialize() throws IOException {
        CBProject response = DefaultObjectMapper.get()
                .readValue(CBProject.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        CBProject.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
