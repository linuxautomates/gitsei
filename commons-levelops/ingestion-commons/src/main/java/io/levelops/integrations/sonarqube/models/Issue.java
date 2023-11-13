package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = Issue.IssueBuilder.class)
public class Issue {

    @JsonProperty("key")
    String key;

    @JsonProperty("rule")
    String rule;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("component")
    String component;

    @JsonProperty("project")
    String project;

    @JsonProperty("pullRequest")
    String pullRequest;

    @JsonProperty("line")
    long line;

    @JsonProperty("hash")
    String hash;

    @JsonProperty("textRange")
    TextRange textRange;

    @JsonProperty("flows")
    List<Flow> flows;

    @JsonProperty("status")
    String status;

    @JsonProperty("message")
    String message;

    @JsonProperty("effort")
    String effort;

    @JsonProperty("debt")
    String debt;

    @JsonProperty("author")
    String author;

    @JsonProperty("tags")
    List<String> tags;

    @JsonProperty("comments")
    List<Comment> comments;     //additional fields

    @JsonProperty("creationDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date creationDate;

    @JsonProperty("updateDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date updateDate;

    @JsonProperty("type")
    String type;

    @JsonProperty("organization")
    String organization;

    @JsonProperty("components")
    Project componentsByKey;

    @JsonProperty("transitions")
    List<String> transitions;   //additional fields

    @JsonProperty("rules")
    Rule rules;           //additional fields

    @JsonProperty("users")
    List<User> users;           //additional fields

    @JsonProperty("actions")
    List<String> actions;       //additional fields

}
