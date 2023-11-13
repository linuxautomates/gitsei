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
public class AdfsOauthToken implements TokenData {

    public static final String TOKEN_TYPE = "adfs";

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("name")
    private String name;

    @JsonProperty("adfs_url")
    String adfsUrl;

    @JsonProperty("adfs_client_id")
    String adfsClientId;

    @JsonProperty("adfs_resource")
    String adfsResource;

    @JsonProperty("username")
    String username;

    @JsonProperty("password")
    String password;

    @JsonProperty("token")
    String token;

    @JsonProperty("created_at")
    private Long createdAt;

}
