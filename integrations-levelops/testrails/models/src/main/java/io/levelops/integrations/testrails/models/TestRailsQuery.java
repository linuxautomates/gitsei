package io.levelops.integrations.testrails.models;

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
@JsonDeserialize(builder = TestRailsQuery.TestRailsQueryBuilder.class)
public class TestRailsQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    Date from;

    @JsonProperty("should_fetch_users")
    Boolean shouldFetchUsers;

    @JsonProperty("should_fetch_test_executions")
    Boolean shouldFetchTestExecutions;
}
