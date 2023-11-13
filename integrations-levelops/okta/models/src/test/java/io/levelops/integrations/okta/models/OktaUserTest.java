package io.levelops.integrations.okta.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaUserTest {

    private static final String RESPONSE_FILE_NAME = "users.json";
    private static final int EXPECTED_NUM_USERS = 3;

    @Test
    public void deSerialize() throws IOException {
        List<OktaUser> response = DefaultObjectMapper.get()
                .readValue(OktaUserTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, OktaUser.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_USERS);
    }
}
