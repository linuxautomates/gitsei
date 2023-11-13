package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.integrations.droneci.models.DroneCIIngestionQuery;
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
 * DroneCI's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class DroneCITrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "DroneCIController";
    private static final String DRONECI = "droneci";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 90;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    DroneCITrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService,
                   @Value("${DRONECI__ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS), onboardingSpanInDays);
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        DroneCITriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.getFrom();
        log.debug("run: scanning  from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(DroneCIIngestionQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(now)
                        .build())
                .build());
        DroneCITriggerMetadata updatedMetadata = DroneCITriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return DRONECI;
    }

    private DroneCITriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return DroneCITriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), DroneCITriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DroneCITriggerMetadata.DroneCITriggerMetadataBuilder.class)
    public static class DroneCITriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
