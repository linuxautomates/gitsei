package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceSearchContent.ConfluenceSearchContentBuilder.class)
public class ConfluenceSearchContent {

    @JsonProperty("id")
    String id;
    @JsonProperty("type")
    String type;
    @JsonProperty("status")
    String status;
    @JsonProperty("title")
    String title;

    // TODO there's more
}
