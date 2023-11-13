package io.levelops.commons.token_services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.token_exceptions.InvalidTokenDataException;
import io.levelops.commons.token_exceptions.TokenException;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static io.levelops.commons.client.ClientConstants.ACCEPT;
import static io.levelops.commons.client.ClientConstants.APPLICATION_FORM_URL_ENCODED;

@Log4j2
public class MSTeamsTokenService extends TokenService {
    private static final Log LOGGER = LogFactory.getLog(MSTeamsTokenService.class);
    private static final String MSTEAMS_OAUTH_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    private static final String SCOPE = "Channel.ReadBasic.All ChannelMessage.Send Chat.Create Chat.ReadWrite offline_access Team.ReadBasic.All User.Read User.ReadBasic.All";
    private final String redirectHost;
    private final MSTeamsSecrets secrets;

    @Builder
    public MSTeamsTokenService(OkHttpClient httpClient, ObjectMapper mapper, MSTeamsSecrets secrets, String redirectHost) {
        super(httpClient, mapper);
        this.secrets = secrets;
        this.redirectHost = redirectHost;
    }

    @Override
    public Tokens getTokensFromCode(String code, String state) throws TokenException {
        if (StringUtils.isEmpty(code)) {
            throw new InvalidTokenDataException("Bad request. Missing code in request.");
        }
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", secrets.getClientId())
                .add("client_secret", secrets.getClientSecret())
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", redirectHost + "/integration-callback")
                .add("scope", SCOPE)
                .build();
        return getTokens(requestBody);
    }

    @Override
    public Tokens getTokensFromRefreshToken(String refreshToken) throws TokenException {
        if (StringUtils.isEmpty(refreshToken)) {
            throw new InvalidTokenDataException("Bad request. Missing refresh token in request.");
        }
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", secrets.getClientId())
                .add("client_secret", secrets.getClientSecret())
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .add("scope", SCOPE)
                .build();
        return getTokens(requestBody);
    }

    private Tokens getTokens(RequestBody requestBody) throws TokenException {
        HttpUrl url = HttpUrl.parse(MSTEAMS_OAUTH_URL);
        Request request = new Request.Builder()
                .url(url)
                .header(ACCEPT, APPLICATION_FORM_URL_ENCODED.toString())
                .post(requestBody)
                .build();
        String rawResponse = getClientHelper().executeRequest(request);
        log.info("rawResponse = {}", rawResponse);
        MSTeamsOauthV2Response response = getClientHelper().parseResponse(rawResponse, MSTeamsOauthV2Response.class);
        log.info("response = {}", response);
        if (response.getAccessToken() == null) {
            throw new TokenException("Response not successful: " + rawResponse);
        }
        return Tokens.builder()
                .accessToken(response.getAccessToken())
                .botToken(response.getAccessToken())
                .refreshToken(response.getRefreshToken())
                .rawTokenResponse(rawResponse)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = MSTeamsOauthV2Response.MSTeamsOauthV2ResponseBuilder.class)
    public static class MSTeamsOauthV2Response {

        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("expires_in")
        Long expires_in;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("scope")
        String scope;

    }

    @Getter
    @Builder
    public static class MSTeamsSecrets {
        private final String clientId;
        private final String clientSecret;

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("clientId", clientId)
                    .append("clientSecret", StringUtils.repeat("*", clientSecret.length()))
                    .toString();
        }
    }
}
