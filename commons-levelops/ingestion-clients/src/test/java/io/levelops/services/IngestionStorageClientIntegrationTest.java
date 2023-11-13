package io.levelops.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.IngestionPushClientException;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import okhttp3.OkHttpClient;

import org.junit.Test;

public class IngestionStorageClientIntegrationTest {

    @Test
    public void test() throws IngestionPushClientException {
        IngestionStorageClient client = new IngestionStorageClient(new OkHttpClient(), DefaultObjectMapper.get(),
                "https://testapi1.levelops.io/");
        StorageResult out = client.push(System.getenv("TOKEN"), StorageData.builder()
                .integrationKey(IntegrationKey.builder()
                        .tenantId("foo")
                        .integrationId("apitest")
                        .build())
                .jobId("123")
                .relativePath("testdata")
                .dataType("testdatatype")
                .content("vive la data!!!".getBytes())
                .contentType("text/plain")
                .build());
        DefaultObjectMapper.prettyPrint(out);
    }
}