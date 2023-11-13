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
public class JobRunsByUser implements Comparable<JobRunsByUser>{
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("runs_count")
    private int runsCount;
    @JsonProperty("details_by_job")
    private List<DetailsByJob> detailsByJob;

    @Override
    public int compareTo(JobRunsByUser o) {
        return o.getRunsCount() - this.getRunsCount();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetailsByJob implements Comparable<DetailsByJob>{
        @JsonProperty("job_name")
        private String jobName;
        @JsonProperty("runs_count")
        private Integer runsCount;

        @Override
        public int compareTo(DetailsByJob o) {
            return o.getRunsCount() - this.getRunsCount();
        }
    }

}