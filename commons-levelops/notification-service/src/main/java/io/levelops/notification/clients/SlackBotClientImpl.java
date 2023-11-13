package io.levelops.notification.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.notification.models.SlackApiChannel;
import io.levelops.notification.models.SlackApiChannelResponse;
import io.levelops.notification.models.SlackApiChatMessagePermalinkResponse;
import io.levelops.notification.models.SlackApiFileUploadResponse;
import io.levelops.notification.models.SlackApiResponse;
import io.levelops.notification.models.SlackApiUser;
import io.levelops.notification.models.SlackApiUserResponse;
import io.levelops.notification.models.SlackApiViewResponse;
import io.levelops.notification.models.SlackChatPostMessageResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Log4j2
public class SlackBotClientImpl implements SlackBotClient {

    private final static Set<String> CHANNEL_JOIN_ERROR_MSGS = Set.of("not_in_channel", "channel_not_found");

    private final ClientHelper<SlackClientException> clientHelper;
    private final ObjectMapper objectMapper;

    @Builder
    public SlackBotClientImpl(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        clientHelper = ClientHelper.<SlackClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SlackClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse("https://slack.com/api").newBuilder();
    }

    @SuppressWarnings("rawtypes")
    private static void expectOkResponse(SlackApiResponse apiResponse) throws SlackClientException {
        if (apiResponse == null || !apiResponse.isOk()) {
            throw new SlackClientException("Response not successful: " + apiResponse, apiResponse);
        }
    }

