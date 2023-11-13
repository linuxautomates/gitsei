package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonDeserialize(builder = Link.LinkBuilder.class)
public class Link {
    @JsonProperty("href")
    private final String href;
}
