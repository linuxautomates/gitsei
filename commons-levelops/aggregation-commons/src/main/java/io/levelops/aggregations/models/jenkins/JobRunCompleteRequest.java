package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
@JsonDeserialize(builder = JobRunCompleteRequest.JobRunCompleteRequestBuilder.class)
public class JobRunCompleteRequest {
    @JsonProperty("job_name")
    @JsonAlias("pipeline")
    private final String jobName;

    @JsonProperty("user_id")
    @JsonAlias("triggered_by")
    private final String userId;

    @JsonProperty("job_run_params")
    @JsonAlias("execution_parameters")
    private final List<JobRunParam> jobRunParams;

    @JsonProperty("repo_url")
    private final String repoUrl;

    @JsonProperty("scm_user_id")
    private final String scmUserId;

    @JsonProperty("start_time")
    private final long startTime;

    @JsonProperty("result")
    private final String result;

    @JsonProperty("duration")
    private final long duration;

    @JsonProperty("build_number")
    private final long buildNumber;

    @JsonProperty("jenkins_instance_guid")
    @JsonAlias("instance_guid")
    private final String jenkinsInstanceGuid;

    @JsonProperty("jenkins_instance_name")
    @JsonAlias("instance_name")
    private final String jenkinsInstanceName;

    @JsonProperty("jenkins_instance_url")
    @JsonAlias("instance_url")
    private final String jenkinsInstanceUrl;

    @JsonProperty("job_run")
    private final JobRun jobRun;

    @JsonProperty("product_ids")
    private final List<String> productIds;

    @JsonProperty("job_full_name")
    private String jobFullName;

    @JsonProperty("job_normalized_full_name")
    @JsonAlias("qualified_name")
    private String jobNormalizedFullName;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("module_name")
    private String moduleName;

    @JsonProperty("scm_commit_ids")
    private final List<String> scmCommitIds;

    @JsonProperty("trigger_chain")
    private final Set<CICDJobTrigger> triggerChain;

    @JsonProperty("ci")
    private final Boolean ci;

    @JsonProperty("cd")
    private final Boolean cd;

    @JsonProperty("artifacts")
    private final List<CiCdJobRunArtifact> artifacts;
}
