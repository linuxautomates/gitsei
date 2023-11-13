package io.levelops.controlplane.trigger.strategies;

import io.levelops.commons.dates.DateUtils;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy.*;
import static io.levelops.controlplane.trigger.strategies.JobTags.BACKWARD_SCAN_TAG;
import static io.levelops.controlplane.trigger.strategies.JobTags.FORWARD_SCAN_TAG;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCursorStrategyTest {


    @Test
    public void test() {
        DefaultCursorStrategy strat = new DefaultCursorStrategy(50); // 3000 secs

        Date t0 = DateUtils.fromEpochSecondToDate(0L);
        Date t1 = DateUtils.fromEpochSecondToDate(1000L);
        Date t2 = DateUtils.fromEpochSecondToDate(2000L);
        Date t3 = DateUtils.fromEpochSecondToDate(3000L);
        Date t4 = DateUtils.fromEpochSecondToDate(4000L);
        Date t5 = DateUtils.fromEpochSecondToDate(5000L);

        // first onboarding: null -> t0 (lastFullScan = t0, partial=False, tags=Backward)
        DefaultCursor cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(null)
                .lastFullScan(null)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t1)
                .build());
        assertThat(cursor.getFrom()).isEqualTo((Date)null);
        assertThat(cursor.getTo()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.isPartial()).isFalse();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);

        // 1st iterative scan: t1 -> t2 (lastFullScan = t1, partial=True, tags=Forward)
        cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(t1)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t2)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t1);
        assertThat(cursor.getTo()).isEqualTo(t2);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);

        // 2nd iterative scan: t2 -> t3 (lastFullScan = t1, partial=True, tags=Forward)
        cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(t2)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t3)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t2);
        assertThat(cursor.getTo()).isEqualTo(t3);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);

        // re-onboarding due (t4 - t0 > 3000): null -> t4 (lastFullScan = t4, partial=False, tags=Backward)
        cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(t3)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t4)
                .build());
        assertThat(cursor.getFrom()).isEqualTo((Date)null);
        assertThat(cursor.getTo()).isEqualTo(t4);
        assertThat(cursor.getLastFullScan()).isEqualTo(t4);
        assertThat(cursor.isPartial()).isFalse();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);

        // 1st iterative scan after re-onboarding: t4 -> t5 (lastFullScan = t4, partial=False, tags=Backward)
        cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(t4)
                .lastFullScan(t4)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t5)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t4);
        assertThat(cursor.getTo()).isEqualTo(t5);
        assertThat(cursor.getLastFullScan()).isEqualTo(t4);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
    }

    @Test
    public void testOnboardingSpan() {
        DefaultCursorStrategy strat = new DefaultCursorStrategy(50, 7L); // 3000 secs

        Date t0 = DateUtils.fromEpochSecondToDate(10000000L);
        Date t1 = DateUtils.fromEpochSecondToDate(20000000L);

        // first onboarding: null -> t0 (lastFullScan = t0, partial=False, tags=Backward)
        DefaultCursor cursor = strat.getNextCursor(DefaultCursorMetadata.builder()
                .currentCursor(null)
                .lastFullScan(null)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t1)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(DateUtils.fromEpochSecondToDate(10000000L - TimeUnit.DAYS.toSeconds(7)));
        assertThat(cursor.getTo()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.isPartial()).isFalse();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);
    }
}