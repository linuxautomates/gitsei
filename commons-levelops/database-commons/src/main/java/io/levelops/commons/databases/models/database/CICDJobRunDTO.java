package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJobRunDTO.CICDJobRunDTOBuilder.class)
public class CICDJobRunDTO {
    @JsonProperty("id")
    private final UUID id;

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

    @JsonProperty("job_normalized_full_name")
    private final String jobNormalizedFullName;

    @JsonProperty("project_name")
    private final String projectName;

    @JsonProperty("integration_id")
    private final String integrationId;

    @JsonProperty("scm_commit_ids")
    private final List<String> scmCommitIds;

    @JsonProperty("scm_url")
    private final String scmUrl;

    @JsonProperty("log_gcspath")
    private final String logGcspath;

    @JsonProperty("cicd_instance_name")
    private final String cicdInstanceName;

    @JsonProperty("cicd_build_url")
    private final String cicdBuildUrl;

    @JsonProperty("cicd_instance_guid")
    private final UUID cicdInstanceGuid;

    @JsonProperty("url")
    private final String url;

    @JsonProperty("logs")
    private final Boolean logs;

    @JsonProperty("env_ids")
    private final List<String> environmentIds;

    @JsonProperty("infra_ids")
    private final List<String> infraIds;

    @JsonProperty("service_ids")
    private final List<String> serviceIds;

    @JsonProperty("service_types")
    private final List<String> serviceTypes;

    @JsonProperty("tags")
    private final List<String> tags;

    @JsonProperty("repo_url")
    private final String repoUrl;

    @JsonProperty("cicd_branch")
    private final String cicdBranch;

    @JsonProperty("rollback")
    private final Boolean rollBack;
}
