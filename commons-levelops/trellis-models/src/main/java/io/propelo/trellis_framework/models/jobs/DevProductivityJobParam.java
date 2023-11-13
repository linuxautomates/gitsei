package io.propelo.trellis_framework.models.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= DevProductivityJobParam.DevProductivityJobParamBuilder.class)
public class DevProductivityJobParam {
    @JsonProperty("feature_types")
    private final List<DevProductivityProfile.FeatureType> featureTypes;
}
