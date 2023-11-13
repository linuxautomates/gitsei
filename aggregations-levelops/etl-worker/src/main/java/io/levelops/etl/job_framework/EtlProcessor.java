package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.models.JobContext;

import java.util.List;

/**
 * There are 2 data structures that an ETLProcessor and it's stages deal with:
 * 1. JobContext: This contains crucial and for the most part immutable information
 * that a job has to deal with. Most fields here are only useful for the job framework/
 * jobRunner.
 * 2. jobState: This is a custom type that each job defines for itself. It is meant
 * to hold custom state that needs to be persisted (in memory) between the different
 * job hooks and stages.
 * Ex. if you do some processing in preProcess() and need to save that for use in one
 * of the stages, store the state in the jobState.
 *
 * @param <S>: Type of the jobState that this job deals with
 *             <p>
 *             NOTE: BaseJobDefinition includes an implementation for getJobStateType() and
 *             should be subclassed instead of directly implementing this interface
 */
public interface EtlProcessor<S> {
    /**
     * Runs before the job. Can be used for setting up state or initialization
     * before each individual stage runs
     */
    void preProcess(JobContext context, S jobState);

    /**
     * Runs after all stages of this job have completed. Can be used for cleanup
     * or any state manipulation required after all GCS data has been processed.
     */
    void postProcess(JobContext context, S jobState);

    /**
     * Lists all job stages that this job has
     */
    List<JobProcessingStageBase<S>> getJobStages();

    /**
     * Creates an instance of the jobState which is then passed around by the
     * jobRunner to all the hooks and stages of this job
     */
    S createState(JobContext context);

    /**
     * Returns the type of the jobState for this job
     */
    Class<S> getJobStateType();

    default String getComponentClass() {
        return this.getClass().getSimpleName();
    }
}
