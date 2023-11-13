package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerLink.BitbucketServerLinkBuilder.class)
public class BitbucketServerLink {

    @JsonProperty("self")
    List<Self> self;

    @JsonProperty("clone")
    List<Clone> clone;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Self.SelfBuilder.class)
    private static class Self {

        @JsonProperty("href")
        String href;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Clone.CloneBuilder.class)
    private static class Clone {

        @JsonProperty("href")
        String href;

        @JsonProperty("name")
        String name;
    }
}
