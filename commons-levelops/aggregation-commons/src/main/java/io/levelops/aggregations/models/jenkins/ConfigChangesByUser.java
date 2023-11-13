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
public class ConfigChangesByUser implements Comparable<ConfigChangesByUser>{
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("changes_count")
    private int changesCount;
    @JsonProperty("details_by_job")
    private List<DetailsByJob> detailsByJob;

    @Override
    public int compareTo(ConfigChangesByUser o) {
        return o.getChangesCount() - this.getChangesCount();
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetailsByJob implements Comparable<DetailsByJob>{
        @JsonProperty("job_name")
        private String jobName;
        @JsonProperty("changes_count")
        private Integer changesCount;

        @Override
        public int compareTo(DetailsByJob o) {
            return o.getChangesCount() - this.getChangesCount();
        }
    }
}