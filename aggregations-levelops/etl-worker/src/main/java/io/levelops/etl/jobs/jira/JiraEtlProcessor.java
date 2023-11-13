package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.aggregations_shared.utils.IntegrationUtils;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.levelops.aggregations_shared.utils.IntegrationUtils.DISABLE_SNAPSHOTTING;

@Log4j2
@Service
public class JiraEtlProcessor extends BaseIngestionResultEtlProcessor<JiraJobState> {
    private final JiraIssueService issueService;
    private final IntegrationService integrationService;
    private final IntegrationTrackingService trackingService;
    private final JiraIssuesStage jiraIssuesStage;
    private final JiraSprintStage jiraSprintStage;
    private final JiraStatusStage jiraStatusStage;
    private final JiraProjectsStage jiraProjectsStage;
    private final JiraFieldsStage jiraFieldsStage;
    private final JiraUsersStage jiraUsersStage;
    private final SnapshottingSettings snapshottingSettings;

    @Autowired
    JiraEtlProcessor(JiraIssueService issueService,
                     IntegrationService integrationService,
                     IntegrationTrackingService trackingService,
                     JiraIssuesStage jiraIssuesStage,
                     JiraSprintStage jiraSprintStage,
                     JiraStatusStage jiraStatusStage,
                     JiraProjectsStage jiraProjectsStage,
                     JiraFieldsStage jiraFieldsStage,
                     JiraUsersStage jiraUsersStage,
                     SnapshottingSettings snapshottingSettings) {
        super(JiraJobState.class);
        this.issueService = issueService;
        this.integrationService = integrationService;
        this.trackingService = trackingService;
        this.jiraIssuesStage = jiraIssuesStage;
        this.jiraSprintStage = jiraSprintStage;
        this.jiraStatusStage = jiraStatusStage;
        this.jiraProjectsStage = jiraProjectsStage;
        this.jiraFieldsStage = jiraFieldsStage;
        this.jiraUsersStage = jiraUsersStage;
        this.snapshottingSettings = snapshottingSettings;
    }

    @Override
    public void preProcess(JobContext context, JiraJobState jobState) {
        if (!ensureFullJobIfNewAggregationDate((context))) {
            throw new IllegalStateException(
                    "Expected a full job, but received an incremental for: " + context.getJobInstanceId());
        }

        // -- set the job's config version to the latest integration config's updated at
        // Note: in the future, if we want to capture more changes, we can set the config version to something else
        long configUpdatedAt = IntegrationUtils.getConfigUpdatedAt(integrationService, context.getTenantId(), context.getIntegrationId(), 0L);
        jobState.setConfigVersion(configUpdatedAt);
        log.debug("job={}, configUpdatedAt={}", context.getJobInstanceId(), configUpdatedAt);
    }

    /**
     * Ensures that if this job is running with a scheduled start time greater
     * than the current latest aggregated at date, it should necessarily be a full.
     * If this is not the case then it will mess with our queries because we expect
     * a full snapshot for every new aggregated at date
     */
    private boolean ensureFullJobIfNewAggregationDate(JobContext context) {
        if (!snapshottingSettings.isSnapshottingEnabled(context.getTenantId(), context.getIntegrationId())) {
            return true;
        }
        Optional<IntegrationTracker> tracker = trackingService.get(context.getTenantId(), context.getIntegrationId());
        Optional<Long> currentLatestAggregatedAt = tracker.map(IntegrationTracker::getLatestAggregatedAt);
        var scheduledStartTime = context.getJobScheduledStartTime();
        Long truncatedScheduledStartTime = DateUtils.truncate(scheduledStartTime, Calendar.DATE);

        if (currentLatestAggregatedAt.isEmpty() || truncatedScheduledStartTime <= currentLatestAggregatedAt.get()) {
            return true;
        }
        if (context.getIsFull()) {
            return true;
        }
        log.error("Expected a full job for a new aggregation date, but received incremental. Job id : {}, " +
                        "Truncated Scheduled Start Time: {}, Scheduled Start Time: {}, Latest Aggregated At: {}",
                context.getJobInstanceId(), truncatedScheduledStartTime, context.getJobScheduledStartTime(), currentLatestAggregatedAt);
        return false;
    }

