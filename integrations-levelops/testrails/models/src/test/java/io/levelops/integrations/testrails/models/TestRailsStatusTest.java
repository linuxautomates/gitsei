package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsStatusTest {

    private static final String RESPONSE_FILE_NAME = "statuses.json";

    @Test
    public void deSerialize() throws IOException {
        io.levelops.integrations.testrails.models.Test.Status response = DefaultObjectMapper.get()
                .readValue(io.levelops.integrations.testrails.models.Test.Status.class.getClassLoader()
                                .getResourceAsStream(RESPONSE_FILE_NAME),
                        io.levelops.integrations.testrails.models.Test.Status.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
