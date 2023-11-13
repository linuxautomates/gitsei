package io.levelops.etl.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.aggregations_shared.utils.IntegrationUtils;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier.ShouldTakeFull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.levelops.commons.etl.models.DbJobInstance.SCHEDULER_CREATED_TAG;

@Log4j2
public class FullOrIncrementalUtils {

    private static final String LAST_CONFIG_VERSION_METADATA_FIELD = "last_config_version";

    // TODO replace this by a formal JobDefinitionMetadata class
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ConfigVersionMetadata.ConfigVersionMetadataBuilder.class)
    public static class ConfigVersionMetadata {
        @JsonProperty("last_config_version")
        Long lastConfigVersion;

        @Nonnull
        public static ConfigVersionMetadata fromJobDefinition(DbJobDefinition jobDefinition) {
            if (jobDefinition == null || jobDefinition.getMetadata() == null) {
                return ConfigVersionMetadata.builder().build();
            }
            return DefaultObjectMapper.get().convertValue(jobDefinition.getMetadata(), ConfigVersionMetadata.class);
        }
    }

    /**
     * If snapshotting is disabled, checks the integration's config version to make the decision.
     * Otherwise, fallbacks to the original behavior of {@link FullOrIncrementalUtils#shouldTakeLastAggregatedDateBasedFull(DbJobDefinition, IntegrationTrackingService, Instant)}.
     */
    public static ShouldTakeFull shouldTakeFullBasedOnSnapshotDisablingLogic(
            DbJobDefinition jobDefinition,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            IntegrationService integrationService,
            IntegrationTrackingService integrationTrackingService,
            SnapshottingSettings snapshottingSettings,
            Instant now
    ) {
        String tenantId = jobDefinition.getTenantId();
        String integrationId = jobDefinition.getIntegrationId();
        log.debug("entering shouldTakeFullBasedOnSnapshotDisablingLogic for jobDefinitionId={}, tenantId={}, integrationId={}", jobDefinition.getId(), tenantId, integrationId);

        // -- if snapshotting is disabled, fallback to the original behavior
        if (snapshottingSettings.isSnapshottingEnabled(tenantId, integrationId)) {
            log.debug("snapshotting not enabled, using aggregated at for jobDefinitionId={}", jobDefinition.getId());
            return ShouldTakeFull.of(shouldTakeLastAggregatedDateBasedFull(jobDefinition, integrationTrackingService, now));
        }

        log.debug("snapshotting is disabled for jobDefinitionId={}", jobDefinition.getId());

        // -- if snapshotting is enabled, then we can't rely on last aggregated at since it will be hardcoded to MAX_LONG
        // get the current version for that integration
        long latestConfigVersion = IntegrationUtils.getConfigUpdatedAt(integrationService, tenantId, integrationId, 0L);

        // compare it to the last version used by ETL
        Long lastConfigVersion = ConfigVersionMetadata.fromJobDefinition(jobDefinition).getLastConfigVersion();
        log.debug("jobDefinitionId={}, lastConfigVersion={}, latestConfigVersion={}", jobDefinition.getId(), lastConfigVersion, latestConfigVersion);
        if (lastConfigVersion == null || latestConfigVersion > lastConfigVersion) {
            // if we are out-of-date, take a full scan and signal to update the metadata with the latest version
            log.debug("config version out-of-date, taking full scan for jobDefinitionId={}", jobDefinition.getId());
            return ShouldTakeFull.builder()
                    .takeFull(true)
                    .metadataUpdate(Map.of(LAST_CONFIG_VERSION_METADATA_FIELD, latestConfigVersion))
                    .build();
        }

        // -- for consolidation purposes, we can take a periodic full scan:
        if (shouldTakeScheduleBasedFull(jobDefinition, jobInstanceDatabaseService, snapshottingSettings.getPeriodicReprocessingInMinutes(), now)) {
            log.debug("periodic rescan for jobDefinitionId={}", jobDefinition.getId());
            return ShouldTakeFull.of(true);
        }

        // if we are up-to-date, no need to take full
        log.debug("config version up to date, taking partial scan for jobDefinitionId={}", jobDefinition.getId());
        return ShouldTakeFull.of(false);
    }


    /**
     * For Jira/ADO our full logic depends on whether the last_aggregated_at
     * has changed in the integration tracker. This is because our queries
     * expect that a complete new snapshot is available every single day.
     * <p>
     * This function looks at the integration tracker last aggregated at date,
     * and compares that to today's date. If today is later than the last
     * aggregated date it takes a full, else takes an incremental.
     */
    public static boolean shouldTakeLastAggregatedDateBasedFull(
            DbJobDefinition jobDefinition,
            IntegrationTrackingService integrationTrackingService,
            Instant now
    ) {
        Long latestAggregatedAt = integrationTrackingService.get(jobDefinition.getTenantId(), jobDefinition.getIntegrationId())
                .map(IntegrationTracker::getLatestAggregatedAt)
                .orElse(null);
        if (latestAggregatedAt == null) {
            log.info("No integration tracker present, taking full for tenant {}, integration {} {}",
                    jobDefinition.getTenantId(), jobDefinition.getIntegrationId(), jobDefinition.getIntegrationType());
            return true;
        }
        if (IntegrationUtils.isSnaphottingDisabled(latestAggregatedAt)) {
            log.info("Snapshotting used to be disabled before, taking full to generate new snapshot. jobDefId={}", jobDefinition.getId());
            return true;
        }
        Long lastAggregatedAtDate = Objects.requireNonNull(DateUtils.truncate(DateUtils.fromEpochSecondToDate(latestAggregatedAt), Calendar.DATE));
        Long nowTruncatedToDate = Objects.requireNonNull(DateUtils.truncate(Date.from(now), Calendar.DATE));
        if (nowTruncatedToDate > lastAggregatedAtDate) {
            log.info("Date changed - taking full, jobDefinition id: {}, lastAggregatedAt {}, now {}", jobDefinition.getId(), lastAggregatedAtDate, nowTruncatedToDate);
            return true;
        }
        if (nowTruncatedToDate.equals(lastAggregatedAtDate)) {
            log.info("Date remained same - taking incremental, jobDefinition id: {}, lastAggregatedAt {}, now {}", jobDefinition.getId(), lastAggregatedAtDate, nowTruncatedToDate);
            return false;
        } else {
            throw new IllegalStateException("Current time is before latest aggregated at date. This should never happen. " +
                    "JobDefinition Id: " + jobDefinition.getId() + ", Current date: " + nowTruncatedToDate + ", lastAggregatedAt: " + lastAggregatedAtDate);
        }
    }

    /**
     * This function makes a full/incremental decision based on the full frequency
     * of the job definition. If the duration between the start time of the last
     * successful full agg and now is greater than the full frequency we will
     * take a full
     */
    public static boolean shouldTakeScheduleBasedFull(
            DbJobDefinition jobDefinition,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            @Nullable Integer overrideFullFrequencyInMinutes,
            Instant now
    ) {
        int fullFrequency = ObjectUtils.firstNonNull(overrideFullFrequencyInMinutes, jobDefinition.getFullFrequencyInMinutes(), 0);
        if (fullFrequency <= 0) {
            log.debug("Taking incremental for job definition {}, full-scan scheduling disabled.", jobDefinition.getId());
            return false;
        }

        DbListResponse<DbJobInstance> lastFullResponse = jobInstanceDatabaseService.filter(0, 1, DbJobInstanceFilter.builder()
                .jobDefinitionIds(List.of(jobDefinition.getId()))
                .isFull(true)
                .tags(List.of(SCHEDULER_CREATED_TAG))
                .jobStatuses(List.of(JobStatus.SUCCESS))
                .orderBy(List.of(
                        DbJobInstanceFilter.JobInstanceOrderByField.builder()
                                .orderByColumn(DbJobInstanceFilter.JobInstanceOrderBy.INSTANCE_ID)
                                .isAscending(false)
                                .build()))
                .build());

        if (lastFullResponse.getCount() <= 0) {
            log.info("No full found for job definition {}, tenant {}, integration id {}. Taking full now",
                    jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId());
            return true;
        }

        Instant lastFullStartTime = lastFullResponse.getRecords().get(0).getStartTime();
        Duration duration = Duration.between(lastFullStartTime, now);
        if (duration.toMinutes() > fullFrequency) {
            log.debug("Taking full for job definition {}, lastFullStartTime = {}, fullFrequency = {}, " +
                    "elapsed time = {}", jobDefinition.getId(), lastFullStartTime, fullFrequency, duration);
            return true;
        } else {
            log.debug("Taking incremental for job definition {}, lastFullStartTime = {}, fullFrequency = {}, " +
                    "elapsed time = {}", jobDefinition.getId(), lastFullStartTime, fullFrequency, duration);
            return false;
        }
    }

    public static boolean shouldTakeScheduleBasedFull(
            DbJobDefinition jobDefinition,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            Instant now
    ) {
        return shouldTakeScheduleBasedFull(jobDefinition, jobInstanceDatabaseService, null, now);
    }
}
