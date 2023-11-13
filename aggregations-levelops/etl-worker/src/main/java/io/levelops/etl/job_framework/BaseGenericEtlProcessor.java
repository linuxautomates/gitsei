package io.levelops.etl.job_framework;

import java.util.List;

// Convenience class for generic ETL job that does not process ingestion results
public abstract class BaseGenericEtlProcessor<S> extends BaseEtlProcessor<S> {
    protected BaseGenericEtlProcessor(Class<S> jobStateType) {
        super(jobStateType);
    }

    @Override
    public List<JobProcessingStageBase<S>> getJobStages() {
        return getGenericJobProcessingStages().stream().map(s -> (JobProcessingStageBase<S>) s).toList();
    }

    public abstract List<GenericJobProcessingStage<S>> getGenericJobProcessingStages();
}
