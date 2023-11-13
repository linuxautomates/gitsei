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
public class ApiKey implements TokenData {

    public static final String TOKEN_TYPE = "apikey";

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("name")
    private String name;

    @JsonProperty("apikey")
    private String apiKey;

    @JsonProperty("username")
    private String userName;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonIgnore
    public String toBase64() {
        String toEncode = getUserName() + ":" + getApiKey();
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    @Override
    public String getAuthorizationHeader() {
        return toBase64();
    }
}
