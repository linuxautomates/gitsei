package io.levelops.integrations.okta.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OktaGroupTest {

    private static final String RESPONSE_FILE_NAME = "groups.json";
    private static final int EXPECTED_NUM_GROUPS = 6;

    @Test
    public void deSerialize() throws IOException {
        List<OktaGroup> response = DefaultObjectMapper.get()
                .readValue(OktaGroupTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, OktaGroup.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_GROUPS);
    }
}
