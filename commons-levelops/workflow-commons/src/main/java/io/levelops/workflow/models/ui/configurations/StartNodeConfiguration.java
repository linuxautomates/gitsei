package io.levelops.workflow.models.ui.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = StartNodeConfiguration.StartNodeConfigurationBuilder.class)
public class StartNodeConfiguration implements NodeConfiguration {

    @JsonProperty("product_ids")
    List<String> productIds;

    @JsonProperty("release_ids")
    List<String> releaseIds;

    @JsonProperty("severity")
    String severity; //<low|medium|high>

    @JsonProperty("stage_ids")
    List<String> stageIds;
}
