package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckIssuesListResponse.BlackDuckIssuesListResponseBuilder.class)
public class BlackDuckIssuesListResponse {
    @JsonProperty("totalCount")
    int totalCount;

    @JsonProperty("items")
    List<BlackDuckIssue> blackDuckIssues;
}
