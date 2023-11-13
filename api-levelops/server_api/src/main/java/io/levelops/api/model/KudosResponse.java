package io.levelops.api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = KudosResponse.KudosResponseBuilder.class)
public class KudosResponse {
        UUID id;
        UUID dashboardId;
        String level;
        String author;
        String type;
        String icon;
        String breadcrumbs;
        Boolean anonymousLink;
        Instant expiration;
        String body;
        Boolean includeWidgetDetails;
        Instant createdAt;
        Instant updatedAt;
        String screenshot;

        @JsonPOJOBuilder(withPrefix = "")
        public final static class KudosResponseBuilderImpl extends KudosResponseBuilder {

        }
}
