package io.levelops.commons.client.oauth.oauth1;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Oauth1Credentials {
    String consumerKey;
    String consumerSecret; // a.k.a. private key
    String accessToken;
    String accessSecret; // a.k.a. verification code
}
