package io.levelops.integrations.salesforce.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SOQLJobRequest.SOQLJobRequestBuilder.class)
public class SOQLJobRequest {
    @JsonProperty
    String operation;

    @JsonProperty
    String query;
}
