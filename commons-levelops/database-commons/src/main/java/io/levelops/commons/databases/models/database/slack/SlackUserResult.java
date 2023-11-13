package io.levelops.commons.databases.models.database.slack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackUserResult.SlackUserResultBuilder.class)
public class SlackUserResult {
    @JsonProperty("success")
    Boolean success;
    @JsonProperty("found")
    Boolean found;

    @JsonProperty("slack_user_id")
    String slackUserId;

    @JsonProperty("work_item_note_id")
    UUID workItemNoteId;

    @JsonProperty("slack_user")
    SlackUser slackUser;
}
