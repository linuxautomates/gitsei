package io.levelops.api.model.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FeatureRawStat.FeatureRawStatBuilder.class)
public class FeatureRawStat {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("count")
    private final Long count; //counts or time in secs

    //Rating
    @JsonProperty("rating")
    private final DevProductivityProfile.Rating rating;

}
