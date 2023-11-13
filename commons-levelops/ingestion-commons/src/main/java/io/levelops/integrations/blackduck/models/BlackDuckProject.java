package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckProject.BlackDuckProjectBuilder.class)
public class BlackDuckProject {

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("projectLevelAdjustments")
    String projectLevelAdjustments;

    @JsonProperty("projectTier")
    String projectTier;

    @JsonProperty("cloneCategories")
    List<String> cloneCategories;

    @JsonProperty("customSignatureEnabled")
    Boolean customSignatureEnabled;

    @JsonProperty("licenseConflictsEnabled")
    Boolean licenseConflictsEnabled;

    @JsonProperty("snippetAdjustmentApplied")
    Boolean snippetAdjustmentApplied;

    @JsonProperty("deepLicenseDataEnabled")
    Boolean deepLicenseDataEnabled;

    @JsonProperty("createdAt")
    Date projCreatedAt;

    @JsonProperty("updatedAt")
    Date projUpdatedAt;

}
