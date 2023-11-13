package io.levelops.ingestion.integrations.slack.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackUserQuery.SlackUserQueryBuilder.class)
public class SlackUserQuery implements DataQuery  {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("work_item_note_id")
    UUID workItemNoteId;

    @JsonProperty("slack_user_id")
    String slackUserId;
}
