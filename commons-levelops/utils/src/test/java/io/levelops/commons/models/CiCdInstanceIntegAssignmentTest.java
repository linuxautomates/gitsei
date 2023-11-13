package io.levelops.commons.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdInstanceIntegAssignmentTest {

    @Test
    public void serial() throws JsonProcessingException {
        CICDInstanceIntegAssignment updateRequest = CICDInstanceIntegAssignment.builder()
                .integrationId("1")
                .addIds(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .removeIds(Set.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .build();
        String serializedRequest = DefaultObjectMapper.get().writeValueAsString(updateRequest);
        CICDInstanceIntegAssignment deSerializedRequest = DefaultObjectMapper.get().readValue(serializedRequest, CICDInstanceIntegAssignment.class);
        assertThat(deSerializedRequest).isEqualTo(updateRequest);
    }
}
