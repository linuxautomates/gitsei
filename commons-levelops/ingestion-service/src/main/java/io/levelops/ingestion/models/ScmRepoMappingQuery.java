package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScmRepoMappingQuery.ScmRepoMappingQueryBuilder.class)
public class ScmRepoMappingQuery implements DataQuery {
    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("user_ids")
    List<String> userIds;
}
