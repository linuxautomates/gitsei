package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabIssueNote.GitlabIssueNoteBuilder.class)
public class GitlabIssueNote {
    @JsonProperty("id")
    String id;
    @JsonProperty("type")
    String type;
    @JsonProperty("body")
    String body;
    @JsonProperty("attachment")
    String attachment;
    @JsonProperty("author")
    GitlabUser author;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("system")
    boolean system;
    @JsonProperty("noteable_id")
    String noteableId;
    @JsonProperty("noteable_type")
    String noteableType;
    @JsonProperty("resolvable")
    boolean resolvable;
    @JsonProperty("confidential")
    boolean confidential;
    @JsonProperty("noteable_iid")
    String noteableIID;
}
