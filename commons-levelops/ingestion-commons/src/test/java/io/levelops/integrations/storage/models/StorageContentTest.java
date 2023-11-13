package io.levelops.integrations.storage.models;

import io.levelops.commons.models.ListResponse;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class StorageContentTest {

    @Test
    public void deserialize() throws IOException {
        String input = ResourceUtils.getResourceAsString("integrations/storage/storage_content.json");
        StorageContent<ListResponse<String>> o = DefaultObjectMapper.get().readValue(input,
                StorageContent.getListStorageContentJavaType(DefaultObjectMapper.get(), String.class));
        assertThat(o.getStorageMetadata().getDataType()).isEqualTo("trucs");
        assertThat(o.getStorageMetadata().getIntegrationType()).isEqualTo("nope");
        assertThat(o.getStorageMetadata().getKey().getTenantId()).isEqualTo("max");
        assertThat(o.getStorageMetadata().getKey().getIntegrationId()).isEqualTo("1");
        assertThat(o.getData().getRecords()).containsExactly("il", "y", "a", "plein", "de", "trucs");


    }

}