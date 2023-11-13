package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykIgnored.SnykIgnoredBuilder.class)
public final class SnykIgnored {
    @JsonProperty("path")
    private final List<String> paths;

    @JsonProperty("reason")
    private final String reason;
    @JsonProperty("source")
    private final String source;

    @JsonProperty("created")
    private final Date created;
    @JsonProperty("expires")
    private final Date expires;

    @JsonProperty("ignoredBy")
    private final IgnoredBy ignoredBy;

    @JsonProperty("reasonType")
    private final String reasonType;
    @JsonProperty("disregardIfFixable")
    private final Boolean disregardIfFixable;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IgnoredBy.IgnoredByBuilder.class)
    public static final class IgnoredBy {
        @JsonProperty("id")
        private final String id;
        @JsonProperty("name")
        private final String name;
        @JsonProperty("email")
        private final String email;
    }
}