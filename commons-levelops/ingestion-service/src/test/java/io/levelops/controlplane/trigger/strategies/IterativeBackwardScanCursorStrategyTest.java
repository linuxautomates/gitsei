package io.levelops.controlplane.trigger.strategies;

import io.levelops.commons.dates.DateUtils;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata;
import org.junit.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static io.levelops.controlplane.trigger.strategies.JobTags.BACKWARD_SCAN_TAG;
import static io.levelops.controlplane.trigger.strategies.JobTags.FORWARD_SCAN_TAG;
import static org.assertj.core.api.Assertions.assertThat;

public class IterativeBackwardScanCursorStrategyTest {

    @Test
    public void test() {

        // re-onboarding strategy: every 4 days, re-scan the last 2 weeks, 1 week at a time
        IterativeBackwardScanCursorStrategy strat = new IterativeBackwardScanCursorStrategy(
                TimeUnit.DAYS.toMinutes(4), // (> than t5-t0 so reonboarding at t5)
                14,
                TimeUnit.DAYS.toMinutes(7));

        Date t0 = DateUtils.parseDateTimeToDate("2000-01-31T01:02:03.456Z"); // expected onboarding, 1st bw
        Date t0_minus2weeks = Date.from(t0.toInstant().minus(14, ChronoUnit.DAYS));
        Date t0_minus1week = Date.from(t0.toInstant().minus(7, ChronoUnit.DAYS));
        Date t1 = DateUtils.parseDateTimeToDate("2000-02-01T01:02:03.456Z"); // expected 1st fw
        Date t2 = DateUtils.parseDateTimeToDate("2000-02-02T01:02:03.456Z"); // expected 2nd bw (onboarding done)
        Date t3 = DateUtils.parseDateTimeToDate("2000-02-03T01:02:03.456Z"); // expected 2nd fw
        Date t4 = DateUtils.parseDateTimeToDate("2000-02-04T01:02:03.456Z"); // expected 3rd fw
        // ---- expected reonboarding ----
        Date t5 = DateUtils.parseDateTimeToDate("2000-02-05T01:02:03.456Z"); // expected reonboarding, 1st bw
        Date t5_minus2weeks = Date.from(t5.toInstant().minus(14, ChronoUnit.DAYS));
        Date t5_minus1week = Date.from(t5.toInstant().minus(7, ChronoUnit.DAYS));
        Date t6 = DateUtils.parseDateTimeToDate("2000-02-06T01:02:03.456Z"); // expected 1st fw after reonboarding
        Date t7 = DateUtils.parseDateTimeToDate("2000-02-07T01:02:03.456Z"); // expected 2nd bw after reonboarding (reonboarding done)
        Date t8 = DateUtils.parseDateTimeToDate("2000-02-08T01:02:03.456Z"); // expected 2nd fw after reonboarding
        Date t9 = DateUtils.parseDateTimeToDate("2000-02-09T01:02:03.456Z"); // expected 3rd fw after reonboarding

        /*
            BW and FW scans alternate until onboarding scan is done,
            then there are only FW scans until reonboarding is due:

            t0: BW t-2w -> t-1w   -- 1st bw scan
            t1: FW t0 -> t1       -- 1st fw scan
            t2: BW t-1w -> t0     -- 2nd bw scan (bw scan done)
            t3: FW t1 -> t3       -- 2nd fw scan
            t4: FW t3 -> t4       -- 3rd fw scan (no more bw scan needed)
            t5: BW t5-2w -> t5-1w -- reonboarding, 1st bw scan
            t6: FW t4 -> t6       -- 1st fw scan after reonboarding
            t7: BW t5-1w -> t5    -- 2nd bw scan (bw scan done)
            t8: FW t6 -> t8       -- 2nd fw scan
            t9: FW t8 -> t9       -- 3rd fw scan (no more bw scan needed)
         */

        // (t0) 1st backward scan (onboarding): t-2w -> t-1w (lastFullScan = t0, partial=False, tags=Backward)
        IterativeBackwardScanCursor cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(null)
                .currentBackwardCursor(null)
                .lastScanType(null)
                .lastFullScan(null)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t0)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t0_minus2weeks);
        assertThat(cursor.getTo()).isEqualTo(t0_minus1week);
        assertThat(cursor.isPartial()).isFalse();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(BACKWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t0);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0_minus1week);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);

        // (t1) 1st forward scan (t0 -> t1)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t0)
                .currentBackwardCursor(t0_minus1week)
                .lastScanType(BACKWARD_SCAN_TAG)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t1)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t0);
        assertThat(cursor.getTo()).isEqualTo(t1);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t1);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0_minus1week);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);

        // (t2) 2nd backward scan (t-1w -> t0) - backward scan is done
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t1)
                .currentBackwardCursor(t0_minus1week)
                .lastScanType(FORWARD_SCAN_TAG)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t2)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t0_minus1week);
        assertThat(cursor.getTo()).isEqualTo(t0);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(BACKWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t1);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);

        // (t3) 2nd forward scan (t1 -> t3)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t1)
                .currentBackwardCursor(t0)
                .lastScanType(BACKWARD_SCAN_TAG)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t3)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t1);
        assertThat(cursor.getTo()).isEqualTo(t3);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t3);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);

        // (t4) 3rd forward scan, because backward is already done (t3 -> t4)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t3)
                .currentBackwardCursor(t0)
                .lastScanType(FORWARD_SCAN_TAG)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t4)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t3);
        assertThat(cursor.getTo()).isEqualTo(t4);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t4);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);

        // (t5) re-onboarding due, 1st backward scan after reonboarding (t5-2w -> t5-1w)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t4)
                .currentBackwardCursor(t0)
                .lastScanType(FORWARD_SCAN_TAG)
                .lastFullScan(t0)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t5)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t5_minus2weeks);
        assertThat(cursor.getTo()).isEqualTo(t5_minus1week);
        assertThat(cursor.isPartial()).isFalse();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(BACKWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t4);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t5_minus1week);
        assertThat(cursor.getLastFullScan()).isEqualTo(t5);

        // (t6) 1st forward scan after re-onboarding (t4 -> t6)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t4)
                .currentBackwardCursor(t5_minus1week)
                .lastScanType(BACKWARD_SCAN_TAG)
                .lastFullScan(t5)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t6)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t4);
        assertThat(cursor.getTo()).isEqualTo(t6);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t6);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t5_minus1week);
        assertThat(cursor.getLastFullScan()).isEqualTo(t5);

        // (t7) 2nd backward scan after re-onboarding (t5-1w -> t5) - backward scan is done
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t6)
                .currentBackwardCursor(t5_minus1week)
                .lastScanType(FORWARD_SCAN_TAG)
                .lastFullScan(t5)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t7)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t5_minus1week);
        assertThat(cursor.getTo()).isEqualTo(t5);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(BACKWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(BACKWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t6);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t5);
        assertThat(cursor.getLastFullScan()).isEqualTo(t5);

        // (t8) 2nd fw scan after re-onboarding (t6 -> t8)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t6)
                .currentBackwardCursor(t5)
                .lastScanType(BACKWARD_SCAN_TAG)
                .lastFullScan(t5)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t8)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t6);
        assertThat(cursor.getTo()).isEqualTo(t8);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t8);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t5);
        assertThat(cursor.getLastFullScan()).isEqualTo(t5);

        // (t9) 3rd fw scan after re-onboarding. because bw scan is already done (t8 -> t9)
        cursor = strat.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(t8)
                .currentBackwardCursor(t5)
                .lastScanType(FORWARD_SCAN_TAG)
                .lastFullScan(t5)
                .triggerCreatedAtEpoch(DateUtils.toEpochSecond(t0))
                .now(t9)
                .build());
        assertThat(cursor.getFrom()).isEqualTo(t8);
        assertThat(cursor.getTo()).isEqualTo(t9);
        assertThat(cursor.isPartial()).isTrue();
        assertThat(cursor.getTags()).containsExactlyInAnyOrder(FORWARD_SCAN_TAG);
        assertThat(cursor.getScanType()).isEqualTo(FORWARD_SCAN_TAG);
        assertThat(cursor.getForwardCursor()).isEqualTo(t9);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t5);
        assertThat(cursor.getLastFullScan()).isEqualTo(t5);
    }

    @Test
    public void testSuccessiveBackwardScans() {
        //
        IterativeBackwardScanCursorStrategy strategy = new IterativeBackwardScanCursorStrategy(
                TimeUnit.HOURS.toMinutes(4), 90, TimeUnit.DAYS.toMinutes(30), 2);

        Date t0 = DateUtils.fromEpochSecondToDate(0L);
        Date t0_minus90days = dateSub(t0, 90, ChronoUnit.DAYS);
        Date t0_minus60days = dateSub(t0, 60, ChronoUnit.DAYS);
        Date t0_minus30days = dateSub(t0, 30, ChronoUnit.DAYS);

        var metadata = IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(null)
                .currentBackwardCursor(null)
                .lastFullScan(null)
                .triggerCreatedAtEpoch(0L)
                .lastScanType(null)
                .lastScanTypeCount(null)
                .now(t0)
                .build();

        // backward scan
        var cursor = strategy.getNextCursor(metadata);
        assertThat(cursor.getFrom()).isEqualTo(t0_minus90days);
        assertThat(cursor.getTo()).isEqualTo(t0_minus60days);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0_minus60days);
        assertThat(cursor.getForwardCursor()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.getScanType()).isEqualTo("backward");
        assertThat(cursor.getLastScanTypeCount()).isEqualTo(1);
        assertThat(cursor.isPartial()).isEqualTo(false);

        // 2nd backward scan because successiveBackwardScanCount is set to 2
        cursor = strategy.getNextCursor(updateMetadataFromCursor(metadata, cursor, dateAdd(t0, 1, ChronoUnit.HOURS)));
        assertThat(cursor.getFrom()).isEqualTo(t0_minus60days);
        assertThat(cursor.getTo()).isEqualTo(t0_minus30days);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0_minus30days);
        assertThat(cursor.getForwardCursor()).isEqualTo(t0);
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.getScanType()).isEqualTo("backward");
        assertThat(cursor.getLastScanTypeCount()).isEqualTo(2);
        assertThat(cursor.isPartial()).isEqualTo(true);

        // forward scan
        cursor = strategy.getNextCursor(updateMetadataFromCursor(metadata, cursor, dateAdd(t0, 2, ChronoUnit.HOURS)));
        assertThat(cursor.getFrom()).isEqualTo(t0);
        assertThat(cursor.getTo()).isEqualTo(dateAdd(t0, 2, ChronoUnit.HOURS));
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0_minus30days);
        assertThat(cursor.getForwardCursor()).isEqualTo(dateAdd(t0, 2, ChronoUnit.HOURS));
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.getScanType()).isEqualTo("forward");
        assertThat(cursor.getLastScanTypeCount()).isEqualTo(1);
        assertThat(cursor.isPartial()).isEqualTo(true);

        // backward scan
        cursor = strategy.getNextCursor(updateMetadataFromCursor(metadata, cursor, dateAdd(t0, 3, ChronoUnit.HOURS)));
        assertThat(cursor.getFrom()).isEqualTo(t0_minus30days);
        assertThat(cursor.getTo()).isEqualTo(t0);
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0);
        assertThat(cursor.getForwardCursor()).isEqualTo(dateAdd(t0, 2, ChronoUnit.HOURS));
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.getScanType()).isEqualTo("backward");
        assertThat(cursor.getLastScanTypeCount()).isEqualTo(1);
        assertThat(cursor.isPartial()).isEqualTo(true);

        // forward scan because we have completed the backward scan
        cursor = strategy.getNextCursor(updateMetadataFromCursor(metadata, cursor, dateAdd(t0, 4, ChronoUnit.HOURS)));
        assertThat(cursor.getFrom()).isEqualTo(dateAdd(t0, 2, ChronoUnit.HOURS)); // Date add
        assertThat(cursor.getTo()).isEqualTo(dateAdd(t0, 4, ChronoUnit.HOURS));
        assertThat(cursor.getBackwardCursor()).isEqualTo(t0);
        assertThat(cursor.getForwardCursor()).isEqualTo(dateAdd(t0, 4, ChronoUnit.HOURS));
        assertThat(cursor.getLastFullScan()).isEqualTo(t0);
        assertThat(cursor.getScanType()).isEqualTo("forward");
        assertThat(cursor.getLastScanTypeCount()).isEqualTo(1);
        assertThat(cursor.isPartial()).isEqualTo(true);
    }

    private IterativeBackwardScanCursorMetadata updateMetadataFromCursor(IterativeBackwardScanCursorMetadata metadata, IterativeBackwardScanCursor cursor, Date now){
        return metadata.toBuilder()
                .currentBackwardCursor(cursor.getBackwardCursor())
                .currentForwardCursor(cursor.getForwardCursor())
                .lastFullScan(cursor.getLastFullScan())
                .lastScanType(cursor.getScanType())
                .lastScanTypeCount(cursor.getLastScanTypeCount())
                .now(now)
                .build();
    }

    private Date dateAdd(Date d, int qty, ChronoUnit unit) {
        return Date.from(d.toInstant().plus(qty, unit));
    }

    private Date dateSub(Date d, int qty, ChronoUnit unit) {
        return Date.from(d.toInstant().minus(qty, unit));
    }
}