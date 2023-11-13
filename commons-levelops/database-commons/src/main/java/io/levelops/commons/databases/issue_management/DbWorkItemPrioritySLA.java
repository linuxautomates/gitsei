package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbWorkItemPrioritySLA.DbWorkItemPrioritySLABuilder.class)
public class DbWorkItemPrioritySLA {

    @JsonProperty("id")
    String id;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("project")
    String project;

    @JsonProperty("workitem_type")
    String workitemType;

    @JsonProperty("resp_sla")
    Long respSla;

    @JsonProperty("solve_sla")
    Long solveSla;
}