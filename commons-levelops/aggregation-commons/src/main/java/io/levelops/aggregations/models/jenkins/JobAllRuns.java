package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({ "job_name", "job_full_name", "job_normalized_full_name", "branch_name", "module_name", "scm_url", "scm_user_id", "runs" })
public class JobAllRuns {
    @JsonProperty("job_name")
    private String jobName;

    @JsonProperty("job_full_name")
    private String jobFullName;

    @JsonProperty("job_normalized_full_name")
    private String jobNormalizedFullName;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("module_name")
    private String moduleName;

    @JsonProperty("scm_url")
    private String scmUrl;

    @JsonProperty("scm_user_id")
    private String scmUserId;

    @JsonProperty("runs")
    @Singular
    private List<JobRunDetails> runs;
}
