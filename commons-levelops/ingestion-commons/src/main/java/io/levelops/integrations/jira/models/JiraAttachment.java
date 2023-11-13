package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraAttachment.JiraAttachmentBuilder.class)
public class JiraAttachment {

    @JsonProperty("self")
    String self;

    @JsonProperty("id")
    String id;

    @JsonProperty("filename")
    String filename;

    @JsonProperty("author")
    JiraUser author;

    @JsonProperty("created")
    Date created;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("mime_type")
    String mimeType;

    @JsonProperty("content")
    String content;

    @JsonProperty("thumbnail")
    String thumbnail;
}
