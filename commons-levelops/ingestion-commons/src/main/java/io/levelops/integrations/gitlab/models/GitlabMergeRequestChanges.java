package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabMergeRequestChanges.GitlabMergeRequestChangesBuilder.class)
public class GitlabMergeRequestChanges {
    @JsonProperty("subscribed")
    boolean subscribed;
    @JsonProperty("changes_count")
    int changesCount;
    @JsonProperty("latest_build_started_at")
    Date latestBuildStartedAt;
    @JsonProperty("latest_build_finished_at")
    Date lastBuildFinishedAt;
    @JsonProperty("first_deployed_to_production_at")
    Date firstDeployedToProductionAt;
    @JsonProperty("pipeline")
    GitlabPipeline pipeline;
    @JsonProperty("head_pipeline")
    GitlabPipeline headPipeline;
    @JsonProperty("diff_refs")
    DiffRefs diffRefs;
    @JsonProperty("merge_error")
    String mergeError;
    @JsonProperty("user")
    CanMerge user;
    @JsonProperty("changes")
    List<GitlabChange> changes;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DiffRefs.DiffRefsBuilder.class)
    public static class DiffRefs {
        @JsonProperty("base_sha")
        String baseSha;
        @JsonProperty("head_sha")
        String headSha;
        @JsonProperty("start_sha")
        String startSha;
    }
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CanMerge.CanMergeBuilder.class)
    public static class CanMerge {
        @JsonProperty("can_merge")
        boolean canMerge;
    }
}

