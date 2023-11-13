package io.levelops.ingestion.strategies.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;

@Log4j2
public abstract class BasePaginationStrategy<D,Q extends DataQuery> implements PaginationStrategy<D, Q> {

    protected final ObjectMapper objectMapper;
    protected final String integrationType;
    protected final String dataType;
    protected final boolean uniqueOutputFiles;

    public BasePaginationStrategy(ObjectMapper objectMapper,
                                  String integrationType,
                                  String dataType,
                                  Boolean uniqueOutputFiles) {
        this.objectMapper = objectMapper;
        this.integrationType = integrationType;
        this.dataType = dataType;
        this.uniqueOutputFiles = BooleanUtils.isTrue(uniqueOutputFiles);
    }

    @Override
    public ObjectMapper get() {
        return objectMapper;
    }

    @Override
    public String getIntegrationType() {
        return integrationType;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    protected String generateFileNamePrefix() {
        if (uniqueOutputFiles) {
            return (System.currentTimeMillis() / 1000) + "_";
        }
        return null;
    }

    @Override
    public StorageResult ingestAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "jobId cannot be null or empty.");

        try {
            StorageResult storageResult = fetchAndStoreAllPages(jobContext, integrationKey, query, intermediateStateUpdater);

            return addMetadataToStorageResult(integrationKey, storageResult);
        } catch (ResumableIngestException e) {
            if (e.getResult() instanceof StorageResult) {
                throw e.toBuilder()
                        .result(addMetadataToStorageResult(integrationKey, (StorageResult) e.getResult()))
                        .build();
            } else {
                throw e;
            }
        }
    }

    @Override
    public StorageResult ingestAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException {
        return ingestAllPages(jobContext, integrationKey, query, null);
    }

    private StorageResult addMetadataToStorageResult(IntegrationKey integrationKey, StorageResult storageResult) {
        return storageResult.toBuilder()
                .prefixUri(StorageStrategy.getRelativePathPrefix(dataType))
                .storageMetadata(StorageMetadata.builder()
                        .key(integrationKey)
                        .integrationType(integrationType)
                        .dataType(dataType)
                        .build())
                .build();
    }

    public abstract StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException;

    public StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        return fetchAndStoreAllPages(jobContext, integrationKey, query);
    }
}