    @Override
    public SlackApiViewResponse openView(String triggerId, String viewMessage) throws SlackClientException {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        Validate.notBlank(viewMessage, "viewMessage cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("views.open")
                .build();
        RequestBody formBody = new FormBody.Builder()
                .add("trigger_id", triggerId)
                .add("view", viewMessage)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        SlackApiResponse<SlackApiViewResponse> slackApiResponse = clientHelper.executeAndParse(request, SlackApiViewResponse.getJavaType(objectMapper));
        expectOkResponse(slackApiResponse);
        return slackApiResponse.getPayload();
    }

    @Override
    @SuppressWarnings("unchecked")
    public SlackChatPostMessageResponse postChatInteractiveMessage(String channelId, String text, @Nullable String botName) throws SlackClientException {
        Validate.notBlank(channelId, "channelId cannot be null or empty.");
        SlackChatPostMessageResponse response;
        try {
            log.info("sending chat interactive message internal, channelId {}", channelId);
            response = postChatInteractiveMessageInternal(channelId, text, botName);
            log.info("successfully sent chat interactive message, channelId {}", channelId);
            return response;
        } catch (SlackClientException e) {
            String error = (e.getApiResponse() != null) ? (String) e.getApiResponse().getDynamicProperties().getOrDefault("error", "") : "";
            if ((e.getApiResponse() != null) && (CHANNEL_JOIN_ERROR_MSGS.contains(error))) {
                log.info("Error trying to send chat interactive message, will try to join channel, channelId {}, error {}", channelId, error);
                try {
                    joinChannel(channelId);
                    log.info("successfully joined channel {}", channelId);
                } catch (SlackClientException ex) {
                    String channelJoinError = (ex.getApiResponse() != null) ? (String) ex.getApiResponse().getDynamicProperties().getOrDefault("error", "") : "";
                    if ("invalid_name_specials".equals(channelJoinError)) {
                        log.info("The channelId is a id not name, using joinConversation instead of joinChannel");
                        try {
                            joinConversation(channelId);
                            log.info("successfully joined conversation {}", channelId);
                        } catch (SlackClientException exx) {
                            log.error("failed to join conversation, channelId {}", channelId);
                            throw exx;
                        }
                    } else {
                        if ("name_taken".equals(channelJoinError)) {
                            log.error("failed to join channel {}, most probably it is private channel!", channelId);
                        } else {
                            log.error("failed to join channel, channelId {}", channelId);
                        }
                        throw ex;
                    }
                }
                log.info("Retrying send chat interactive message after joining channel, channelId {}", channelId);
                response = postChatInteractiveMessageInternal(channelId, text, botName);
                log.info("successfully sent chat interactive message after joining channel, channelId {}", channelId);
                return response;
            } else {
                throw e;
            }
        }
    }

    @Override
    public Optional<SlackApiUser> lookupUserByEmail(String email) throws SlackClientException {
        Validate.notBlank(email, "email cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("users.lookupByEmail")
                .addQueryParameter("email", email)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        SlackApiResponse<SlackApiUserResponse> response = clientHelper.executeAndParse(request,
                SlackApiUserResponse.getJavaType(objectMapper));
        expectOkResponse(response);
        return Optional.ofNullable(response)
                .filter(SlackApiResponse::isOk)
                .map(SlackApiResponse::getPayload)
                .map(SlackApiUserResponse::getUser);
    }

    @Override
    public Optional<SlackApiChannel> openImChannel(String userId) throws SlackClientException {
        Validate.notBlank(userId, "userId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("conversations.open")
                .addQueryParameter("users", userId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        SlackApiResponse<SlackApiChannelResponse> response = clientHelper.executeAndParse(request,
                SlackApiChannelResponse.getJavaType(objectMapper));
        expectOkResponse(response);
        return Optional.ofNullable(response)
                .filter(SlackApiResponse::isOk)
                .map(SlackApiResponse::getPayload)
                .map(SlackApiChannelResponse::getChannel);
    }

    @Override
    public SlackApiFileUploadResponse fileUpload(List<String> channelNamesOrIds, String fileName, String fileContent, String threadId) throws SlackClientException {
        Validate.isTrue(CollectionUtils.isNotEmpty(channelNamesOrIds), "channelNamesOrIds cannot be null or empty.");
        Validate.notBlank(fileName, "fileName cannot be null or empty.");
        Validate.notBlank(fileContent, "fileContent cannot be null or empty.");

        HttpUrl url = baseUrlBuilder()
                .addPathSegment("files.upload")
                .build();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("channels", String.join(",", channelNamesOrIds))
                .addFormDataPart("filename", fileName)
                .addFormDataPart("file", fileName, RequestBody.create(fileContent.getBytes(StandardCharsets.UTF_8)));

        if(StringUtils.isNotBlank(threadId)) {
            requestBodyBuilder.addFormDataPart("thread_ts", threadId);
        }
        MultipartBody requestBody = requestBodyBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        SlackApiResponse<SlackApiFileUploadResponse> slackApiResponse = clientHelper.executeAndParse(request, SlackApiFileUploadResponse.getJavaType(objectMapper));
        expectOkResponse(slackApiResponse);
        return slackApiResponse.getPayload();
    }

    @Override
    public SlackApiChatMessagePermalinkResponse getChatMessagePermalink(String channelId, String messageTs) throws SlackClientException {
        Validate.notBlank(channelId, "channelId cannot be null or empty.");
        Validate.notBlank(messageTs, "messageTs cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("chat.getPermalink")
                .addQueryParameter("channel", channelId)
                .addQueryParameter("message_ts", messageTs)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        SlackApiResponse<SlackApiChatMessagePermalinkResponse> response = clientHelper.executeAndParse(request,
                SlackApiChatMessagePermalinkResponse.getJavaType(objectMapper));
        expectOkResponse(response);
        return response.getPayload();
    }

    @Override
    public Optional<SlackApiUser> getUserInfo(String userId) throws SlackClientException {
        Validate.notBlank(userId, "userId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("users.info")
                .addQueryParameter("user", userId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        SlackApiResponse<SlackApiUserResponse> response = clientHelper.executeAndParse(request,
                SlackApiUserResponse.getJavaType(objectMapper));
        expectOkResponse(response);
        return Optional.ofNullable(response)
                .filter(SlackApiResponse::isOk)
                .map(SlackApiResponse::getPayload)
                .map(SlackApiUserResponse::getUser);
    }

    private SlackChatPostMessageResponse postChatInteractiveMessageInternal(
            String channelId,
            String text,
            @Nullable String botName) throws SlackClientException {
        Validate.notBlank(channelId, "channelId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("chat.postMessage")
                .build();

        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("channel", channelId)
                .add("username", botName)
                .add("blocks", text);
        RequestBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        SlackApiResponse<SlackChatPostMessageResponse> slackApiResponse = clientHelper.executeAndParse(request,
                SlackChatPostMessageResponse.getJavaType(objectMapper));
        expectOkResponse(slackApiResponse);
        return slackApiResponse.getPayload();
    }


    public SlackApiChannelResponse joinChannel(String channelNameOrId) throws SlackClientException {
        Validate.notBlank(channelNameOrId, "channelNameOrId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("conversations.join")
                .build();
        RequestBody formBody = new FormBody.Builder()
                .add("channel", channelNameOrId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        SlackApiResponse<SlackApiChannelResponse> slackApiResponse = clientHelper.executeAndParse(request,
                SlackApiChannelResponse.getJavaType(objectMapper));
        expectOkResponse(slackApiResponse);
        return slackApiResponse.getPayload();
    }

    public SlackApiChannelResponse joinConversation(String channelId) throws SlackClientException {
        Validate.notBlank(channelId, "channelId cannot be null or empty.");
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("conversations.join")
                .build();
        RequestBody formBody = new FormBody.Builder()
                .add("channel", channelId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        SlackApiResponse<SlackApiChannelResponse> slackApiResponse = clientHelper.executeAndParse(request,
                SlackApiChannelResponse.getJavaType(objectMapper));
        expectOkResponse(slackApiResponse);
        return slackApiResponse.getPayload();
    }
}
