package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class IntegrationTracker {
    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("last_agg_started_at")
    private Long lastAggStartedAt;

    @JsonProperty("last_agg_ended_at")
    private Long lastAggEndedAt;

    @JsonProperty("latest_ingested_at")
    private Long latestIngestedAt;

    @JsonProperty("latest_aggregated_at")
    private Long latestAggregatedAt;

    @JsonProperty("latest_es_indexed_at")
    private Long latestESIndexedAt;
}
