package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceSearchResult.ConfluenceSearchResultBuilder.class)
public class ConfluenceSearchResult {

    @JsonProperty("content")
    ConfluenceSearchContent content;

    @JsonProperty("title")
    String title;

    @JsonProperty("excerpt")
    String excerpt;

    @JsonProperty("url")
    String url;

    @JsonProperty("resultParentContainer")
    ConfluenceContainerSummary resultParentContainer;

    @JsonProperty("resultGlobalContainer")
    ConfluenceContainerSummary resultGlobalContainer;

    @JsonProperty("breadcrumbs")
    List<ConfluenceBreadcrumb> breadcrumbs;

    @JsonProperty("entityType")
    String entityType;

    @JsonProperty("lastModified")
    Date lastModified;

    @JsonProperty("friendlyLastModified")
    String friendlyLastModified;

    @JsonProperty("score")
    Double score;


}
