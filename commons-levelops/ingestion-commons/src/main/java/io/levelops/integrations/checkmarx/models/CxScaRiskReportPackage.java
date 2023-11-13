package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxScaRiskReportPackage.CxScaRiskReportPackageBuilder.class)
public class CxScaRiskReportPackage {

    @JsonProperty("id")
    String id;

    @JsonProperty("versions")
    String versions;

    @JsonProperty("highVulnerabilityCount")
    Integer highVulnerabilityCount;

    @JsonProperty("mediumVulnerabilityCount")
    Integer mediumVulnerabilityCount;

    @JsonProperty("lowVulnerabilityCount")
    Integer lowVulnerabilityCount;

    @JsonProperty("ignoreVulnerabilityCount")
    Integer ignoreVulnerabilityCount;

    @JsonProperty("numberOfVersionsSinceLastUpdate")
    Integer numberOfVersionsSinceLastUpdate;

    @JsonProperty("newestVersion")
    String newestVersion;

    @JsonProperty("outdated")
    Boolean isOutdated;

    @JsonProperty("riskScore")
    Integer riskScore;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("locations")
    List<String> locations;

    @JsonProperty("packageRepository")
    String packageRepository;

    @JsonProperty("isDirectDependency")
    Boolean isDirectDependency;

    @JsonProperty("isDevelopment")
    Boolean isDevelopment;

    @JsonProperty("dependencyPaths")
    DependencyPath dependencyPath;
}
