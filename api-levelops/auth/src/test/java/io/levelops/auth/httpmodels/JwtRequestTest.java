package io.levelops.auth.httpmodels;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtRequestTest {

    @Test
    public void testDeserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("json/httpmodels/jwt_request.json");
        JwtRequest output = DefaultObjectMapper.get().readValue(input, JwtRequest.class);

        assertThat(output.getUsername()).isEqualTo("maxime");
        assertThat(output.getPassword()).isEqualTo("hunter2");
    }
}