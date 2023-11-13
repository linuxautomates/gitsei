package io.levelops.etl.utils;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import io.levelops.etl.job_framework.EtlProcessor;

import java.util.Map;

public class JobUtils {
    public static JobStatus determineJobSuccessStatus(JobContext jobContext, EtlProcessor<?> ETLProcessor) {
        if (jobContext.getJobType().isGeneric()) {
            return JobStatus.SUCCESS;
        } else {
            return determineJobSuccessStatus(jobContext.getStageProgressDetailMap(), ETLProcessor);
        }
    }

    /*
        Determines whether the job has encountered any partial failures up to this point.
        This looks at the job progress detail to make this decision.
     */
    public static JobStatus determineJobSuccessStatus(Map<String, StageProgressDetail> stageProgressDetailMap, EtlProcessor<?> ETLProcessor) {
        if (stageProgressDetailMap.isEmpty()) {
            return JobStatus.FAILURE;
        }
        if (stageProgressDetailMap.size() != ETLProcessor.getJobStages().size()) {
            return JobStatus.PARTIAL_SUCCESS;
        }
        boolean failurePresent = stageProgressDetailMap.values().stream().anyMatch((stageProgressDetail ->
                stageProgressDetail.getFileProgressMap().values().stream().anyMatch(fileProgressDetail ->
                        fileProgressDetail.getFailures().size() > 0 || fileProgressDetail.getEntityProgressDetail().getFailed() > 0 ||
                                !fileProgressDetail.getEntityProgressDetail().getSuccessful().equals(fileProgressDetail.getEntityProgressDetail().getTotalEntities()))));
        if (failurePresent) {
            return JobStatus.PARTIAL_SUCCESS;
        } else {
            return JobStatus.SUCCESS;
        }
    }
}
