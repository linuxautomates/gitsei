package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DevProductivityFilter.DevProductivityFilterBuilder.class)
public class DevProductivityFilter {
    //Use this for fixed intervals calculation
    @JsonProperty("interval")
    ReportIntervalType interval;

    //Use this for custom time range
    @JsonProperty("time_range")
    ImmutablePair<Long, Long> timeRange;

    @JsonProperty("force_source")
    String forceSource;

    public static DevProductivityFilter fromListRequest(DefaultListRequest filter) {
        ReportIntervalType reportIntervalType = ReportIntervalType.fromString(filter.getFilterValue("interval", String.class).orElse(null));
        ImmutablePair<Long, Long> timeRange = filter.getNumericRangeFilter("time_range");
        if(timeRange == null || (timeRange.getLeft()== null && timeRange.getRight() == null)) {
            //If Time range is not available see if fixed interval is set
            if(reportIntervalType != null) {
                //If fixed interval is set use the time from the interval
                timeRange = reportIntervalType.getIntervalTimeRange(Instant.now()).getTimeRange();
            }
        }
        String forceSource = (String) filter.getFilter().get("force_source");
        return DevProductivityFilter.builder()
                .interval(reportIntervalType)
                .timeRange(timeRange)
                .forceSource(forceSource)
                .build();
    }
}
