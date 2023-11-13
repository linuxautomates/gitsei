package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsTestTest {

    private static final String RESPONSE_FILE_NAME = "tests.json";

    @Test
    public void deSerialize() throws IOException {
        io.levelops.integrations.testrails.models.Test response = DefaultObjectMapper.get()
                .readValue(io.levelops.integrations.testrails.models.Test.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        io.levelops.integrations.testrails.models.Test.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getDynamicCustomFields()).isNotNull();
        assertThat(response.getDynamicCustomFields().size()).isEqualTo(12);
    }
}
