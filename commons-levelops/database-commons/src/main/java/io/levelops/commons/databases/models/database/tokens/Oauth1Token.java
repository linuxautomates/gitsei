package io.levelops.commons.databases.models.database.tokens;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Oauth1Token.Oauth1TokenBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Oauth1Token implements TokenData {

    public static final String TOKEN_TYPE = "oauth1";

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("name")
    private String name;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("private_key")
    private String privateKey;
    @JsonProperty("consumer_key")
    private String consumerKey;
    @JsonProperty("verification_code")
    private String verificationCode;
    @JsonProperty("access_token")
    private String accessToken;

}
