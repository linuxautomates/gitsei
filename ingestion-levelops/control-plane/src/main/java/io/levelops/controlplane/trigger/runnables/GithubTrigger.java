package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata;
import io.levelops.ingestion.integrations.github.models.GithubIterativeScanQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class GithubTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "GithubIterativeScanController";

    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(14);
    private static final long DEFAULT_USER_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 14;
    private static final long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(7);
    private static final long REPO_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);
    // Set this to large number because we will only ever be using this strategy for 1 onboarding scan and then
    // switch over to the default strategy.
    private static final long HISTORICAL_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1000);
    private static final long HISTORICAL_SPAN_IN_DAYS = 90;
    private static final long HISTORICAL_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(10);
    private static final int HISTORICAL_SUCCESSIVE_BACKWARD_SCAN_COUNT = 2;

    private final ObjectMapper objectMapper;
    private final TriggeredJobService triggeredJobService;
    private final TriggerDatabaseService triggerDatabaseService;
    private final IterativeBackwardScanCursorStrategy cursorStrategy;
    private final Long repoScanFrequencyInMin;
    private final Long tagScanFrequencyInMin;
    private final Long userScanFrequencyInMin;

    private final long historicalSpanInDays;
    private final long historicalSubJobSpanInMin;
    private final int historicalSuccessiveBackwardScanCount;

    @Autowired
    public GithubTrigger(ObjectMapper objectMapper,
                         TriggeredJobService triggeredJobService,
                         TriggerDatabaseService triggerDatabaseService,
                         @Value("${GITHUB__FULL_SCAN_FREQ_IN_MIN:}") Long fullScanFreqInMin,
                         @Value("${GITHUB__USER_SCAN_FREQ_IN_MIN:}") Long userScanFrequencyInMin,
                         @Value("${GITHUB__ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays,
                         @Value("${GITHUB__BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN:}") Long backwardScanSubJobSpanInMin,
                         @Value("${GITHUB__REPO_SCAN_FREQ_IN_MIN:}") Long repoScanFrequencyInMin,
                         @Value("${GITHUB__HISTORICAL_SPAN_IN_DAYS:}") Long historicalSpanInDays,
                         @Value("${GITHUB__HISTORICAL_SUB_JOB_SPAN_IN_MIN:}") Long historicalSubJobSpanInMin,
                         @Value("${GITHUB__HISTORICAL_SUCCESSIVE_BACKWARD_SCAN_COUNT:}") Integer historicalSuccessiveBackwardScanCount) {
        this.objectMapper = objectMapper;
        this.triggeredJobService = triggeredJobService;
        this.triggerDatabaseService = triggerDatabaseService;

        this.repoScanFrequencyInMin = ObjectUtils.firstNonNull(repoScanFrequencyInMin, REPO_SCAN_FREQ_IN_MIN);
        this.tagScanFrequencyInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        this.userScanFrequencyInMin = ObjectUtils.firstNonNull(userScanFrequencyInMin, DEFAULT_USER_SCAN_FREQ_IN_MIN);
        fullScanFreqInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        backwardScanSubJobSpanInMin = ObjectUtils.firstNonNull(backwardScanSubJobSpanInMin, DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN);

        cursorStrategy = new IterativeBackwardScanCursorStrategy(fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin);

        this.historicalSpanInDays = ObjectUtils.firstNonNull(historicalSpanInDays, HISTORICAL_SPAN_IN_DAYS);
        this.historicalSubJobSpanInMin = ObjectUtils.firstNonNull(historicalSubJobSpanInMin, HISTORICAL_SUB_JOB_SPAN_IN_MIN);
        this.historicalSuccessiveBackwardScanCount = ObjectUtils.firstNonNull(historicalSuccessiveBackwardScanCount, HISTORICAL_SUCCESSIVE_BACKWARD_SCAN_COUNT);

        log.info("Configured Github Trigger: fullScanFreqInMin={}, onboardingSpanInDays={}, backwardScanSubJobSpanInMin={}, repoScanFrequencyInMin={}", fullScanFreqInMin, onboardingSpanInDays, backwardScanSubJobSpanInMin, repoScanFrequencyInMin);
    }

    @Override
    public String getTriggerType() {
        return "github";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        GithubTriggerMetadata metadata = parseMetadata(trigger);

        IterativeBackwardScanCursorStrategy longHistoryStrategy = createLongHistoryStrategy(metadata);

        Date now = new Date();

        IterativeBackwardScanCursor cursor;
        IterativeBackwardScanCursorMetadata nextMetadata;
        IterativeBackwardScanCursorStrategy nextStrategy;
        GithubTriggerMetadata updatedMetadata;
        boolean shouldContinueFetchingHistory = false;

        if (BooleanUtils.isTrue(metadata.getShouldFetchHistory())
                && BooleanUtils.isTrue(metadata.getShouldStartFetchingHistory())) {
            log.info("Switching over to history strategy for trigger {}", trigger.getId());
            nextMetadata = IterativeBackwardScanCursorMetadata.builder()
                    .currentBackwardCursor(null)
                    .currentForwardCursor(null)
                    .lastFullScan(null)
                    .lastScanType(null)
                    .triggerCreatedAtEpoch(trigger.getCreatedAt())
                    .now(now)
                    .lastScanTypeCount(null)
                    .build();
        } else {
            nextMetadata = IterativeBackwardScanCursorMetadata.builder()
                    .currentForwardCursor(metadata.getCursor())
                    .currentBackwardCursor(metadata.getBackwardCursor())
                    .lastScanType(metadata.getLastScanType())
                    .lastFullScan(metadata.getLastFullScan())
                    .triggerCreatedAtEpoch(trigger.getCreatedAt())
                    .now(now)
                    .lastScanTypeCount(metadata.getLastScanTypeCount())
                    .build();
        }

        if (BooleanUtils.isTrue(metadata.getShouldFetchHistory())) {
            nextStrategy = longHistoryStrategy;
        } else {
            nextStrategy = cursorStrategy;
        }

        cursor = nextStrategy.getNextCursor(nextMetadata);
        boolean shouldFetchRepos = shouldFetchRepos(metadata.getLastRepoScan(), now);
        boolean shouldFetchTags = shouldFetchTags(metadata.getLastFullScan(), now);
        boolean shouldFetchUsers = shouldFetchUsers(metadata.getLastUserScan(), now);

        triggeredJobService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(GithubIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .to(cursor.getTo())
                        .shouldFetchAllCards(!cursor.isPartial())
                        .shouldFetchRepos(shouldFetchRepos)
                        .shouldFetchTags(shouldFetchTags)
                        .shouldFetchUsers(shouldFetchUsers)
                        .build())
                .build());

        if (BooleanUtils.isTrue(metadata.getShouldFetchHistory())) {
            if (cursor.getBackwardCursor().before(cursor.getLastFullScan())) {
                shouldContinueFetchingHistory = true;
                log.info("Continuing long history strategy for trigger {}", trigger.getId());
            }
        }

        updatedMetadata = GithubTriggerMetadata.builder()
                .cursor(cursor.getForwardCursor())
                .backwardCursor(cursor.getBackwardCursor())
                .lastScanType(cursor.getScanType())
                .lastFullScan(cursor.getLastFullScan())
                .lastRepoScan(shouldFetchRepos ? now : metadata.getLastRepoScan())
                .lastUserScan(shouldFetchUsers ? now : metadata.getLastUserScan())
                .lastScanTypeCount(cursor.getLastScanTypeCount())
                .shouldStartFetchingHistory(false)
                .shouldFetchHistory(shouldContinueFetchingHistory)
                .historicalSpanInDays(metadata.historicalSpanInDays)
                .historicalSubJobSpanInMin(metadata.historicalSubJobSpanInMin)
                .historicalSuccesiveBackwardScanCount(metadata.historicalSuccesiveBackwardScanCount)
                .build();
        log.info("Updated trigger metadata: {}", updatedMetadata);

        triggerDatabaseService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    public boolean shouldFetchRepos(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(repoScanFrequencyInMin, ChronoUnit.MINUTES));
    }

    public boolean shouldFetchTags(Date lastTagScan, Date now) {
        return lastTagScan == null || lastTagScan.toInstant().isBefore(
                now.toInstant().minus(tagScanFrequencyInMin, ChronoUnit.MINUTES));
    }

    public boolean shouldFetchUsers(Date lastUserScan, Date now) {
        return lastUserScan== null || lastUserScan.toInstant().isBefore(
                now.toInstant().minus(userScanFrequencyInMin, ChronoUnit.MINUTES));
    }


    private GithubTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return GithubTriggerMetadata.builder()
                    .build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), GithubTriggerMetadata.class);
    }

    private IterativeBackwardScanCursorStrategy createLongHistoryStrategy(GithubTriggerMetadata triggerMetadata) {
        long finalHistoricalSpanInDays = ObjectUtils.firstNonNull(triggerMetadata.historicalSpanInDays, historicalSpanInDays);
        long finalHistoricalSubJobSpanInMin = ObjectUtils.firstNonNull(triggerMetadata.historicalSubJobSpanInMin, historicalSubJobSpanInMin);
        int finalHistoricalSuccessiveBackwardScanCount = ObjectUtils.firstNonNull(triggerMetadata.historicalSuccesiveBackwardScanCount, historicalSuccessiveBackwardScanCount);

        return new IterativeBackwardScanCursorStrategy(
                HISTORICAL_FULL_SCAN_FREQ_IN_MIN,
                finalHistoricalSpanInDays,
                finalHistoricalSubJobSpanInMin,
                finalHistoricalSuccessiveBackwardScanCount);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubTriggerMetadata.GithubTriggerMetadataBuilder.class)
    public static class GithubTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("backward_cursor")
        Date backwardCursor;

        @JsonProperty("last_scan_type")
        String lastScanType;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_repo_scan")
        Date lastRepoScan;

        @JsonProperty("last_user_scan")
        Date lastUserScan;

        @JsonProperty("last_scan_type_count")
        Integer lastScanTypeCount;

        // This flag remains switched on until we have completed fetching the history
        @JsonProperty("should_fetch_history")
        Boolean shouldFetchHistory;

        // This flag is meant to be enabled only once when we want to start fetching history. It helps us set up the
        // initial arguments for the history strategy
        @JsonProperty("should_start_fetching_history")
        Boolean shouldStartFetchingHistory;

        // The historical_* properties control the historical cursor strategy
        // dynamically. Sensible defaults are provided if these are not set.
        @JsonProperty("historical_span_in_days")
        Long historicalSpanInDays;

        @JsonProperty("historical_sub_job_span_in_min")
        Long historicalSubJobSpanInMin;

        @JsonProperty("historical_successive_backward_scan_count")
        Integer historicalSuccesiveBackwardScanCount;
    }
}
