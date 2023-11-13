package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketTag.BitbucketTagBuilder.class)
public class BitbucketTag {

    @JsonProperty("name")
    String name;

    @JsonProperty("message")
    String message;

    @JsonProperty("type")
    String type;

    @JsonProperty("date")
    Date date;

    @JsonProperty("tagger")
    User tagger;

    @JsonProperty("author")
    User author;

    @JsonProperty("target")
    BitbucketTarget target;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketTag.BitbucketTarget.BitbucketTargetBuilder.class)
    public static class BitbucketTarget {

        @JsonProperty("hash")
        String hash;

        @JsonProperty("type")
        String type;

        @JsonProperty("name")
        String name;

        @JsonProperty("full_name")
        String fullName;

        @JsonProperty("uuid")
        String uuid;
}

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketTag.User.UserBuilder.class)
    public static class User {

        @JsonProperty("raw")
        String raw;

        @JsonProperty("type")
        String type;

        @JsonProperty("user")
        BitbucketUser user;
    }
}