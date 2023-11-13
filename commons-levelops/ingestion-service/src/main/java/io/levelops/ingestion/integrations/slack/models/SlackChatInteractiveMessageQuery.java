package io.levelops.ingestion.integrations.slack.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.DataQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackChatInteractiveMessageQuery.SlackChatInteractiveMessageQueryBuilder.class)
public class SlackChatInteractiveMessageQuery implements DataQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("work_item_id")
    UUID workItemId;

    @JsonProperty("questionnaire_id")
    UUID questionnaireId;

    @JsonProperty("text")
    String text;

    @JsonProperty("recipients")
    List<String> recipients; // can contain channel ids, channel names, or user emails

    @JsonProperty("bot_name")
    String botName; // optional

    @JsonProperty("file_uploads")
    List<FileUpload> fileUploads;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FileUpload.FileUploadBuilder.class)
    public static class FileUpload {
        @JsonProperty("file_name")
        String fileName;
        @JsonProperty("file_content")
        String fileContent;
    }
}
