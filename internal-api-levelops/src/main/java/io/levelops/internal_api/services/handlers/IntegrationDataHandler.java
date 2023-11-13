package io.levelops.internal_api.services.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class IntegrationDataHandler {

    final ObjectMapper objectMapper;
    final private Storage storage;

    IntegrationDataHandler(Storage storage, ObjectMapper objectMapper) {
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    public abstract IntegrationType getIntegrationType();

    public abstract String getDataType();

    public abstract Boolean handleStorageResult(String company, String integrationId,
                                                StorageResult storageResult) throws IOException, SQLException;

    List<String> getDataToPush(List<GcsDataResult> gcsDataResults) {
        List<String> data = new ArrayList<>();
        for (GcsDataResult gcsResult : gcsDataResults) {
            io.levelops.integrations.gcs.models.BlobId blobId = gcsResult.getBlobId();
            //TODO: optimize by doing some stream processing, so we dont have to load all data.
            data.add(new String(storage.readAllBytes(BlobId.of(
                    blobId.getBucket(), blobId.getName(), blobId.getGeneration()))));
        }
        return data;
    }
}
