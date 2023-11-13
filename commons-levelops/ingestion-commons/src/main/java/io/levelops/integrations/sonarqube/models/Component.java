package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Component.ComponentBuilder.class)
public class Component {

    @JsonProperty("organization")
    String organization;

    @JsonProperty("key")
    String key;

    @JsonProperty("name")
    String name;

    @JsonProperty("qualifier")
    String qualifier;

    @JsonProperty("visibility")
    String visibility;

    @JsonProperty("lastAnalysisDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy'T'hh:mm:ssZ")
    Date lastAnalysisDate;

    @JsonProperty("revision")
    String revision;

    @JsonProperty("uuid")
    String uuid;

    @JsonProperty("path")
    String path;

    @JsonProperty("longName")
    String longName;

    @JsonProperty("enabled")
    Boolean enabled;

    @JsonProperty("measures")
    List<Measure> measures;


}
