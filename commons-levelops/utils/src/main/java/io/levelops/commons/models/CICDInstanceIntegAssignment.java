package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Set;

@Value
@JsonDeserialize(builder = CICDInstanceIntegAssignment.CICDInstanceIntegAssignmentBuilder.class)
public class CICDInstanceIntegAssignment {
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("add")
    Set<String> addIds;
    @JsonProperty("remove")
    Set<String> removeIds;

    @Builder(toBuilder = true)
    public CICDInstanceIntegAssignment(@JsonProperty("integration_id") String integrationId,
                                       @JsonProperty("add") Set<String> addIds,
                                       @JsonProperty("remove") Set<String> removeIds) {
        this.integrationId = integrationId;
        this.addIds = (addIds != null) ? addIds : Collections.emptySet();
        this.removeIds = (removeIds != null) ? removeIds : Collections.emptySet();
    }
}