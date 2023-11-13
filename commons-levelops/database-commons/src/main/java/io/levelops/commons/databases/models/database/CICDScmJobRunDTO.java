package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDScmJobRunDTO.CICDScmJobRunDTOBuilder.class)
public class CICDScmJobRunDTO {
    @JsonProperty("id")
    private final UUID id;

    @JsonIgnore
    private final UUID buildId;

    @JsonProperty("cicd_job_id")
    private final UUID cicdJobId;
    @JsonProperty("job_run_number")
    private final Long jobRunNumber;
    @JsonProperty("status")
    private final String status;

    @JsonProperty("start_time")
    private final Instant startTime;
    @JsonProperty("duration")
    private final Integer duration;
    @JsonProperty("end_time")
    private final Instant endTime;
    @JsonProperty("cicd_user_id")
    private final String cicdUserId;

    @JsonProperty("params")
    private final List<CICDJobRun.JobRunParam> params;

    @JsonProperty("job_name")
    private final String jobName;

    @JsonProperty("cicd_instance_name")
    private final String cicdInstanceName;

    @JsonProperty("cicd_instance_type")
    private final String cicdInstanceType;

    @JsonProperty("cicd_instance_guid")
    private final UUID cicdInstanceGuid;

    @JsonProperty("job_normalized_full_name")
    private final String jobNormalizedFullName;

    @JsonProperty("project_name")
    private final String projectName;

    @JsonProperty("scm_integration_id")
    private final String scmIntegrationId;

    @JsonProperty("cicd_integration_id")
    private final String cicdIntegrationId;

    @JsonProperty("initial_commit_to_deploy_time")
    private final Long initialCommitToDeployTime;

    @JsonProperty("lines_modified")
    private final Integer linesModified;

    @JsonProperty("files_modified")
    private final Integer filesModified;

    @JsonProperty("scm_url")
    private final String scmUrl;

    @JsonProperty("build_run_ids")
    private final List<UUID> buildRunIds;

    @JsonProperty("commits")
    private final List<ScmCommit> commits;



    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ScmCommit.ScmCommitBuilder.class)
    public static class ScmCommit {
        @JsonProperty("commit_id")
        private String commitId;

        @JsonProperty("repo_id")
        private List<String> repoIds; //repository id -- something like: "levelops/api-levelops"

        @JsonProperty("commit_url")
        private String commitUrl;

        @JsonProperty("integration_id")
        private String integrationId;

        @JsonProperty("author")
        private String author; //name from git author field

        @JsonProperty("message")
        private String message; //commit msg

        @JsonProperty("files_changed_count")
        private Integer filesChangedCount;

        @JsonProperty("lines_added_count")
        private Integer linesAddedCount;

        @JsonProperty("lines_removed_count")
        private Integer linesRemovedCount;

        @JsonProperty("committed_at")
        private Long committedAt;
    }
}
