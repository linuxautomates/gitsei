package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.checkmarx.models.cxsast.CxSastIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * CxSast's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class CxSastTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "CxSastIterativeScanController";
    private static final String CXSAST = "cxsast";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final Long PROJECTS_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public CxSastTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link CxSastTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        CxSastTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("run: scanning tickets from: {}, isFullScan: {}", from, !cursor.isPartial());
        boolean fetchOnce = shouldFetch(metadata.getLastIterativeScan(), now);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(CxSastIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .fetchOnce(fetchOnce)
                        .build())
                .build());
        CxSastTriggerMetadata updatedMetadata = CxSastTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    public boolean shouldFetch(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(PROJECTS_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link CxSastTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link CxSastTriggerMetadata}
     */
    private CxSastTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return CxSastTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), CxSastTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return CXSAST;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxSastTriggerMetadata.CxSastTriggerMetadataBuilder.class)
    public static class CxSastTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
