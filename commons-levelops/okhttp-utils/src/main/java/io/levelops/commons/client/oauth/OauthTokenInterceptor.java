package io.levelops.commons.client.oauth;

import io.levelops.commons.client.ClientConstants;
import okhttp3.Interceptor;

public class OauthTokenInterceptor extends GenericTokenInterceptor implements Interceptor {

    public OauthTokenInterceptor(OauthTokenProvider tokenProvider) {
        super(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_, tokenProvider);
    }

    public static OauthTokenInterceptor using(OauthTokenProvider provider) {
        return new OauthTokenInterceptor(provider);
    }

}
