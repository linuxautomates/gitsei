package io.levelops.controlplane.triggers.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.bitbucket.models.BitbucketIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class BitbucketTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "BitbucketIterativeScanController";

    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(14);
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 14;
    private static final long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(7);
    private static final long REPO_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final TriggerActionService triggerActionService;
    private final IterativeBackwardScanCursorStrategy cursorStrategy;
    private final long repoScanFrequencyInMin;

    @Autowired
    public BitbucketTrigger(ObjectMapper objectMapper,
                            TriggerActionService triggerActionService,
                            @Value("${BITBUCKET__FULL_SCAN_FREQ_IN_MIN:}") Long fullScanFreqInMin,
                            @Value("${BITBUCKET__ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays,
                            @Value("${BITBUCKET__BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN:}") Long backwardScanSubJobSpanInMin,
                            @Value("${BITBUCKET__REPO_SCAN_FREQ_IN_MIN:}") Long repoScanFrequencyInMin) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;

        this.repoScanFrequencyInMin = ObjectUtils.firstNonNull(repoScanFrequencyInMin, REPO_SCAN_FREQ_IN_MIN);
        fullScanFreqInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        backwardScanSubJobSpanInMin = ObjectUtils.firstNonNull(backwardScanSubJobSpanInMin, DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN);

        this.cursorStrategy = new IterativeBackwardScanCursorStrategy(fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin);
        log.info("Configured Bitbucket Trigger: fullScanFreqInMin={}, onboardingSpanInDays={}, backwardScanSubJobSpanInMin={}, repoScanFrequencyInMin={}", fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin, repoScanFrequencyInMin);

    }

    @Override
    public String getTriggerType() {
        return "bitbucket";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        BitbucketTriggerMetadata metadata = parseMetadata(trigger);

        Date now = new Date();
        IterativeBackwardScanCursor cursor = cursorStrategy.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(metadata.getCursor())
                .currentBackwardCursor(metadata.getBackwardCursor())
                .lastScanType(metadata.getLastScanType())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());

        log.debug("Triggering Bitbucket iterative scan: isPartial={} ({} - {})", cursor.isPartial(), cursor.getFrom(), cursor.getTo());
        boolean shouldFetchRepos = shouldFetchRepos(metadata.getLastRepoScan(), now);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(BitbucketIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .to(cursor.getTo())
                        .shouldFetchRepos(shouldFetchRepos)
                        .build())
                .build());

        BitbucketTriggerMetadata updatedMetadata = BitbucketTriggerMetadata.builder()
                .cursor(cursor.getForwardCursor())
                .backwardCursor(cursor.getBackwardCursor())
                .lastScanType(cursor.getScanType())
                .lastFullScan(cursor.getLastFullScan())
                .lastRepoScan(shouldFetchRepos ? now : metadata.getLastRepoScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    public boolean shouldFetchRepos(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(repoScanFrequencyInMin, ChronoUnit.MINUTES));
    }


    private BitbucketTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return BitbucketTriggerMetadata.builder()
                    .build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), BitbucketTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketTriggerMetadata.BitbucketTriggerMetadataBuilder.class)
    public static class BitbucketTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("backward_cursor")
        Date backwardCursor;

        @JsonProperty("last_scan_type")
        String lastScanType;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_repo_scan")
        Date lastRepoScan;
    }
}
