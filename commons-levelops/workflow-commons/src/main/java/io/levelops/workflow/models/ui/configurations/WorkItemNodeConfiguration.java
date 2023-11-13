package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemNodeConfiguration.WorkItemNodeConfigurationBuilder.class)
public class WorkItemNodeConfiguration {
    @JsonProperty("assignee_ids")
    List<String> assignee_ids;

    @JsonProperty("priority")
    String priority; // <LOW|MEDIUM|HIGH>

    @JsonProperty("status")
    String status; // <OPEN|CLOSED|IN_PROGRESS>,

    @JsonProperty("notification")
    String notification; // <EMAIL|SLACK>
}
