package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CxPathNode {

    @JsonProperty("FileName")
    String fileName;

    @JsonProperty("Line")
    Integer line;

    @JsonProperty("Column")
    Integer column;

    @JsonProperty("NodeId")
    String nodeId;

    @JsonProperty("Name")
    String name;

    @JsonProperty("Type")
    String type;

    @JsonProperty("Length")
    String length;

    @JsonProperty("Snippet")
    Snippet snippet;

    @Getter
    @ToString
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {

        @JsonProperty("Line")
        Line line;

        @Getter
        @ToString
        @EqualsAndHashCode
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Line {

            @JsonProperty("Number")
            Integer number;

            @JsonProperty("Code")
            String code;
        }
    }
}
