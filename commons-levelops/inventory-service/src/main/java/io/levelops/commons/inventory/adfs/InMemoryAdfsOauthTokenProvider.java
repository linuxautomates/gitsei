package io.levelops.commons.inventory.adfs;

import io.levelops.commons.client.oauth.OauthTokenProvider;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

/**
 * This makes a call to ADFS to generate or refresh a token and stores it **in memory**. The ADFS client must be already authenticated.
 * Doing this in memory only makes sense for the satellite use case. We will have to build a token service and use
 * the regular Oauth Token provider if we ever plan to support ADFS in the cloud.
 */
@Log4j2
public class InMemoryAdfsOauthTokenProvider implements OauthTokenProvider  {

    private final AdfsClient adfsClient;
    private String token = null;

    @Builder
    public InMemoryAdfsOauthTokenProvider(AdfsClient adfsClient) {
        this.adfsClient = adfsClient;
    }

    @Override
    @Nullable
    public String getToken() {
        if (this.token == null) {
            refreshToken();
        }
        return this.token;
    }

    @Override
    @Nullable
    public String refreshToken() {
        try {
            log.debug("Refreshing ADFS token...");
            this.token = adfsClient.getAccessToken();
        } catch (AdfsClientException e) {
            throw new RuntimeException("Could not get an access token from ADFS", e);
        }
        return this.token;
    }

}
