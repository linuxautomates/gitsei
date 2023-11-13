package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraComment.JiraCommentBuilder.class)
public class JiraComment {

    @JsonProperty("self")
    String self;

    @JsonProperty("id")
    String id;

    @JsonProperty("author")
    JiraUser author;

    @JsonProperty("updateAuthor")
    JiraUser updateAuthor;

    @JsonProperty("body")
    Object body; // JiraContent

    @JsonProperty("body_text")
    public String getBodyText() {
        return JiraContent.toString(body);
    }

    @JsonProperty("created")
    Date created;

    @JsonProperty("updated")
    Date updated;

    @JsonProperty("visibility")
    JiraCommentVisibility visibility;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraCommentVisibility.JiraCommentVisibilityBuilder.class)
    public static class JiraCommentVisibility {

        @JsonProperty("type")
        String type;

        @JsonProperty("value")
        String value;
    }
}
