package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy.DefaultCursor;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.pagerduty.models.PagerDutyIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class PagerDutyTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "PagerDutyIterativeScanController";
    private final int ONBOARDING_IN_DAYS = 90;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    public PagerDutyTrigger(@Value("${PD_FULL_SCAN_FREQ_IN_MIN:7}") Integer fullScanFrequencyInMinutes,
                            ObjectMapper objectMapper,
                            TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(fullScanFrequencyInMinutes));
        this.triggerActionService = triggerActionService;
    }

    @Override
    public String getTriggerType() {
        return "pagerduty";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering PagerDuty iterative scan: {}", trigger);
        PagerDutyTriggerMetadata metadata = parseMetadata(trigger);
        DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(new Date())
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());

        log.info("PagerDuty scan - onboard?={} ({} - {})", !cursor.isPartial(), cursor.getFrom(), cursor.getTo());

        log.debug("Instances: {}", triggerActionService);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(PagerDutyIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom() != null ? cursor.getFrom().toInstant().getEpochSecond() :
                                new Date().toInstant().minus(ONBOARDING_IN_DAYS, ChronoUnit.DAYS).getEpochSecond())
                        .to(cursor.getTo().toInstant().getEpochSecond())
                        .build())
                .build());

        log.debug("Trigger created!");
        PagerDutyTriggerMetadata updatedMetadata = PagerDutyTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : cursor.getTo())
                .lastIterativeScan(cursor.getTo())
                .build();
        log.debug("Updated metadata: {}", updatedMetadata);
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
        log.debug("Trigger updated!");
    }

    private PagerDutyTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return PagerDutyTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), PagerDutyTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PagerDutyTriggerMetadata.PagerDutyTriggerMetadataBuilder.class)
    public static class PagerDutyTriggerMetadata {
        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
