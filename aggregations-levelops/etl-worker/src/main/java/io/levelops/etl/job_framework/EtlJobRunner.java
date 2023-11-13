package io.levelops.etl.job_framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.MoreExecutors;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.MetricUtils;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.etl.models.job_progress.EntityProgressDetail;
import io.levelops.commons.etl.models.job_progress.FileProgressDetail;
import io.levelops.etl.models.EntityProcessingResult;
import io.levelops.etl.services.JobTrackingUtilsService;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.etl.utils.JobUtils;
import io.levelops.etl.utils.LoggingUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Log4j2
public class EtlJobRunner {
    private final GcsUtils gcsUtils;
    private final ObjectMapper objectMapper;
    private final JobTrackingUtilsService jobTrackingUtils;
    private final JobInstanceDatabaseService jobInstanceDatabaseService;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public EtlJobRunner(
            GcsUtils gcsUtils,
            ObjectMapper objectMapper,
            JobInstanceDatabaseService jobInstanceDatabaseService,
            JobTrackingUtilsService jobTrackingUtilsService,
            JobDefinitionDatabaseService jobDefinitionDatabaseService,
            MeterRegistry meterRegistry) {
        this.gcsUtils = gcsUtils;
        this.objectMapper = objectMapper;
        this.jobTrackingUtils = jobTrackingUtilsService;
        this.jobInstanceDatabaseService = jobInstanceDatabaseService;
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
        this.meterRegistry = meterRegistry;
    }

    private void updateJobInstanceMetrics(JobContext context) {
        MetricUtils.getTenantCounter(meterRegistry, "etl.worker.job.run", context).increment();
        if (context.getGcsRecords() != null) {
            MetricUtils.getTenantGauge(meterRegistry, "etl.worker.job.run.gcsRecords", context, context.getGcsRecords().size());
        }
    }

    public <S> JobStatus run(JobContext context, EtlProcessor<S> etlProcessor) throws JsonProcessingException {
        try {
            LoggingUtils.setupThreadLocalContext(context);
            updateJobInstanceMetrics(context);
            if (!jobTrackingUtils.updateJobInstanceToPending(context.getJobInstanceId())) {
                throw new RuntimeException("Unable to update job instance status to PENDING");
            }
            log.info("Running job {} for tenant {} integration id {} integration type {} - gcs files to process: {}",
                    context.getJobInstanceId(),
                    context.getTenantId(),
                    context.getIntegrationId(),
                    context.getIntegrationType(),
                    context.getGcsRecords() != null ? context.getGcsRecords().size() : 0
            );
            S jobState = etlProcessor.getJobStateType().cast(etlProcessor.createState(context));

            if (!context.getJobType().isGeneric() && context.getGcsRecords().size() == 0) {
                log.info("Empty payload found for job instance {} - Short circuiting and setting status to success",
                        context.getJobInstanceId());
                markJobAsSuccess(context, JobStatus.SUCCESS);
                return JobStatus.SUCCESS;
            }

            try {
                log.info("Starting preProcessing");
                etlProcessor.preProcess(context, jobState);
                log.info("Finished preProcessing");
            } catch (Exception e) {
                log.error("Failed to run preProcess()", e);
                throw e;
            }

            try {
                // Logging inside don't log again here
                runJobStages(context, etlProcessor, jobState);
            } catch (Exception e) {
                log.error("Failed to run job stages", e);
                throw e;
            }

            try {
                log.info("Starting postProcessing");
                throwIfThreadInterrupted(context);
                etlProcessor.postProcess(context, jobState);
                log.info("Finished postProcessing");
            } catch (Exception e) {
                log.error("Failed to run postProcess()", e);
                throw e;
            }
            JobStatus status = JobUtils.determineJobSuccessStatus(context, etlProcessor);
            Boolean updated = markJobAsSuccess(context, status);
            if (!updated) {
                log.warn("Failed to mark job instance {} as success", context.getJobInstanceId());
            }
            return status;
        } catch (Exception e) {
            log.warn("Could not complete job instance id={}", context.getJobInstanceId(), e);
        } catch (Error e) {
            log.error("Could not complete job instance id={}", context.getJobInstanceId(), e);
            markJobAsFailed(context);
            throw e; // this could be OOM, etc.
        } finally {
            LoggingUtils.clearThreadLocalContext();
        }
        markJobAsFailed(context);
        return JobStatus.FAILURE;
    }

