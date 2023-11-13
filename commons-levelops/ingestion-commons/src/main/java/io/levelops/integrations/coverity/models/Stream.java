package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Stream.StreamBuilder.class)
public class Stream {

    @JsonProperty("id")
    Map<String, String> id;

    @JsonProperty("language")
    String language;

    @JsonProperty("primary_project_id")
    Map<String, String> primaryProjectId;

    @JsonProperty("triage_store_id")
    Map<String, String> triageStoreId;
}
