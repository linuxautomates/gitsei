package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Link.LinkBuilder.class)
public class Link {

    @JsonProperty("type")
    String type;

    @JsonProperty("rel")
    String rel;

    @JsonProperty("uri")
    String uri;
}
