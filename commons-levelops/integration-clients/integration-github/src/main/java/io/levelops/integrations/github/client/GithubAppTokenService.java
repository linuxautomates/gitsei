package io.levelops.integrations.github.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.crypto.CryptoUtils;
import io.levelops.commons.token_services.TokenService;
import lombok.Builder;
import lombok.Value;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static io.levelops.commons.client.ClientConstants.ACCEPT;
import static io.levelops.commons.client.ClientConstants.AUTHORIZATION;
import static io.levelops.commons.client.ClientConstants.BEARER_;

public class GithubAppTokenService {

    private final ClientHelper<IOException> clientHelper;
    private final String seiAppId;
    private final String seiPemPrivateKey;

    public GithubAppTokenService(OkHttpClient okHttpClient, ObjectMapper objectMapper, String seiAppId, String seiPemPrivateKey) {
        clientHelper = ClientHelper.<IOException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(IOException.class)
                .build();
        this.seiAppId = seiAppId;
        this.seiPemPrivateKey = seiPemPrivateKey;
    }

    public String generateAccessToken(String githubApiUrl, String appId, String installationId, String pemPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String jwtToken = generateGithubAppJwtToken(pemPrivateKey, appId, Instant.now());
        return getNewAccessToken(githubApiUrl, installationId, jwtToken);
    }

    public String generateAccessTokenForSeiApp(String githubApiUrl, String installationId) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return generateAccessToken(githubApiUrl, seiAppId, installationId, seiPemPrivateKey);
    }

    public TokenService.Tokens getTokenForSeiApp(String githubApiUrl, String installationId) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String accessToken = generateAccessToken(githubApiUrl, seiAppId, installationId, seiPemPrivateKey);
        return TokenService.Tokens.builder()
                .accessToken(accessToken)
                .refreshToken(seiPemPrivateKey)
                .build();
    }

    public String getSeiAppId() {
        return seiAppId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubAppAccessTokenResponse.GithubAppAccessTokenResponseBuilder.class)
    public static class GithubAppAccessTokenResponse {
        @JsonProperty("token")
        String token;
    }

    protected String getNewAccessToken(String githubApiUrl, String installationId, String jwtToken) throws IOException {
        Validate.notBlank(githubApiUrl, "githubApiUrl cannot be null or empty.");
        Validate.notBlank(installationId, "installationId cannot be null or empty.");
        Validate.notBlank(jwtToken, "jwtToken cannot be null or empty.");
        HttpUrl url = HttpUrl.parse(githubApiUrl).newBuilder()
                .addPathSegment("app")
                .addPathSegment("installations")
                .addPathSegment(installationId)
                .addPathSegment("access_tokens")
                .build();
        RequestBody requestBody = RequestBody.create(new byte[0]);
        Request request = new Request.Builder()
                .url(url)
                .header(AUTHORIZATION, BEARER_ + jwtToken)
                .header(ACCEPT, "application/vnd.github+json")
                .post(requestBody)
                .build();
        GithubAppAccessTokenResponse response = clientHelper.executeAndParse(request, GithubAppAccessTokenResponse.class);
        return response.getToken();
    }

    public static String generateGithubAppJwtToken(String pemPrivateKey, String appId, Instant now) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {

        PrivateKey privateKey = CryptoUtils.loadRSAPrivateKey(pemPrivateKey);

        // issued at time, 60 seconds in the past to allow for clock drift
        Instant issuedAt = now.minusSeconds(60);
        // expiration time (cannot be more than 10 minutes)
        Instant expiration = issuedAt.plus(10, ChronoUnit.MINUTES);

        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiration))
                .setIssuer(appId)
                .signWith(SignatureAlgorithm.RS256, privateKey);

        return builder.compact(); // NOTE: requires jaxb-api at runtime
    }


}
