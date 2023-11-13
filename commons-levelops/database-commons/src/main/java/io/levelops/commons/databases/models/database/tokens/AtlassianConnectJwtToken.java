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

import java.util.Base64;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AtlassianConnectJwtToken implements TokenData {

    public static final String TOKEN_TYPE = "atlassian_connect_jwt";

    @JsonProperty("name")
    private String name;

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("client_key")
    private String clientKey;

    @JsonProperty("app_key")
    private String appKey;

    @JsonProperty("shared_secret")
    private String sharedSecret;

    @JsonProperty("base_url")
    private String baseUrl;

    @Override
    public String getAuthorizationHeader() {
        return sharedSecret;
    }
}
