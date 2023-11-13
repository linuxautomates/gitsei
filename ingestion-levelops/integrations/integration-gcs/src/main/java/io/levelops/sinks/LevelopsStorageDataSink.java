package io.levelops.sinks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.exceptions.IngestionPushClientException;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.PushException;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.services.IngestionStorageClient;
import lombok.Builder;
import okhttp3.OkHttpClient;

import java.util.stream.Stream;

public class LevelopsStorageDataSink implements StorageDataSink {

    private final IngestionStorageClient ingestionStorageClient;
    private final String token;

    @Builder
    public LevelopsStorageDataSink(OkHttpClient okHttpClient,
                                   ObjectMapper objectMapper,
                                   String serverApiUrl,
                                   String token) {
        this.token = token;
        ingestionStorageClient = new IngestionStorageClient(okHttpClient, objectMapper, serverApiUrl);
    }

    @Override
    public StorageResult pushOne(Data<StorageData> data) throws PushException {
        try {
            return ingestionStorageClient.push(token, data.getPayload());
        } catch (IngestionPushClientException e) {
            throw new PushException(e);
        }
    }

    @Override
    public StorageResult pushMany(Stream<Data<StorageData>> dataStream) throws PushException {
        throw new UnsupportedOperationException("PushMany not yet supported");
    }
}
