package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketProject.BitbucketProjectBuilder.class)
public class BitbucketProject {
    @JsonProperty("key")
    String key;
    @JsonProperty("type")
    String type;
    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("name")
    String name;
}
