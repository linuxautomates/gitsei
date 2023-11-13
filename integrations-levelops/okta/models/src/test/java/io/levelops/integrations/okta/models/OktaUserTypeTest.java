package io.levelops.integrations.okta.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaUserTypeTest {

    private static final String RESPONSE_FILE_NAME = "user_type.json";

    @Test
    public void deSerialize() throws IOException {
        OktaUserType response = DefaultObjectMapper.get()
                .readValue(OktaUserTypeTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME), OktaUserType.class);
        assertThat(response).isNotNull();
        System.out.println(response);
    }
}
