package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface JobDefinitionParameterSupplier {
    JobDefinitionParameters getJobDefinitionParameters();

    IntegrationType getIntegrationType();

    JobType getJobType();

    /**
     * This is the name of the ETL processor that will be used to process the job,
     * on the worker node.
     * <p>
     * For generic per tenant or per integration job, this is also expected to be
     * a unique key to register the job on the scheduler.
     */
    @NonNull
    String getEtlProcessorName();

    @Value
    @Builder(toBuilder = true)
    class ShouldTakeFull {
        boolean takeFull;

        /**
         * Optional. If provided, this will get added to the job definition's metadata.
         */
        Map<String, Object> metadataUpdate;

        public static ShouldTakeFull of(boolean shouldTakeFull) {
            return ShouldTakeFull.builder()
                    .takeFull(shouldTakeFull)
                    .build();
        }
    }

    /**
     * Decides if the next job should be a full scan or iterative scan.
     */
    ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now);

    /**
     * NOTE: This only works for generic jobs. For ingestion result processing jobs,
     * there is a separate whitelist. Look at IngestionTriggerMonitorService
     * <p>
     * If this is defined then the job will only be registered for the whitelisted
     * integrations. If this is not defined then the job will be registered for all
     */
    default List<IntegrationWhitelistEntry> getIntegrationWhitelistEntries() {
        return null;
    }

    /**
     * NOTE: This only works for generic jobs. For ingestion result processing jobs,
     * there is a separate whitelist. Look at IngestionTriggerMonitorService
     * <p>
     * If this is defined then the job will only be registered for the whitelisted
     * tenants. If this is not defined then the job will be registered for all
     */
    default List<String> getTenantWhitelist() {
        return null;
    }
}
