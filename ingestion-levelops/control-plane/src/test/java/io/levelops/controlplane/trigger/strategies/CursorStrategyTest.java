package io.levelops.controlplane.trigger.strategies;

import io.levelops.commons.dates.DateUtils;
import io.levelops.controlplane.trigger.strategies.CursorStrategy.Cursor;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CursorStrategyTest {

    DefaultCursorStrategy strategy = new DefaultCursorStrategy(10);

    @Test
    public void testOnboarding() {
        Date now = new Date();
        Date createdAt = DateUtils.fromEpochSecondToDate(1000000000L);
        DefaultCursorStrategy.DefaultCursor cursor = strategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .triggerCreatedAtEpoch(1000000000L)
                .now(now)
                .build());
        assertThat(cursor.getFrom()).as("from").isNull();
        assertThat(cursor.getTo()).as("to").isEqualTo(createdAt);
        assertThat(cursor.isPartial()).as("partial").isFalse();
        assertThat(cursor.getLastFullScan()).as("lastFullScan").isEqualTo(createdAt);
        assertThat(cursor.getTags()).containsExactlyInAnyOrder("backward");
    }

    @Test
    public void testLastFullScanNull() {
        Date now = new Date();
        Date curr = DateUtils.fromEpochSecondToDate(2000000000L);
        DefaultCursorStrategy.DefaultCursor cursor = strategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(curr)
                .triggerCreatedAtEpoch(1000000000L)
                .now(now)
                .build());

        assertThat(cursor.getFrom()).as("from").isNull();
        assertThat(cursor.getTo()).as("to").isEqualTo(now);
        assertThat(cursor.isPartial()).as("partial").isFalse();
        assertThat(cursor.getLastFullScan()).as("lastFullScan").isEqualTo(now);
        assertThat(cursor.getTags()).containsExactlyInAnyOrder("backward");
    }

    @Test
    public void testNext() {
        Date curr = DateUtils.fromEpochSecondToDate(2000000000L);
        Date now = DateUtils.fromEpochSecondToDate(3000000000L);
        Date lastFullScan = DateUtils.fromEpochSecondToDate(3000000000L - 1);
        DefaultCursorStrategy.DefaultCursor cursor = strategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(curr)
                .lastFullScan(lastFullScan)
                .triggerCreatedAtEpoch(1000000000L)
                .now(now)
                .build());

        assertThat(cursor.getFrom()).as("from").isEqualTo(curr);
        assertThat(cursor.getTo()).as("to").isEqualTo(now);
        assertThat(cursor.isPartial()).as("partial").isTrue();
        assertThat(cursor.getLastFullScan()).as("lastFullScan").isEqualTo(lastFullScan);
        assertThat(cursor.getTags()).containsExactlyInAnyOrder("forward");
    }

    @Test
    public void testFullScanDue() {
        Date curr = DateUtils.fromEpochSecondToDate(2000000000L);
        Date now = DateUtils.fromEpochSecondToDate(4000000000L);
        Date lastFullScan = Date.from(now.toInstant().minus(TimeUnit.MINUTES.toMinutes(20), ChronoUnit.MINUTES));
        DefaultCursorStrategy.DefaultCursor cursor = strategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(curr)
                .lastFullScan(lastFullScan)
                .triggerCreatedAtEpoch(1000000000L)
                .now(now)
                .build());
        assertThat(cursor.getFrom()).as("from").isNull();
        assertThat(cursor.getTo()).as("to").isEqualTo(now);
        assertThat(cursor.isPartial()).as("partial").isFalse();
        assertThat(cursor.getLastFullScan()).as("lastFullScan").isEqualTo(now);
        assertThat(cursor.getTags()).containsExactlyInAnyOrder("backward");
    }

}