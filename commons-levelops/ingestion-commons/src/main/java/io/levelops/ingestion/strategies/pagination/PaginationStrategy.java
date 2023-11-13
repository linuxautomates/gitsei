package io.levelops.ingestion.strategies.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.storage.models.StorageResult;

public interface PaginationStrategy<D, Q extends DataQuery> {
    ObjectMapper get();

    String getDataType();

    String getIntegrationType();

    StorageResult ingestAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException;

    default StorageResult ingestAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        return ingestAllPages(jobContext, integrationKey, query);
    }
}
