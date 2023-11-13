package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobConfigChangeDetail implements Comparable<JobConfigChangeDetail> {
    @JsonProperty("change_time")
    private Long changeTime;
    @JsonProperty("operation")
    private String operation;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("user_full_name")
    private String userFullName;

    @Override
    public int compareTo(@NotNull JobConfigChangeDetail o) {
        return (int) (this.getChangeTime() - o.getChangeTime());
    }
}