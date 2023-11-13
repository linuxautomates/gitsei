package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Comment.CommentBuilder.class)
public class Comment {

    @JsonProperty("key")
    String key;

    @JsonProperty("login")
    String login;

    @JsonProperty("htmlText")
    String htmlText;

    @JsonProperty("markdown")
    String markdown;

    @JsonProperty("updatable")
    Boolean updatable;

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date createdAt;

}
