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
@JsonDeserialize(builder = Branch.BranchBuilder.class)
public class Branch {

    @JsonProperty("name")
    String name;

    @JsonProperty("isMain")
    String isMain;

    @JsonProperty("type")
    String type;

    @JsonProperty("analysisDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date analysisDate;

    @JsonProperty("excludedFromPurge")
    Boolean excludedFromPurge;

    @JsonProperty("status")
    Status status;

    @JsonProperty("measures")
    List<Measure> measures; // enriched
}