package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookDTO.RunbookDTOBuilder.class)
public class RunbookDTO {

    @JsonProperty("id")
    String id;

    @JsonProperty("permanent_id")
    String permanentId;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("nodes_dirty")
    Boolean nodesDirty;

    @JsonProperty("previous_id")
    String previousId;

    @JsonProperty("trigger_type")
    TriggerType triggerType;

    @JsonProperty("trigger_template_type")
    String triggerTemplateType;

    @JsonProperty("ui_data")
    RunbookUiData uiData;

    @JsonProperty("settings")
    Runbook.Setting settings;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookUiData.RunbookUiDataBuilder.class)
    public static class RunbookUiData {
        @JsonProperty("nodes")
        Map<String, Node> nodes;

        @JsonProperty("links")
        Map<String, Link> links;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Node.NodeBuilder.class)
        public static class Node {
            @JsonProperty("id")
            String id;

            @JsonProperty("type")
            String type;

            @JsonProperty("name")
            String name;

            @JsonProperty("position")
            JsonNode position; // content not needed for BE but storing as is for FE

            @JsonProperty("ports")
            JsonNode ports; // content not needed for BE but storing as is for FE
//            Map<String, Port> ports;

            @JsonProperty("input")
            Map<String, UiFieldData> input;

        }

        @lombok.Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = UiFieldData.UiFieldDataBuilder.class)
        public static class UiFieldData {
            @JsonProperty("key")
            private String key;
            @JsonProperty("values")
            private List<Value> values;
            @JsonProperty("type")
            private String type;

            @lombok.Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = Value.ValueBuilder.class)
            public static class Value {

                // not all values are strings so using Object (e.g. Json blob data)
                @JsonProperty("value")
                private Object value;

                @JsonProperty("type")
                private String type;
            }
        }


//        @Value
//        @Builder(toBuilder = true)
//        @JsonDeserialize(builder = Port.PortBuilder.class)
//        public static class Port {
//            @JsonProperty("id")
//            String id;
//
//            String type;
//
//            @JsonProperty("properties")
//            PortProperties properties;
//        }

//        @Value
//        @Builder(toBuilder = true)
//        @JsonDeserialize(builder = PortProperties.PortPropertiesBuilder.class)
//        public static class PortProperties {
//            @JsonProperty("action")
//            String action;
//        }

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

            @JsonProperty("properties")
            LinkProperties properties;

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = LinkProperties.LinkPropertiesBuilder.class)
            public static class LinkProperties {
                @JsonProperty("option")
                String option;

                @JsonProperty("wait")
                Boolean wait;
            }

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

}