    private Boolean markJobAsSuccess(JobContext ctx, JobStatus status) throws JsonProcessingException {
        log.info("Marking job instance {} as {}", ctx.getJobInstanceId(), status);
        // TODO this should ideally be in a transaction
        Boolean updated = jobTrackingUtils.updateJobInstanceStatus(ctx.getJobInstanceId(), status);
        if (!updated) {
            log.error("Failed to mark job instance {} as {}", ctx.getJobInstanceId(), status);
            return false;
        }
        return true;
    }

    private void markJobAsFailed(JobContext ctx) throws JsonProcessingException {
        log.info("Marking job instance {} as failed", ctx.getJobInstanceId());
        Boolean updated = jobTrackingUtils.updateJobInstanceStatus(ctx.getJobInstanceId(), JobStatus.FAILURE);
        if (!updated) {
            log.info("Failed to update job status to failure for job instance {}", ctx.getJobInstanceId());
        }
    }

    private Optional<String> findLatestIngestionJobIdWithDataType(JobContext ctx, String stageDataTypeName) {
        var gcsRecords = ctx.getGcsRecords();
        for (int i = ctx.getGcsRecords().size() - 1; i >= 0; i--) {
            var gcsRecord = gcsRecords.get(i);
            if (gcsRecord.getDataTypeName().equals(stageDataTypeName)) {
                log.info("Latest ingestion job id for data type {} found: {}", stageDataTypeName, gcsRecord.getIngestionJobId());
                return Optional.of(gcsRecord.getIngestionJobId());
            }
        }
        return Optional.empty();
    }

    private <T, S> List<GcsDataResultWithDataType> getGcsRecordsForStage(JobContext ctx, IngestionResultProcessingStage<T, S> stage) {
        if (stage.onlyProcessLatestIngestionJob()) {
            Optional<String> latestIngestionJobId = findLatestIngestionJobIdWithDataType(ctx, stage.getDataTypeName());
            return latestIngestionJobId
                    .map(s -> ctx.getGcsRecords().stream().filter(record -> record.getIngestionJobId().equals(s)).collect(Collectors.toList()))
                    .orElseGet(List::of);
        } else {
            return ctx.getGcsRecords();
        }
    }

