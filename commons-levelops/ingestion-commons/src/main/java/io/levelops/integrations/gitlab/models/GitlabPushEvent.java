package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabPushEvent.GitlabPushEventBuilder.class)
public class GitlabPushEvent {

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

    @JsonProperty("push_data")
    GitlabPushData pushData;

    @JsonProperty("author_username")
    String authorUsername;

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

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabPushData.GitlabPushDataBuilder.class)
    public static class GitlabPushData {
        @JsonProperty("commit_count")
        Integer commitCount;
        @JsonProperty("action")
        String action;
        @JsonProperty("ref_type")
        String refType;
        @JsonProperty("commit_from")
        String commitFrom;
        @JsonProperty("commit_to")
        String commitTo;
        @JsonProperty("ref")
        String ref;
        @JsonProperty("commit_title")
        String commitTitle;
        @JsonProperty("ref_count")
        Integer refCount;
    }
}
