package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.models.JobContext;

public interface GenericJobProcessingStage<S> extends JobProcessingStageBase<S> {
    void process(JobContext context, S jobState);
}
