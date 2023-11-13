package io.levelops.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.exceptions.IngestionPushClientException;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.Objects;

import static io.levelops.commons.client.ClientConstants.APIKEY_;
import static io.levelops.commons.client.ClientConstants.AUTHORIZATION;

public class IngestionStorageClient {

    private final ClientHelper<IngestionPushClientException> clientHelper;
    private final String serverApiUrl;

    @Builder
    public IngestionStorageClient(OkHttpClient okHttpClient,
                                  ObjectMapper objectMapper,
                                  String serverApiUrl) {
        this.serverApiUrl = serverApiUrl;
        clientHelper = new ClientHelper<>(okHttpClient, objectMapper, IngestionPushClientException.class);
    }

    public StorageResult push(String token, StorageData storageData) throws IngestionPushClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(serverApiUrl)).newBuilder()
                .addPathSegments("v1/ingestion/storage/push")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(AUTHORIZATION, APIKEY_ + token)
                .post(clientHelper.createJsonRequestBody(storageData))
                .build();
        return clientHelper.executeAndParse(request, StorageResult.class);
    }

}
