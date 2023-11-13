package io.levelops.integrations.jira.client;

import com.atlassian.jwt.SigningAlgorithm;
import com.atlassian.jwt.core.writer.JsonSmartJwtJsonBuilder;
import com.atlassian.jwt.core.writer.JwtClaimsBuilder;
import com.atlassian.jwt.core.writer.NimbusJwtWriterFactory;
import com.atlassian.jwt.httpclient.CanonicalHttpUriRequest;
import com.atlassian.jwt.writer.JwtJsonBuilder;
import com.atlassian.jwt.writer.JwtWriterFactory;
import lombok.Builder;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static io.levelops.commons.client.ClientConstants.ACCEPT;
import static io.levelops.commons.client.ClientConstants.APPLICATION_JSON;
import static io.levelops.commons.client.ClientConstants.AUTHORIZATION;

// Inspiration for this code taken from:
// https://developer.atlassian.com/cloud/jira/software/understanding-jwt/#creating-a-jwt-toke
@Builder(toBuilder = true)
public class AtlassianConnectJwtInterceptor implements Interceptor {
    private final String appKey;
    private final String sharedSecret;

    public AtlassianConnectJwtInterceptor(String appKey, String sharedSecret) {
        this.appKey = appKey;
        this.sharedSecret = sharedSecret;
    }

    public static HashMap<String, String[]> getQueryParams(Request request) {
        HashMap<String, String[]> queryPairs = new HashMap<String, String[]>();
        String queryString = request.url().encodedQuery();
        if (queryString == null) {
            return queryPairs;
        }
        request
                .url()
                .queryParameterNames()
                .forEach(
                        param -> {
                            queryPairs.put(
                                    param, request.url().queryParameterValues(param).toArray(new String[0]));
                        });
        return queryPairs;
    }

    public String createAtlassianConnectToken(Request request)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        long issuedAt = System.currentTimeMillis() / 1000L;
        long expiresAt = issuedAt + 180L;

        JwtJsonBuilder jwtBuilder =
                new JsonSmartJwtJsonBuilder()
                        .issuedAt(issuedAt)
                        .expirationTime(expiresAt)
                        .issuer(appKey);

        String method = request.method().toUpperCase();
        String apiPath = request.url().encodedPath();
        String contextPath = "/";

        CanonicalHttpUriRequest canonical =
                new CanonicalHttpUriRequest(method, apiPath, contextPath, getQueryParams(request));
        JwtClaimsBuilder.appendHttpRequestClaims(jwtBuilder, canonical);

        JwtWriterFactory jwtWriterFactory = new NimbusJwtWriterFactory();
        String jwtbuilt = jwtBuilder.build();

        return jwtWriterFactory.macSigningWriter(SigningAlgorithm.HS256, sharedSecret).jsonToJwt(jwtbuilt);
    }


    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        builder.header(ACCEPT, APPLICATION_JSON.toString());
        try {
            String tokenAtlassian = createAtlassianConnectToken(request);
            builder.header(AUTHORIZATION, "JWT " + tokenAtlassian);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return chain.proceed(builder.build());
    }
}
