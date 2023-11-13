package io.levelops.commons.databases.models.database.tokens;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OauthToken.class, name = OauthToken.TOKEN_TYPE),
        @JsonSubTypes.Type(value = ApiKey.class, name = ApiKey.TOKEN_TYPE),
        @JsonSubTypes.Type(value = DBAuth.class, name = DBAuth.TOKEN_TYPE),
        @JsonSubTypes.Type(value = MultipleApiKeys.class, name = MultipleApiKeys.TOKEN_TYPE),
        @JsonSubTypes.Type(value = AtlassianConnectJwtToken.class, name = AtlassianConnectJwtToken.TOKEN_TYPE),
        @JsonSubTypes.Type(value = AdfsOauthToken.class, name = AdfsOauthToken.TOKEN_TYPE)
})
public interface TokenData {

    String getType();

    String getName();

    default String getAuthorizationHeader() {
        throw new UnsupportedOperationException();
    }

}
