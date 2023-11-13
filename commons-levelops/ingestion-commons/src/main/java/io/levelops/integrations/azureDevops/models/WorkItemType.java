package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemType.WorkItemTypeBuilder.class)
public class WorkItemType {

    @JsonProperty("color")
    String color;

    @JsonProperty("description")
    String description;

    @JsonProperty("fieldInstances")
    List<FieldInstances> fieldInstances;

    @JsonProperty("fields")
    List<FieldInstances> fields;

    @JsonProperty("icon")
    WorkItemTypeIcon workItemTypeIcon;

    @JsonProperty("isDisabled")
    Boolean isDisabled;

    @JsonProperty("name")
    String name;

    @JsonProperty("referenceName")
    String referenceName;

    @JsonProperty("states")
    List<WorkItemStateColor>  states;

    @JsonProperty("transitions")
    Object transitions;

    @JsonProperty("url")
    String url;

    @JsonProperty("xmlForm")
    String xmlForm;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemStateColor.WorkItemStateColorBuilder.class)
    private static class WorkItemStateColor {

        @JsonProperty("category")
        String category;

        @JsonProperty("color")
        String color;

        @JsonProperty("name")
        String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemTypeIcon.WorkItemTypeIconBuilder.class)
    private static class WorkItemTypeIcon {
        @JsonProperty("id")
        String id;

        @JsonProperty("url")
        String url;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldInstances.FieldInstancesBuilder.class)
    private static class FieldInstances{
        @JsonProperty("allowedValues")
        List<String> allowedValues;

        @JsonProperty("alwaysRequired")
        Boolean alwaysRequired;

        @JsonProperty("defaultValue")
        String defaultValue;

        @JsonProperty("dependentFields")
        List<WorkItemFieldReference> workItemFieldReferences;

        @JsonProperty("helpText")
        String helpText;

        @JsonProperty("name")
        String name;

        @JsonProperty("referenceName")
        String referenceName;

        @JsonProperty("url")
        String url;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = WorkItemFieldReference.WorkItemFieldReferenceBuilder.class)
        private static class WorkItemFieldReference {

            @JsonProperty("name")
            String name;

            @JsonProperty("referenceName")
            String referenceName;

            @JsonProperty("url")
            String url;
        }
    }
}
