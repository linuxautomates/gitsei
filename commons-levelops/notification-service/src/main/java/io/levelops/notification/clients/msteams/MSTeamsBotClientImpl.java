package io.levelops.notification.clients.msteams;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.notification.models.msteams.MSTeamsApiResponse;
import io.levelops.notification.models.msteams.MSTeamsChannel;
import io.levelops.notification.models.msteams.MSTeamsChat;
import io.levelops.notification.models.msteams.MSTeamsChatMessage;
import io.levelops.notification.models.msteams.MSTeamsChatRequest;
import io.levelops.notification.models.msteams.MSTeamsTeam;
import io.levelops.notification.models.msteams.MSTeamsTeamRequest;
import io.levelops.notification.models.msteams.MSTeamsUser;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.*;

@Log4j2
public class MSTeamsBotClientImpl implements MSTeamsBotClient {
    private final ClientHelper<MSTeamsClientException> clientHelper;
    private final ObjectMapper objectMapper;

    private static final String TEAMS_PATH = "teams";
    private static final String CHANNELS_PATH = "channels";

    private static final String ME_PATH = "me";
    private static final String DATA_TYPE = "#microsoft.graph.aadUserConversationMember";

    private static final List<String> ROLES = List.of("owner");

    private static final String USER_DATA_BIND = "https://graph.microsoft.com/v1.0/users";

    @Builder
    public MSTeamsBotClientImpl(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        clientHelper = ClientHelper.<MSTeamsClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(MSTeamsClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse("https://graph.microsoft.com/v1.0").newBuilder();
    }

    @Override
    public MSTeamsUser getLoggedInUser() throws MSTeamsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(ME_PATH)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        MSTeamsApiResponse<MSTeamsUser> response = clientHelper.executeAndParse(request, MSTeamsUser.getJavaType(objectMapper));
        return response.getPayload();
    }

    @Override
    public MSTeamsChat createOrGetChat(String toEmail, String fromEmail) throws MSTeamsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("chats")
                .build();

        MSTeamsChatRequest.MSTeamsChatMember toMember = MSTeamsChatRequest.MSTeamsChatMember.builder()
                .dataType(DATA_TYPE)
                .roles(ROLES)
                .userDataBind(USER_DATA_BIND + "('" + toEmail + "')")
                .build();

        MSTeamsChatRequest.MSTeamsChatMember fromMember = MSTeamsChatRequest.MSTeamsChatMember.builder()
                .dataType(DATA_TYPE)
                .roles(ROLES)
                .userDataBind(USER_DATA_BIND + "('" + fromEmail + "')")
                .build();

        MSTeamsChatRequest msTeamsChatRequest = MSTeamsChatRequest.builder()
                .chatType("oneOnOne")
                .members(List.of(toMember, fromMember))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(msTeamsChatRequest))
                .build();
        MSTeamsApiResponse<MSTeamsChat> response = clientHelper.executeAndParse(request, MSTeamsChat.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getPayload();
    }

    @Override
    public MSTeamsChatMessage sendMessageToUser(String chatId, String text) throws MSTeamsClientException {
        Validate.notBlank(chatId, "chatId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("chats")
                .addPathSegment(chatId)
                .addPathSegment("messages")
                .build();

        MSTeamsChatMessage.MSTeamsChatMessageBody msTeamsChatMessageBody = MSTeamsChatMessage.MSTeamsChatMessageBody.builder()
                .content(StringUtils.defaultString(text))
                .build();
        MSTeamsChatMessage msTeamsChatMessage = MSTeamsChatMessage.builder()
                .body(msTeamsChatMessageBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(msTeamsChatMessage))
                .build();
        MSTeamsApiResponse<MSTeamsChatMessage> response = clientHelper.executeAndParse(request, MSTeamsChatMessage.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getPayload();
    }

    @Override
    public MSTeamsChatMessage sendMessageToChannel(String teamId, String channelId, String text) throws MSTeamsClientException {

        Validate.notBlank(channelId, "channelId cannot be null or empty.");
        Validate.notBlank(teamId, "teamId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TEAMS_PATH)
                .addPathSegment(teamId)
                .addPathSegment(CHANNELS_PATH)
                .addPathSegment(channelId)
                .addPathSegment("messages")
                .build();

        MSTeamsChatMessage.MSTeamsChatMessageBody messageBody = MSTeamsChatMessage.MSTeamsChatMessageBody.builder()
                .content(StringUtils.defaultString(text))
                .build();

        MSTeamsChatMessage msTeamsChatMessage = MSTeamsChatMessage.builder()
                .body(messageBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(msTeamsChatMessage))
                .build();
        MSTeamsApiResponse<MSTeamsChatMessage> response = clientHelper.executeAndParse(request, MSTeamsChatMessage.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getPayload();
    }

    @Override
    public List<MSTeamsTeam> getAllTeams() throws MSTeamsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(ME_PATH)
                .addPathSegment("joinedTeams")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        MSTeamsApiResponse<MSTeamsTeam> response = clientHelper.executeAndParse(request, MSTeamsTeam.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getValues();
    }

    @Override
    public List<MSTeamsChannel> getChannels(String teamId) throws MSTeamsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(TEAMS_PATH)
                .addPathSegment(teamId)
                .addPathSegment("allChannels")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        MSTeamsApiResponse<MSTeamsChannel> response = clientHelper.executeAndParse(request, MSTeamsChannel.getJavaType(objectMapper));
        return response.getValues();
    }

    @Override
    public Optional<MSTeamsUser> getUser(String emailId) throws MSTeamsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("users")
                .addQueryParameter("$filter", "userPrincipalName eq '" + emailId + "' or mail eq '" + emailId + "'")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();
        MSTeamsApiResponse<MSTeamsUser> response = clientHelper.executeAndParse(request, MSTeamsUser.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getValues().stream().findFirst();
    }

    private static void expectOkResponse(MSTeamsApiResponse apiResponse) throws MSTeamsClientException {
        if (apiResponse == null || apiResponse.getError() != null) {
            throw new MSTeamsClientException("Response not successful: " + apiResponse);
        }
    }
}
