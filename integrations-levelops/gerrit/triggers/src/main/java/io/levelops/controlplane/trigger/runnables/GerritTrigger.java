package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.gerrit.models.GerritIterativeScanQuery;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class GerritTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "GerritIterativeScanController";
    private static final String GERRIT = "gerrit";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final Long REPO_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    public GerritTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        GerritTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getCursor())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        boolean shouldFetchRepos = shouldFetchRepos(metadata.getLastRepoScan(), now);

        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(GerritIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .shouldFetchRepos(shouldFetchRepos)
                        .build())
                .build());
        GerritTriggerMetadata updateMetadata = GerritTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.getLastFullScan())
                .lastRepoScan(shouldFetchRepos? now : metadata.getLastRepoScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updateMetadata);
    }

    public boolean shouldFetchRepos(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(REPO_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    @Override
    public String getTriggerType() {
        return GERRIT;
    }

    private GerritTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return GerritTriggerMetadata.builder().build();
        }

        return objectMapper.convertValue(trigger.getMetadata(), GerritTriggerMetadata.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GerritTriggerMetadata.GerritTriggerMetadataBuilder.class)
    public static class GerritTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_repo_scan")
        Date lastRepoScan;
    }
}
