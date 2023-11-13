package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PullRequestHistoryResponse.PullRequestHistoryResponseBuilder.class)
public class PullRequestHistoryResponse {

    @JsonProperty("value")
    List<PullRequestHistory> pullRequestHistories;

    @JsonProperty("count")
    int count;
}
