package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGAPIResponse.HarnessNGAPIResponseBuilder.class)
public class HarnessNGAPIResponse<T> {
    @JsonProperty("status")
    String status;
    @JsonProperty("data")
    T data;
}
