package io.levelops.integrations.storage.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = StorageMetadata.StorageMetadataBuilder.class)
public class StorageMetadata implements Serializable {

    @JsonProperty("integration_key")
    IntegrationKey key;

    @JsonProperty("integration_type")
    String integrationType;

    @JsonProperty("data_type")
    String dataType;

    @JsonProperty("page_number")
    Integer pageNumber;

}
