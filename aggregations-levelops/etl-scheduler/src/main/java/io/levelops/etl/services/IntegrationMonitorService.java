package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class IntegrationMonitorService {
    private final ScheduledExecutorService executorService;
    private Future<?> schedulingFuture;
    private final int schedulingIntervalInSec;
    private final int warmupDelaySecs;
    private final MeterRegistry meterRegistry;

    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final JobDefinitionParameterSupplierRegistry paramSupplierRegistry;
    private final InventoryService inventoryService;

    IntegrationMonitorService(
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            JobDefinitionParameterSupplierRegistry paramSupplierRegistry,
            InventoryService inventoryService,
            MeterRegistry meterRegistry,
            @Value("${INTEGRATION_MONITOR_SCHEDULING_SECONDS:600}") int schedulingIntervalInSec,
            @Value("${INTEGRATION_MONITOR_WARMUP_SECONDS:10}") int warmupDelaySecs
    ) {
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.paramSupplierRegistry = paramSupplierRegistry;
        this.inventoryService = inventoryService;
        this.schedulingIntervalInSec = schedulingIntervalInSec;
        this.warmupDelaySecs = warmupDelaySecs;
        this.meterRegistry = meterRegistry;
        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("trigger-monitor-loop-%d")
                .build());
        initScheduling();
    }

    private void initScheduling() {
        if (schedulingIntervalInSec <= 0) {
            log.warn("Scheduling interval for integration monitor is set to {}, not scheduling", schedulingIntervalInSec);
            return;
        }
        schedulingFuture = executorService.scheduleAtFixedRate(
                new IntegrationMonitorRunnable(), warmupDelaySecs, schedulingIntervalInSec, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    public void stopScheduling() {
        schedulingFuture.cancel(true);
    }

    private void createJobDefinition(JobDefinitionParameterSupplier paramSupplier, Tenant tenant, Integration integration) throws JsonProcessingException {
        JobDefinitionParameters params = paramSupplier.getJobDefinitionParameters();
        String integrationId = null;
        String integrationType = null;
        if (integration != null) {
            integrationId = integration.getId();
            integrationType = integration.getApplication();
        }
        DbJobDefinition dbJobDefinition = DbJobDefinition.builder()
                .tenantId(tenant.getId())
                .integrationId(integrationId)
                .integrationType(integrationType)
                .ingestionTriggerId(null)
                .jobType(paramSupplier.getJobType())
                .isActive(true)
                .defaultPriority(params.getJobPriority())
                .attemptMax(params.getAttemptMax())
                .retryWaitTimeInMinutes(params.getRetryWaitTimeInMinutes())
                .timeoutInMinutes(params.getTimeoutInMinutes())
                .frequencyInMinutes(params.getFrequencyInMinutes())
                .fullFrequencyInMinutes(params.getFullFrequencyInMinutes())
                .aggProcessorName(paramSupplier.getEtlProcessorName())
                .build();
        UUID id = jobDefinitionDatabaseService.insert(dbJobDefinition);
        meterRegistry.counter("etl.scheduler.jobdefinition.created.count").increment();
        log.info("Created new job definition {} {}", id, dbJobDefinition);
    }

    private void syncTenantJobs(
            Tenant tenant,
            Integration integration,
            List<JobDefinitionParameterSupplier> paramSuppliers,
            List<DbJobDefinition> existingJobDefinitions) throws JsonProcessingException {
        var supplierMap = paramSuppliers.stream()
                .collect(Collectors.toMap(JobDefinitionParameterSupplier::getEtlProcessorName, Function.identity()));
        var existingJobMap = existingJobDefinitions.stream()
                .collect(Collectors.toMap(DbJobDefinition::getAggProcessorName, Function.identity()));

        /*
            1. Supplier exists but not the job definition
                -> Create job definition
            2. Supplier does not exist but job definition exists
                -> Disable job definition
            3. Supplier exists but job definition exists but is disabled
                -> Enable job definition
         */
        var jobDefinitionsToCreate = supplierMap.entrySet().stream()
                .filter(entry -> !existingJobMap.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        var jobDefinitionsToEnable = supplierMap.entrySet().stream()
                .filter(entry -> existingJobMap.containsKey(entry.getKey()))
                .filter(entry -> existingJobMap.get(entry.getKey()).isDisabled())
                .map(Map.Entry::getValue)
                .toList();

        var jobDefinitionsToDisable = existingJobMap.entrySet().stream()
                .filter(entry -> !supplierMap.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (jobDefinitionsToCreate.size() > 0) {
            log.info("Creating {} job definitions", jobDefinitionsToCreate.size());
            for (JobDefinitionParameterSupplier paramSupplier : jobDefinitionsToCreate) {
                createJobDefinition(paramSupplier, tenant, integration);
            }
        }

        if (jobDefinitionsToEnable.size() > 0) {
            log.info("Enabling {} job definitions", jobDefinitionsToEnable.size());
            jobDefinitionsToEnable.forEach(paramSupplier -> {
                DbJobDefinition jobDefinition = existingJobMap.get(paramSupplier.getEtlProcessorName());
                try {
                    jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                            .isActive(true)
                            .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                    .id(jobDefinition.getId())
                                    .build())
                            .build());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        if (jobDefinitionsToDisable.size() > 0) {
            log.info("Disabling {} job definitions", jobDefinitionsToDisable.size());
            jobDefinitionsToDisable.forEach(jobDefinition -> {
                try {
                    jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                            .isActive(false)
                            .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                    .id(jobDefinition.getId())
                                    .build())
                            .build());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void syncPerTenantGenericJobs(Tenant tenant) throws JsonProcessingException {
        List<JobDefinitionParameterSupplier> suppliers = paramSupplierRegistry.getGenericTenantJobSuppliers(tenant.getId());
        List<DbJobDefinition> existingJobDefinitions = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .tenantIds(List.of(tenant.getId()))
                        .jobTypes(List.of(JobType.GENERIC_TENANT_JOB))
                        .build())
                .toList();
        syncTenantJobs(tenant, null, suppliers, existingJobDefinitions);
    }

    private void processDeletedIntegrations(Tenant tenant, List<Integration> integrations) throws JsonProcessingException {
        var integrationIdsWithActiveJobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .jobTypes(List.of(JobType.GENERIC_INTEGRATION_JOB))
                        .tenantIds(List.of(tenant.getId()))
                        .build())
                .filter(DbJobDefinition::getIsActive)
                .map(DbJobDefinition::getIntegrationId)
                .collect(Collectors.toSet());
        var allIntegrationIds = integrations.stream()
                .map(Integration::getId)
                .collect(Collectors.toSet());
        var deletedIntegrations = Sets.difference(integrationIdsWithActiveJobs, allIntegrationIds);
        for (String integrationId : deletedIntegrations) {
            jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                    .isActive(false)
                    .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                            .tenantId(tenant.getId())
                            .integrationId(integrationId)
                            .build())
                    .build());
        }
    }

    private void syncPerIntegrationGenericJobs(Tenant tenant) throws InventoryException, JsonProcessingException {
        List<Integration> integrations = inventoryService.listIntegrations(tenant.getId()).getRecords();
        for (Integration integration : integrations) {
            List<JobDefinitionParameterSupplier> suppliers = paramSupplierRegistry
                    .getGenericIntegrationJobParameterSupplier(
                            integration.getApplication(),
                            tenant.getId(),
                            integration.getId());
            List<DbJobDefinition> existingJobDefinitions = jobDefinitionDatabaseService
                    .stream(DbJobDefinitionFilter.builder()
                            .tenantIdIntegrationIdPair(Pair.of(tenant.getId(), integration.getId()))
                            .jobTypes(List.of(JobType.GENERIC_INTEGRATION_JOB))
                            .build())
                    .toList();
            syncTenantJobs(tenant, integration, suppliers, existingJobDefinitions);
        }
        processDeletedIntegrations(tenant, integrations);
    }

    private void processDeletedTenants(List<Tenant> tenants) throws JsonProcessingException {
        Set<String> allTenantIds = tenants.stream()
                .map(Tenant::getId)
                .collect(Collectors.toSet());
        Set<String> tenantIdsWithActiveJobs = jobDefinitionDatabaseService.stream(DbJobDefinitionFilter.builder()
                        .jobTypes(List.of(JobType.GENERIC_TENANT_JOB, JobType.GENERIC_INTEGRATION_JOB))
                        .build())
                .filter(DbJobDefinition::getIsActive)
                .map(DbJobDefinition::getTenantId)
                .collect(Collectors.toSet());
        Set<String> deletedTenantIds = Sets.difference(tenantIdsWithActiveJobs, allTenantIds);
        if (deletedTenantIds.size() > 0) {
            for (String tenantId : deletedTenantIds) {
                log.info("Found {} deleted tenants. Disabling all jobs", tenantId);
                jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                        .isActive(false)
                        .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                .tenantId(tenantId)
                                .build())
                        .build());
            }
        }
    }

    @VisibleForTesting
    public void syncJobs() throws InventoryException, JsonProcessingException {
        List<Tenant> tenants = inventoryService.listTenants();
        for (Tenant tenant : tenants) {
            syncPerTenantGenericJobs(tenant);
            syncPerIntegrationGenericJobs(tenant);
        }
        processDeletedTenants(tenants);
    }

    public class IntegrationMonitorRunnable implements Runnable {
        @Override
        public void run() {
            try {
                syncJobs();
                meterRegistry.counter("etl.scheduler.ingestion.trigger.loop.run.count").increment();
            } catch (Throwable e) {
                log.error("Exception in ingestion trigger monitor, this should never happen", e);
            }
        }
    }
}
