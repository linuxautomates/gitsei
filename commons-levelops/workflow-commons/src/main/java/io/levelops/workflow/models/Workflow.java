package io.levelops.workflow.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Workflow.WorkflowBuilder.class)
public class Workflow {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("product_ids")
    List<String> productIds; // empty if applies to "all"

    @JsonProperty("release_ids")
    List<String> releaseIds;

    @JsonProperty("stage_ids")
    List<String> stageIds;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("ui_data")
    String uiData; // link to GCS

}
