package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteResponseTest {
    @Test
    public void serial() throws JsonProcessingException {
        DeleteResponse updateResponse = DeleteResponse.builder().id("1").success(true).build();
        String serializedResponse = DefaultObjectMapper.get().writeValueAsString(updateResponse);
        DeleteResponse deSerializedResponse = DefaultObjectMapper.get().readValue(serializedResponse, DeleteResponse.class);
        assertThat(deSerializedResponse).isEqualTo(updateResponse);
    }
}
