package io.levelops.commons.databases.models.database.bullseye;

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
public class FolderCoverage {

    @JsonProperty("name")
    @JacksonXmlProperty(isAttribute = true)
    String name;

    @JsonProperty("fn_cov")
    @JacksonXmlProperty(localName = "fn_cov", isAttribute = true)
    Integer functionsCovered;

    @JsonProperty("fn_total")
    @JacksonXmlProperty(localName = "fn_total", isAttribute = true)
    Integer totalFunctions;

    @JsonProperty("cd_cov")
    @JacksonXmlProperty(localName = "cd_cov", isAttribute = true)
    Integer conditionsCovered;

    @JsonProperty("cd_total")
    @JacksonXmlProperty(localName = "cd_total", isAttribute = true)
    Integer totalConditions;

    @JsonProperty("d_cov")
    @JacksonXmlProperty(localName = "d_cov", isAttribute = true)
    Integer decisionsCovered;

    @JsonProperty("d_total")
    @JacksonXmlProperty(localName = "d_total", isAttribute = true)
    Integer totalDecisions;

    @JacksonXmlProperty(localName = "src")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<SourceFileCoverage> sourceFiles;

    @JacksonXmlProperty(localName = "folder")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<FolderCoverage> folderCoverages;
}
