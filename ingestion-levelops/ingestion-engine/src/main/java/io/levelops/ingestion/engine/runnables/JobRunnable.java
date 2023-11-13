package io.levelops.ingestion.engine.runnables;

import com.google.common.base.MoreObjects;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.engine.CallbackService;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Log4j2
public class JobRunnable<T> implements Runnable {

    private final IngestionEngine.EngineJob job;
    private final JobContext jobContext;
    private final Callable<T> callable;
    private final CallbackService callbackService;

    @Builder
    public JobRunnable(IngestionEngine.EngineJob job,
                       JobContext jobContext,
                       Callable<T> callable,
                       @Nullable CallbackService callbackService) {
        this.job = job;
        this.jobContext = jobContext;
        this.callable = callable;
        this.callbackService = callbackService;
    }

    @Override
    public void run() {
        try {
            if (jobContext != null) {
                ThreadContext.put("job_id", "[" + StringUtils.right(StringUtils.defaultString(jobContext.getJobId()), 7) + "]");
                ThreadContext.put("tenant_id", "[" + StringUtils.defaultString(jobContext.getTenantId()) + "]");
                ThreadContext.put("integration_id", "[" + StringUtils.defaultString(jobContext.getIntegrationId()) + "]");
            }
            log.info("Thread '{}' assigned to job_id={}", Thread.currentThread().getName(), job.getId());

            // 1- attempt to run job
            boolean status = runJob();

            // 2- mark as done
            job.markAsDone();

            // 3- callback
            callback();

            // log completion
            long duration = (job.getCreatedAt() != null && job.getDoneAt() != null)
                    ? Duration.between(job.getCreatedAt().toInstant(), job.getDoneAt().toInstant()).getSeconds()
                    : -1;
            log.info("{} Completed job={} in {} s {}", status ? "✔︎" : "✘", job.getId(), duration, status ? "successfully" : "with failure");
        } finally {
           ThreadContext.clearAll();
        }
    }

    private boolean runJob() {
        try {
            T call = callable.call();
            handleIngestionFailures(call);
            job.setResult(call);
        } catch (Throwable e) {
            log.warn("Failed to run job_id={} (controller={})", job.getId(), job.getController().getComponentClass(), e);
            if (e instanceof ResumableIngestException) {
                ResumableIngestException resumable = ((ResumableIngestException) e);
                job.setException(MoreObjects.firstNonNull(resumable.getError(), e));
                job.setIntermediateState(resumable.getIntermediateState());
                job.setResult(resumable.getResult());
            } else {
                job.setException(e);
            }
            return false;
        }
        return true;
    }

    private void handleIngestionFailures(T call) {
        if (!(call instanceof ControllerIngestionResultList)) {
            return;
        }
        List<ControllerIngestionResult> records = ((ControllerIngestionResultList) call).getRecords();
        List<IngestionFailure> ingestionFailures = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(records)) {
            records.forEach(record -> {
                if (record instanceof StorageResult) {
                    if (CollectionUtils.isNotEmpty(((StorageResult) record).getIngestionFailures())) {
                        ingestionFailures.addAll(((StorageResult) record).getIngestionFailures());
                    }
                    ((StorageResult) record).setIngestionFailures(null);
                }
            });
        }
        job.setIngestionFailures(ingestionFailures);
    }

    private void callback() {
        if (Strings.isEmpty(job.getCallbackUrl())) {
            return;
        }
        if (callbackService == null) {
            log.error("No callback service configured but callback was requested for job_id={}!", job.getId());
            return;
        }

        try {
            callbackService.callback(job);
        } catch (Throwable e) {
            log.warn("Failed to send callback for job_id={}", job.getId(), e);
        }
    }
}
