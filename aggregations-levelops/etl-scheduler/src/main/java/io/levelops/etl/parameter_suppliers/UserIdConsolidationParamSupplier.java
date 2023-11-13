package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class UserIdConsolidationParamSupplier implements JobDefinitionParameterSupplier {
    @Override
    public JobDefinitionParameters getJobDefinitionParameters() {
        return JobDefinitionParameters.builder()
                .jobPriority(JobPriority.MEDIUM)
                .attemptMax(3)
                .retryWaitTimeInMinutes(10)
                .timeoutInMinutes(TimeUnit.HOURS.toMinutes(2))
                .frequencyInMinutes((int) TimeUnit.HOURS.toMinutes(24))
                .fullFrequencyInMinutes((int) TimeUnit.DAYS.toMinutes(7))
                .build();
    }

    @Override
    public IntegrationType getIntegrationType() {
        return null;
    }

    @Override
    public JobType getJobType() {
        return JobType.GENERIC_TENANT_JOB;
    }

    @Override
    public @NonNull String getEtlProcessorName() {
        return "UserIdConsolidationEtlProcessor";
    }

    @Override
    public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
        return ShouldTakeFull.of(false);
    }
}
