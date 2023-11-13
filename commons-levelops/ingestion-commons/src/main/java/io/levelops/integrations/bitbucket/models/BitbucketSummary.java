package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketSummary.BitbucketSummaryBuilder.class)
public class BitbucketSummary {
    @JsonProperty("raw")
    String raw;
    @JsonProperty("markup")
    String markup;
    @JsonProperty("html")
    String html;
    @JsonProperty("type")
    String type;
}
