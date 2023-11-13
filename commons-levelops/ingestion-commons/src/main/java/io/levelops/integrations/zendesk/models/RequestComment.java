package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RequestComment.RequestCommentBuilder.class)
public class RequestComment {

    @JsonProperty
    Long id;

    @JsonProperty
    String type;

    @JsonProperty("request_id")
    Long requestId;

    @JsonProperty
    String body;

    @JsonProperty("html_body")
    String htmlBody;

    @JsonProperty("plain_body")
    String plainBody;

    @JsonProperty("public")
    Boolean publicComment;

    @JsonProperty("author_id")
    Long authorId;

    @JsonProperty
    List<Attachment> attachments;

    @JsonProperty("created_at")
    Date createdAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Attachment.AttachmentBuilder.class)
    public static class Attachment {

        @JsonProperty
        Long id;

        @JsonProperty("file_name")
        String fileName;

        @JsonProperty("content_url")
        String contentUrl;

        @JsonProperty("content_type")
        String contentType;

        @JsonProperty
        Long size;

        @JsonProperty
        List<Attachment> thumbnails;

        @JsonProperty
        Boolean inline;

        @JsonProperty
        Boolean deleted;
    }
}
