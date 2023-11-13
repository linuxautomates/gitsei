package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobRunGitChangesMessage implements Comparable<JobRunGitChangesMessage> {
    @JsonProperty("build_number")
    private Long buildNumber;
    @JsonProperty("commit_ids")
    private List<String> commitIds;

    @Override
    public int compareTo(JobRunGitChangesMessage o) {
        return (int) (this.buildNumber - o.getBuildNumber());
    }
}
