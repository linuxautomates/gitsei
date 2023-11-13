package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGListAPIResponse.HarnessNGListAPIResponseBuilder.class)
public class HarnessNGListAPIResponse<T> {
    @JsonProperty("content")
    List<T> content;

    @JsonProperty("empty")
    Boolean isEmpty;
}
