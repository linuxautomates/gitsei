package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckProject.BlackDuckProjectBuilder.class)
@JsonIgnoreProperties(value={ "_meta" }, allowSetters= true)
public class BlackDuckProject {

    @JsonProperty("name")
    String projectName;

    @JsonProperty("description")
    String projectDescription;

    @JsonProperty("projectLevelAdjustments")
    Boolean projectLevelAdjustments;

    @JsonProperty("cloneCategories")
    List<String> cloneCategories;

    @JsonProperty("customSignatureEnabled")
    Boolean customSignatureEnabled;

    @JsonProperty("customSignatureDepth")
    int customSignatureDepth;

    @JsonProperty("licenseConflictsEnabled")
    Boolean licenseConflictsEnabled;

    @JsonProperty("snippetAdjustmentApplied")
    Boolean snippetAdjustmentApplied;

    @JsonProperty("projectTier")
    String projectTier;

    @JsonProperty("deepLicenseDataEnabled")
    Boolean deepLicenseDataEnabled;

    @JsonProperty("createdAt")
    String projectCreatedAt;

    @JsonProperty("updatedAt")
    String projectUpdatedAt;

    @JsonProperty("_meta")
    BlackDuckMetadata blackDuckMetadata;
}
