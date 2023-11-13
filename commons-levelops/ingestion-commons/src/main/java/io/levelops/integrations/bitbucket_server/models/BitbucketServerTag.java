package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerTag.BitbucketServerTagBuilder.class)
public class BitbucketServerTag {

    @JsonProperty("id")
    String id;

    @JsonProperty("displayId")
    String displayId;

    @JsonProperty("type")
    String type;

    @JsonProperty("latestCommit")
    String latestCommit;

    @JsonProperty("latestChangeset")
    String latestChangeset;

    @JsonProperty("hash")
    String hash;
}