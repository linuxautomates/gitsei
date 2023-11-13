package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = KudosRequest.KudosRequestBuilder.class)
public class KudosRequest {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("dashboard_id")
    UUID dashboardId;
    @JsonProperty("level")
    String level;
    @JsonProperty("author")
    String author;
    @JsonProperty("type")
    String type;
    @JsonProperty("icon")
    String icon;
    @JsonProperty("breadcrumbs")
    String breadcrumbs;
    @JsonProperty("anonymous_link")
    Boolean anonymousLink;
    @JsonProperty("expiration")
    Instant expiration;
    @JsonProperty("body")
    String body;
    @JsonProperty("include_widget_details")
    Boolean includeWidgetDetails;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;
    @JsonProperty("widget_id")
    UUID widgetId;
    @JsonProperty("position")
    Integer position;
    @JsonProperty("size")
    Integer size;
    @JsonProperty("data")
    Map<String, Object> data;
    @JsonProperty("share")
    KudosSharingRequest share;

    @JsonPOJOBuilder(withPrefix = "")
    public final static class KudosRequestBuilderImpl extends KudosRequestBuilder {

    }
}
