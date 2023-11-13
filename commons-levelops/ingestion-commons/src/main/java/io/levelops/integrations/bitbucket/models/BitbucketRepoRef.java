package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketRepoRef.BitbucketRepoRefBuilder.class)
public class BitbucketRepoRef {
    @JsonProperty("type")
    String type;
    @JsonProperty("name")
    String name;
    @JsonProperty("full_name")
    String fullName;
    @JsonProperty("uuid")
    String uuid;
}
