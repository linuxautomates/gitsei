package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaProject.CxScaProjectBuilder.class)
public class CxScaProject {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("assignedTeams")
    List<String> assignedTeams;

    @JsonProperty("Branch")
    String branch;

    @JsonProperty("isManaged")
    Boolean isManaged;

    @JsonProperty("riskReport")
    CxScaRiskReport riskReport;

}
