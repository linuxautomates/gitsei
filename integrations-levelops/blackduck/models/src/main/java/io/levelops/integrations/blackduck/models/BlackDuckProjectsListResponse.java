package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckProjectsListResponse.BlackDuckProjectsListResponseBuilder.class)
public class BlackDuckProjectsListResponse {

    @JsonProperty("totalCount")
    int totalCount;

    @JsonProperty("items")
    List<BlackDuckProject> blackDuckProjects;

    @JsonProperty("appliedFilters")
    List<String> appliedFilters;

    @JsonProperty("_meta")
    BlackDuckProjectMeta blackDuckProjectMeta;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize( builder = BlackDuckProjectMeta.BlackDuckProjectMetaBuilder.class)
    public static class BlackDuckProjectMeta {
        @JsonProperty("allow")
        List<String> allow;
        @JsonProperty("href")
        String href;
        @JsonProperty("links")
        List<BlackDuckProjectLinks> blackDuckProjectLinks;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckProjectLinks.BlackDuckProjectLinksBuilder.class)
    public static class BlackDuckProjectLinks {
        @JsonProperty("rel")
        String rel;
        @JsonProperty("href")
        String projectHref;
        @JsonProperty("name")
        String name;
        @JsonProperty("label")
        String label;
    }
}
