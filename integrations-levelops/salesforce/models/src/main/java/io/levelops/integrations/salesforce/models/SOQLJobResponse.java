package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SOQLJobResponse.SOQLJobResponseBuilder.class)
public class SOQLJobResponse {
    @JsonProperty
    String id;

    @JsonProperty
    String state;

    @JsonProperty
    String object;

    @JsonProperty
    String operation;
}