    private void throwIfThreadInterrupted(JobContext ctx) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            log.error("Thread has been interrupted");
            throw new InterruptedException(ctx.getJobInstanceId() + "has been interrupted.");
        }
    }

    private <T, S> EntityProcessingResult processEntities(
            JobContext ctx, S jobState,
            IngestionResultProcessingStage<T, S> stage,
            List<T> gcsEntities,
            ExecutorService executorService,
            GcsDataResultWithDataType fileRecord) {
        AtomicInteger totalEntities = new AtomicInteger();
        AtomicInteger successfulEntities = new AtomicInteger();
        AtomicInteger failedEntities = new AtomicInteger();
        AtomicInteger processedCount = new AtomicInteger();
        totalEntities.set(gcsEntities.size());
        var futures = gcsEntities.stream().map(entity -> executorService.submit(() -> {
            try {
                stage.process(ctx, jobState, fileRecord.getIngestionJobId(), entity);
                processedCount.getAndIncrement();
                MetricUtils.getTenantCounter(meterRegistry, "etl.worker.entity.processed", ctx).increment();
                successfulEntities.getAndIncrement();
                throwIfThreadInterrupted(ctx);
            } catch (InterruptedException e) {
                executorService.shutdown();
                throw new RuntimeException(e);
            } catch (Exception e) {
                if (!stage.allowFailure()) {
                    throw new RuntimeException(e);
                }
                failedEntities.getAndIncrement();
                log.error("Failed to process entity but still continuing. " +
                                "Job instance {}, tenant: {}, entity: {} , stage: {}, file index: {}",
                        ctx.getJobInstanceId(), ctx.getTenantId(), entity, stage.getName(), fileRecord.getIndex(), e);
            }
        })).toList();

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                if (!stage.allowFailure()) {
                    throw new RuntimeException(e);
                } else {
                    log.error("Caught exception while running job stage, but continuing: ", e);
                }
            }
        });
        return EntityProcessingResult.builder()
                .processedCount(processedCount.get())
                .successfulEntities(successfulEntities.get())
                .failedEntities(failedEntities.get())
                .totalEntities(totalEntities.get())
                .build();
    }

    private <T, S> void runIngestionProcessingStage(JobContext ctx, S jobState, IngestionResultProcessingStage<T, S> stage) throws JsonProcessingException, SQLException, InterruptedException {
        int progress = ctx.getStageProgress(stage.getName()).orElse(-1);
        AtomicInteger processedCount = new AtomicInteger();
        List<GcsDataResultWithDataType> fileRecords = getGcsRecordsForStage(ctx, stage);
        JavaType javaType = objectMapper.constructType(stage.getGcsDataType());
        // The prestage() hook runs even if there is nothing to do for this stage
        // because there might be state dependencies from future stages/hooks
        stage.preStage(ctx, jobState);

        List<GcsDataResultWithDataType> filteredFileRecords = fileRecords.stream()
                .filter(record -> record.getDataTypeName().equals(stage.getDataTypeName()))
                .filter(record -> record.getIndex() > progress)
                .collect(Collectors.toList());
        log.info("Running job stage {}, starting progress {}, dataType: {}, count of files to process: {}",
                stage.getName(), progress, stage.getDataTypeName(), filteredFileRecords.size());

        if (filteredFileRecords.size() == 0) {
            // No gcs files for this stage found. Add an empty stage to the progress detail
            ctx.addStageToProgressDetail(stage.getName(), jobInstanceDatabaseService);
        }

        // Currently we only parallelize the entries within each GCS file.
        // This is because it simplifies the checkpointing logic dramatically.
        // However this will not work for some apps like github where there is 1 giant entity
        // at the top level, and the real processing happens to a subentity within
        // this file.
        ExecutorService executorService;
        if (stage.allowParallelProcessing(ctx.getTenantId(), ctx.getIntegrationId())) {
            log.info("Parallel processing is enabled for stage: {}, tenant: {}, integration: {}",
                    stage.getName(), ctx.getTenantId(), ctx.getIntegrationId());
            int threadpoolSize = stage.getParallelProcessingThreadCount();
            executorService = Executors.newFixedThreadPool(threadpoolSize);
        } else {
            executorService = MoreExecutors.newDirectExecutorService();
        }

        // This loop processes 1 file at a time
        for (int i = 0; i < filteredFileRecords.size(); i++) {
            throwIfThreadInterrupted(ctx);
            Stopwatch st = Stopwatch.createStarted();
            EntityProcessingResult entityProcessingResult;
            GcsDataResultWithDataType fileRecord = filteredFileRecords.get(i);
            // Only process if the data type in the gcs metadata matches that of the stage
            log.info("Stage {} relevant progress: {}/{} - payload index: {} - datatype: {}",
                    stage.getName(), i, filteredFileRecords.size(), fileRecord.getIndex(), fileRecord.getDataTypeName());
            var storageContent = gcsUtils.fetchRecordsFromGcs(
                    fileRecord.getGcsDataResult(),
                    stage.getGcsDataType(),
                    javaType,
                    ctx.getTenantId(),
                    ctx.getIntegrationId()
            );
            if (storageContent == null || storageContent.getData() == null) {
                log.error("Failed to read GCS file {}", fileRecord.getGcsDataResult().toString());
                entityProcessingResult = EntityProcessingResult.builder()
                        .totalEntities(0)
                        .successfulEntities(-1)
                        .failedEntities(-1)
                        .build();
            } else {
                List<T> gcsEntities = storageContent.getData().getRecords();
                entityProcessingResult = processEntities(ctx, jobState, stage, gcsEntities, executorService, fileRecord);
            }
            st.stop();
            FileProgressDetail fileProgressDetail = FileProgressDetail.builder()
                    .entityProgressDetail(EntityProgressDetail.builder()
                            .totalEntities(entityProcessingResult.getTotalEntities())
                            .successful(entityProcessingResult.getSuccessfulEntities())
                            .failed(entityProcessingResult.getFailedEntities())
                            .build())
                    .failures(List.of())
                    .durationMilliSeconds(st.elapsed(TimeUnit.MILLISECONDS))
                    .build();
            ctx.batchUpdateProgressDetail(stage.getName(), fileRecord.getIndex(), fileProgressDetail, jobInstanceDatabaseService);
            // Checkpoint progress for this stage since we have completed processing 1 GCS file
            if (stage.shouldCheckpointIndividualFiles()) {
                ctx.setStageProgressMap(stage.getName(), fileRecord.getIndex(), jobInstanceDatabaseService);
            }
            processedCount.addAndGet(fileProgressDetail.getEntityProgressDetail().getSuccessful());
        }
        ctx.flushProgressDetailUpdates();
        executorService.shutdown();
        stage.postStage(ctx, jobState);
        if (!stage.shouldCheckpointIndividualFiles() && fileRecords.size() > 0) {
            ctx.setStageProgressMap(stage.getName(), fileRecords.get(fileRecords.size() - 1).getIndex(), jobInstanceDatabaseService);
        }
        log.info("Completed job stage {} - successfully processed records {}", stage.getName(), processedCount);
    }

    private <S> void runGenericStage(JobContext ctx, S jobState, GenericJobProcessingStage<S> stage) throws JsonProcessingException, SQLException, InterruptedException {
        // TODO: Potentially add checkpointing here if needed. Currently we do not have a strong use case for this.
        log.info("Running generic job stage {}", stage.getName());
        stage.preStage(ctx, jobState);
        stage.process(ctx, jobState);
        stage.postStage(ctx, jobState);
    }

    private boolean isCausedByInterruptedException(Exception e) {
        if (e instanceof InterruptedException) {
            return true;
        } else {
            return e instanceof RuntimeException && e.getCause() != null && e.getCause() instanceof InterruptedException;
        }
    }

    private <S> void runJobStages(JobContext ctx, EtlProcessor<S> ETLProcessor, S jobState) throws JsonProcessingException, SQLException, InterruptedException {
        for (JobProcessingStageBase<S> stage : ETLProcessor.getJobStages()) {
            try {
                throwIfThreadInterrupted(ctx);
                if (ctx.getJobType() == null || ctx.getJobType() == JobType.INGESTION_RESULT_PROCESSING_JOB) {
                    if (stage instanceof IngestionResultProcessingStage<?, S>) {
                        runIngestionProcessingStage(ctx, jobState, (IngestionResultProcessingStage<?, S>) stage);
                    } else {
                        throw new RuntimeException("Invalid stage type for ingestion result processing job");
                    }
                } else {
                    // Run the generic stage
                    if (stage instanceof GenericJobProcessingStage<S>) {
                        runGenericStage(ctx, jobState, (GenericJobProcessingStage<S>) stage);
                    } else {
                        throw new RuntimeException("Invalid stage type for generic job");
                    }
                }
            } catch (Exception e) {
                if (!isCausedByInterruptedException(e) && stage.allowFailure()) {
                    log.warn("Exception encountered for stage {}, but still continuing", stage.getName(), e);
                } else {
                    throw e;
                }
            }
        }
    }
}
