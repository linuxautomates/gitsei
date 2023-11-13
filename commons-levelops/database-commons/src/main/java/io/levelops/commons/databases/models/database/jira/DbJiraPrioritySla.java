package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraPrioritySla.DbJiraPrioritySlaBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraPrioritySla {
    @JsonProperty("id")
    String id;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("project")
    String project;

    @JsonProperty("issue_type") //in db its task_type :(
    String taskType;

    @JsonProperty("resp_sla")
    Long respSla;

    @JsonProperty("solve_sla")
    Long solveSla;
}
