package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

public class HarnessNGExecutionInputSetTest {

    @Test
    public void testDeserial() throws JsonProcessingException {
        String input = "{\"status\":\"SUCCESS\",\"data\":{\"inputSetTemplateYaml\":\"template-yaml\", \"inputSetYaml\":\"yaml\"}}";
        HarnessNGAPIResponse<HarnessNGExecutionInputSet> output = DefaultObjectMapper.get().readValue(input,
                DefaultObjectMapper.get().getTypeFactory().constructParametricType(HarnessNGAPIResponse.class, HarnessNGExecutionInputSet.class));
        assertThat(output).isNotNull();
        assertThat(output.getData()).isNotNull();
        assertThat(output.getData().getInputSetYaml()).isEqualTo("yaml");
        assertThat(output.getData().getInputSetTemplateYaml()).isEqualTo("template-yaml");
    }
}