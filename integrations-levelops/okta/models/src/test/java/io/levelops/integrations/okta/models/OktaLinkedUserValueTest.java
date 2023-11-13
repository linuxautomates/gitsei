package io.levelops.integrations.okta.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaLinkedUserValueTest {
    private static final String RESPONSE_FILE_NAME = "linked_user_values.json";

    @Test
    public void deSerialize() throws IOException {
        List<OktaLinkedUserValues> response = DefaultObjectMapper.get()
                .readValue(OktaLinkedUserValueTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, OktaLinkedUserValues.class));
        assertThat(response).isNotNull();
        DefaultObjectMapper.prettyPrint(response);
    }

}
