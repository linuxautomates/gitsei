package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobConfigChangeRequest {
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("branch_name")
    private String branchName;
    @JsonProperty("module_name")
    private String moduleName;
    @JsonProperty("job_full_name")
    private String jobFullName;
    @JsonProperty("job_normalized_full_name")
    private String jobNormalizedFullName;

    @JsonProperty("jenkins_instance_guid")
    private String jenkinsInstanceGuid;
    @JsonProperty("jenkins_instance_name")
    private String jenkinsInstanceName;
    @JsonProperty("jenkins_instance_url")
    private String jenkinsInstanceUrl;

    @JsonProperty("repo_url")
    private String repoUrl;
    @JsonProperty("scm_user_id")
    private String scmUserId;

    @JsonProperty("change_type")
    private String changeType;
    @JsonProperty("change_time")
    private long changeTime;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("users_name")
    private String usersName;
}
