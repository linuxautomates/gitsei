package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIBuild.DroneCIBuildBuilder.class)
public class DroneCIBuild {
    @JsonProperty("id")
    Long id;

    @JsonProperty("repo_id")
    Long repoId;

    @JsonProperty("trigger")
    String trigger;

    @JsonProperty("number")
    Long number;

    @JsonProperty("status")
    String status;

    @JsonProperty("event")
    String event;

    @JsonProperty("action")
    String action;

    @JsonProperty("link")
    String link;

    @JsonProperty("timestamp")
    Long timestamp;

    @JsonProperty("message")
    String message;

    @JsonProperty("before")
    String before;

    @JsonProperty("after")
    String after;

    @JsonProperty("ref")
    String ref;

    @JsonProperty("source_repo")
    String sourceRepo;

    @JsonProperty("source")
    String source;

    @JsonProperty("target")
    String target;

    @JsonProperty("author_login")
    String authorLogin;

    @JsonProperty("author_name")
    String authorName;

    @JsonProperty("author_email")
    String authorEmail;

    @JsonProperty("author_avatar")
    String authorAvatar;

    @JsonProperty("sender")
    String sender;

    @JsonProperty("cron")
    String cron;

    @JsonProperty("deploy_to")
    String deployTo;

    @JsonProperty("started")
    Long started;

    @JsonProperty("finished")
    Long finished;

    @JsonProperty("created")
    Long created;

    @JsonProperty("updated")
    Long updated;

    @JsonProperty("version")
    Long version;

    @JsonProperty("stages")
    List<DroneCIBuildStage> stages;
}
