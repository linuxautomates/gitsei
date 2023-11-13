package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CxResult {

    @JsonProperty("NodeId")
    String nodeId;

    @JsonProperty("FileName")
    String fileName;

    @JsonProperty("Status")
    String status;

    @JsonProperty("Line")
    Integer line;

    @JsonProperty("Column")
    Integer column;

    @JsonProperty("FalsePositive")
    Boolean falsePositive;

    @JsonProperty("Severity")
    String severity;

    @JsonProperty("AssignToUser")
    String assignToUser;

    @JsonProperty("state")
    String state;

    @JsonProperty("Remark")
    String remark;

    @JsonProperty("DeepLink")
    String deepLink;

    @JsonProperty("SeverityIndex")
    String severityIndex;

    @JsonProperty("DetectionDate")
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonSerialize(using = CustomDateSerializer.class)
    Date detectionDate;

    @JsonProperty("paths")
    @JacksonXmlProperty(localName = "Path")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<CxPath> paths; // child

}
