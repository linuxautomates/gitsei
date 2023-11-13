package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketUser.BitbucketUserBuilder.class)
public class BitbucketUser {
    @JsonProperty("username")
    String username;
    @JsonProperty("display_name")
    String displayName;
    @JsonProperty("type")
    String type;
    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("nickname")
    String nickname;
    @JsonProperty("account_id")
    String accountId;
}
