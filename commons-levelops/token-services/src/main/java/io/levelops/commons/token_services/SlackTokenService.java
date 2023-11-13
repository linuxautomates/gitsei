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
public class SlackTokenService extends TokenService {
    private static final Log LOGGER = LogFactory.getLog(SlackTokenService.class);

    private static final String SLACK_OAUTH_URL = "https://slack.com/api/oauth.v2.access";
    private final SlackSecrets secrets;
    private final String redirectHost;

    @Builder
    public SlackTokenService(OkHttpClient httpClient, ObjectMapper mapper, SlackSecrets secrets, String redirectHost) {
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
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", redirectHost  + "/integration-callback")
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
                .build();
        return getTokens(requestBody);
    }

    private Tokens getTokens(RequestBody requestBody) throws TokenException {
        HttpUrl url = HttpUrl.parse(SLACK_OAUTH_URL);
        Request request = new Request.Builder()
                .url(url)
                .header(ACCEPT, APPLICATION_FORM_URL_ENCODED.toString())
                .post(requestBody)
                .build();
        String rawResponse = getClientHelper().executeRequest(request);
        log.info("rawResponse = {}", rawResponse);
        SlackOauthV2Response response = getClientHelper().parseResponse(rawResponse, SlackOauthV2Response.class);
        log.info("response = {}", response);
        if (Boolean.FALSE.equals(response.getOk())) {
            throw new TokenException("Response not successful: " + rawResponse);
        }
        return Tokens.builder()
                .accessToken((response.getAuthedUser() != null)? response.getAuthedUser().getAccessToken() : null)
                .botToken(response.getAccessToken())
                .refreshToken(null)
                .rawTokenResponse(rawResponse)
                .teamId((response.getTeam() != null) ? response.getTeam().getId() : null)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackOauthResponse.SlackOauthResponseBuilder.class)
    public static class SlackOauthResponse {
        @JsonProperty("ok")
        Boolean ok;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("scope")
        String scope;
        @JsonProperty("team_name")
        String teamName;
        @JsonProperty("team_id")
        String teamId;
        @JsonProperty("bot")
        SlackBotToken bot;
        /*
        "incoming_webhook": {
            "url": "https://hooks.slack.com/TXXXXX/BXXXXX/XXXXXXXXXX",
            "channel": "#channel-it-will-post-to",
            "configuration_url": "https://teamname.slack.com/services/BXXXXX"
         }
         */
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackOauthV2Response.SlackOauthV2ResponseBuilder.class)
    public static class SlackOauthV2Response {
        @JsonProperty("ok")
        Boolean ok;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("scope")
        String scope;
        @JsonProperty("bot_user_id")
        String botUserId;

        @JsonProperty("app_id")
        String appId;

        @JsonProperty("team")
        SlackTeam team;

        @JsonProperty("authed_user")
        SlackAuthedUser authedUser;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackTeam.SlackTeamBuilder.class)
    public static class SlackTeam {
        @JsonProperty("name")
        String name;
        @JsonProperty("id")
        String id;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackAuthedUser.SlackAuthedUserBuilder.class)
    public static class SlackAuthedUser {
        @JsonProperty("id")
        String id;
        @JsonProperty("scope")
        String scope;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("token_type")
        String tokenType;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackBotToken.SlackBotTokenBuilder.class)
    public static class SlackBotToken {
        @JsonProperty("bot_user_id")
        String botUserId;
        @JsonProperty("bot_access_token")
        String botAccessToken;
    }

    @Getter
    @Builder
    public static class SlackSecrets {
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
