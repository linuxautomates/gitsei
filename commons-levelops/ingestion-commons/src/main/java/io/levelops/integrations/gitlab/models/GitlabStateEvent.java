package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabStateEvent.GitlabStateEventBuilder.class)
public class GitlabStateEvent {
    @JsonProperty("id")
    String id;
    @JsonProperty("user")
    GitlabUser user;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("resource_type")
    String resourceType;
    @JsonProperty("resource_id")
    String resourceId;
    @JsonProperty("state")
    String state;

    public Optional<GitlabEvent> toEvent() {
        if (!state.equalsIgnoreCase("closed") || !resourceType.equalsIgnoreCase("MergeRequest")) {
            return Optional.empty();
        }
        return Optional.of(GitlabEvent.builder()
                .id(id)
                .actionName("closed")
                .targetId(resourceId)
                .author(user.toGitlabEventAuthor())
                .createdAt(createdAt)
                .build());
    }
}
