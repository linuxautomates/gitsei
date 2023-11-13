package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.slack.models.SlackChatMessageQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.slack.client.SlackBotClientFactory;
import io.levelops.integrations.slack.client.SlackClientException;
import io.levelops.integrations.slack.models.SlackApiChannel;
import io.levelops.integrations.slack.models.SlackApiUser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class SlackChatMessageController implements DataController<SlackChatMessageQuery> {

    private final ObjectMapper objectMapper;
    private final SlackBotClientFactory slackBotClientFactory;

    @Builder
    public SlackChatMessageController(ObjectMapper objectMapper,
                                      SlackBotClientFactory slackBotClientFactory) {
        this.objectMapper = objectMapper;
        this.slackBotClientFactory = slackBotClientFactory;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, SlackChatMessageQuery query) throws IngestException {
        int totalRecipients = CollectionUtils.size(query.getRecipients());
        MutableInt nbOfSuccesses = new MutableInt(0);
        List<SlackMessagePostStatus> statuses = ListUtils.emptyIfNull(query.getRecipients()).stream()
                .map(recipient -> postOneMessage(query.getIntegrationKey(), recipient, query.getBotName(), query.getText()))
                .peek(status -> {
                    if (status.isSuccess()) {
                        nbOfSuccesses.increment();
                    }
                })
                .collect(Collectors.toList());
        boolean success = (totalRecipients == nbOfSuccesses.intValue());
        return new SlackChatMessageResult(success, totalRecipients, nbOfSuccesses.intValue(), statuses);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackMessagePostStatus.SlackMessagePostStatusBuilder.class)
    public static class SlackMessagePostStatus {
        boolean success;
        String recipient;
        String error;
    }

    private SlackMessagePostStatus postOneMessage(IntegrationKey integrationKey, String recipient, String botName, String text) {
        SlackMessagePostStatus.SlackMessagePostStatusBuilder statusBuilder = SlackMessagePostStatus.builder()
                .recipient(recipient);
        // if empty, skip it silently
        if (StringUtils.isBlank(recipient)) {
            return statusBuilder.success(true).build();
        }
        Optional<String> channelOpt = convertRecipientToChannel(integrationKey, recipient);
        if (channelOpt.isEmpty()) {
            return statusBuilder.success(false).error("no channel provided").build();
        }
        String channel = channelOpt.get();
        log.info("Sending chat message to channel={}", channel);
        try {
            postMessage(integrationKey, text, channel, botName);
            return statusBuilder.success(true).build();
        } catch (SlackClientException e) {
            log.error("Could not send Slack message to channel=" + channel, e);
            return statusBuilder.success(false).error(e.toString()).build();
        }
    }

    private Optional<String> convertRecipientToChannel(IntegrationKey integrationKey, String recipient) {
        if (StringUtils.isBlank(recipient)) {
            return Optional.empty();
        }
        // if it's a channel name or channel id, return as is
        if (!recipient.contains("@")) {
            return Optional.of(recipient);
        }
        // otherwise it's a user email, so look up channel id by email
        try {
            var channelOpt = getDirectMessageChannelFromEmail(integrationKey, recipient)
                    .map(SlackApiChannel::getId);
            if (channelOpt.isEmpty()) {
                log.error("Could not find user or failed to open IM channel for: " + recipient);
            }
            return channelOpt;
        } catch (SlackClientException e) {
            log.error("Failed to look up Slack user channel by email: " + recipient, e);
            return Optional.empty();
        }
    }

    private Optional<SlackApiChannel> getDirectMessageChannelFromEmail(IntegrationKey key, String userEmail) throws SlackClientException {
        Optional<String> userIdOpt = lookUpUserByEmail(key, userEmail)
                .map(SlackApiUser::getId);
        if (userIdOpt.isEmpty()) {
            return Optional.empty();
        }
        return openImChannel(key, userIdOpt.get());
    }

    private Optional<SlackApiChannel> openImChannel(IntegrationKey key, String userId) throws SlackClientException {
        return slackBotClientFactory.get(key).openImChannel(userId);
    }

    private Optional<SlackApiUser> lookUpUserByEmail(IntegrationKey integrationKey, String userEmail) throws SlackClientException {
        return slackBotClientFactory.get(integrationKey).lookupUserByEmail(userEmail);
    }

    private void postMessage(IntegrationKey integrationKey, String text, String channelId, String botName) throws SlackClientException {
        slackBotClientFactory.get(integrationKey).postChatMessage(channelId, text, botName);
    }

    @Override
    public SlackChatMessageQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, SlackChatMessageQuery.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackChatMessageResult.SlackChatMessageResultBuilder.class)
    public static class SlackChatMessageResult implements ControllerIngestionResult {
        @JsonProperty("success")
        Boolean success;

        @JsonProperty("total_recipients")
        Integer totalRecipients;

        @JsonProperty("successfully_sent")
        Integer successfullySent;

        @JsonProperty("statuses")
        List<SlackMessagePostStatus> statuses;
    }
}
