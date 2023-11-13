package io.levelops.integrations.awsdevtools.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * Implementation of {@link IntegrationQuery} which holds information related to an ingestion job
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AWSDevToolsQuery.AWSDevToolsQueryBuilder.class)
public class AWSDevToolsQuery implements IntegrationQuery {

    @JsonProperty("region_integration_key")
    RegionIntegrationKey regionIntegrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("to")
    Date to;

    @JsonProperty("token")
    String token;

    @Override
    public IntegrationKey getIntegrationKey() {
        return regionIntegrationKey.getIntegrationKey();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RegionIntegrationKey.RegionIntegrationKeyBuilder.class)
    public static class RegionIntegrationKey {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("region")
        String region;
    }
}
