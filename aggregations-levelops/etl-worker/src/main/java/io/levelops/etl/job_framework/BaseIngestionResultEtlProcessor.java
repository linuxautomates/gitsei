package io.levelops.etl.job_framework;

import java.util.List;

// Convenience class for ETL jobs that process ingestion results
public abstract class BaseIngestionResultEtlProcessor<S> extends BaseEtlProcessor<S> {
    protected BaseIngestionResultEtlProcessor(Class<S> jobStateType) {
        super(jobStateType);
    }

    @Override
    public List<JobProcessingStageBase<S>> getJobStages() {
        return getIngestionProcessingJobStages().stream().map(s -> (JobProcessingStageBase<S>) s).toList();
    }

    public abstract List<IngestionResultProcessingStage<?, S>> getIngestionProcessingJobStages();
}
