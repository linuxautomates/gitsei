package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JsonDiff.JsonDiffBuilder.class)
public class JsonDiff {
    @JsonProperty("base_path")
    String basePath;
    @JsonProperty("added")
    List<String> added;
    @JsonProperty("removed")
    List<String> removed;
    @JsonProperty("changed")
    List<String> changed;
    @JsonProperty("data_changes")
    Map<String, DataChange> dataChanges;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DataChange.DataChangeBuilder.class)
    public static class DataChange {
        @JsonProperty("operation")
        Operation operation;
        @JsonProperty("before")
        JsonNode before;
        @JsonProperty("after")
        JsonNode after;
    }

    public enum Operation {
        ADDED, REMOVED, CHANGED;
    }

}
