package io.levelops.commons.client.oauth;

import javax.annotation.Nullable;

public interface OauthTokenProvider {

    @Nullable
    String getToken();

    @Nullable
    String refreshToken();

}
