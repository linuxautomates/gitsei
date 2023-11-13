package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IssueMgmtSprintMappingAggResult.IssueMgmtSprintMappingAggResultBuilder.class)
public class IssueMgmtSprintMappingAggResult {
    @JsonProperty("sprint_mapping")
    DbIssueMgmtSprintMapping sprintMapping;
    @JsonProperty("workitem_type")
    String workitemType;
}
