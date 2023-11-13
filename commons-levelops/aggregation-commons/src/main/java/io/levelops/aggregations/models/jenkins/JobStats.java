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
public class JobStats {
    @JsonProperty("name")
    private String name;
    @JsonProperty("avg_build_duration")
    private Long avgBuildDuration;
    @JsonProperty("mttr_all_failed_info")
    private Long mttrAllFailedInfo;
    @JsonProperty("mttf_all_builds")
    private Long mttfAllBuilds;
    @JsonProperty("std_dev_all_failed_info")
    private Long stdDevAllFailedInfo;
    @JsonProperty("success_count")
    private Integer successCount;
    @JsonProperty("un_stable_count")
    private Integer unStableCount;
    @JsonProperty("failure_count")
    private Integer failureCount;
    @JsonProperty("not_built_count")
    private Integer notBuiltCount;
    @JsonProperty("aborted_count")
    private Integer abortedCount;
}
