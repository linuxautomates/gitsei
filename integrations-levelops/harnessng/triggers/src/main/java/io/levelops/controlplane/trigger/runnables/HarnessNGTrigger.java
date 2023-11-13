package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy;
import io.levelops.integrations.harnessng.models.HarnessNGIngestionQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * HarnessNG's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class HarnessNGTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "HarnessNGController";
    private static final String HARNESSNG = "harnessng";
    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(14);
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 30;
    private static final long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(5);

    private final ObjectMapper objectMapper;
    private final IterativeBackwardScanCursorStrategy iterativeBackwardScanCursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    HarnessNGTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService,
                     @Value("${HARNESSNG_FULL_SCAN_FREQ_IN_MIN:}") Long fullScanFreqInMin,
                     @Value("${HARNESSNG_ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays,
                     @Value("${HARNESSNG_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN:}") Long backwardScanSubJobSpanInMin) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        fullScanFreqInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        backwardScanSubJobSpanInMin = ObjectUtils.firstNonNull(backwardScanSubJobSpanInMin, DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN);
        this.iterativeBackwardScanCursorStrategy = new IterativeBackwardScanCursorStrategy(fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin);
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        HarnessNGTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor cursor = iterativeBackwardScanCursorStrategy.getNextCursor(IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(metadata.getCursor())
                .currentBackwardCursor(metadata.getBackwardCursor())
                .lastScanType(metadata.getLastScanType())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.getFrom();
        log.debug("run: scanning  from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(HarnessNGIngestionQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(cursor.getTo())
                        .build())
                .build());
        HarnessNGTriggerMetadata updatedMetadata = HarnessNGTriggerMetadata.builder()
                .cursor(cursor.getForwardCursor())
                .backwardCursor(cursor.getBackwardCursor())
                .lastScanType(cursor.getScanType())
                .lastFullScan(cursor.getLastFullScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return HARNESSNG;
    }

    private HarnessNGTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return HarnessNGTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), HarnessNGTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = HarnessNGTriggerMetadata.HarnessNGTriggerMetadataBuilder.class)
    public static class HarnessNGTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("backward_cursor")
        Date backwardCursor;

        @JsonProperty("last_scan_type")
        String lastScanType;

        @JsonProperty("last_full_scan")
        Date lastFullScan;
    }
}
