package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsUserTest {

    private static final String RESPONSE_FILE_NAME = "users.json";

    @Test
    public void deSerialize() throws IOException {
        User response = DefaultObjectMapper.get()
                .readValue(User.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        User.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
