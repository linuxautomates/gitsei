package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * TestRails's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class TestRailsTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "TestRailsController";
    private static final String TESTRAILS = "testrails";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final Long USER_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public TestRailsTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link TestRailsTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation.
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        TestRailsTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        Boolean shouldFetchUsers = shouldFetchUsers(metadata.getLastProjectScan(), now);
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        log.debug("run: scanning projects from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(TestRailsQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .shouldFetchUsers(shouldFetchUsers)
                        .build())
                .build());
        TestRailsTriggerMetadata updatedMetadata = TestRailsTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .lastProjectScan(shouldFetchUsers ? now : metadata.getLastProjectScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return TESTRAILS;
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link TestRailsTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link TestRailsTriggerMetadata}
     */
    private TestRailsTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return TestRailsTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), TestRailsTriggerMetadata.class);
    }

    public boolean shouldFetchUsers(Date lastProjectScan, Date now) {
        return lastProjectScan == null || lastProjectScan.toInstant().isBefore(
                now.toInstant().minus(USER_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TestRailsTriggerMetadata.TestRailsTriggerMetadataBuilder.class)
    public static class TestRailsTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;

        @JsonProperty("last_project_scan")
        Date lastProjectScan;
    }
}
