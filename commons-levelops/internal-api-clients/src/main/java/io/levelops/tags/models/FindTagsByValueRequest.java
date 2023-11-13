package io.levelops.tags.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FindTagsByValueRequest.FindTagsByValueRequestBuilder.class)
public class FindTagsByValueRequest {
    @JsonProperty("values")
    List<String> values;
    @JsonProperty("create_if_missing")
    Boolean createIfMissing;
}