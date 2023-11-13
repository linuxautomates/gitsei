package io.levelops.workflow.models.ui;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkflowUiData.WorkflowUiDataBuilder.class)
public class WorkflowUiData {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("nodes")
    Map<String, Node> nodes;

    @JsonProperty("links")
    Map<String, Link> links;

    public enum NodeType {
        START,
        CONDITION,
        WAIT,
        ACTION_EMAIL(true),
        ACTION_SLACK(true),
        ACTION_KB(true),
        ACTION_QUESTIONNAIRE(true),
        ACTION_WORKITEM(true);

        private final boolean action;

        NodeType() {
            this(false);
        }

        NodeType(boolean action) {
            this.action = action;
        }

        public boolean isAction() {
            return action;
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        public static NodeType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(NodeType.class, value);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Node.NodeBuilder.class)
    public static class Node {
        @JsonProperty("id")
        String id;

        @JsonProperty("type")
        String type; // start, ...

        @JsonProperty("ports")
        Map<String, Port> ports;

        @JsonProperty("properties")
        NodeProperties properties;

        @JsonIgnore
        public <T> Optional<T> getConfiguration(ObjectMapper objectMapper, Class<T> clazz) {
            return Optional.ofNullable(properties)
                    .map(NodeProperties::getConfigurations)
                    .map(configMap -> objectMapper.convertValue(configMap, clazz));
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = NodeProperties.NodePropertiesBuilder.class)
    public static class NodeProperties {
        @JsonProperty("type")
        String type; // start, condition, ...

        @JsonProperty("name")
        String name;

        @JsonProperty("configurations")
        Map<String, Object> configurations; // polymorphic based on type
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Port.PortBuilder.class)
    public static class Port {
        @JsonProperty("id")
        String id;

        @JsonProperty("properties")
        PortProperties properties;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PortProperties.PortPropertiesBuilder.class)
    public static class PortProperties {
        @JsonProperty("action")
        String action;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Link.LinkBuilder.class)
    public static class Link {
        @JsonProperty("id")
        String id;

        @JsonProperty("from")
        NodePort from;

        @JsonProperty("to")
        NodePort to;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = NodePort.NodePortBuilder.class)
    public static class NodePort {
        @JsonProperty("nodeId")
        String nodeId;

        @JsonProperty("portId")
        String portId;
    }

}
