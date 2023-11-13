package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGIngestionQuery.HarnessNGIngestionQueryBuilder.class)
public class HarnessNGIngestionQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("organization")
    List<String> organizations;

    @JsonProperty("project")
    List<String> projects;

    @JsonProperty("accountId")
    String accountIdentifier;

}
