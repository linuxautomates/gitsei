package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CxPath {

    @JsonProperty("ResultId")
    String resultId;

    @JsonProperty("PathId")
    String pathId;

    @JsonProperty("SimilarityId")
    String similarityId;

    @JsonProperty("pathNodes")
    @JacksonXmlProperty(localName = "PathNode")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<CxPathNode> pathNodes;
}
