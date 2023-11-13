package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class GithubRepoMappingParameterSupplier implements JobDefinitionParameterSupplier {
    private final List<IntegrationWhitelistEntry> integrationWhitelistEntries;
    private final boolean useIntegrationWhitelist;

    GithubRepoMappingParameterSupplier(
            @Value("${GITHUB_REPO_MAPPING_USE_INTEGRATION_WHITELIST:true}") boolean useIntegrationWhitelist,
            @Value("${GITHUB_REPO_MAPPING_INTEGRATION_WHITELIST:}") String integrationIdWhitelist
    ) {
        this.useIntegrationWhitelist = useIntegrationWhitelist;
        this.integrationWhitelistEntries = IntegrationWhitelistEntry.fromCommaSeparatedString(integrationIdWhitelist);
        log.info("GithubRepoMappingParameterSupplier: useIntegrationWhitelist: {}, integrationWhitelistEntries={}",
                useIntegrationWhitelist, integrationWhitelistEntries);
    }

    @Override
    public JobDefinitionParameters getJobDefinitionParameters() {
        return JobDefinitionParameters.builder()
                .jobPriority(JobPriority.MEDIUM)
                .attemptMax(3)
                .retryWaitTimeInMinutes(10)
                .timeoutInMinutes(TimeUnit.HOURS.toMinutes(2))
                .frequencyInMinutes((int) TimeUnit.HOURS.toMinutes(24))
                .fullFrequencyInMinutes((int) TimeUnit.HOURS.toMinutes(24))
                .build();
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.GITHUB;
    }

    @Override
    public JobType getJobType() {
        return JobType.GENERIC_INTEGRATION_JOB;
    }

    @Override
    public @NonNull String getEtlProcessorName() {
        return "GithubRepoMappingEtlProcessor";
    }

    @Override
    public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
        return ShouldTakeFull.of(false);
    }

    @Override
    public List<IntegrationWhitelistEntry> getIntegrationWhitelistEntries() {
        if (useIntegrationWhitelist) {
            return integrationWhitelistEntries;
        } else {
            return null;
        }
    }
}
