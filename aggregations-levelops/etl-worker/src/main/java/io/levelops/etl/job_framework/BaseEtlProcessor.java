package io.levelops.etl.job_framework;

// This is the base class for all ETL processors
// Inherit from {{ BaseIngestionResultEtlProcessor }} or {{ BaseGenericEtlProcessor }} instead
public abstract class BaseEtlProcessor<S> implements EtlProcessor<S> {
    Class<S> jobStateType;

    protected BaseEtlProcessor(Class<S> jobStateType) {
        this.jobStateType = jobStateType;
    }

    @Override
    public Class<S> getJobStateType() {
        return this.jobStateType;
    }
}
