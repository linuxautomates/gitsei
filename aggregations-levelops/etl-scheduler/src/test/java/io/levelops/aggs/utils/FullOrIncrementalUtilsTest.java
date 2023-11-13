package io.levelops.aggs.utils;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.etl.utils.FullOrIncrementalUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class FullOrIncrementalUtilsTest {
    @Mock
    IntegrationTrackingService integrationTrackingService;

    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    IntegrationService integrationService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLastAggregatedDateBasedFull() {
        Instant now = Instant.now();
        Instant now_minus_1_day = now.minus(1, ChronoUnit.DAYS);
        Long now_minus_1_epoch = DateUtils.truncate(Date.from(now_minus_1_day), Calendar.DATE);
        Long now_epoch = DateUtils.truncate(Date.from(now), Calendar.DATE);

        lastAggregatedDateBasedFullHelper(now, now_minus_1_epoch, true);
        lastAggregatedDateBasedFullHelper(now, now_epoch, false);
    }

    private void lastAggregatedDateBasedFullHelper(Instant now, Long lastAggregatedAt, boolean expectedFull) {
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .fullFrequencyInMinutes(30)
                .tenantId("1")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        when(integrationTrackingService.get(any(), any())).thenReturn(
                Optional.ofNullable(IntegrationTracker.builder()
                        .latestAggregatedAt(lastAggregatedAt)
                        .build())
        );

        boolean shouldTakeFull = FullOrIncrementalUtils.shouldTakeLastAggregatedDateBasedFull(
                jobDefinition, integrationTrackingService, now
        );
        assertThat(shouldTakeFull).isEqualTo(expectedFull);
        reset(integrationTrackingService);
    }

    @Test
    public void testShouldTakeScheduleBasedFull() {
        Instant now = Instant.now();
        scheduleBasedFullTestHelper(now, 30, null, true);

        DbJobInstance instance = DbJobInstance.builder()
                .jobDefinitionId(UUID.randomUUID())
                .instanceId(1)
                .startTime(now.minus(31, ChronoUnit.MINUTES))
                .build();
        scheduleBasedFullTestHelper(now, 30, instance, true);

        instance = instance.toBuilder().startTime(now).build();
        scheduleBasedFullTestHelper(now, 30, instance, false);
    }

    private void scheduleBasedFullTestHelper(Instant now, int fullFrequency, DbJobInstance previousFull, boolean expectedFull) {
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .fullFrequencyInMinutes(fullFrequency)
                .tenantId("1")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        DbJobInstance jobInstance = DbJobInstance.builder()
                .jobDefinitionId(UUID.randomUUID())
                .instanceId(1)
                .build();
        List<DbJobInstance> previousFulls = new ArrayList<>();
        if (Objects.nonNull(previousFull)) {
            previousFulls.add(previousFull);
        }
        when(jobInstanceDatabaseService.filter(any(), any(), any())).thenReturn(DbListResponse.of(
                previousFulls, previousFulls.size()
        ));
        var shouldTakeFull = FullOrIncrementalUtils.shouldTakeScheduleBasedFull(jobDefinition, jobInstanceDatabaseService, now);
        assertThat(shouldTakeFull).isEqualTo(expectedFull);
        reset(jobInstanceDatabaseService);
    }

    private ShouldTakeFull setupShouldTakeFullBasedOnSnapshotDisablingLogic(Long latestConfigVersion, Map<String, Object> jobDefMetadata) {
        Instant now = Instant.now();
        SnapshottingSettings snapshottingSettings = SnapshottingSettings.builder()
                .disableSnapshottingForTenants(Set.of("foo"))
                .build();
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .fullFrequencyInMinutes(30)
                .tenantId("foo")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .metadata(jobDefMetadata)
                .build();
        when(integrationService.listConfigs(eq("foo"), eq(List.of("1")), eq(0), eq(1)))
                .thenReturn(DbListResponse.of(List.of(
                        IntegrationConfig.builder()
                                .metadata(IntegrationConfig.Metadata.builder().configUpdatedAt(latestConfigVersion).build())
                                .build()), 1));
        when(integrationTrackingService.get(any(), any())).thenReturn(
                Optional.ofNullable(IntegrationTracker.builder()
                        .latestAggregatedAt(Long.MAX_VALUE)
                        .build())
        );

        return FullOrIncrementalUtils.shouldTakeFullBasedOnSnapshotDisablingLogic(
                jobDefinition,
                jobInstanceDatabaseService,
                integrationService,
                integrationTrackingService,
                snapshottingSettings,
                now);
    }

    @Test
    public void testShouldTakeFullBasedOnSnapshotDisablingLogic1() {
        ShouldTakeFull shouldTakeFull = setupShouldTakeFullBasedOnSnapshotDisablingLogic(123L, null);

        assertThat(shouldTakeFull.isTakeFull()).isTrue();
        assertThat(shouldTakeFull.getMetadataUpdate()).containsExactlyEntriesOf(Map.of("last_config_version", 123L));
    }

    @Test
    public void testShouldTakeFullBasedOnSnapshotDisablingLogic2() {
        ShouldTakeFull shouldTakeFull = setupShouldTakeFullBasedOnSnapshotDisablingLogic(123L, Map.of("last_config_version", 123L));

        assertThat(shouldTakeFull.isTakeFull()).isFalse();
        assertThat(shouldTakeFull.getMetadataUpdate()).isNullOrEmpty();
    }

    @Test
    public void testShouldTakeFullBasedOnSnapshotDisablingLogic3() {
        ShouldTakeFull shouldTakeFull = setupShouldTakeFullBasedOnSnapshotDisablingLogic(456L, Map.of("last_config_version", 123L));

        assertThat(shouldTakeFull.isTakeFull()).isTrue();
        assertThat(shouldTakeFull.getMetadataUpdate()).containsExactlyEntriesOf(Map.of("last_config_version", 456L));
    }
}