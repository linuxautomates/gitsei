package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.exceptions.AggregationFailedException;
import io.levelops.aggregations.functions.SnykAggQueries;
import io.levelops.aggregations.helpers.SnykAggHelper;
import io.levelops.aggregations.helpers.SnykAggregatorService;
import io.levelops.aggregations.models.SnykAggData;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.temporary.SnykVulnQueryTable;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.snyk.models.SnykVulnerabilityAggWrapper;
import lombok.extern.log4j.Log4j2;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Log4j2
public class SnykAggregationsController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";
    private static final String DEFAULT_TABLE_SUFFIX = "snyk_query_table";

    private final PGSimpleDataSource simpleDataSource;
    private final SnykAggHelper snykAggHelper;
    private final SnykAggregatorService snykAggregatorService;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final String subscriptionName;
    private final ObjectMapper objectMapper;
    private final AggregationHelper<AppAggMessage> aggregationHelper;
    private final Storage storage;

    @Autowired
    public SnykAggregationsController(Storage storage, IntegrationService integrationService,
                                      ControlPlaneService controlPlaneService,
                                      @Qualifier("custom") ObjectMapper mapper,
                                      @Value("${SNYK_AGG_SUB:dev-snyk-sub}") String subscriptionName,
                                      @Value("${GOOGLE_CLOUD_PROJECT}") String projectName,
                                      @Qualifier("simple_data_source") PGSimpleDataSource simpleDataSource,
                                      IntegrationAggService integrationAggService, AggregationsDatabaseService aggregationsDatabaseService,
                                      SnykAggHelper aggHelper,
                                      SnykAggregatorService aggregatorService,
                                      final AggregationHelper<AppAggMessage> aggregationHelper) {
        this.subscriptionName = subscriptionName;
        this.objectMapper = mapper;
        this.aggregationHelper = aggregationHelper;
        this.storage = storage;
        this.snykAggHelper = aggHelper;
        this.snykAggregatorService = aggregatorService;
        this.simpleDataSource = simpleDataSource;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
    }

    @Override
    @Async("snykTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on SNYK Agg: {} ", message.getMessageId());
            Integration it = integrationService.get(message.getCustomer(), message.getIntegrationId())
                    .orElse(null);
            if (it == null || IntegrationType.SNYK != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false, false, true);

            SnykAggData aggData = runSnykAgg(message, results);
            aggData.setResults(results);

            if (!aggregationHelper.saveAppAggregation(message.getCustomer(), List.of(message.getIntegrationId()),
                    message.getOutputBucket(), IntegrationAgg.AnalyticType.SNYK, aggData, AGGREGATION_VERSION)) {
                log.warn("Failed to save analytic!");
            }

            final Date currentTime = new Date();
            if (!snykAggHelper.setupSnykIssues(message.getCustomer(), message.getIntegrationId(), results, currentTime)) {
                log.warn("doTask: Failed to setup snyk issues aggregation for message: {}", message);
            }

        } catch (Exception e) {
            log.error("Fatal error. ", e);
        } finally {
            log.info("Completed work on Agg: {} ", message.getMessageId());
            LoggingUtils.clearThreadLocalContext();
        }
    }

    private SnykAggData runSnykAgg(AppAggMessage message, MultipleTriggerResults results)
            throws AggregationFailedException {
        SnykAggData aggData = SnykAggData.builder()
                .aggVersion(AGGREGATION_VERSION)
                .build();

        try (SnykVulnQueryTable queryTable = new SnykVulnQueryTable(simpleDataSource,
                message.getCustomer(), DEFAULT_TABLE_SUFFIX, objectMapper)) {
            queryTable.createTempTable();
            if (!snykAggHelper.setupSnykIssues(message.getCustomer(), queryTable, results)) {
                throw new AggregationFailedException("Failed to insert agg");
            }

            aggData = snykAggregatorService.aggregateSnykVulns(queryTable, aggData);

            updateWithNewVulnAndTimeSeries(message.getCustomer(), message.getIntegrationId(), message.getOutputBucket(),
                    queryTable.getRows(Collections.emptyList(), false, 0, 100000000),
                    aggData);

        } catch (IOException | SQLException e) {
            throw new AggregationFailedException("Failed to agg snyk vulnerabilities.", e);
        }
        return aggData;
    }

    //TODO: optimize vulnerability handling so that de duping can be done in postgres or similar instead of current memory load
    private void updateWithNewVulnAndTimeSeries(String customer, String integrationId, String bucketName,
                                                List<SnykVulnerabilityAggWrapper> todaysVulns, SnykAggData aggData)
            throws SQLException, IOException {
        Optional<IntegrationAgg> optionalIntegrationAgg = aggregationHelper.getLatestIntegrationAggOlderThanToday(
                customer, List.of(integrationId), IntegrationAgg.AnalyticType.SNYK);

        if (optionalIntegrationAgg.isPresent()) {
            IntegrationAgg agg = optionalIntegrationAgg.get();
            String bucketPath = agg.getGcsPath();
            SnykAggData oldData = objectMapper.readValue(storage.readAllBytes(BlobId.of(bucketName, bucketPath)),
                    SnykAggData.class);
            if (oldData.getResults() != null) {
                Set<String> oldVulnSet = snykAggHelper.getVulnSet(customer, oldData.getResults());
                if (oldVulnSet != null) {
                    List<SnykVulnerabilityAggWrapper> newVulnsFound = new ArrayList<>();
                    todaysVulns.forEach(newVuln -> {
                        if (newVuln.getSnykVulnerability() != null) {
                            if (!oldVulnSet.contains(SnykAggQueries.generateUniqueString(newVuln.getSnykVulnerability()))) {
                                newVulnsFound.add(newVuln);
                            }
                        } else {
                            log.warn("newVuln inner is null {}", newVuln);
                        }
                    });
                    aggData.setNewVulns(newVulnsFound.stream().map(SnykVulnerabilityAggWrapper::getSnykVulnerability).collect(Collectors.toList()));
                    aggData.setNewVulnsCount(newVulnsFound.size());
                }
            }
            if (oldData.getAggBySeverityTimeSeries() != null) {
                aggData.getAggBySeverityTimeSeries().putAll(oldData.getAggBySeverityTimeSeries());
            }
            if (aggData.getAggBySeverityTimeSeries().size() > 30) {
                Long lowest = aggData.getAggBySeverityTimeSeries()
                        .keySet()
                        .stream()
                        .mapToLong(k -> k)
                        .min()
                        .orElse(0);
                aggData.getAggBySeverityTimeSeries().remove(lowest);
            }
        }
        Long time = Instant.now().atZone(ZoneOffset.UTC).toEpochSecond();
        aggData.getAggBySeverityTimeSeries().put(time, aggData.getAggBySeverity());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.SNYK;
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
