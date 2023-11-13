package io.levelops.integrations.slack.sinks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.PushException;
import io.levelops.ingestion.sinks.DataSink;
import io.levelops.ingestion.sinks.SinkIngestionResult;
import io.levelops.integrations.slack.client.SlackClientException;
import io.levelops.integrations.slack.client.SlackBotClientFactory;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.stream.Stream;

@Log4j2
public class SlackMessageSink implements DataSink<SlackMessageSink.SlackMessageData, SlackMessageSink.SlackMessageResult> {

    private final SlackBotClientFactory slackBotClientFactory;

    @Builder
    public SlackMessageSink(SlackBotClientFactory slackBotClientFactory) {
        this.slackBotClientFactory = slackBotClientFactory;
    }

    @Override
    public SlackMessageResult pushOne(Data<SlackMessageData> data) throws PushException {
        SlackMessageData payload = data.getPayload();
        try {
            slackBotClientFactory.get(payload.getIntegrationKey())
                    .postChatMessage(payload.getChannelId(), payload.getText(), null);
            return SlackMessageResult.builder()
                    .ok(true)
                    .build();
        } catch (SlackClientException e) {
            throw new PushException(e);
        }
    }

    @Override
    public SlackMessageResult pushMany(Stream<Data<SlackMessageData>> dataStream) throws PushException {
        dataStream.forEach(data -> {
            try {
                pushOne(data);
            } catch (PushException e) {
                log.warn("Failed to push slack message to channel {}", data.getPayload().getChannelId());
            }
        });
        return SlackMessageResult.builder()
                .ok(true)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackMessageData.SlackMessageDataBuilder.class)
    public static class SlackMessageData {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("channel_id")
        String channelId;

        @JsonProperty("text")
        String text;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SlackMessageResult.SlackMessageResultBuilder.class)
    public static class SlackMessageResult implements SinkIngestionResult {
        @JsonProperty("ok")
        Boolean ok;
    }
}
