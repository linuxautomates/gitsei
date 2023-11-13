package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabEvent.GitlabEventBuilder.class)
public class GitlabEvent {
    public static Map<String, String> GITLAB_MR_EVENTS_TO_SCM_MAPPINGS = Map.of(
        // "", "UNAPPROVED",
        // "", "REQUESTED",
        // "", "CHANGES_REQUESTED",
        // "", "REQUESTED_FURTHER_REVIEW_OF"
        "approved", "APPROVED",
//        "accepted", "APPROVED", // LEV-5226 not sure what accepted is, but it is definitely not the same as approved
        "commented on", "COMMENTED",
        "commented", "COMMENTED",
        "closed", "DECLINED"
        );

    @JsonProperty("id")
    String id;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("action_name")
    String actionName;

    @JsonProperty("target_id")
    String targetId;

    @JsonProperty("target_iid")
    String targetIid;

    @JsonProperty("target_type")
    String targetType;

    @JsonProperty("author_id")
    String authorId;

    @JsonProperty("target_title")
    String targetTitle;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("author")
    GitlabEventAuthor author;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabEventAuthor.GitlabEventAuthorBuilder.class)
    public static class GitlabEventAuthor {
        @JsonProperty("id")
        String id;

        @JsonProperty("name")
        String authorName;

        @JsonProperty("username")
        String username;

        @JsonProperty("state")
        String state;
    }

    @JsonProperty("author_username")
    String authorUsername;
}
