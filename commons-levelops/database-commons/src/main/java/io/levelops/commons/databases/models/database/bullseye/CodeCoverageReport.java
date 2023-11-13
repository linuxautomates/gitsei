package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JacksonXmlRootElement(localName = "BullseyeCoverage")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeCoverageReport {

    @JacksonXmlProperty(isAttribute = true)
    String name;

    @JacksonXmlProperty(isAttribute = true, localName = "dir")
    String directory;

    @JacksonXmlProperty(isAttribute = true)
    String buildId;

    @JacksonXmlProperty(isAttribute = true, localName = "fn_total")
    Integer totalFunctions;

    @JacksonXmlProperty(isAttribute = true, localName = "fn_cov")
    Integer functionsCovered;

    @JacksonXmlProperty(isAttribute = true, localName = "cd_total")
    Integer totalConditions;

    @JacksonXmlProperty(isAttribute = true, localName = "cd_cov")
    Integer conditionsCovered;

    @JacksonXmlProperty(isAttribute = true, localName = "d_total")
    Integer totalDecisions;

    @JacksonXmlProperty(isAttribute = true, localName = "d_cov")
    Integer decisionsCovered;

    @JacksonXmlProperty(localName = "folder")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<FolderCoverage> folderCoverages;

    @JacksonXmlProperty(localName = "src")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<SourceFileCoverage> sourceFiles;
}
