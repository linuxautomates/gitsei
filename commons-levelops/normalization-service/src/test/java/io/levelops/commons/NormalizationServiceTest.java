package io.levelops.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.normalization.services.NormalizationService;
import io.levelops.normalization.Normalizer;
import io.levelops.normalization.exceptions.NormalizationException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NormalizationServiceTest {

    @Normalizer(contentType = "__NormalizationServiceTest__test__")
    public static Long method(ObjectMapper objectMapper, String o) {
        System.out.println("called");
        return Long.valueOf(o);
    }

    @Test
    public void normalize() throws NormalizationException {
        NormalizationService normalizationService = new NormalizationService(DefaultObjectMapper.get());

        String inputString = "123";
        JsonNode input = DefaultObjectMapper.get().convertValue(inputString, JsonNode.class);
        Object output = normalizationService.normalize(ContentType.fromString("__NormalizationServiceTest__test__"), input);

        System.out.println(output);
        assertThat(output).isInstanceOf(Long.class);
        assertThat(output).isEqualTo(123L);
    }
}