package io.levelops.controlplane.trigger.strategies;

import io.levelops.commons.dates.DateUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import static io.levelops.controlplane.trigger.strategies.JobTags.BACKWARD_SCAN_TAG;
import static io.levelops.controlplane.trigger.strategies.JobTags.FORWARD_SCAN_TAG;

public class IterativeBackwardScanCursorStrategy implements CursorStrategy<IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor, IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata> {

    private final long fullScanFreqInMin;
    private final long onboardingSpanInDays;
    private final long backwardScanSubJobSpanInMin;
    private final int successiveBackwardScanCount;

    /**
     * By default, successiveBackwardScanCount is set to 1, which means that we'll alternate between backward and forward
     * scans when applicable
     */
    public IterativeBackwardScanCursorStrategy(long fullScanFreqInMin, long onboardingSpanInDays, long backwardScanSubJobSpanInMin) {
        this.fullScanFreqInMin = fullScanFreqInMin;
        this.onboardingSpanInDays = onboardingSpanInDays;
        this.backwardScanSubJobSpanInMin = backwardScanSubJobSpanInMin;
        this.successiveBackwardScanCount = 1;
    }

    /**
     *
     * @param successiveBackwardScanCount The number of successive backward scans that the strategy will run before alternating
     *                                    to a forward scan. So if this is set to 2 the pattern will be: B, B, F, B, B, F
     *                                    (as long as the other conditions for backward scans are met)
     */
    public IterativeBackwardScanCursorStrategy(long fullScanFreqInMin, long onboardingSpanInDays, long backwardScanSubJobSpanInMin, int successiveBackwardScanCount) {
        this.fullScanFreqInMin = fullScanFreqInMin;
        this.onboardingSpanInDays = onboardingSpanInDays;
        this.backwardScanSubJobSpanInMin = backwardScanSubJobSpanInMin;
        this.successiveBackwardScanCount = successiveBackwardScanCount;
    }


    @Value
    @Builder(toBuilder = true)
    public static class IterativeBackwardScanCursor implements CursorStrategy.Cursor {
        // to create new job
        Date from;
        Date to;
        boolean partial;
        Set<String> tags;

        // cursor metadata
        String scanType;
        Date forwardCursor;
        Date backwardCursor;
        Date lastFullScan;
        Integer lastScanTypeCount;
    }

    @Value
    @Builder(toBuilder = true)
    public static class IterativeBackwardScanCursorMetadata implements CursorStrategy.CursorMetadata {
        @Nullable
        Date currentForwardCursor; // last forward 'to'
        @Nullable
        Date currentBackwardCursor; // last backward 'to'
        @Nullable
        String lastScanType; // 'forward' or 'backward'
        @Nullable
        Date lastFullScan; // when onboarding scan was last triggered (t0 in t(-X) -> ... -> t0)
        @Nonnull
        Long triggerCreatedAtEpoch;
        @Nonnull
        Date now;
        @Nullable
        Integer lastScanTypeCount; // how many times the last scan type has run
    }

    @Override
    public IterativeBackwardScanCursor getNextCursor(IterativeBackwardScanCursorMetadata currentCursorMetadata) {
        Instant from;
        Instant to;
        boolean partial = true; // always true, except for re-onboarding
        String newScanType; // either "backward" or "forward"
        // unless overwritten, use previous values:
        Instant newForwardCursor = DateUtils.toInstant(currentCursorMetadata.getCurrentForwardCursor());
        Instant newBackwardCursor = DateUtils.toInstant(currentCursorMetadata.getCurrentBackwardCursor());
        Date newLastFullScan = currentCursorMetadata.getLastFullScan();

        // Assume that the last scan type count is 1 if it's null
        int lastScanTypeCount = ObjectUtils.firstNonNull(currentCursorMetadata.getLastScanTypeCount(), 1);

        // Always 1 except for successive backward scans
        int newLastScanTypeCount = 1;


        boolean firstOnboardingEver = (currentCursorMetadata.getCurrentForwardCursor() == null || currentCursorMetadata.getCurrentBackwardCursor() == null || currentCursorMetadata.getLastFullScan() == null);
        boolean reOnboardingDue = isReOnboardingDue(currentCursorMetadata.getLastFullScan(), currentCursorMetadata.getNow());

        if (firstOnboardingEver || reOnboardingDue) {
            // --- onboarding or re-onboarding

            newLastFullScan = currentCursorMetadata.getNow();
            from = newLastFullScan.toInstant().minus(onboardingSpanInDays, ChronoUnit.DAYS);
            to = DateUtils.earliest(from.plus(backwardScanSubJobSpanInMin, ChronoUnit.MINUTES), newLastFullScan.toInstant());
            partial = false;
            newScanType = BACKWARD_SCAN_TAG;
            if (newForwardCursor == null) {
                // if there was no forward cursor, then set it to now
                // otherwise keep it as is (SEI-2128)
                newForwardCursor = newLastFullScan.toInstant();
            }
            newBackwardCursor = to;
        } else {
            // --- iterative scan

            Instant currentBackwardCursor = currentCursorMetadata.getCurrentBackwardCursor().toInstant();
            Instant nextBackwardCursor = DateUtils.earliest( // move cursor by 'backwardScanSubJobSpanInMin', but no later than 'newLastFullScan'
                    currentBackwardCursor.plus(backwardScanSubJobSpanInMin, ChronoUnit.MINUTES),
                    currentCursorMetadata.getLastFullScan().toInstant());
            boolean isOnboardingDone = currentBackwardCursor.equals(nextBackwardCursor) || currentBackwardCursor.isAfter(nextBackwardCursor);

            // if last scan was backward, and we have completed the number of successive backward scans then trigger a forward scan.
            boolean shouldTriggerForwardScan = isOnboardingDone || (
                    BACKWARD_SCAN_TAG.equalsIgnoreCase(currentCursorMetadata.getLastScanType()) && lastScanTypeCount >= successiveBackwardScanCount);
            if (shouldTriggerForwardScan) {
                // FORWARD scan
                from = currentCursorMetadata.getCurrentForwardCursor().toInstant();
                to = currentCursorMetadata.getNow().toInstant();
                newScanType = FORWARD_SCAN_TAG;
                newForwardCursor = to;
            } else {
                // BACKWARD scan
                from = currentBackwardCursor;
                to = nextBackwardCursor;
                newScanType = BACKWARD_SCAN_TAG;
                newBackwardCursor = to;
                if (BACKWARD_SCAN_TAG.equalsIgnoreCase(currentCursorMetadata.getLastScanType())) {
                    newLastScanTypeCount = lastScanTypeCount + 1;
                }
            }
        }
        return IterativeBackwardScanCursor.builder()
                .from(DateUtils.toDate(from))
                .to(DateUtils.toDate(to))
                .partial(partial)
                .tags(Set.of(newScanType))
                .scanType(newScanType)
                .forwardCursor(DateUtils.toDate(newForwardCursor))
                .backwardCursor(DateUtils.toDate(newBackwardCursor))
                .lastFullScan(newLastFullScan)
                .lastScanTypeCount(newLastScanTypeCount)
                .build();
    }

    private boolean isReOnboardingDue(@Nullable Date lastFullScan, @Nonnull Date now) {
        // TODO maybe also check if last onboarding was complete?
        return lastFullScan == null || now.toInstant()
                .minus(fullScanFreqInMin, ChronoUnit.MINUTES)
                .isAfter(lastFullScan.toInstant());
    }
}
