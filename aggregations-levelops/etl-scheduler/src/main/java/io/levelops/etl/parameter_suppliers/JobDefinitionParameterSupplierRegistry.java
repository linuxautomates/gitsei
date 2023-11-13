package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.etl.models.JobType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JobDefinitionParameterSupplierRegistry {
    private final HashMap<String, JobDefinitionParameterSupplier> ingestionResultJobSuppliersMap;
    private final HashMap<String, List<JobDefinitionParameterSupplier>> genericIntegrationJobSuppliersMap;
    private final List<JobDefinitionParameterSupplier> genericTenantJobSuppliers;

    @Autowired
    public JobDefinitionParameterSupplierRegistry(List<JobDefinitionParameterSupplier> suppliers) {
        genericIntegrationJobSuppliersMap = new HashMap<>();
        ingestionResultJobSuppliersMap = new HashMap<>();

        genericTenantJobSuppliers = suppliers.stream()
                .filter(supplier -> supplier.getJobType().equals(JobType.GENERIC_TENANT_JOB))
                .toList();

        for (JobDefinitionParameterSupplier supplier : suppliers) {
            log.info("Registering parameter supplier for jobType: {}," +
                    "integration type: {}", supplier.getJobType(), supplier.getIntegrationType());
            if (supplier.getJobType().equals(JobType.INGESTION_RESULT_PROCESSING_JOB)) {
                ingestionResultJobSuppliersMap.put(supplier.getIntegrationType().toString(), supplier);
            } else if (supplier.getJobType().equals(JobType.GENERIC_INTEGRATION_JOB)) {
                List<JobDefinitionParameterSupplier> s = genericIntegrationJobSuppliersMap.getOrDefault(supplier.getIntegrationType().toString(), new ArrayList<>());
                s.add(supplier);
                genericIntegrationJobSuppliersMap.put(supplier.getIntegrationType().toString(), s);
            } else if (supplier.getJobType().equals(JobType.GENERIC_TENANT_JOB)) {
                // Do nothing, already added to genericTenantJobSuppliers
            } else {
                throw new IllegalArgumentException("Job type not supported: " + supplier.getJobType());
            }
        }
    }

    public JobDefinitionParameterSupplier getSupplier(DbJobDefinition jobDefinition) {
        if (jobDefinition.getJobType().equals(JobType.INGESTION_RESULT_PROCESSING_JOB)) {
            return getIngestionResultProcessingSupplier(jobDefinition.getIntegrationType());
        } else if (jobDefinition.getJobType().equals(JobType.GENERIC_INTEGRATION_JOB)) {
            return getGenericIntegrationJobParameterSupplier(
                    jobDefinition.getIntegrationType(),
                    jobDefinition.getTenantId(),
                    jobDefinition.getIntegrationId()
            )
                    .stream()
                    .filter(supplier -> supplier.getEtlProcessorName().equals(jobDefinition.getAggProcessorName()))
                    .findFirst()
                    .get();
        } else if (jobDefinition.getJobType().equals(JobType.GENERIC_TENANT_JOB)) {
            return getGenericTenantJobSuppliers(jobDefinition.getTenantId()).stream()
                    .filter(supplier -> supplier.getEtlProcessorName().equals(jobDefinition.getAggProcessorName()))
                    .findFirst()
                    .get();
        } else {
            throw new IllegalArgumentException("Job type not supported: " + jobDefinition.getJobType());
        }
    }

    public JobDefinitionParameterSupplier getIngestionResultProcessingSupplier(String integrationType) {
        if (!ingestionResultJobSuppliersMap.containsKey(integrationType)) {
            throw new NotImplementedException("No ingestion result processing supplier for integration type: " + integrationType);
        }
        return ingestionResultJobSuppliersMap.get(integrationType);
    }

    private Optional<Boolean> isIntegrationWhitelisted(
            String tenantId,
            String integrationId,
            List<IntegrationWhitelistEntry> whitelistEntries) {
        if (whitelistEntries == null) {
            return Optional.empty();
        }
        return Optional.of(whitelistEntries.stream()
                .anyMatch(entry -> entry.getTenantId().equals(tenantId) && entry.getIntegrationId().equals(integrationId)));
    }

    private Optional<Boolean> isTenantWhitelisted(String tenantId, List<String> whitelistEntries) {
        if (whitelistEntries == null) {
            return Optional.empty();
        }
        return Optional.of(whitelistEntries.stream()
                .anyMatch(entry -> entry.equals(tenantId)));
    }

    public List<JobDefinitionParameterSupplier> getGenericIntegrationJobParameterSupplier(
            String integrationType,
            String tenantId,
            String integrationId
    ) {
        List<JobDefinitionParameterSupplier> suppliers =
                genericIntegrationJobSuppliersMap.getOrDefault(integrationType, List.of());
        return suppliers.stream()
                .filter(supplier -> {
                    var isIntegrationWhitelisted = isIntegrationWhitelisted(tenantId, integrationId, supplier.getIntegrationWhitelistEntries());
                    var isTenantWhitelisted = isTenantWhitelisted(tenantId, supplier.getTenantWhitelist());

                    if (isIntegrationWhitelisted.isPresent() && isTenantWhitelisted.isPresent()) {
                        return isIntegrationWhitelisted.get() || isTenantWhitelisted.get();
                    } else if (isIntegrationWhitelisted.isPresent()) {
                        return isIntegrationWhitelisted.get();
                    } else return isTenantWhitelisted.orElse(true);
                })
                .collect(Collectors.toList());
    }

    public List<JobDefinitionParameterSupplier> getGenericTenantJobSuppliers(String tenantId) {
        return genericTenantJobSuppliers.stream()
                .filter(supplier -> {
                    var isTenantWhitelisted = isTenantWhitelisted(tenantId, supplier.getTenantWhitelist());
                    return isTenantWhitelisted.orElseGet(() -> true);
                })
                .collect(Collectors.toList());
    }
}
