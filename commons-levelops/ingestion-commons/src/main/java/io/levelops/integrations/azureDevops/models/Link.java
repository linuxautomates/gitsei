package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Link.LinkBuilder.class)
public class Link {
    @JsonProperty("self")
    @JsonAlias({"avatar"})
    Self self;

    @JsonProperty("web")
    Web web;

    @JsonProperty("memberships")
    Web memberships;

    @JsonProperty("membershipState")
    Web membershipState;

    @JsonProperty("storageKey")
    Web storageKey;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Self.SelfBuilder.class)
    public static class Self {
        @JsonProperty("href")
        String href;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Web.WebBuilder.class)
    public static class Web {
        @JsonProperty("href")
        String href;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Membership.MembershipBuilder.class)
    public static class Membership {
        @JsonProperty("href")
        String href;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = MemebershipState.MemebershipStateBuilder.class)
    public static class MemebershipState {
        @JsonProperty("href")
        String href;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = StorageKey.StorageKeyBuilder.class)
    public static class StorageKey {
        @JsonProperty("href")
        String href;
    }
}
