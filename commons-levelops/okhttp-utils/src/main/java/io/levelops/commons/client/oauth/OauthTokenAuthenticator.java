package io.levelops.commons.client.oauth;

import io.levelops.commons.client.ClientConstants;
import okhttp3.Authenticator;

public class OauthTokenAuthenticator extends GenericTokenAuthenticator implements Authenticator {

    public OauthTokenAuthenticator(OauthTokenProvider tokenProvider) {
        super(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_, tokenProvider);
    }

    public static OauthTokenAuthenticator using(OauthTokenProvider provider) {
        return new OauthTokenAuthenticator(provider);
    }
}
