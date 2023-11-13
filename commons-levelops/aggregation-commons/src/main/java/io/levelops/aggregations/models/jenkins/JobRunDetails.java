package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobRunDetails implements Comparable<JobRunDetails>{
    @JsonProperty("number")
    private Long number;
    @JsonProperty("status")
    private String status;
    @JsonProperty("start_time")
    private Long startTime;
    @JsonProperty("duration")
    private Long duration;
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("commit_ids")
    @Singular
    private List<String> commitIds;

    @JsonProperty("params")
    @Singular
    private List<JobRunParam> params;
    @JsonProperty("triggers")
    @Singular
    private Set<CICDJobTrigger> triggers;

    @Override
    public int compareTo(@NotNull JobRunDetails o) {
        return (int) (this.getNumber() - o.getNumber());
    }
}

