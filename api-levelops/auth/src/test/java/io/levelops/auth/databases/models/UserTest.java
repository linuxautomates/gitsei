package io.levelops.auth.databases.models;

import io.levelops.commons.databases.models.database.User;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTest {

    @Test
    public void testDeserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("json/databases/user.json");
        User output = DefaultObjectMapper.get().readValue(input, User.class);

        assertThat(output.getEmail()).isEqualTo("maxime@levelops.io");
        assertThat(output.getBcryptPassword()).isEqualTo("hunter2");
    }
}