package io.levelops.commons.client.oauth.oauth1;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;


@Builder
public final class Oauth1SigningInterceptor implements Interceptor {
    private static final Escaper ESCAPER = UrlEscapers.urlFormParameterEscaper();
    private static final String OAUTH_CONSUMER_KEY = "oauth_consumer_key";
    private static final String OAUTH_NONCE = "oauth_nonce";
    private static final String OAUTH_SIGNATURE = "oauth_signature";
    private static final String OAUTH_SIGNATURE_METHOD_FIELD = "oauth_signature_method";
    private static final String OAUTH_TIMESTAMP = "oauth_timestamp";
    private static final String OAUTH_ACCESS_TOKEN = "oauth_token";
    private static final String OAUTH_VERSION = "oauth_version";
    private static final String OAUTH_VERSION_VALUE = "1.0";

    private final Oauth1Credentials credentials;
    private final Oauth1SigningMethod signingMethod;
    private final Random random;
    private final Clock clock;
    private final boolean clockInSeconds;
    private final boolean includeBodyInSignature; // NOTE: OAuth1 standard only requires form-data payloads to be signed; JSON data must not be included.

    private Oauth1SigningInterceptor(Oauth1Credentials credentials, Oauth1SigningMethod signingMethod,
                                     Random random, Clock clock, boolean clockInSeconds, boolean includeBodyInSignature) {
        this.credentials = credentials;
        Validate.notNull(credentials, "credentials cannot be null.");
        Validate.notBlank(credentials.getConsumerKey(), "consumerKey cannot be null or empty.");
        Validate.notBlank(credentials.getConsumerSecret(), "consumerSecret cannot be null or empty.");
        Validate.notBlank(credentials.getAccessToken(), "accessToken cannot be null or empty.");
        Validate.notBlank(credentials.getAccessSecret(), "accessSecret cannot be null or empty.");
        Validate.notNull(signingMethod, "signingMethod cannot be null.");
        this.signingMethod = signingMethod;
        this.random = random != null ? random : new SecureRandom();
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.clockInSeconds = clockInSeconds;
        this.includeBodyInSignature = includeBodyInSignature;
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(signRequest(chain.request()));
    }

    public Request signRequest(Request request) throws IOException {
        byte[] nonce = new byte[32];
        random.nextBytes(nonce);
        String oauthNonce = ByteString.of(nonce).base64().replaceAll("\\W", "");
        long timestamp = clock.millis();
        if (clockInSeconds) {
            timestamp /= 1000;
        }
        String oauthTimestamp = String.valueOf(timestamp);

        String consumerKeyValue = ESCAPER.escape(credentials.getConsumerKey());
        String accessTokenValue = ESCAPER.escape(credentials.getAccessToken());

        SortedMap<String, String> parameters = new TreeMap<>();
        parameters.put(OAUTH_CONSUMER_KEY, consumerKeyValue);
        parameters.put(OAUTH_ACCESS_TOKEN, accessTokenValue);
        parameters.put(OAUTH_NONCE, oauthNonce);
        parameters.put(OAUTH_TIMESTAMP, oauthTimestamp);
        parameters.put(OAUTH_SIGNATURE_METHOD_FIELD, signingMethod.getValue());
        parameters.put(OAUTH_VERSION, OAUTH_VERSION_VALUE);

        parseRequest(request, parameters);

        Buffer data = serializeRequest(request, parameters);
        String signature = signingMethod.sign(credentials, data);

        String authorization = "OAuth "
                + OAUTH_CONSUMER_KEY + "=\"" + consumerKeyValue + "\", "
                + OAUTH_NONCE + "=\"" + oauthNonce + "\", "
                + OAUTH_SIGNATURE + "=\"" + ESCAPER.escape(signature) + "\", "
                + OAUTH_SIGNATURE_METHOD_FIELD + "=\"" + signingMethod.getValue() + "\", "
                + OAUTH_TIMESTAMP + "=\"" + oauthTimestamp + "\", "
                + OAUTH_ACCESS_TOKEN + "=\"" + accessTokenValue + "\", "
                + OAUTH_VERSION + "=\"" + OAUTH_VERSION_VALUE + "\"";

        return request.newBuilder()
                .addHeader("Authorization", authorization)
                .build();
    }

    private void parseRequest(Request request, SortedMap<String, String> parameters) throws IOException {
        HttpUrl url = request.url();
        for (int i = 0; i < url.querySize(); i++) {
            parameters.put(ESCAPER.escape(url.queryParameterName(i)),
                    ESCAPER.escape(StringUtils.defaultString(url.queryParameterValue(i))));
        }

        // NOTE: OAuth1 standard only requires form-data payloads to be signed; JSON data must not be included.
        if (includeBodyInSignature) {
            RequestBody requestBody = request.body();
            Buffer body = new Buffer();
            if (requestBody != null) {
                requestBody.writeTo(body);
            }

            while (!body.exhausted()) {
                long keyEnd = body.indexOf((byte) '=');
                if (keyEnd == -1) {
                    throw new IllegalStateException("Key with no value: " + body.readUtf8());
                }
                String key = body.readUtf8(keyEnd);
                body.skip(1); // Equals.

                long valueEnd = body.indexOf((byte) '&');
                String value = valueEnd == -1 ? body.readUtf8() : body.readUtf8(valueEnd);
                if (valueEnd != -1) {
                    body.skip(1); // Ampersand.
                }

                parameters.put(key, value);
            }
        }
    }

    private Buffer serializeRequest(Request request, SortedMap<String, String> parameters) {
        Buffer base = new Buffer();
        String method = request.method();
        base.writeUtf8(method);
        base.writeByte('&');
        base.writeUtf8(ESCAPER.escape(request.url().newBuilder().query(null).build().toString()));
        base.writeByte('&');

        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!first) {
                base.writeUtf8(ESCAPER.escape("&"));
            }
            first = false;
            base.writeUtf8(ESCAPER.escape(entry.getKey()));
            base.writeUtf8(ESCAPER.escape("="));
            base.writeUtf8(ESCAPER.escape(entry.getValue()));
        }
        return base;
    }

}
