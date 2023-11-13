package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScanType.ScanTypeBuilder.class)
public class ScanType {

    @JsonProperty("id")
    String id;

    @JsonProperty("value")
    String value;
}
