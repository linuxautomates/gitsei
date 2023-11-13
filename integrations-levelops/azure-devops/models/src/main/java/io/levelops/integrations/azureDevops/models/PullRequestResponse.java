package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PullRequestResponse.PullRequestResponseBuilder.class)
public class PullRequestResponse {

    @JsonProperty("value")
    List<PullRequest> pullRequests;

    @JsonProperty("count")
    int count;
}
