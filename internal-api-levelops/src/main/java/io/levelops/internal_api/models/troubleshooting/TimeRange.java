package io.levelops.internal_api.models.troubleshooting;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Iterables;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
public class TimeRange {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @JsonProperty("from")
    Instant from;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @JsonProperty("to")
    Instant to;

    public static List<TimeRange> createTimeline(List<TimeRange> intervals) {
        if (intervals.size() < 2) {
            return intervals;
        }
        ArrayList<TimeRange> timeline = new ArrayList<>();
        List<TimeRange> sortedIntervals = intervals.stream()
                .sorted(Comparator.comparing(TimeRange::getFrom))
                .collect(Collectors.toList());
        timeline.add(sortedIntervals.get(0));

        for (int i = 1; i < sortedIntervals.size(); i++) {
            TimeRange current = sortedIntervals.get(i);
            TimeRange last = Iterables.getLast(timeline);
            if (last.getTo().compareTo(current.getFrom()) >= 0) {
                if (current.getTo().compareTo(last.getTo()) > 0) {
                    // extend the current time interval
                    timeline.remove(timeline.size() - 1);
                    timeline.add(TimeRange.builder()
                            .from(last.getFrom())
                            .to(current.getTo())
                            .build());
                }
            } else {
                // There is a hole in the time range, add a new range
                timeline.add(current);
            }
        }

        return timeline;
    }
}
