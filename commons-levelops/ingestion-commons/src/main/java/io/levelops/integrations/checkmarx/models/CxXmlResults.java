package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CxXmlResults {

    @JsonProperty("InitiatorName")
    String initiatorName;

    @JsonProperty("Owner")
    String owner;

    @JsonProperty("ScanId")
    String scanId;

    @JsonProperty("ProjectId")
    String projectId;

    @JsonProperty("ProjectName")
    String projectName;

    @JsonProperty("TeamFullPathOnReportDate")
    String teamFullPathOnReportDate;

    @JsonProperty("DeepLink")
    String deepLink;

    @JsonProperty("ScanStart")
    String scanStart;

    @JsonProperty("Preset")
    String preset;

    @JsonProperty("ScanTime")
    String scanTime;

    @JsonProperty("LinesOfCodeScanned")
    Integer linesOfCodeScanned;

    @JsonProperty("FilesScanned")
    Integer filesScanned;

    @JsonProperty("ReportCreationTime")
    String reportCreationTime;

    @JsonProperty("Team")
    String team;

    @JsonProperty("CheckmarxVersion")
    String checkmarxVersion;

    @JsonProperty("ScanComments")
    String scanComments;

    @JsonProperty("ScanType")
    String scanType;

    @JsonProperty("SourceOrigin")
    String sourceOrigin;

    @JsonProperty("Visibility")
    String visibility;

    @JacksonXmlProperty(localName = "Query")
    @JacksonXmlElementWrapper(useWrapping = false)
    @Setter
    List<CxQuery> queries; // children
}
