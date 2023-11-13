package io.levelops.aggregations.models.jenkins;

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
public class JobNameDetails {
    @JsonProperty("job_name")
    private String jobName;

    @JsonProperty("branch_name")
    private String branchName;

    @JsonProperty("job_full_name")
    private String jobFullName;

    @JsonProperty("job_normalized_full_name")
    private String jobNormalizedFullName;

    @JsonProperty("module_name")
    private String moduleName;
}
