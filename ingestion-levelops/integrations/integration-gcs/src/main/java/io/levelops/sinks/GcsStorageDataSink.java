package io.levelops.sinks;

import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.PushException;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.services.GcsStorageService;
import lombok.Builder;

import java.util.stream.Stream;

/**
 * Needs GOOGLE_APPLICATION_CREDENTIALS defined as an environment variable for authentication.
 */
public class GcsStorageDataSink implements StorageDataSink {

    private final GcsStorageService gcsStorageService;

    @Builder
    public GcsStorageDataSink(String bucketName, String pathPrefix) {
        gcsStorageService = new GcsStorageService(bucketName, pathPrefix);
    }

    @Override
    public StorageResult pushOne(Data<StorageData> data) throws PushException {
        return gcsStorageService.pushOne(data.getPayload());
    }

    @Override
    public StorageResult pushMany(Stream<Data<StorageData>> dataStream) throws PushException {
        return gcsStorageService.pushMany(dataStream.map(Data::getPayload));
    }

}
