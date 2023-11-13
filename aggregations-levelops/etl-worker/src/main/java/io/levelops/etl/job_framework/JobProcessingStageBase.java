package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.models.JobContext;

import java.sql.SQLException;

public interface JobProcessingStageBase<S> {
    // Name of the stage. This is used as a key for storing progress and logging
    String getName();

    /**
     * This function is run every time before a stage is executed.
     */
    void preStage(JobContext context, S jobState) throws SQLException;

    /**
     * This function is run every time after a stage is executed.
     */
    void postStage(JobContext context, S jobState) throws SQLException;

    /**
     * Determines if failures/exceptions for this stage are fatal. i.e whether
     * other hooks and stages will be run if an exception is encountered.
     *
     * false by default
     */
    default boolean allowFailure() {
        return true;
    }
}
