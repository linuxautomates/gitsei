package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceSearchResponse.ConfluenceSearchResponseBuilder.class)
public class ConfluenceSearchResponse {

    @JsonProperty("results")
    List<ConfluenceSearchResult> results;

}
