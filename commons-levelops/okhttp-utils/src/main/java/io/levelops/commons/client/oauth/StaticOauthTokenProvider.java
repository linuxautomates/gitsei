package io.levelops.commons.client.oauth;

import lombok.Builder;

import javax.annotation.Nullable;

@Builder
public class StaticOauthTokenProvider implements OauthTokenProvider {

    private final String token;
    private final String refreshToken;

    @Nullable
    @Override
    public String getToken() {
        return token;
    }

    @Nullable
    @Override
    public String refreshToken() {
        return refreshToken;
    }
}
