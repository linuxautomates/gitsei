package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.AggData;
import io.levelops.aggregations.models.messages.AggregationMessage;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class DefaultAggregationHelper<T extends AggregationMessage> implements AggregationHelper<T> {
    protected final Storage storage;
    protected final ObjectMapper objectMapper;
    protected final IntegrationAggService integrationAggService;
    protected final AggregationsDatabaseService aggregationsDatabaseService;

    @Autowired
    public DefaultAggregationHelper(
            ObjectMapper objectMapper,
            Storage storage,
            IntegrationAggService integrationAggService,
            AggregationsDatabaseService aggregationsDatabaseService) {
        this.storage = storage;
        this.integrationAggService = integrationAggService;
        this.aggregationsDatabaseService = aggregationsDatabaseService;
        this.objectMapper = objectMapper;
    }

    public Optional<IntegrationAgg> getLatestAggregationFromToday(String customer, String productId, IntegrationAgg.AnalyticType analyticType) throws SQLException {
        List<IntegrationAgg> aggs = integrationAggService.listByFilter(customer, List.of(analyticType), null, true, 0, 1).getRecords();
        if (CollectionUtils.isEmpty(aggs)) {
            return Optional.empty();
        }
        final Long startOfToday = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        IntegrationAgg oldest = null;
        for (IntegrationAgg aggRecord : aggs) {
            if (aggRecord.getCreatedAt() < startOfToday) {
                return Optional.ofNullable(aggRecord);
            } else if (oldest != null && aggRecord.getCreatedAt() < oldest.getCreatedAt()) {
                oldest = aggRecord;
            }
        }
        if (oldest != null) {
            return Optional.ofNullable(oldest);
        }
        return Optional.empty();
    }

    public Optional<IntegrationAgg> getLatestIntegrationAggOlderThanToday(String customer, List<String> integrationIds, IntegrationAgg.AnalyticType analyticType) throws SQLException {
        List<IntegrationAgg> aggs = integrationAggService.listByFilter(customer, List.of(analyticType), integrationIds, true, 0, 2).getRecords();
        if (CollectionUtils.isEmpty(aggs)) {
            return Optional.empty();
        }
        final Long startOfToday = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        for (IntegrationAgg aggRecord : aggs) {
            if (aggRecord.getCreatedAt() < startOfToday) {
                return Optional.ofNullable(aggRecord);
            }
        }

        return Optional.empty();
    }

    public Boolean saveAppAggregation(final String customer, final List<String> integrationIds, final String outputBucket,
                                      final IntegrationAgg.AnalyticType type, final AggData aggData, final String aggregationVersion) throws SQLException {
        try {
            final UUID aggId = UUID.randomUUID();
            final List<IntegrationAgg> integrationAggs = integrationAggService.listByFilter(customer, List.of(type),
                    null, true, 0, 1).getRecords();
            String bucketPath = generatePath(customer, Instant.now(),
                    String.format("%s-agg/integration-%s/%s", type,
                            String.join("-", integrationIds), aggId));
            final Long startOfToday = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
            if (integrationAggs.size() != 0 && integrationAggs.get(0).getCreatedAt() > startOfToday) {
                bucketPath = integrationAggs.get(0).getGcsPath();
                //this is to update the agg version but also to ensure we record the updatedat stamp.
                integrationAggService.update(customer, IntegrationAgg.builder()
                        .id(integrationAggs.get(0).getId())
                        .version(aggregationVersion)
                        .build());
            } else {
                final IntegrationAgg agg = IntegrationAgg.builder()
                        .integrationIds(integrationIds)
                        .type(type)
                        .version(aggregationVersion)
                        .id(aggId.toString())
                        .gcsPath(bucketPath)
                        .successful(true)
                        .build();
                integrationAggService.insert(customer, agg);
            }
            //overwrite or create file in gcs
            storage.create(BlobInfo.newBuilder(outputBucket, bucketPath).setContentType("application/json").build(),
                    objectMapper.writeValueAsString(aggData).getBytes());
            log.info("{} aggs written to: {} bucket: {}", type, bucketPath, outputBucket);
        } catch (final JsonProcessingException e) {
            log.warn("Exception writing to bucket: ", e);
            return false;
        }
        return true;
    }

    public Optional<AggregationRecord> getLatestAggOlderThanToday(String customer, List<String> productIds, AggregationRecord.Type aggregationType, String toolType) throws SQLException {
        List<AggregationRecord.Type> aggregationTypes = (aggregationType != null) ? List.of(aggregationType) : null;
        List<String> toolTypes = (StringUtils.isNotBlank(toolType)) ? List.of(toolType) : null;
        List<AggregationRecord> aggs = aggregationsDatabaseService.listByFilter(customer, 0, 2, null, aggregationTypes, toolTypes).getRecords();
        if (CollectionUtils.isEmpty(aggs)) {
            return Optional.empty();
        }
        final Long startOfToday = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        for (AggregationRecord aggRecord : aggs) {
            if (aggRecord.getCreatedAt().getEpochSecond() < startOfToday) {
                return Optional.ofNullable(aggRecord);
            }
        }
        return Optional.empty();
    }

    /*
     /dev-ingestion-levelops/data/tenant-foo/integration-78/2020/05/14/job-fba248a8-a3f4-4791-82f9-3041c44aaeac/fields/fields.json
     /dev-ingestion-levelops/data/tenant-foo/integration-78/2020/05/14/job-fba248a8-a3f4-4791-82f9-3041c44aaeac/issues/issues.0.json
     /dev-ingestion-levelops/data/tenant-foo/integration-78/2020/05/14/job-fba248a8-a3f4-4791-82f9-3041c44aaeac/projects/projects.json
     /dev-ingestion-levelops/results/tenant-foo/tool-jenkins_config/2020/06/04/{UUID}

     /dev-aggregations-levelops/tenant-foo/2020/06/04/product-78/JIRA-agg/integration-99/{UUID}
     /dev-aggregations-levelops/tenant-foo/2020/06/04/jira_config-agg/{UUID}
     */
    public Boolean savePluginAggregation(final String customer, final List<String> productIdStrings, final String outputBucket, final AggregationRecord.Type type,
                                         final String pluginToolName, final AggData aggData, final String aggregationVersion) throws SQLException {
        try {
            Instant now = Instant.now();
            final UUID aggId = UUID.randomUUID();
            List<Integer> productIds = productIdStrings.stream().map(Integer::parseInt).collect(Collectors.toList());
            final List<AggregationRecord> aggregationRecords = aggregationsDatabaseService.listByFilter(customer, 0, 1, null, List.of(type),
                    List.of(pluginToolName)).getRecords();
            String bucketPath = generatePath(customer, now,
                    String.format("%s-agg/%s", pluginToolName, aggId));
            final Long startOfToday = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
            if (aggregationRecords.size() != 0 && aggregationRecords.get(0).getCreatedAt().getEpochSecond() > startOfToday) {
                AggregationRecord existing = aggregationRecords.get(0);
                bucketPath = existing.getGcsPath();
                //this is to update the agg version but also to ensure we record the updatedat stamp.
                final AggregationRecord agg = AggregationRecord.builder()
                        .id(existing.getId())
                        .version(aggregationVersion)
                        .successful(true)
                        .type(type)
                        .toolType(pluginToolName)
                        .gcsPath(bucketPath)
                        .productIds(productIds)
                        .build();
                aggregationsDatabaseService.update(customer, agg);
            } else {
                final AggregationRecord agg = AggregationRecord.builder()
                        .id(aggId.toString())
                        .version(aggregationVersion)
                        .successful(true)
                        .type(type)
                        .toolType(pluginToolName)
                        .gcsPath(bucketPath)
                        .productIds(productIds)
                        .build();
                aggregationsDatabaseService.insertAndReturnId(customer, agg);
            }
            //overwrite or create file in gcs
            storage.create(BlobInfo.newBuilder(outputBucket, bucketPath).setContentType("application/json").build(),
                    objectMapper.writeValueAsString(aggData).getBytes());
            log.info("{} aggs for plugin tool {} written to: {} bucket: {}", type, pluginToolName, bucketPath, outputBucket);
        } catch (final JsonProcessingException e) {
            log.warn("Exception writing to bucket: ", e);
            return false;
        }
        return true;
    }

    public String generatePath(final String tenantId, final Instant date, final String pathSuffix) {
        return String.format("tenant-%s/%s/%s",
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                pathSuffix);
    }

    public boolean send(final String path, final String company, final String jsonBody) throws IOException {
        try (CloseableHttpClient basicHttpClient = HttpClients.createMinimal()) {
            final HttpPost postReq = new HttpPost(String.format(
                    "http://internal-api-lb/internal/v1/tenants/%s/%s", company, path));
            postReq.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse resp = basicHttpClient.execute(postReq)) {
                return (resp.getStatusLine().getStatusCode() - 200) < 99;
            }
        }
    }
}
