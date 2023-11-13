package io.levelops.integrations.confluence.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfluenceBreadcrumb.ConfluenceBreadcrumbBuilder.class)
public class ConfluenceBreadcrumb {
    @JsonProperty("label")
    String label;
    @JsonProperty("url")
    String url;
    @JsonProperty("separator")
    String separator;
}
