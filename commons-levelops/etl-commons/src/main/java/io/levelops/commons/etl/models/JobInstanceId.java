package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.etl.exceptions.InvalidJobInstanceIdException;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

// Convenience class that uniquely identifies a job instance. Serializes and
// deserializes to a string appropriately
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= JobInstanceId.JobInstanceIdBuilder.class)
public class JobInstanceId {
    @JsonProperty("definition_id")
    @NonNull
    UUID jobDefinitionId;

    @JsonProperty("instance_id")
    @NonNull
    Integer instanceId;

    @JsonIgnore
    public String toString() {
        return jobDefinitionId + "::" + instanceId;
    }

    public static JobInstanceId fromString(String str) throws InvalidJobInstanceIdException {
        var s = str.split("::");
        try {
            return JobInstanceId.builder()
                    .jobDefinitionId(UUID.fromString(s[0]))
                    .instanceId(Integer.valueOf(s[1]))
                    .build();
        } catch (Exception e) {
            throw new InvalidJobInstanceIdException(str);
        }
    }
}
