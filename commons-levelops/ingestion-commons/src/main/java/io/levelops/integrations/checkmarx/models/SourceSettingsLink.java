package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SourceSettingsLink.SourceSettingsLinkBuilder.class)
public class SourceSettingsLink {

    @JsonProperty("type")
    String type;

    @JsonProperty("rel")
    String rel;

    @JsonProperty("uri")
    String uri;
}
