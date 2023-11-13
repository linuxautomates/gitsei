package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.models.DbListResponse;
import org.joda.time.Instant;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static io.levelops.aggregations_shared.utils.IntegrationUtils.DISABLE_SNAPSHOTTING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraEtlProcessorTest {
    @Mock
    JiraIssueService jiraIssueService;
    @Mock
    JiraFieldsStage jiraFieldsStage;
    @Mock
    IntegrationService integrationService;
    @Mock
    IntegrationTrackingService integrationTrackingService;
    @Mock
    JiraIssuesStage jiraIssuesStage;
    @Mock
    JiraSprintStage jiraSprintStage;
    @Mock
    JiraStatusStage jiraStatusStage;
    @Mock
    JiraProjectsStage jiraProjectsStage;
    @Mock
    JiraUsersStage jiraUsersStage;

    @Mock
    JiraJobState jiraJobState;

    private static final String COMPANY = "test";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private JobContext createJobContext() {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(UUID.randomUUID()).instanceId(1).build())
                .integrationId("1")
                .jobScheduledStartTime(new Date(2022, Calendar.DECEMBER, 10))
                .tenantId(COMPANY)
                .integrationType("jira")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .gcsRecords(List.of())
                .etlProcessorName("TestJobDefinition")
                .isFull(false)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
    }

    @Test
    public void testEnsureFullCheck() {
        Long dec_9 = 1670544000L;
        MockitoAnnotations.openMocks(this);
        when(integrationTrackingService.get(any(), any())).thenReturn(
                Optional.of(IntegrationTracker.builder().latestAggregatedAt(dec_9).build())
        );

        JiraEtlProcessor jiraEtlProcessor = new JiraEtlProcessor(
                jiraIssueService,
                integrationService,
                integrationTrackingService,
                jiraIssuesStage,
                jiraSprintStage,
                jiraStatusStage,
                jiraProjectsStage,
                jiraFieldsStage,
                jiraUsersStage,
                SnapshottingSettings.builder().build()
        );

        var jobContext = createJobContext().toBuilder()
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .jobScheduledStartTime(new Date(122, Calendar.DECEMBER, 10))
                .isFull(true)
                .build();

        // It's a new date, and a full so everything should be alright
        jiraEtlProcessor.preProcess(jobContext, jiraJobState);
        jobContext = jobContext.toBuilder()
                .isFull(false)
                .build();

        // It's a new date, but not full so everything should be alright
        JobContext finalJobContext = jobContext;
        assertThatThrownBy(() -> {
            jiraEtlProcessor.preProcess(finalJobContext, jiraJobState);
        }).isInstanceOf(IllegalStateException.class);

        jobContext = jobContext.toBuilder()
                .jobScheduledStartTime(new Date(122, Calendar.DECEMBER, 1))
                .isFull(false)
                .build();
        jiraEtlProcessor.preProcess(jobContext, jiraJobState);

        jobContext = jobContext.toBuilder()
                .isFull(true)
                .build();
        jiraEtlProcessor.preProcess(jobContext, jiraJobState);
    }

    @Test
    public void testEnsureFullCheckSnapshottingDisabled() {
        MockitoAnnotations.openMocks(this);
        when(integrationTrackingService.get(any(), any())).thenReturn(
                Optional.of(IntegrationTracker.builder().latestAggregatedAt(DISABLE_SNAPSHOTTING).build())
        );
        when(integrationService.listConfigs(eq(COMPANY), eq(List.of("1")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        JiraEtlProcessor jiraEtlProcessor = new JiraEtlProcessor(
                jiraIssueService, integrationService, integrationTrackingService, jiraIssuesStage, jiraSprintStage,
                jiraStatusStage, jiraProjectsStage, jiraFieldsStage, jiraUsersStage,
                SnapshottingSettings.builder().disableSnapshottingForTenants(Set.of(COMPANY)).build()
        );

        // -- regardless of the date, if snapshotting is disabled, then we should let the job go through
        var jobContext = createJobContext().toBuilder()
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .jobScheduledStartTime(new Date())
                .isFull(true)
                .build();
        jiraEtlProcessor.preProcess(jobContext, jiraJobState);

        jobContext = jobContext.toBuilder()
                .isFull(false)
                .build();
        jiraEtlProcessor.preProcess(jobContext, jiraJobState);

        // -- also, the state should contain the config version
        verify(jiraJobState, times(2)).setConfigVersion(eq(123L));

    }

    private void setupPostProcessTest(Long lastAggAtSeconds, Long nowMillis, boolean isFull, boolean disableSnapshotting, boolean deleteSnapshotData) {
        MockitoAnnotations.openMocks(this);
        when(integrationTrackingService.get(any(), any())).thenReturn(
                Optional.of(IntegrationTracker.builder().latestAggregatedAt(lastAggAtSeconds).build())
        );
        when(integrationService.listConfigs(eq(COMPANY), eq(List.of("1")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(123L).build())
                                .build()), 1));

        JiraEtlProcessor jiraEtlProcessor = new JiraEtlProcessor(
                jiraIssueService, integrationService, integrationTrackingService, jiraIssuesStage, jiraSprintStage,
                jiraStatusStage, jiraProjectsStage, jiraFieldsStage, jiraUsersStage,
                SnapshottingSettings.builder()
                        .disableSnapshottingForTenants(disableSnapshotting ? Set.of(COMPANY) : Set.of())
                        .deleteSnapshotDataForTenants(deleteSnapshotData ? Set.of(COMPANY) : Set.of())
                        .build()
        );

        // -- regardless of the date, if snapshotting is disabled, then we should let the job go through
        var jobContext = createJobContext().toBuilder()
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .jobScheduledStartTime(new Date(nowMillis))
                .isFull(isFull)
                .build();
        jiraEtlProcessor.postProcess(jobContext, jiraJobState);
    }

    @Test
    public void testPostProcessSnapshottingAlreadyDisabled() throws SQLException {
        setupPostProcessTest(DISABLE_SNAPSHOTTING, 456L, true, true, false);

        verify(integrationTrackingService, never()).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), any());
        verify(jiraIssueService, never()).cleanUpOldData(eq(COMPANY), any(), any());
    }

    @Test
    public void testPostProcessSnapshottingDisabled() throws SQLException {
        setupPostProcessTest(123L, 456L, true, true, false);

        verify(integrationTrackingService, times(1)).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), eq(DISABLE_SNAPSHOTTING));
        verify(jiraIssueService, never()).cleanUpOldData(eq(COMPANY), any(), any());
    }

    @Test
    public void testPostProcessSnapshottingDisabledWithDelete() throws SQLException {
        setupPostProcessTest(123L, 456L, true, true, true);

        verify(integrationTrackingService, times(1)).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), eq(DISABLE_SNAPSHOTTING));
        verify(jiraIssueService, times(1)).cleanUpOldData(eq(COMPANY), eq(DISABLE_SNAPSHOTTING), any());
    }

    @Test
    public void testPostProcessSnapshottingEnabled() throws SQLException {
        setupPostProcessTest(Instant.parse("2000-01-09T00:00:00Z").getMillis() / 1000, Instant.parse("2000-01-10T00:00:00Z").getMillis(),
                true, false, false);

        verify(integrationTrackingService, times(1)).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), eq(947462400L));
        verify(jiraIssueService, times(1)).cleanUpOldData(eq(COMPANY), eq(947462400L), any());
    }

    @Test
    public void testPostProcessSnapshottingEnabledOutdated() throws SQLException {
        setupPostProcessTest(Instant.parse("2000-01-10T00:00:00Z").getMillis() / 1000, Instant.parse("2000-01-09T00:00:00Z").getMillis(),
                true, false, false);

        verify(integrationTrackingService, never()).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), any());
        verify(jiraIssueService, times(1)).cleanUpOldData(eq(COMPANY), eq(947376000L), any());
    }

    @Test
    public void testPostProcessSnapshottingEnabledNotFull() throws SQLException {
        setupPostProcessTest(Instant.parse("2000-01-09T00:00:00Z").getMillis() / 1000, Instant.parse("2000-01-10T00:00:00Z").getMillis(),
                false, false, false);

        verify(integrationTrackingService, never()).upsertJiraWIDBAggregatedAt(eq(COMPANY), eq(1), any());
        verify(jiraIssueService, times(1)).cleanUpOldData(eq(COMPANY), eq(947462400L), any());
    }


}