package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaScanStatus.CxScaScanStatusBuilder.class)
public class CxScaScanStatus {

    @JsonProperty("name")
    String name;

    @JsonProperty("message")
    String message;

}
