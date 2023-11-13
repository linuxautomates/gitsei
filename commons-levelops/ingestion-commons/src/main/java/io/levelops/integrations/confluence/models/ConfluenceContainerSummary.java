package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceContainerSummary.ConfluenceContainerSummaryBuilder.class)
public class ConfluenceContainerSummary {

    @JsonProperty("title")
    String title;
    @JsonProperty("displayUrl")
    String displayUrl;
}
