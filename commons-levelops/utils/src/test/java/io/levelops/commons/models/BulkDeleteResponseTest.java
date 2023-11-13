package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkDeleteResponseTest {
    @Test
    public void serial() throws JsonProcessingException {
        BulkDeleteResponse updateResponse = BulkDeleteResponse.createBulkDeleteResponse(List.of("1", "2", "3"), true, null);
        String serializedResponse = DefaultObjectMapper.get().writeValueAsString(updateResponse);
        BulkDeleteResponse deSerializedResponse = DefaultObjectMapper.get().readValue(serializedResponse, BulkDeleteResponse.class);
        assertThat(deSerializedResponse).isEqualTo(updateResponse);
    }
}
