package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PaginatedOktaResponse.PaginatedOktaResponseBuilder.class)
public class PaginatedOktaResponse<T> {

    @JsonProperty
    String presentCursor;

    @JsonProperty
    String nextCursor;

    @JsonProperty
    List<T> values;
}
