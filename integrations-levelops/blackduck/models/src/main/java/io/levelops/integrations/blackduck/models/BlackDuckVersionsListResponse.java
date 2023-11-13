package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckVersionsListResponse.BlackDuckVersionsListResponseBuilder.class)
public class BlackDuckVersionsListResponse {
    @JsonProperty("totalCount")
    int totalCount;

    @JsonProperty("items")
    List<BlackDuckVersion> blackDuckVersions;
}