    @Override
    public void postProcess(JobContext context, JiraJobState jobState) {
        if (snapshottingSettings.isSnapshottingEnabled(context.getTenantId(), context.getIntegrationId())) {
            handleSnapshotting(context);
        } else {
            log.info("Snapshotting disabled for tenant={}", context.getTenantId());
            handleSnapshotDisabling(context);
        }

        log.info("Completed work on Jira Agg for agg job id={} ", context.getJobInstanceId());
    }

    public void handleSnapshotting(JobContext context) {
        Long ingestedAt = Objects.requireNonNull(DateUtils.truncate(context.getJobScheduledStartTime(), Calendar.DATE));

        Optional<IntegrationTracker> tracker = trackingService.get(context.getTenantId(), context.getIntegrationId());
        Optional<Long> currentLatestAggregatedAt = tracker.map(IntegrationTracker::getLatestAggregatedAt);

        // Skip the update if the latest aggregated at is more current than the one in this job. This can happen if this
        // is a manually created job, and we don't want to rewind this value here.
        // And only skip the update if snapshotting is enabled (in case snapshotting was disabled before and re-enabled, we do want to update it)
        if (currentLatestAggregatedAt.isPresent() && currentLatestAggregatedAt.get() > ingestedAt && IntegrationUtils.isSnaphottingEnabled(currentLatestAggregatedAt.get())) {
            log.info("Current tracker ingested at {} is greater than current job's ingested at date {}. Will not update tracker",
                    currentLatestAggregatedAt.get(), ingestedAt);
        } else if (!context.getIsFull()) {
            log.info("Current job instance {} is not a full, hence not attempting to update integration tracker latest aggregated at date",
                    context.getJobInstanceId());
        } else {
            try {
                log.info("Updating jira tracker latest aggregated at date to {}", ingestedAt);
                trackingService.upsertJiraWIDBAggregatedAt(context.getTenantId(), Integer.parseInt(context.getIntegrationId()), ingestedAt);
            } catch (SQLException e) {
                log.error("Error upserting latest_aggregated_at!, company {}, integrationId {}, ingestedAt {}", context.getTenantId(), context.getIntegrationId(), ingestedAt, e);
            }
        }

        //cleanup data older than 91 days.
        log.info("cleaning up data: issues count - {}",
                issueService.cleanUpOldData(context.getTenantId(),
                        ingestedAt,
                        86400 * 91L));
    }

    public void handleSnapshotDisabling(JobContext context) {
        Optional<IntegrationTracker> tracker = trackingService.get(context.getTenantId(), context.getIntegrationId());
        Optional<Long> currentLatestAggregatedAt = tracker.map(IntegrationTracker::getLatestAggregatedAt);

        if (currentLatestAggregatedAt.isPresent() && IntegrationUtils.isSnaphottingDisabled(currentLatestAggregatedAt.get())) {
            // if snapshotting was already disabled before, we don't need to do anything
            return;
        }

        // set the tracker to disabled
        try {
            log.info("Updating jira tracker to disable snapshotting");
            trackingService.upsertJiraWIDBAggregatedAt(context.getTenantId(), Integer.parseInt(context.getIntegrationId()), DISABLE_SNAPSHOTTING);
        } catch (SQLException e) {
            log.error("Error updating jira tracker to disable snapshotting", e);
        }

        // optionally, delete pre-existing snapshot data
        if (snapshottingSettings.isDeleteSnapshotDataEnabled(context.getTenantId(), context.getIntegrationId())) {
            log.info("Cleaning up snapshot data...");
            log.info("Cleaned up {} issues",
                    issueService.cleanUpOldData(context.getTenantId(),
                            DISABLE_SNAPSHOTTING,
                            86400L));
        }
    }

    @Override
    public List<IngestionResultProcessingStage<?, JiraJobState>> getIngestionProcessingJobStages() {
        return List.of(
                jiraUsersStage,
                jiraFieldsStage,
                jiraStatusStage,
                jiraProjectsStage,
                jiraSprintStage,
                jiraIssuesStage
        );
    }

    @Override
    public JiraJobState createState(JobContext context) {
        return new JiraJobState();
    }
}


