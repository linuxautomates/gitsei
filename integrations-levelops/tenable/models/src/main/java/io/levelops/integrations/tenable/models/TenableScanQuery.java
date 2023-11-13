package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

/**
 * 'Bean for Export request query criteria. For example <a href="https://developer.tenable.com/docs/refine-vulnerability-export-requests</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TenableScanQuery.TenableScanQueryBuilder.class)
public class TenableScanQuery implements IntegrationQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("chunk_size")
    Integer chunkSize;

    @JsonProperty
    Long since;

    Boolean partial;
}
