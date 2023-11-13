package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@JsonDeserialize(builder = ZendeskTicketQuery.ZendeskTicketQueryBuilder.class)
public class ZendeskTicketQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonIgnore
    String cursor;
}