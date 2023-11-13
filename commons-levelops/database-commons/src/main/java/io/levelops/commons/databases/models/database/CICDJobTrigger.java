package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = CICDJobTrigger.CICDJobTriggerBuilder.class)
public class CICDJobTrigger {
    @JsonProperty("id")
    private String id;

    @JsonProperty("job_run_number")
    private String buildNumber;

    @JsonProperty("type")
    private String type;

    @JsonProperty("direct_parents")
    private Set<CICDJobTrigger> directParents;

}