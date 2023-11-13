package io.levelops.aggs.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.etl.models.JobType;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.etl.parameter_suppliers.JiraParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.etl.services.IngestionTriggerMonitorService;
import io.levelops.etl.utils.SchedulingUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.TriggerType;
import io.levelops.ingestion.services.ControlPlaneService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class IngestionTriggerMonitorServiceTest {

    @Mock
    ControlPlaneService controlPlaneService;

    @Mock
    JobDefinitionDatabaseService jobDefinitionDatabaseService;

    @Mock
    SchedulingUtils schedulingUtils;

    @Mock
    IntegrationService integrationService;
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    IntegrationTrackingService integrationTrackingService;

    @Mock
    MeterRegistry meterRegistry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSyncTriggersCreateJob() throws JsonProcessingException {
        var ingestionTriggers = List.of(
                createDbTrigger("t1", "i1", 10, TriggerType.JIRA), // whitelisted
                createDbTrigger("t2", "i2", 10, TriggerType.JIRA), // not white listed
                createDbTrigger("t3", "i3", -10, TriggerType.JIRA),// disabled because frequency < 10
                createDbTrigger("t4", "i4", 10, TriggerType.GITHUB)// trigger type not whitelisted
        );
        // Test whitelist and creating a single job that does not exist in the db yet
        testSyncTriggers(
                ingestionTriggers.stream(),
                Stream.of(),
                List.of("1::i1"),
                List.of(TriggerType.JIRA),
                true,
                false,
                List.of("t1"), List.of());


        // open up the whitelist to t1, t4 and t5
        ingestionTriggers = List.of(
                createDbTrigger("t1", "i1", 10, TriggerType.JIRA), // whitelisted
                createDbTrigger("t2", "i2", 10, TriggerType.JIRA), // not white listed
                createDbTrigger("t3", "i3", -10, TriggerType.JIRA),// disabled because frequency < 10
                createDbTrigger("t4", "i4", 10, TriggerType.GITHUB),// trigger type not whitelisted
                createDbTrigger("t5", "i5", 10, TriggerType.JIRA) // whitelisted
        );
        testSyncTriggers(
                ingestionTriggers.stream(),
                Stream.of(),
                List.of("1::i1", "1::i5", "1::i4"),
                List.of(),
                true,
                false,
                List.of("t1", "t4", "t5"), List.of());

        // all github integratoins are whitelisted + t1
        testSyncTriggers(
                ingestionTriggers.stream(),
                Stream.of(),
                List.of("1::i1"),
                List.of(TriggerType.GITHUB),
                true,
                true,
                List.of("t1", "t4"), List.of());

        // Everything whitelisted
        testSyncTriggers(
                ingestionTriggers.stream(),
                Stream.of(),
                List.of("1::i1,1::i2,1::i3"),
                List.of(TriggerType.GITHUB, TriggerType.JIRA),
                true,
                true,
                List.of("t1", "t2", "t3", "t4", "t5"), List.of());
    }

    @Test
    public void testSyncTriggersUpdateJob() throws JsonProcessingException {
        var ingestionTriggers = List.of(
                createDbTrigger("t1", "i1", 10, TriggerType.JIRA),
                createDbTrigger("t2", "i2", 10, TriggerType.JIRA),
                createDbTrigger("t3", "i3", -10, TriggerType.JIRA)
        );

        var existingJobs = List.of(
                createJobDefinition("t3", "i3", true, TriggerType.JIRA),
                createJobDefinition("t2", "i2", true, TriggerType.JIRA),
                createJobDefinition("t4", "i4", true, TriggerType.JIRA)
        );

        // t1 should be added and t3 and t4 should be updated to isActive = false
        testSyncTriggers(
                ingestionTriggers.stream(),
                existingJobs.stream(),
                List.of(),
                List.of(TriggerType.JIRA),
                false,
                true,
                List.of("t1"), List.of(existingJobs.get(0).getId(), existingJobs.get(2).getId()));

        // Ensure that an existing job definition that is disabled remains disabled if the trigger
        // continues to be disabled as well
        existingJobs = List.of(
                createJobDefinition("t3", "i3", false, TriggerType.JIRA)
        );
        ingestionTriggers = List.of(
                createDbTrigger("t3", "i3", -10, TriggerType.JIRA)
        );
        testSyncTriggers(
                ingestionTriggers.stream(),
                existingJobs.stream(),
                List.of(),
                List.of(TriggerType.JIRA),
                false,
                true,
                List.of(),
                List.of());
    }

    private void testSyncTriggers(Stream<DbTrigger> ingestionTriggers,
                                  Stream<DbJobDefinition> existingJobs,
                                  List<String> integrationWhitelist,
                                  List<TriggerType> triggerTypeWhitelist,
                                  boolean useIntegrationWhitelist,
                                  boolean useTriggerWhitelist,
                                  List<String> expectedCreatedTriggerIds,
                                  List<UUID> expectedUpdatedDefinitionIds) throws JsonProcessingException {
        when(controlPlaneService.streamTriggers(any(), any(), any())).thenReturn(ingestionTriggers);
        when(jobDefinitionDatabaseService.stream(any())).thenReturn(existingJobs);

        // Hack the github param supplier because we haven't implemented it while this UT was being written
        JobDefinitionParameterSupplierRegistry repository = new JobDefinitionParameterSupplierRegistry(
                List.of(
                        new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()),
                        new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
                            @Override
                            public IntegrationType getIntegrationType() {
                                return IntegrationType.GITHUB;
                            }

                            @Override
                            public @NonNull String getEtlProcessorName() {
                                return "GithubProcessor";
                            }
                        })
        );

        IngestionTriggerMonitorService triggerMonitorService = new IngestionTriggerMonitorService(
                controlPlaneService,
                jobDefinitionDatabaseService,
                repository,
                meterRegistry,
                String.join(",", integrationWhitelist),
                String.join(",", triggerTypeWhitelist.stream().map(TriggerType::getType).collect(Collectors.toList())),
                useIntegrationWhitelist,
                useTriggerWhitelist,
                10, 100);

        triggerMonitorService.syncJobsAndTriggers();

        ArgumentCaptor<DbJobDefinition> insertArg = ArgumentCaptor.forClass(DbJobDefinition.class);
        verify(jobDefinitionDatabaseService, times(expectedCreatedTriggerIds.size())).insert(insertArg.capture());
        var allInsertedJobDefinitions = insertArg.getAllValues().stream().map(DbJobDefinition::getIngestionTriggerId).toList();
        assertThat(allInsertedJobDefinitions).containsExactlyInAnyOrderElementsOf(expectedCreatedTriggerIds);

        ArgumentCaptor<DbJobDefinitionUpdate> updateArgumentCaptor = ArgumentCaptor.forClass(DbJobDefinitionUpdate.class);
        verify(jobDefinitionDatabaseService, times(expectedUpdatedDefinitionIds.size())).update(updateArgumentCaptor.capture());
        var allUpdatedIds = updateArgumentCaptor.getAllValues().stream().map(u -> u.getWhereClause().getId()).toList();
        for (int i = 0; i < allUpdatedIds.size(); i++) {
            assertThat(allUpdatedIds.get(i)).isEqualTo(expectedUpdatedDefinitionIds.get(i));
        }
        reset(controlPlaneService);
        reset(jobDefinitionDatabaseService);
    }

    private DbTrigger createDbTrigger(String id, String integrationId, int frequency, TriggerType triggerType) {
        return DbTrigger.builder()
                .integrationId(integrationId)
                .tenantId("1")
                .id(id)
                .frequency(frequency)
                .type(triggerType.getType())
                .build();
    }

    private DbJobDefinition createJobDefinition(String triggerId, String integrationId, boolean isActive, TriggerType triggerType) {
        return DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .integrationId(integrationId)
                .tenantId("1")
                .ingestionTriggerId(triggerId)
                .integrationType(triggerType.getType())
                .isActive(isActive)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
    }

    @Test
    public void testIntegrationIdWhitelist() {
        var whitelist = List.of("sid::1", "foo::2");
        var parsedWhitelist = whitelist.stream()
                .map(IntegrationWhitelistEntry::fromString)
                .collect(Collectors.toList());
        assertThat(parsedWhitelist).containsExactlyInAnyOrder(
                IntegrationWhitelistEntry.builder()
                        .tenantId("sid")
                        .integrationId("1")
                        .build(),
                IntegrationWhitelistEntry.builder()
                        .tenantId("foo")
                        .integrationId("2")
                        .build()
        );
    }
}
