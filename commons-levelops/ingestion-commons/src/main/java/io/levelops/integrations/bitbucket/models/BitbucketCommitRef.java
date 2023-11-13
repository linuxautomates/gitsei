package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketCommitRef.BitbucketCommitRefBuilder.class)
public class BitbucketCommitRef {
    @JsonProperty("hash")
    String hash;
    @JsonProperty("type")
    String type;
}
