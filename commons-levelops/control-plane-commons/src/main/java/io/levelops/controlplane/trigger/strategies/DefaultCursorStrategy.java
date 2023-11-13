package io.levelops.controlplane.trigger.strategies;

import io.levelops.commons.dates.DateUtils;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

public class DefaultCursorStrategy implements CursorStrategy<DefaultCursorStrategy.DefaultCursor, DefaultCursorStrategy.DefaultCursorMetadata> {

    private final long fullScanFreqInMin;
    private final Long onboardingSpanInDays;

    public DefaultCursorStrategy(long fullScanFreqInMin) {
        this(fullScanFreqInMin, null);
    }

    /**
     * Create cursor strategy
     * @param fullScanFreqInMin how often to re-onboard
     * @param onboardingSpanInDays how far back the re-onboarding goes - if empty, "from" will be null
     */
    public DefaultCursorStrategy(long fullScanFreqInMin,
                                 @Nullable Long onboardingSpanInDays) {
        this.fullScanFreqInMin = fullScanFreqInMin;
        this.onboardingSpanInDays = onboardingSpanInDays;
    }

    @Value
    @Builder(toBuilder = true)
    public static class DefaultCursor implements CursorStrategy.Cursor {
        Date from;
        Date to; // use as new cursor
        Date lastFullScan;
        boolean partial;
        Set<String> tags;
    }

    @Value
    @Builder(toBuilder = true)
    public static class DefaultCursorMetadata implements CursorStrategy.CursorMetadata {
        @Nullable Date currentCursor; // previous "to"
        @Nullable Date lastFullScan; // when onboarding scan was last triggered (t0 in t(-X) -> t0)
        @Nonnull Long triggerCreatedAtEpoch;
        @Nonnull Date now;
    }

    @Override
    public DefaultCursor getNextCursor(DefaultCursorMetadata currentCursorMetadata) {
        Date from;
        Date to;
        boolean partial;
        Date newLastFullScan = currentCursorMetadata.getLastFullScan();

        boolean firstOnboardingEver = (currentCursorMetadata.getCurrentCursor() == null);
        boolean reOnboardingDue = isReOnboardingDue(currentCursorMetadata.getLastFullScan(), currentCursorMetadata.getNow());
        if (firstOnboardingEver || reOnboardingDue) {
            to = firstOnboardingEver
                    ? DateUtils.fromEpochSecondToDate(currentCursorMetadata.getTriggerCreatedAtEpoch(), currentCursorMetadata.getNow()) // for the first onboarding ever, use the trigger created at
                    : currentCursorMetadata.getNow(); // for re-onboarding, just use 'now'
            from = (onboardingSpanInDays == null) ? null : Date.from(to.toInstant().minus(Duration.ofDays(onboardingSpanInDays)));
            partial = false;
            newLastFullScan = to;
        } else {
            from = currentCursorMetadata.getCurrentCursor();
            to = currentCursorMetadata.getNow();
            partial = true;
        }
        return DefaultCursor.builder()
                .from(from)
                .to(to)
                .partial(partial)
                .lastFullScan(newLastFullScan)
                .tags(Set.of(partial? JobTags.FORWARD_SCAN_TAG : JobTags.BACKWARD_SCAN_TAG))
                .build();
    }

    private boolean isReOnboardingDue(@Nullable Date lastFullScan, @Nonnull Date now) {
        return lastFullScan == null || now.toInstant()
                .minus(fullScanFreqInMin, ChronoUnit.MINUTES)
                .isAfter(lastFullScan.toInstant());
    }
}
