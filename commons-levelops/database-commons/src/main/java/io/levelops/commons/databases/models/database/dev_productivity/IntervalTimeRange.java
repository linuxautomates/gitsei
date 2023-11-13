package io.levelops.commons.databases.models.database.dev_productivity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder= IntervalTimeRange.IntervalTimeRangeBuilder.class)
public class IntervalTimeRange {

    @JsonProperty("time_range")
    private final ImmutablePair<Long, Long> timeRange;

    @JsonProperty("week_of_the_year")
    private final Integer weekOfTheYear;

    @JsonProperty("year")
    private final Integer year;

}
