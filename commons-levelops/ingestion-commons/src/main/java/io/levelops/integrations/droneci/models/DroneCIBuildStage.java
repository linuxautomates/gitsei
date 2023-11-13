package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIBuildStage.DroneCIBuildStageBuilder.class)
public class DroneCIBuildStage {
    @JsonProperty("id")
    Long id;

    @JsonProperty("repo_id")
    Long repoId;

    @JsonProperty("build_id")
    Long buildId;

    @JsonProperty("number")
    Long number;

    @JsonProperty("name")
    String name;

    @JsonProperty("kind")
    String kind;

    @JsonProperty("type")
    String type;

    @JsonProperty("status")
    String status;

    @JsonProperty("error")
    String error;

    @JsonProperty("errignore")
    Boolean errIgnore;

    @JsonProperty("exit_code")
    Long exitCode;

    @JsonProperty("machine")
    String machine;

    @JsonProperty("os")
    String os;

    @JsonProperty("arch")
    String arch;

    @JsonProperty("started")
    Long started;

    @JsonProperty("stopped")
    Long stopped;

    @JsonProperty("created")
    Long created;

    @JsonProperty("updated")
    Long updated;

    @JsonProperty("version")
    Long version;

    @JsonProperty("on_success")
    Boolean onSuccess;

    @JsonProperty("on_failure")
    Boolean onFailure;

    @JsonProperty("stage_url")
    String stageUrl;

    @JsonProperty("steps")
    List<DroneCIBuildStep> steps;
}
