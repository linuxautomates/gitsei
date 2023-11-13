package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class SonarQubeTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "SonarQubeIterativeScanController";
    private static final String SONARQUBE = "sonarqube";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final long FULL_SONARQUBE_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public SonarQubeTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link SonarQubeTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        SonarQubeTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("run: scanning projects from: {}, isFullScan: {}", from, !cursor.isPartial());
        boolean fetchOnce = fetchOnce(metadata.getLastSonarqubeFullScan(),now);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(SonarQubeIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .fetchOnce(fetchOnce)
                        .build())
                .build());
        SonarQubeTriggerMetadata updatedMetadata = SonarQubeTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .lastSonarqubeFullScan(fetchOnce?now:metadata.getLastSonarqubeFullScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private boolean fetchOnce(Date lastSonarqubeFullScan,Date now) {
        return lastSonarqubeFullScan == null || lastSonarqubeFullScan.toInstant().isBefore(
                now.toInstant().minus(FULL_SONARQUBE_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link SonarQubeTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link SonarQubeTriggerMetadata}
     */
    private SonarQubeTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return SonarQubeTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), SonarQubeTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return SONARQUBE;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SonarQubeTriggerMetadata.SonarQubeTriggerMetadataBuilder.class)
    public static class SonarQubeTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;

        @JsonProperty("last_full_sonarqube_scan")
        Date lastSonarqubeFullScan;
    }
}