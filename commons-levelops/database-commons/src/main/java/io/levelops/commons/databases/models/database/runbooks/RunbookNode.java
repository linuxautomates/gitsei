package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.MapUtils;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookNode.RunbookNodeBuilder.class)
public class RunbookNode {

    @JsonProperty("id")
    String id;
    @JsonProperty("node_handler")
    String nodeHandler;
    @JsonProperty("type")
    String type;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty("from_nodes")
    Map<String, NodeTransition> fromNodes; // upstream nodes (transitions by node_id)
    @JsonProperty("to_nodes")
    Map<String, NodeTransition> toNodes; // downstream nodes (transitions by node_id)
    @JsonProperty("input")
    Map<String, RunbookVariable> input;
    @JsonProperty("ui_data")
    Map<String, Object> uiData;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = NodeTransition.NodeTransitionBuilder.class)
    public static class NodeTransition {
        /**
         * If provided, the downstream node will only be triggered if the given option was selected.
         * (this is read by upstream node)
         */
        @JsonProperty("option")
        String option;

        /**
         * If True, the node will wait for its upstream to be done.
         * (this is read by downstream node)
         */
        @JsonProperty("wait")
        Boolean wait;

        public static NodeTransition mustWait() {
            return NodeTransition.builder()
                    .wait(true)
                    .build();
        }

        public static NodeTransition dontWait() {
            return NodeTransition.builder()
                    .wait(false)
                    .build();
        }

        public static NodeTransition option(String option) {
            return NodeTransition.builder()
                    .option(option)
                    .build();
        }
    }

    public static class RunbookNodeBuilder {

        public RunbookNodeBuilder inputVariable(RunbookVariable variable) {
            this.input = MapUtils.append(this.input, variable.getName(), variable);
            return this;
        }

        public RunbookNodeBuilder from(String nodeId, NodeTransition transition) {
            this.fromNodes = MapUtils.append(this.fromNodes, nodeId, transition);
            return this;
        }

        public RunbookNodeBuilder to(String nodeId, NodeTransition transition) {
            this.toNodes = MapUtils.append(this.toNodes, nodeId, transition);
            return this;
        }

    }
}
