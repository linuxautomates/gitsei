package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykOrg.SnykOrgBuilder.class)
public class SnykOrg {
    @JsonProperty("id")
    private final String id;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("slug")
    private final String slug;
    @JsonProperty("url")
    private final String url;
    @JsonProperty("group")
    private final SnykOrg.Group group;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Group.GroupBuilder.class)
    public static final class Group {
        @JsonProperty("name")
        private final String name;
        @JsonProperty("id")
        private final String id;
    }
}
