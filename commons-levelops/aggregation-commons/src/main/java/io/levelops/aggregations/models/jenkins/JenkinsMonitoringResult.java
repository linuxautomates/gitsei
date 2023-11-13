package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsMonitoringResult {
    @JsonProperty("jenkins_instance_guid")
    private UUID jenkinsInstanceGuid;

    @JsonProperty("jenkins_instance_name")
    private String jenkinsInstanceName;

    @JsonProperty("jenkins_instance_url")
    private String jenkinsInstanceUrl;

    //All Jobs All Config Changes
    @JsonProperty("config_changes")
    @Singular
    private List<JobAllConfigChanges> configChanges;

    //All Jobs All Runs
    @JsonProperty("job_runs")
    @Singular
    private List<JobAllRuns> jobRuns;
}
