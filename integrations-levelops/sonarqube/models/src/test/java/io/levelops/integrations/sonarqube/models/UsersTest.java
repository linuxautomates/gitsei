package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class UsersTest {

    private static final String RESPONSE_FILE_NAME = "users-response.json";

    @Test
    public void deSerialize() throws IOException {
        UserResponse response = DefaultObjectMapper.get()
                .readValue(UsersTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        UserResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getUsers()).isNotNull();
    }
}