package io.levelops.integrations.zendesk.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TicketRequestBody.TicketRequestBodyBuilder.class)
public class TicketRequestBody {

    @JsonProperty
    Ticket ticket;

    @JsonProperty
    TicketComment comment;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TicketComment.TicketCommentBuilder.class)
    public static class TicketComment {

        @JsonProperty
        Long id;

        @JsonProperty
        String type;

        @JsonProperty
        String body;

        @JsonProperty("html_body")
        String htmlBody;

        @JsonProperty("plain_body")
        String plainBody;

        @JsonProperty("public")
        Boolean isPublic;

        @JsonProperty("author_id")
        Long authorId;

        @JsonProperty
        List<RequestComment.Attachment> attachments;

        @JsonProperty
        List<Upload> uploads;

        @JsonProperty
        Ticket.Via via;

        @JsonProperty("created_at")
        Date createdAt;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Upload.UploadBuilder.class)
        public static class Upload {

            @JsonProperty
            String token;

            @JsonProperty
            RequestComment.Attachment attachment;

            @JsonProperty
            List<RequestComment.Attachment> attachments;

        }
    }
}
