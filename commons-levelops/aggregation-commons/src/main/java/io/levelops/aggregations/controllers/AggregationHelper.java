package io.levelops.aggregations.controllers;

import io.levelops.aggregations.models.AggData;
import io.levelops.aggregations.models.messages.AggregationMessage;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.databases.models.database.IntegrationAgg;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AggregationHelper<T extends AggregationMessage> {
    Optional<IntegrationAgg> getLatestAggregationFromToday(String customer, String productId,
            IntegrationAgg.AnalyticType analyticType) throws SQLException;
    Optional<IntegrationAgg> getLatestIntegrationAggOlderThanToday(final String customer, List<String> integrationIds, final IntegrationAgg.AnalyticType analyticType) throws SQLException;
    Optional<AggregationRecord> getLatestAggOlderThanToday(String customer, List<String> productIds,
            AggregationRecord.Type aggregationType, String toolType) throws SQLException;

    Boolean saveAppAggregation(final String customer, final List<String> integrationIds,
            final String outputBucket, final IntegrationAgg.AnalyticType type, final AggData aggData, final String aggregationVersion)
            throws SQLException;
    Boolean savePluginAggregation(final String customer, final List<String> productIdStrings, final String outputBucket,
            final AggregationRecord.Type type, final String pluginToolName, final AggData aggData, final String aggregationVersion) throws SQLException;

    String generatePath(final String tenantId, final Instant date, final String pathSuffix);

    boolean send(final String path, final String company, final String jsonBody) throws IOException;
}