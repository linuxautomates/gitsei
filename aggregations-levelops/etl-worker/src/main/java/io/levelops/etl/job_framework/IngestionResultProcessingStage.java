package io.levelops.etl.job_framework;

import io.levelops.aggregations_shared.models.JobContext;

import java.sql.SQLException;

/**
 * Refer to the documentation for JobDefinition for more details on JobContext
 * vs JobState
 *
 * @param <T> The entity type that this stage deals with
 * @param <S> The type of the custom jobState that this job and stage deals with
 *            <p>
 *            NOTE: BaseIngestionResultProcessingStage includes an implementation for getGcsDataTypeClass() and
 *            should be subclassed instead of directly implementing this interface
 */
public interface IngestionResultProcessingStage<T, S> extends JobProcessingStageBase<S> {
    /**
     * Processes the provided entity
     *
     * @param context        Contains crucial information about the job
     * @param jobState       Contains job specific state. If you want to persist state
     *                       between job stages, this is the place to put it.
     * @param ingestionJobId The ingestion job id that this entity was found in
     * @param entity         The deserialized entity that was found in gcs.
     * @throws SQLException
     */
    void process(JobContext context, S jobState, String ingestionJobId, T entity) throws SQLException;

    /**
     * This should correspond to the data type name that exists in the ingestion
     * metadata
     */
    String getDataTypeName();

    /**
     * The class of the deserialized entity that this stage will process.
     */
    Class<T> getGcsDataType();

    /**
     * The return value determines whether this stage operates on the entire
     * payload or only the latest ingestion job which contains data corresponding
     * to the data type of this stage.
     *
     * The use case here is for processing things like jira users. Every time
     * ingestion gets jira users, it gets the entire list of users present
     * at the time. Thus, we only need to process the latest ingestion jobs
     * data to ensure that we have the most up-to-date information.
     */
    boolean onlyProcessLatestIngestionJob();

    /**
     * Determines if each file that the stage processes should be checkpointed
     * or not. For most stages this should be set to true, so that retries
     * don't end up duplicating work. However, some stages do not persist data
     * as part of their process() methods, they simply accumulate parsed data
     * and then batch upsert in their postStage(). In such cases the stage may
     * desire to switch off file level checkpointing.
     * <p>
     * If this is set to false, we only checkpoint the stage once it has
     * processed all available files AND successfully run the post stage method
     */
    default boolean shouldCheckpointIndividualFiles() {
        return true;
    }

    /**
     * Determines if the process() method can be called in parallel, ensure
     * that this method is thread safe before enabling this feature
     */
    default boolean allowParallelProcessing(String tenantId, String integrationId) {
        return false;
    }

    /**
     * Only applicable if allowParallelProcessing() is set to true
     */
    default int getParallelProcessingThreadCount() {
        return 1;
    }
}
