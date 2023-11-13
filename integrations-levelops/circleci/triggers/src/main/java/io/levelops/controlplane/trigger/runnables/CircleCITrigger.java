package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.integrations.circleci.models.CircleCIIngestionQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * CircleCI's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class CircleCITrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "CircleCIController";
    private static final String CIRCLECI = "circleci";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 90;


    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    CircleCITrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService,
                    @Value("${CIRCLECI_ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS), onboardingSpanInDays);
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        CircleCITriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        log.debug("run: scanning  from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(CircleCIIngestionQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(now)
                        .build())
                .build());
        CircleCITriggerMetadata updatedMetadata = CircleCITriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return CIRCLECI;
    }

    private CircleCITriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return CircleCITriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), CircleCITriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CircleCITriggerMetadata.CircleCITriggerMetadataBuilder.class)
    public static class CircleCITriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
