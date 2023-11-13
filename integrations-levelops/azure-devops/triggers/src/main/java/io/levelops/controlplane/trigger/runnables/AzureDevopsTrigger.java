package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.azureDevops.models.AzureDevopsIterativeScanQuery;
import io.levelops.integrations.azureDevops.utils.JobCategory;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.levelops.controlplane.trigger.strategies.JobTags.BACKWARD_SCAN_TAG;

@Log4j2
@Component
public class AzureDevopsTrigger implements TriggerRunnable {
    private static final String CONTROLLER_NAME = "AzureDevopsIterativeScanController";
    private static final String AZURE_DEVOPS = "azure_devops";

    private static final long FULL_AZURE_DEVOPS_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(14);
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 90;
    private static final long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(30);

    private final ObjectMapper objectMapper;
    private final IterativeBackwardScanCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    @Autowired
    public AzureDevopsTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService,
                              @Value("${AZURE_FULL_SCAN_FREQ_IN_MIN:}") Long fullScanFreqInMin,
                              @Value("${AZURE_ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays,
                              @Value("${AZURE_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN:}") Long backwardScanSubJobSpanInMin
                              ) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;

        fullScanFreqInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        backwardScanSubJobSpanInMin = ObjectUtils.firstNonNull(backwardScanSubJobSpanInMin, DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN);
        this.cursorStrategy = new IterativeBackwardScanCursorStrategy(fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin);
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link AzureDevopsTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        AzureDevopsTriggerMetadata metadata = parseMetadata(trigger);
        log.debug("AzureDevopsTrigger metadata = {}", metadata);
        Date now = new Date();

        IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor cursor = cursorStrategy.getNextCursor(IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(metadata.getCursor())
                .currentBackwardCursor(metadata.getBackwardCursor())
                .lastScanType(metadata.getLastScanType())
                .lastFullScan(metadata.getLastFullScan())
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .now(now)
                .build());
        log.debug("AzureDevopsTrigger cursor = {}", cursor);
        boolean fetchOnce = fetchOnce(metadata.getLastAzureDevopsFullScan(), now);
        log.debug("AzureDevopsTrigger fetchOnce = {}", fetchOnce);
        Set<String> tags;
        if (cursor.getTags().contains(BACKWARD_SCAN_TAG)) {
            int jobIndex = 0; // LEV-3909 - to make sure the first reonboarding job's partial is set to false
            for (var jobCategory : JobCategory.values()) {
                tags = new HashSet<>(cursor.getTags());
                tags.add(jobCategory.name());
                triggerActionService.createTriggeredJob(trigger, jobIndex != 0 || cursor.isPartial(), CreateJobRequest.builder()
                        .controllerName(CONTROLLER_NAME)
                        .tags(tags)
                        .query(AzureDevopsIterativeScanQuery.builder()
                                .integrationKey(trigger.getIntegrationKey())
                                .from(cursor.getFrom())
                                .to(cursor.getTo())
                                .fetchOnce(fetchOnce)//ToDo: VA - Fix this
                                .fetchAllIterations(!cursor.isPartial())
                                .fetchMetadata(!cursor.isPartial())
                                .jobCategory(jobCategory)
                                .build())
                        .build());
                jobIndex++;
            }
        } else {
            triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                    .controllerName(CONTROLLER_NAME)
                    .tags(cursor.getTags())
                    .query(AzureDevopsIterativeScanQuery.builder()
                            .integrationKey(trigger.getIntegrationKey())
                            .from(cursor.getFrom())
                            .to(cursor.getTo())
                            .fetchOnce(fetchOnce)//ToDo: VA - Fix this
                            .fetchAllIterations(!cursor.isPartial())
                            .fetchMetadata(!cursor.isPartial())
                            .build())
                    .build());
        }



        AzureDevopsTriggerMetadata updatedMetadata = AzureDevopsTriggerMetadata.builder()
                .cursor(cursor.getForwardCursor())
                .backwardCursor(cursor.getBackwardCursor())
                .lastScanType(cursor.getScanType())
                .lastFullScan(cursor.getLastFullScan())
                .lastAzureDevopsFullScan(fetchOnce ? now : metadata.getLastAzureDevopsFullScan())
                .build();
        log.debug("AzureDevopsTrigger updatedMetadata = {}", updatedMetadata);
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private boolean fetchOnce(Date lastAzureDevopsFullScan, Date now) {
        return lastAzureDevopsFullScan == null || lastAzureDevopsFullScan.toInstant().isBefore(
                now.toInstant().minus(FULL_AZURE_DEVOPS_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link AzureDevopsTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link AzureDevopsTriggerMetadata}
     */
    private AzureDevopsTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return AzureDevopsTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), AzureDevopsTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return AZURE_DEVOPS;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AzureDevopsTriggerMetadata.AzureDevopsTriggerMetadataBuilder.class)
    public static class AzureDevopsTriggerMetadata {
        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("backward_cursor")
        Date backwardCursor;

        @JsonProperty("last_scan_type")
        String lastScanType;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_full_azure_devops_scan")
        Date lastAzureDevopsFullScan;
    }
}
