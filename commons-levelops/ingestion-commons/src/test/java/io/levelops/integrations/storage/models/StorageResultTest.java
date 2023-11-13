package io.levelops.integrations.storage.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gcs.models.GcsDataResult;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class StorageResultTest {

    @Test
    public void serialize() {
        StorageResult storageResult = StorageResult.builder()
                .record(GcsDataResult.builder()
                        .htmlUri("html")
                        .build())
                .storageMetadata(StorageMetadata.builder()
                        .integrationType("github")
                        .build())
                .build();
        String prettyJson = DefaultObjectMapper.writeAsPrettyJson(storageResult);
        System.out.println(prettyJson);
        assertThat(prettyJson).contains("\"integration_type\" : \"github\"");
        assertThat(prettyJson).contains("\"html_uri\" : \"html\"");
    }

    @Test
    public void deserialize() throws IOException {
        String input = "{\"uri\":\"gs://ingestion-levelops/data/tenant-foo/integration-1/2019/09/12/job-ea6c2531-e18d-42f3-8e43-d036bc385cf1/repositories.json\",\"records\": [ { \"blob_id\":{\"bucket\":\"ingestion-levelops\",\"name\":\"data/tenant-foo/integration-1/2019/09/12/job-ea6c2531-e18d-42f3-8e43-d036bc385cf1/repositories.json\",\"generation\":1568268770457970}} ],\"destination\":\"gcs\"}";
        StorageResult storageResult = DefaultObjectMapper.get().readValue(input, StorageResult.class);
        System.out.println(storageResult);
        assertThat(storageResult.getRecords()).hasSize(1);
        assertThat(storageResult.getRecords().get(0).getBlobId().getBucket()).isEqualTo("ingestion-levelops");
    }
}