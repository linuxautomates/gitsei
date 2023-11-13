package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Date;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabNote.GitlabNoteBuilder.class)
public class GitlabNote {
    @JsonProperty("id")
    String id;

    @JsonProperty("type")
    String type;

    @JsonProperty("body")
    String body;

    @JsonProperty("attachment")
    String attachment;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("system")
    Boolean system;

    @JsonProperty("noteable_id")
    String noteableId;

    @JsonProperty("noteable_type")
    String noteableType;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("resolvable")
    Boolean resolvable;

    @JsonProperty("confidential")
    Boolean confidential;

    @JsonProperty("internal")
    Boolean internal;

    @JsonProperty("noteable_iid")
    String noteableIid;

    @JsonProperty("author")
    GitlabEvent.GitlabEventAuthor author;

    @JsonIgnore
    public Optional<GitlabEvent> toEvent() {
        boolean isSystemEvent = BooleanUtils.isNotFalse(getSystem());
        boolean isApprovedComment = isSystemEvent && getBody().equalsIgnoreCase("approved this merge request");
        if (isSystemEvent && !isApprovedComment) {
            return Optional.empty();
        }
        return Optional.of(GitlabEvent.builder()
                .id(id)
                .projectId(projectId)
                .actionName(isApprovedComment ? "approved" : "commented on")
                .targetId(noteableId)
                .targetIid(noteableId)
                .targetType("Note")
                .authorId(author.getId())
                .targetTitle(body)
                .createdAt(createdAt)
                .author(author)
                .authorUsername(author.getUsername())
                .build());
    }
}
