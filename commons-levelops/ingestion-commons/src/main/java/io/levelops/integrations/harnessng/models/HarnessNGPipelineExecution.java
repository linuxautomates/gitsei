package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGPipelineExecution.HarnessNGPipelineExecutionBuilder.class)
public class HarnessNGPipelineExecution {

    @JsonProperty("pipelineExecutionSummary")
    HarnessNGPipeline pipeline;

    @JsonProperty("executionGraph")
    ExecutionGraph executionGraph;

    @JsonProperty("inputSet")
    HarnessNGExecutionInputSet inputSet; // enrichment


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ExecutionGraph.ExecutionGraphBuilder.class)
    public static class ExecutionGraph {
        @JsonProperty("rootNodeId")
        String rootNodeId;

        @JsonProperty("nodeMap")
        Map<String, HarnessNGPipelineStageStep> nodeMap;

        @JsonProperty("nodeAdjacencyListMap")
        Map<String, NodeAdjacency> nodeAdjacencyListMap;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = NodeAdjacency.NodeAdjacencyBuilder.class)
        public static class NodeAdjacency {
            @JsonProperty("children")
            List<String> children;

            @JsonProperty("nextIds")
            List<String> nextIds;
        }
    }
}
