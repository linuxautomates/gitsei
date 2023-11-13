package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.aggregations.models.jenkins.JobRunParam;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Getter
@ToString
public class JobRunParamsMessage implements Comparable<JobRunParamsMessage> {
    @JsonProperty("build_number")
    private Long buildNumber;
    @JsonProperty("job_run_params")
    private List<JobRunParam> jobRunParams;

    @Override
    public int compareTo(JobRunParamsMessage o) {
        return (int) (this.buildNumber - o.getBuildNumber());
    }

}