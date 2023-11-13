package io.levelops.ingestion.strategies;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.integrations.storage.models.StorageResult;

import javax.annotation.Nullable;

public interface IStorageStrategy {

    <D> StorageResult storeOnePage(
            IntegrationKey integrationKey,
            String integrationType,
            String dataType,
            String jobId,
            ListResponse<D> data,
            @Nullable Integer pageNumber,
            @Nullable String fileNamePrefix) throws IngestException;
}
