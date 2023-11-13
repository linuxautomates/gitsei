package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubSearchResult.GithubSearchResultBuilder.class)
public class GithubSearchResult<T> {
    @JsonProperty("total_count")
    Integer totalCount;
    @JsonProperty("incomplete_results")
    Boolean incompleteResults;
    @JsonProperty("items")
    List<T> items;
}
