package io.levelops.etl.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionFilter;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.ListUtils;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.ingestion.services.ControlPlaneService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monitor's ingestion triggers and creates/updates JobDefinition's as needed
 */
@Log4j2
@Service
public class IngestionTriggerMonitorService {
    private final ControlPlaneService controlPlaneService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final JobDefinitionParameterSupplierRegistry paramSupplierRepository;
    private final List<String> triggerTypeWhitelist;
    private final List<IntegrationWhitelistEntry> integrationIdWhitelist;
    private final boolean shouldUseIntegrationIdWhitelist;
    private final boolean shouldUseTriggerTypeWhitelist;
    private final ScheduledExecutorService executorService;
    private Future<?> schedulingFuture;
    private final int schedulingIntervalInSec;
    private final int warmupDelaySecs;
    private final MeterRegistry meterRegistry;


    public IngestionTriggerMonitorService(
            ControlPlaneService controlPlaneService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            JobDefinitionParameterSupplierRegistry paramSupplierRepository,
            MeterRegistry meterRegistry,
            @Value("${INTEGRATION_WHITELIST:}") String integrationIdWhitelist,
            @Value("${TRIGGER_TYPE_WHITELIST:}") String triggerTypeWhitelist,
            @Value("${USE_INTEGRATION_WHITELIST:true}") boolean useIntegrationWhitelist,
            @Value("${USE_TRIGGER_TYPE_WHITELIST:true}") boolean useTriggerTypeWhitelist,
            @Value("${TRIGGER_MONITOR_SCHEDULING_SECONDS:600}") int schedulingIntervalInSec,
            @Value("${TRIGGER_MONITOR_WARMUP_SECONDS:10}") int warmupDelaySecs) {
        this.controlPlaneService = controlPlaneService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.paramSupplierRepository = paramSupplierRepository;

        this.integrationIdWhitelist = IntegrationWhitelistEntry.fromCommaSeparatedString(integrationIdWhitelist);
        this.triggerTypeWhitelist = CommaListSplitter.split(triggerTypeWhitelist);
        this.shouldUseTriggerTypeWhitelist = useTriggerTypeWhitelist;
        this.shouldUseIntegrationIdWhitelist = useIntegrationWhitelist;
        this.schedulingIntervalInSec = schedulingIntervalInSec;
        this.warmupDelaySecs = warmupDelaySecs;
        this.meterRegistry = meterRegistry;
        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat("trigger-monitor-loop-%d")
                .build());
        initScheduling();
    }

    private void initScheduling() {
        schedulingFuture = executorService.scheduleAtFixedRate(
                new IngestionMonitorRunnable(), warmupDelaySecs, schedulingIntervalInSec, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    public void stopScheduling() {
        schedulingFuture.cancel(true);
    }

    private List<DbTrigger> getIngestionTriggersFilteredByIntegrationId(List<DbTrigger> ingestionTriggers) {
        if (shouldUseIntegrationIdWhitelist) {
            return ingestionTriggers.stream()
                    .filter(trigger -> integrationIdWhitelist.contains(IntegrationWhitelistEntry.builder()
                            .tenantId(trigger.getTenantId())
                            .integrationId(trigger.getIntegrationId())
                            .build()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<DbTrigger> getIngestionTriggersFilteredByType(List<DbTrigger> ingestionTriggers) {
        if (shouldUseTriggerTypeWhitelist) {
            return ingestionTriggers.stream()
                    .filter(trigger -> triggerTypeWhitelist.contains(trigger.getType()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<DbTrigger> getAndFilterAllIngestionTriggers() {
        List<DbTrigger> allTriggers = controlPlaneService.streamTriggers(null, null, null).collect(Collectors.toList());

        List<DbTrigger> triggers;
        // The whitelists are OR'd together
        if (shouldUseIntegrationIdWhitelist || shouldUseTriggerTypeWhitelist) {
            triggers = ListUtils.union(getIngestionTriggersFilteredByIntegrationId(allTriggers), getIngestionTriggersFilteredByType(allTriggers));
            // Remove duplicate values from triggers
            triggers = triggers.stream().collect(Collectors.toMap(DbTrigger::getId, p -> p, (p, q) -> p)).values().stream().collect(Collectors.toList());
        } else {
            triggers = allTriggers;
        }


        log.debug("Count of filtered triggers to process: {}", triggers.size());
        return triggers;
    }

    private void createJobDefinitionForTrigger(DbTrigger trigger) throws JsonProcessingException {
        JobDefinitionParameterSupplier paramSupplier = paramSupplierRepository.getIngestionResultProcessingSupplier(trigger.getType());
        JobDefinitionParameters params = paramSupplier.getJobDefinitionParameters();
        DbJobDefinition dbJobDefinition = DbJobDefinition.builder()
                .tenantId(trigger.getTenantId())
                .integrationId(trigger.getIntegrationId())
                .integrationType(trigger.getType())
                .ingestionTriggerId(trigger.getId())
                .jobType(paramSupplier.getJobType())
                .isActive(!trigger.isDisabled())
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

    @VisibleForTesting
    public synchronized void syncJobsAndTriggers() throws JsonProcessingException {
        List<DbTrigger> ingestionTriggers = getAndFilterAllIngestionTriggers();
        List<DbJobDefinition> dbJobDefinitions = jobDefinitionDatabaseService.stream(
                        DbJobDefinitionFilter
                                .builder()
                                .jobTypes(List.of(JobType.INGESTION_RESULT_PROCESSING_JOB))
                                .build())
                .collect(Collectors.toList());
        Map<String, DbTrigger> triggerById = ingestionTriggers.stream().collect(
                Collectors.toMap(DbTrigger::getId, Function.identity(), (k1, k2) -> k1));
        Map<String, DbJobDefinition> jobDefintionByTriggerId = dbJobDefinitions.stream().collect(
                Collectors.toMap(DbJobDefinition::getIngestionTriggerId, Function.identity(), (k1, k2) -> k1));

        // Check for new triggers - i.e triggers in the ingestion response but without a corresponding job definition
        for (DbTrigger trigger : ingestionTriggers) {
            if (!jobDefintionByTriggerId.containsKey(trigger.getId())) {
                try {
                    log.info("Creating aggs job definition for newly found ingestion trigger: {} tenant: {} integration id: {} type: {}",
                            trigger.getId(), trigger.getTenantId(), trigger.getIntegrationId(), trigger.getType());
                    createJobDefinitionForTrigger(trigger);
                } catch (Exception e) {
                    log.error("Error creating new aggs job definition for trigger {}", trigger.getId(), e);
                }
            } else if (!trigger.isDisabled() && !jobDefintionByTriggerId.get(trigger.getId()).getIsActive()) {
                DbJobDefinition jobDefinition = jobDefintionByTriggerId.get((trigger.getId()));
                log.info("Marking inactive job definition {}, tenant: {}, integration id: {} as active",
                        jobDefinition.getId(), jobDefinition.getTenantId(), jobDefinition.getIntegrationId());
                jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                        .isActive(true)
                        .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                .id(jobDefinition.getId())
                                .build())
                        .build());
            }
        }

        for (DbJobDefinition jobDefinition : dbJobDefinitions) {
            // Check for triggers that have been deleted
            String triggerId = jobDefinition.getIngestionTriggerId();
            if (!triggerById.containsKey(triggerId) && jobDefinition.getIsActive()) {
                log.info("Disabling job definition for deleted trigger: {}", triggerId);
                jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                        .isActive(false)
                        .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                .id(jobDefinition.getId())
                                .build())
                        .build());
            }

            // Check if a trigger's disabled status has been modified and update the job definition accordingly
            if (triggerById.containsKey(triggerId) &&
                    triggerById.get(triggerId).isDisabled() == jobDefinition.getIsActive()) {
                log.info("Updating job definition for updated trigger {}", triggerId);
                jobDefinitionDatabaseService.update(DbJobDefinitionUpdate.builder()
                        .isActive(!triggerById.get(triggerId).isDisabled())
                        .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                                .id(jobDefinition.getId())
                                .build())
                        .build());
            }
        }
    }

    public class IngestionMonitorRunnable implements Runnable {
        @Override
        public void run() {
            try {
                syncJobsAndTriggers();
                meterRegistry.counter("etl.scheduler.ingestion.trigger.loop.run.count").increment();
            } catch (Throwable e) {
                log.error("Exception in ingestion trigger monitor, this should never happen", e);
            }
        }
    }
}
