package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketLinks.BitbucketLinksBuilder.class)
public class BitbucketLinks {
    @JsonProperty("html")
    Link html;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Link.LinkBuilder.class)
    public static class Link {
        @JsonProperty("href")
        String href;
    }
}
