package io.levelops.commons.databases.models.database.tokens;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OauthToken implements TokenData {

    public static final String TOKEN_TYPE = "oauth";
    private static final String BEARER = "Bearer %s";

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("name")
    private String name;

    @JsonProperty("token")
    private String token;

    @JsonProperty("instance_url") // useful for salesforce
    private String instanceUrl;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("bot_token")
    private String botToken; // TODO use token name and create multiple tokens

    @JsonProperty("refreshed_at")
    private Long refreshedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonIgnore
    public String toBearerToken() {
        return String.format(BEARER, token);
    }

    @JsonIgnore
    public String toBearerRefreshToken() {
        return String.format(BEARER, refreshToken);
    }

    @JsonIgnore
    @Override
    public String getAuthorizationHeader() {
        return toBearerToken();
    }
}
