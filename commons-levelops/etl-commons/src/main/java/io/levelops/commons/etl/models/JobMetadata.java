package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores generic data that can be accessed and modified by the job processors through the JobContext.
 */
@Value
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Builder(toBuilder = true)
@JsonDeserialize
public class JobMetadata {

    /**
     * To store generic checkpointing data.
     */
    @JsonProperty("checkpoint")
    Map<String, Object> checkpoint;

    /**
     * For future proofing. Any schema can be read using {@code objectMapper.convertValue(jobMetadata, TargetSchema.class)}.
     */
    @Builder.Default
    Map<String, Object> dynamicFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    @JsonAnySetter
    public void addDynamicField(String key, Object value) {
        if (value == null) {
            return;
        }
        dynamicFields.put(key, value);
    }

}
