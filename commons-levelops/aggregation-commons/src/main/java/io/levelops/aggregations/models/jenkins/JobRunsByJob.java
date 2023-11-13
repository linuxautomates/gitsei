package io.levelops.aggregations.models.jenkins;

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
public class JobRunsByJob implements Comparable<JobRunsByJob>{
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("runs_count")
    private Integer runsCount;
    @JsonProperty("details_by_user")
    private List<DetailsByUser> detailsByUser;

    @Override
    public int compareTo(JobRunsByJob o) {
        return o.getRunsCount() - this.getRunsCount();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetailsByUser implements Comparable<DetailsByUser>{
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("runs_count")
        private int runsCount;

        @Override
        public int compareTo(DetailsByUser o) {
            return o.getRunsCount() - this.getRunsCount();
        }
    }
}