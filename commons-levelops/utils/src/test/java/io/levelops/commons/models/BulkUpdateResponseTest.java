package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkUpdateResponseTest {

    @Test
    public void serial() throws JsonProcessingException {
        BulkUpdateResponse updateResponse = BulkUpdateResponse.createBulkUpdateResponse(Stream.of("1", "2", "3"), true, null);
        String serializedResponse = DefaultObjectMapper.get().writeValueAsString(updateResponse);
        BulkUpdateResponse deSerializedResponse = DefaultObjectMapper.get().readValue(serializedResponse, BulkUpdateResponse.class);
        assertThat(deSerializedResponse).isEqualTo(updateResponse);
    }
}
