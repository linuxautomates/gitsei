package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AiReport.AiReportBuilder.class)
public class AiReport {

    @JsonProperty("id")
    UUID id;

    @JsonProperty("type")
    String type;

    /**
     * Unique identifier **within a given report type**.
     * (Its meaning depends on the type)
     */
    @JsonProperty("key")
    String key;

    @JsonProperty("data")
    Map<String, Object> data;

    @JsonProperty("error")
    Map<String, Object> error;

    @JsonProperty("data_updated_at")
    Instant dataUpdatedAt;

    @JsonProperty("error_updated_at")
    Instant errorUpdatedAt;

    @JsonIgnore
    public TypeKeyIdentifier getTypeKeyIdentifier() {
        return new TypeKeyIdentifier(type, key);
    }

    @Value
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AiReport.AiReportBuilder.class)
    public static class TypeKeyIdentifier {
        @JsonProperty("type")
        String type;
        @JsonProperty("key")
        String key;
    }

}
