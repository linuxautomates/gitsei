package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ClassificationNode.ClassificationNodeBuilder.class)
public class ClassificationNode {

    @JsonProperty("id")
    String id;

    @JsonProperty("identifier")
    String identifier;

    @JsonProperty("name")
    String name;

    @JsonProperty("structureType")
    String structureType;

    @JsonProperty("hasChildren")
    Boolean hasChildren;

    @JsonProperty("path")
    String path;

    @JsonProperty("url")
    String url;

    @JsonProperty("children")
    List<ClassificationNode> children;
}
