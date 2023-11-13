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
public class ConfigChangesByJob implements Comparable<ConfigChangesByJob>{
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("changes_count")
    private Integer changesCount;
    @JsonProperty("details_by_user")
    private List<DetailsByUser> detailsByUser;

    @Override
    public int compareTo(ConfigChangesByJob o) {
        return o.getChangesCount() - this.getChangesCount();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetailsByUser implements Comparable<DetailsByUser>{
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("changes_count")
        private int changesCount;

        @Override
        public int compareTo(DetailsByUser o) {
            return o.getChangesCount() - this.getChangesCount();
        }
    }
}