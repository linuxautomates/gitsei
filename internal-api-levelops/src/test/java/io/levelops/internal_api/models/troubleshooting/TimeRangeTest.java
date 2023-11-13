package io.levelops.internal_api.models.troubleshooting;

import io.levelops.internal_api.controllers.IntegrationTroubleshootingController;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeRangeTest {
    private TimeRange createTimeRange(Instant from, Instant to) {
        return new TimeRange(from, to);
    }

    @Test
    public void testTimeLine() {
        Instant t0 = Instant.now();
        List<Instant> times = new ArrayList<>();
        times.add(t0);
        for (int i = 0; i < 100; i++) {
            times.add(times.get(times.size() - 1).plusSeconds(60));
        }

        List<TimeRange> timeRanges = new ArrayList<>(List.of(
                createTimeRange(times.get(0), times.get(3)),
                createTimeRange(times.get(1), times.get(2)),
                createTimeRange(times.get(1), times.get(7)),
                createTimeRange(times.get(2), times.get(12)),
                createTimeRange(times.get(12), times.get(17)),
                createTimeRange(times.get(20), times.get(25)), // new interval
                createTimeRange(times.get(22), times.get(27)),
                createTimeRange(times.get(26), times.get(29)),
                createTimeRange(times.get(30), times.get(35)) // new interval
        ));
        Collections.shuffle(timeRanges);

        List<TimeRange> expected = List.of(
                createTimeRange(times.get(0), times.get(17)),
                createTimeRange(times.get(20), times.get(29)),
                createTimeRange(times.get(30), times.get(35))
        );

        assertThat(TimeRange.createTimeline(timeRanges))
                .containsExactlyElementsOf(expected);
    }

}