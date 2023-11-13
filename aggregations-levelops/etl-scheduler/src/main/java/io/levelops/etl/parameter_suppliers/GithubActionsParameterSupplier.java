package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
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
public class GithubActionsParameterSupplier implements JobDefinitionParameterSupplier{

    private final JobInstanceDatabaseService jobInstanceDatabaseService;

    @Autowired
    public GithubActionsParameterSupplier(JobInstanceDatabaseService jobInstanceDatabaseService) {
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
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
        return IntegrationType.GITHUB_ACTIONS;
    }

    @Override
    public JobType getJobType() {
        return JobType.INGESTION_RESULT_PROCESSING_JOB;
    }

    @Override
    public @NonNull String getEtlProcessorName() {
        return "GithubActionsEtlProcessor";
    }

    @Override
    public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
        return ShouldTakeFull.of(FullOrIncrementalUtils.shouldTakeScheduleBasedFull(jobDefinition, jobInstanceDatabaseService, now));
    }
}
