package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.exceptions.AggregationFailedException;
import io.levelops.aggregations.helpers.TenableAggHelper;
import io.levelops.aggregations.helpers.TenableAggregatorService;
import io.levelops.aggregations.models.TenableAggData;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.temporary.TenableVulnsQueryTable;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
public class TenableAggregationsController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";
    private static final String DEFAULT_TABLE_SUFFIX = "tenable_query_table";

    private static final String REOPENED = "REOPENED";
    private static final String OPEN = "OPEN";


    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final PGSimpleDataSource simpleDataSource;
    private final TenableAggHelper tenableAggHelper;
    private final TenableAggregatorService tenableAggregatorService;
    private final String subscriptionName;
    private final ObjectMapper objectMapper;
    private final AggregationHelper<AppAggMessage> aggregationHelper;
    private final Storage storage;

    @Autowired
    public TenableAggregationsController(Storage storage, IntegrationService integrationService,
                                         @Value("${TENABLE_AGG_SUB:dev-tenable-sub}") String subscriptionName,
                                         IntegrationAggService integrationAggService, AggregationsDatabaseService aggregationsDatabaseService,
                                         @Qualifier("custom") ObjectMapper mapper,
                                         ControlPlaneService controlPlaneService,
                                         @Qualifier("simple_data_source") PGSimpleDataSource simpleDataSource,
                                         TenableAggHelper tenableAggHelper,
                                         TenableAggregatorService tenableAggregatorService,
                                         final AggregationHelper<AppAggMessage> aggregationHelper) {
        this.subscriptionName = subscriptionName;
        this.objectMapper = mapper;
        this.aggregationHelper = aggregationHelper;
        this.storage = storage;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.simpleDataSource = simpleDataSource;
        this.tenableAggHelper = tenableAggHelper;
        this.tenableAggregatorService = tenableAggregatorService;
    }

    @Override
    @Async("tenableTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Tenable Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.TENABLE != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);

            TenableAggData tenableAggData = runTenableAgg(message, results);
            tenableAggData.setResults(results);
            updateTimeSeries(message.getCustomer(), message.getIntegrationId(), message.getOutputBucket(),
                    tenableAggData);
            if (!aggregationHelper.saveAppAggregation(message.getCustomer(), List.of(message.getIntegrationId()),
                    message.getOutputBucket(), IntegrationAgg.AnalyticType.TENABLE, tenableAggData, AGGREGATION_VERSION)) {
                log.warn("Failed to save analytic!");
            }
        } catch (Exception e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }

    }

    TenableAggData runTenableAgg(AppAggMessage message, MultipleTriggerResults results) throws SQLException, AggregationFailedException {
        TenableAggData tenableAggData = TenableAggData.builder()
                .aggVersion(AGGREGATION_VERSION)
                .build();

        try (TenableVulnsQueryTable queryTable = new TenableVulnsQueryTable(simpleDataSource,
                message.getCustomer(), DEFAULT_TABLE_SUFFIX, objectMapper)) {
            queryTable.createTempTable();
            if (!tenableAggHelper.setupTenableVulnerabilities(message.getCustomer(), queryTable, results)) {
                throw new AggregationFailedException("Failed to insert tenable vulns.");
            }
            if (!tenableAggHelper.setupTenableWasVulnerabilities(message.getCustomer(), queryTable, results)) {
                throw new AggregationFailedException("Failed to insert WAS vulns.");
            }
            tenableAggData = tenableAggregatorService.aggregateTenableVulns(queryTable, tenableAggData);
        } catch (AggregationFailedException e) {
            throw new AggregationFailedException("Failed to agg tenable vulnerabilities.", e);
        }
        return tenableAggData;
    }

    void updateTimeSeries(String customer,
                          String integrationId,
                          String bucketName,
                          TenableAggData aggData)
            throws SQLException, IOException {
        Optional<IntegrationAgg> optionalIntegrationAgg = aggregationHelper.getLatestIntegrationAggOlderThanToday(
                customer, List.of(integrationId), IntegrationAgg.AnalyticType.TENABLE);
        Map<String, Long> mapStatusToCt = Map.of(OPEN,
                (long) CollectionUtils.size(aggData.getAggByStatus().get(OPEN)),
                REOPENED,
                (long) CollectionUtils.size(aggData.getAggByStatus().get(REOPENED)));

        if (optionalIntegrationAgg.isPresent()) {
            IntegrationAgg agg = optionalIntegrationAgg.get();
            String bucketPath = agg.getGcsPath();
            TenableAggData oldData = objectMapper.readValue(storage.readAllBytes(BlobId.of(bucketName, bucketPath)),
                    TenableAggData.class);
            if (oldData.getAggByStatusTimeSeries() != null) {
                aggData.getAggByStatusTimeSeries().putAll(oldData.getAggByStatusTimeSeries());
            }
            if (aggData.getAggByStatusTimeSeries().size() > 30) {
                Long lowest = aggData.getAggByStatusTimeSeries()
                        .keySet()
                        .stream()
                        .mapToLong(k -> k)
                        .min()
                        .orElse(0);
                aggData.getAggByStatusTimeSeries().remove(lowest);
            }
        }
        Long time = Instant.now().atZone(ZoneOffset.UTC).toEpochSecond();
        aggData.getAggByStatusTimeSeries().put(time, mapStatusToCt);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.TENABLE;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }
}

