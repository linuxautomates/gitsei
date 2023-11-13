package io.levelops.ingestion.integrations.slack.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackChatMessageQuery.SlackChatMessageQueryBuilder.class)
public class SlackChatMessageQuery implements DataQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("text")
    String text;

    @JsonProperty("recipients")
    List<String> recipients; // can contain channel ids, channel names, or user emails

    @JsonProperty("bot_name")
    String botName; // optional
}
