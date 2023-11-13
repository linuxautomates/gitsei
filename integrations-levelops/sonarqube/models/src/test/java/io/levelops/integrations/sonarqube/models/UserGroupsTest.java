package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class UserGroupsTest {

    private static final String RESPONSE_FILE_NAME = "usergroup-response.json";

    @Test
    public void deSerialize() throws IOException {
        UserGroupResponse response = DefaultObjectMapper.get()
                .readValue(UserGroupsTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        UserGroupResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getGroups()).isNotNull();
    }
}