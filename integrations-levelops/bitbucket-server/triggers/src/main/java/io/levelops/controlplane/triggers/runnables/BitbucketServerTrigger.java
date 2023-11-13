package io.levelops.controlplane.triggers.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class BitbucketServerTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "BitbucketServerIterativeScanController";

    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(7);
    private static final long REPO_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final TriggerActionService triggerActionService;
    private final DefaultCursorStrategy cursorStrategy;

    @Autowired
    public BitbucketServerTrigger(ObjectMapper objectMapper,
                                  TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(DEFAULT_FULL_SCAN_FREQ_IN_MIN);
    }

    @Override
    public String getTriggerType() {
        return IntegrationType.BITBUCKET_SERVER.toString();
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        BitbucketServerTriggerMetadata metadata = parseMetadata(trigger);
        log.debug("Triggering Template iterative scan: {}", trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());

        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("Triggering Bitbucket Server iterative scan: isPartial={} ({} - {})", cursor.isPartial(), cursor.getFrom(), cursor.getTo());
        boolean shouldFetchRepos = shouldFetchRepos(metadata.getLastIterativeScan(), now);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(BitbucketServerIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .shouldFetchRepos(shouldFetchRepos)
                        .build())
                .build());
        BitbucketServerTriggerMetadata updatedMetadata = BitbucketServerTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .lastBitbucketServerFullScan(cursor.getTo())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private boolean shouldFetchRepos(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(REPO_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }


    private BitbucketServerTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return BitbucketServerTriggerMetadata.builder()
                    .build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), BitbucketServerTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketServerTriggerMetadata.BitbucketServerTriggerMetadataBuilder.class)
    public static class BitbucketServerTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;

        @JsonProperty("last_full_bitbucket_server_scan")
        Date lastBitbucketServerFullScan;
    }
}
