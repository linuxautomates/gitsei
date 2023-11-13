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
import org.apache.commons.collections4.CollectionUtils;

import java.util.Base64;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultipleApiKeys implements TokenData {
    public static final String TOKEN_TYPE = "multiple_api_keys";

    @Builder.Default
    @JsonProperty("type")
    private String type = TOKEN_TYPE;

    @JsonProperty("name")
    private String name;

    @JsonProperty("keys")
    private List<Key> keys;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonIgnore
    public String toBase64() {
        String userName = null;
        String apiKey = null;
        if(CollectionUtils.isNotEmpty(keys)) {
            userName = keys.get(0).getUserName();
            apiKey = keys.get(0).getApiKey();
        }
        String toEncode = userName + ":" + apiKey;
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    @Override
    public String getAuthorizationHeader() {
        return toBase64();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @Builder(toBuilder = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Key {
        @JsonProperty("username")
        private String userName;

        @JsonProperty("apikey")
        private String apiKey;
    }
}
