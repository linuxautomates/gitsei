package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TenantSCMSettings.TenantSCMSettingsBuilder.class)
public class TenantSCMSettings {
    @JsonProperty("code_change_size_unit")
    private final String codeChangeSizeUnit;
    @JsonProperty("code_change_size_small")
    private final Integer codeChangeSizeSmall;
    @JsonProperty("code_change_size_medium")
    private final Integer codeChangeSizeMedium;
    @JsonProperty("comment_density_small")
    private final Integer commentDensitySmall;
    @JsonProperty("comment_density_medium")
    private final Integer commentDensityMedium;
    @JsonProperty("legacy_update_interval_config")
    private final Long legacyUpdateIntervalConfig;
}
