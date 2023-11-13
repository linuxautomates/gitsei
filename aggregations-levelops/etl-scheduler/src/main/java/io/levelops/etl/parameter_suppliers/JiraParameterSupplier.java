package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.etl.utils.FullOrIncrementalUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class JiraParameterSupplier implements JobDefinitionParameterSupplier {
    private final IntegrationService integrationService;
    private final IntegrationTrackingService integrationTrackingService;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final SnapshottingSettings snapshottingSettings;

    @Autowired
    public JiraParameterSupplier(IntegrationService integrationService,
                                 IntegrationTrackingService integrationTrackingService,
                                 JobInstanceDatabaseService jobInstanceDatabaseService,
                                 SnapshottingSettings snapshottingSettings) {
        this.integrationService = integrationService;
        this.integrationTrackingService = integrationTrackingService;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.snapshottingSettings = snapshottingSettings;
    }

    @Override
    public JobDefinitionParameters getJobDefinitionParameters() {
        return JobDefinitionParameters.builder()
                .jobPriority(JobPriority.MEDIUM)
                .attemptMax(3)
                .retryWaitTimeInMinutes(10)
                .timeoutInMinutes(TimeUnit.HOURS.toMinutes(20))
                .frequencyInMinutes((int) TimeUnit.HOURS.toMinutes(1))
                .fullFrequencyInMinutes((int) TimeUnit.HOURS.toMinutes(24))
                .build();
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    @Override
    public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
        return FullOrIncrementalUtils.shouldTakeFullBasedOnSnapshotDisablingLogic(jobDefinition, jobInstanceDatabaseService, integrationService, integrationTrackingService, snapshottingSettings, now);
    }

    @Override
    public JobType getJobType() {
        return JobType.INGESTION_RESULT_PROCESSING_JOB;
    }

    @Override
    public @NonNull String getEtlProcessorName() {
        return "JiraEtlProcessor";
    }
}
