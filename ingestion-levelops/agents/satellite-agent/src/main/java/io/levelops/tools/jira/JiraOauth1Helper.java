package io.levelops.tools.jira;

import com.google.api.client.auth.oauth.OAuthAuthorizeTemporaryTokenUrl;
import com.google.api.client.auth.oauth.OAuthCredentialsResponse;
import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthGetTemporaryToken;
import com.google.api.client.auth.oauth.OAuthRsaSigner;
import com.google.api.client.http.apache.ApacheHttpTransport;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

@Getter
public class JiraOauth1Helper {

    private final String accessTokenUrl;
    private final String requestTokenUrl;
    private final String authorizationUrl;

    public JiraOauth1Helper(String jiraBaseUrl) {
        this.accessTokenUrl = jiraBaseUrl + "/plugins/servlet/oauth/access-token";
        this.requestTokenUrl = jiraBaseUrl + "/plugins/servlet/oauth/request-token";
        this.authorizationUrl = jiraBaseUrl + "/plugins/servlet/oauth/authorize";
    }

    public String getRequestToken(String consumerKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        OAuthGetTemporaryToken temporaryToken = buildRequestTokenRequest(consumerKey, privateKey);
        OAuthCredentialsResponse response = temporaryToken.execute();
        return response.token; // we don't need response.tokenSecret afaik
    }

    public String buildRequestTokenAuthUrl(String requestToken) {
        OAuthAuthorizeTemporaryTokenUrl authorizationURL = new OAuthAuthorizeTemporaryTokenUrl(authorizationUrl);
        authorizationURL.temporaryToken = requestToken;
        return authorizationURL.toString();
    }

    public String getAccessToken(String consumerKey, String privateKey, String requestToken, String verificationCode) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        OAuthGetAccessToken oAuthAccessToken = buildAccessTokenRequest(consumerKey, privateKey, requestToken, verificationCode);
        OAuthCredentialsResponse response = oAuthAccessToken.execute();
        return response.token;
    }

    private OAuthGetTemporaryToken buildRequestTokenRequest(String consumerKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        class JiraOAuthGetTemporaryToken extends OAuthGetTemporaryToken {
            public JiraOAuthGetTemporaryToken(String authorizationServerUrl) {
                super(authorizationServerUrl);
                this.usePost = true;
            }
        }
        OAuthGetTemporaryToken oAuthGetTemporaryToken = new JiraOAuthGetTemporaryToken(requestTokenUrl);
        oAuthGetTemporaryToken.consumerKey = consumerKey;
        oAuthGetTemporaryToken.signer = getOAuthRsaSigner(privateKey);
        oAuthGetTemporaryToken.transport = new ApacheHttpTransport();
        oAuthGetTemporaryToken.callback = "oob";
        return oAuthGetTemporaryToken;
    }

    private OAuthGetAccessToken buildAccessTokenRequest(String consumerKey, String privateKey, String requestToken, String verificationCode) throws NoSuchAlgorithmException, InvalidKeySpecException {
        class JiraOAuthGetAccessToken extends OAuthGetAccessToken {
            public JiraOAuthGetAccessToken(String authorizationServerUrl) {
                super(authorizationServerUrl);
                this.usePost = true;
            }
        }
        JiraOAuthGetAccessToken accessToken = new JiraOAuthGetAccessToken(accessTokenUrl);
        accessToken.consumerKey = consumerKey;
        accessToken.signer = getOAuthRsaSigner(privateKey);
        accessToken.transport = new ApacheHttpTransport();
        accessToken.verifier = verificationCode;
        accessToken.temporaryToken = requestToken;
        return accessToken;
    }

    private OAuthRsaSigner getOAuthRsaSigner(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        OAuthRsaSigner oAuthRsaSigner = new OAuthRsaSigner();
        oAuthRsaSigner.privateKey = getPrivateKey(privateKey);
        return oAuthRsaSigner;
    }

    private PrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // must use Apache's Base64 to handle '+' chars and more
        byte[] privateBytes = Base64.decodeBase64(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

}
