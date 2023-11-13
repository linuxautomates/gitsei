package io.levelops.commons.databases.models.database.blackduck;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbBlackDuckProjectAttributes.DbBlackDuckProjectAttributesBuilder.class)
public class DbBlackDuckProjectAttributes {

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
}
