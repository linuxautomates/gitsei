package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.PagerDutyTimeSeries;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Service;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyService;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyStatus;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyUser;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ServicesDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyServicesDatabaseService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.pagerduty.models.PagerDutyAlert;
import io.levelops.integrations.pagerduty.models.PagerDutyEntity;
import io.levelops.integrations.pagerduty.models.PagerDutyIncident;
import io.levelops.integrations.pagerduty.models.PagerDutyIngestionDataType;
import io.levelops.integrations.pagerduty.models.PagerDutyLogEntry;
import io.levelops.integrations.pagerduty.models.PagerDutyService;
import io.levelops.integrations.pagerduty.models.PagerDutyUser;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@Log4j2
public class PagerDutyAggregationController implements AggregationsController<AppAggMessage> {

    private static final String AGGREGATION_VERSION = "1";
    private static final String TABLE_NAME = "tmp_pager_duty_agg";
    private final Integer MAX_TIME_SERIES_SIZE;
    private final Integer MAX_ITERATIONS = 30;

    private final PGSimpleDataSource dataSource;
    private final int batchSize;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final AggregationHelper<AppAggMessage> aggregationHelper;
    private final ObjectMapper objectMapper;
    private final Storage storage;
    private final String subscriptionName;

    private final PagerDutyAlertsDatabaseService alertsService;
    private final PagerDutyIncidentsDatabaseService incidentsService;
    private final PagerDutyServicesDatabaseService pdServices;
    private final ServicesDatabaseService services;

    private static final Set<String> supportedIngestionDataTypeInStringsForTempTable = Set.of(
            PagerDutyIngestionDataType.ALERT.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.INCIDENT.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.LOG_ENTRY.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.SERVICE.getIngestionPluralDataType());

    private static final Set<String> supportedIngestionDataTypeInStrings = Set.of(
            PagerDutyIngestionDataType.ALERT.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.INCIDENT.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.LOG_ENTRY.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.SERVICE.getIngestionPluralDataType(),
            PagerDutyIngestionDataType.USER.getIngestionPluralDataType());

    /**
     * Aggregations for Pager Duty.
     * 
     * @param mapper                object mapper to serialize and deserialize json
     *                              objects.
     * @param storage               bucket service
     * @param projectName           GCP project name needed to interact with the GCP
     *                              services
     * @param integrationService    service to interact with the integration service
     * @param controlPlaneService   component to interact with the control panel
     *                              service
     * @param integrationAggService component to interact with the aggregations
     *                              service
     * @param batchSize             the size of the batch insert to the tmp table
     * @param dataSource            datasource for the temptable
     */
    @Autowired
    public PagerDutyAggregationController(@Qualifier("custom") final ObjectMapper mapper, final Storage storage,
            @Value("${PAGERDUTY_AGG_SUB:dev-pagerduty-agg-sub}") String subscriptionName,
            @Value("${GOOGLE_CLOUD_PROJECT}") final String projectName, final IntegrationService integrationService,
            final ControlPlaneService controlPlaneService, final IntegrationAggService integrationAggService,
            final AggregationsDatabaseService aggregationsDatabaseService,
            @Value("${PD_AGG_BATCH_SIZE:100}") int batchSize,
            @Qualifier("simple_data_source") final PGSimpleDataSource dataSource,
            @Value("${PAGERDUTY_MAX_TIME_SERIES_SIZE:30}") Integer maxTimeSeriesSize,
            final AggregationHelper<AppAggMessage> aggregationHelper,
            final PagerDutyAlertsDatabaseService alertsService,
            final PagerDutyIncidentsDatabaseService incidentsService,
            final PagerDutyServicesDatabaseService pdServices,
            final ServicesDatabaseService services) {
        // super(mapper, storage, subscriptionName, integrationAggService,
        // aggregationsDatabaseService, AGGREGATION_VERSION, IntegrationType.PAGERDUTY,
        // AppAggMessage.class);
        this.aggregationHelper = aggregationHelper;
        this.subscriptionName = subscriptionName;
        this.objectMapper = mapper;
        this.storage = storage;
        this.dataSource = dataSource;
        this.batchSize = batchSize;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.MAX_TIME_SERIES_SIZE = maxTimeSeriesSize;
        this.alertsService = alertsService;
        this.incidentsService = incidentsService;
        this.pdServices = pdServices;
        this.services = services;
    }

    @Override
    @Async("pagerDutyTaskExecutor")
    public void doTask(final AppAggMessage message) {
        log.info("Starting work on Agg: {} ", message.getMessageId());
        // get integration info
        Integration integration = integrationService.get(message.getCustomer(), message.getIntegrationId())
                .orElse(null);

        try {
            persistLatestResults(message.getCustomer(), integration.getId());
        } catch (IngestionServiceException e) {
            log.error("[{}] Unable to process PD aggs successfully.", message.getMessageId(), e);
        }

        // comented out due to an issue with existing data vs new models that is causing it to break
        // this aggregations will be done differently anyways so no need to keep processing them here.
        //
        // db - setup table
        // try (PagerDutyQueryTable table = new PagerDutyQueryTable(dataSource, message.getCustomer(), TABLE_NAME,
        //         objectMapper, batchSize);) {
        //     // get triggered jobs results
        //     MultipleTriggerResults triggerResults = controlPlaneService.getAllTriggerResults(
        //             IntegrationKey.builder().tenantId(message.getCustomer()).integrationId(integration.getId()).build(),
        //             false, false, true);
        //     // get items to be aggregated
        //     // alerts
        //     // incidents
        //     // log entries
        //     Stream<PagerDutyEntity> items = getPagerDutyEntitiesStream(objectMapper, storage,
        //             triggerResults.getTriggerResults().stream().flatMap(result -> result.getJobs().stream()));
        //     // populate db
        //     table.insertRows(items);
        //     // Descending order..
        //     SortedSet<PagerDutyTimeSeries> finalTimeSeries = new TreeSet<PagerDutyTimeSeries>((PagerDutyTimeSeries a,
        //             PagerDutyTimeSeries b) -> a.from() > b.from() ? -1 : a.from() == b.from() ? 0 : 1);
        //     // aggregate
        //     // db queries
        //     // create agg result object
        //     AggData results = PagerDutyAggData.builder().aggVersion(AGGREGATION_VERSION).results(triggerResults)
        //             .aggIncidentsByPriority(PagerDutyAggregator.aggregateIncidentsByPriority(table))
        //             .aggIncidentsByUrgency(PagerDutyAggregator.aggregateIncidentsByUrgency(table))
        //             // .aggIncidentsByUrgencyPriority(null)
        //             .aggAlertsBySeverity(PagerDutyAggregator.aggregateAlertsBySeverity(table))
        //             .latestAlertsBySeverity(PagerDutyAggregator.getLatestAlertsBySeverity(table))
        //             .latestIncidentsByUrgency(PagerDutyAggregator.getLatestIncidentsByUrgency(table))
        //             .latestIncidentsByPriority(PagerDutyAggregator.getLatestIncidentsByPriority(table))
        //             .timeSeries(finalTimeSeries) // get previous aggregation and merge
        //             .build();
        //     log.info("[{}] Regular aggregations completed.", message.getMessageId());
        //     final Instant fromToday = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC)
        //             .toInstant(); // start of today
        //     // get previous aggregation if any
        //     Optional<IntegrationAgg> priorAgg = aggregationHelper.getLatestIntegrationAggOlderThanToday(
        //             message.getCustomer(), message.getProductId(), List.of(integration.getId()), AnalyticType.PAGERDUTY);
        //     // get latest time series
        //     if (priorAgg.isPresent()) {
        //         var oldData = objectMapper.readValue(
        //                 storage.readAllBytes(BlobId.of(message.getOutputBucket(), priorAgg.get().getGcsPath())),
        //                 PagerDutyAggData.class);
        //         if (oldData != null && CollectionUtils.isNotEmpty(oldData.timeSeries())) {
        //             finalTimeSeries.addAll(oldData.timeSeries());
        //         }
        //         var priorTodaysTimeSeries = finalTimeSeries.stream()
        //                 .filter(item -> item.from() == fromToday.getEpochSecond()).findFirst();
        //         if (priorTodaysTimeSeries.isPresent()) {
        //             finalTimeSeries.remove(priorTodaysTimeSeries.get());
        //         }
        //     } else {
        //         log.info("[{}] No previous aggregations found.", message.getMessageId());
        //     }
        //     // generate the time series for previous days, for a max of
        //     // 'MAX_TIME_SERIES_SIZE' days including today
        //     // if there are other days but for some reason there is a gap between the last
        //     // day and today
        //     // then we will aggregate for all the days in the gap
        //     List<Instant> timeSeries = finalTimeSeries.size() == 0 ? IntStream.range(1, MAX_TIME_SERIES_SIZE - 1)
        //             .mapToObj(i -> fromToday.minus(i, ChronoUnit.DAYS)).collect(Collectors.toList())
        //             : addGapTimeSeries(finalTimeSeries.first(), fromToday);
        //     timeSeries.add(fromToday);
        //     log.info("Processing {} time series", timeSeries.size());

        //     timeSeries.stream().forEach(from -> {
        //         Instant to = from.plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.SECONDS); // end of the day
        //         finalTimeSeries.add(PagerDutyTimeSeries.builder().from(from.toEpochMilli() / 1000)
        //                 .to(to.toEpochMilli() / 1000)
        //                 .byIncidentResolved(PagerDutyAggregator.getIncidentsResolvedLeadTimeStats(table, from, to))
        //                 .byAlertResolved(PagerDutyAggregator.getAlertsResolvedLeadTimeStats(table, from, to))
        //                 .byIncidentUrgency(PagerDutyAggregator.getIncidentsByUrgencyTimeSeriesStats(table, from, to))
        //                 .byAlertSeverity(PagerDutyAggregator.getAlertsBySeverityTimeSeriesStats(table, from, to))
        //                 .build());
        //     });
        //     // remove any record older than the max time series size
        //     var iteration = 0;
        //     while (finalTimeSeries.size() > MAX_TIME_SERIES_SIZE && iteration <= MAX_ITERATIONS) {
        //         iteration++;
        //         log.info("removing old time series elemnts... current={}, max={}", finalTimeSeries.size(),
        //                 MAX_TIME_SERIES_SIZE);
        //         finalTimeSeries.remove(finalTimeSeries.last()); // since it is sorted in DESC order the last object is
        //                                                         // the oldest.
        //     }
        //     // save agg resutls
        //     if (!aggregationHelper.saveAppAggregation(message.getCustomer(), message.getProductId(),
        //             List.of(message.getIntegrationId()), message.getOutputBucket(),
        //             IntegrationAgg.AnalyticType.PAGERDUTY, results, AGGREGATION_VERSION)) {
        //         log.warn("Failed to save analytic!");
        //     }
            // Inset data in permanent tables
        // } catch (IngestionServiceException | IOException | SQLException e) {
        //     log.error("[{}] Unable to process PD aggs successfully.", message.getMessageId(), e);
        // }
    }

    private List<Instant> addGapTimeSeries(PagerDutyTimeSeries first, Instant fromToday) {
        Long maxOld = first.from();
        if (maxOld == null || first.from() < maxOld) {
            maxOld = fromToday.minus(MAX_TIME_SERIES_SIZE, ChronoUnit.DAYS).toEpochMilli() / 1000;
        }
        final long reference = maxOld;
        return IntStream.range(1, MAX_TIME_SERIES_SIZE - 1)
                .filter(i -> (fromToday.minus(i, ChronoUnit.DAYS).toEpochMilli() / 1000) > reference) // if calculated
                                                                                                      // start of time
                                                                                                      // frame is newer
                                                                                                      // than the most
                                                                                                      // recent time
                                                                                                      // from the first
                                                                                                      // record.
                .mapToObj(i -> fromToday.minus(i, ChronoUnit.DAYS)).collect(Collectors.toList());
    }

    private void persistLatestResults(final String customer, final String integrationId)
            throws IngestionServiceException {
        // need to process in order
        var triggerResults = controlPlaneService.getAllTriggerResults(
                IntegrationKey.builder().tenantId(customer).integrationId(integrationId).build(), true, false, true);

        var integrationIdInt = Integer.valueOf(integrationId);
        // first services and users
        var items = getPagerDutyEntitiesStream(objectMapper, storage, 
                        triggerResults.getTriggerResults().stream().flatMap(result -> result.getJobs().stream()), supportedIngestionDataTypeInStrings);
        items.filter(item -> ((PagerDutyIngestionDataType) item.getIngestionDataType()) == PagerDutyIngestionDataType.SERVICE 
                                || ((PagerDutyIngestionDataType) item.getIngestionDataType()) == PagerDutyIngestionDataType.USER )
        .forEach(item -> {
            log.info("Processing item: {}", item);
            var dataType = (PagerDutyIngestionDataType) item.getIngestionDataType();
            switch(dataType){
                case SERVICE:
                    var pdService = (PagerDutyService) item;
                    try {
                        var serviceId = getServiceId(customer, pdService.getName());

                        var dbService = pdServiceToDbServiceConverter(serviceId, integrationIdInt, pdService);
                        pdServices.insert(customer, dbService);
                    } catch (SQLException | NullPointerException e) {
                        log.error("[{}] unable to persist the Service '{} - {}'", customer, pdService.getId(), pdService.getName(), e);
                    }
                    break;
                case USER:
                    var pdUser = (PagerDutyUser) item;
                    try {
                        var dbUser = pdUserToDbUserConverter(pdUser);
                        incidentsService.insertUser(customer, dbUser, null);
                    } catch (Exception e) {
                        log.error("[{}] unable to persist the User '{} - {}'", customer, pdUser.getId(), pdUser.getName(), e);
                    }
                    break;
                default:
                    break;
            }
        });
        // then incidents or alerts
        items = getPagerDutyEntitiesStream(objectMapper, storage, 
                    triggerResults.getTriggerResults().stream().flatMap(result -> result.getJobs().stream()), supportedIngestionDataTypeInStrings);
        items.filter(item -> { 
            return ((PagerDutyIngestionDataType) item.getIngestionDataType()) == PagerDutyIngestionDataType.INCIDENT 
                        || ((PagerDutyIngestionDataType) item.getIngestionDataType()) == PagerDutyIngestionDataType.ALERT
                        || ((PagerDutyIngestionDataType) item.getIngestionDataType()) == PagerDutyIngestionDataType.LOG_ENTRY;
        })
        .forEach(item -> {
            log.info("Processing item: {}", item);
            var dataType = (PagerDutyIngestionDataType) item.getIngestionDataType();
            switch (dataType) {
                // incidents
                case INCIDENT:
                    var pdIncident = (PagerDutyIncident) item;
                    try {
                        var pdService = getPDService(customer, integrationIdInt, pdIncident.getService().getId());
                        if (pdService.isEmpty()) {
                            return;
                        }

                        var dbIncident = pdIncidentToDbIncidentConverter(pdService.get().getId(), pdIncident);
                        incidentsService.insert(customer, dbIncident);
                    } catch (SQLException | NullPointerException e) {
                        log.error("[{}] unable to persist the Incident '{} - {}'", customer, pdIncident.getId(), pdIncident.getTitle(), e);
                    }
                    break;
                // or alerts
                case ALERT:
                    var pdAlert = (PagerDutyAlert) item;
                    try {
                        var pdService = getPDService(customer, integrationIdInt, pdAlert.getService().getId());
                        Optional<DbPagerDutyIncident> optionalIncident = getIncident(customer, pdAlert.getIncident());
                        if (optionalIncident.isEmpty() || pdService.isEmpty()) {
                            break;
                        }

                        var incident = optionalIncident.get();
                        var alert = pdAlertToDbAlertConverter(pdService.get().getId(), incident.getId(), pdAlert);
                        alertsService.insert(customer, alert);
                    } catch (SQLException | NullPointerException e) {
                        log.error("[{}] unable to persist the Alert '{} - {}'", customer, pdAlert.getId(), pdAlert.getSummary(), e);
                    }
                    break;
                case LOG_ENTRY:
                    var pdLog = (PagerDutyLogEntry) item;
                    try {
                        switch(pdLog.getType()){
                            case "acknowledge_log_entry":
                            case "resolve_log_entry":
                            case "unacknowledge_log_entry":
                            case "event_rule_action_log_entry":
                            case "priority_change_log_entry":
                            case "trigger_log_entry":
                                var pdService = getPDService(customer, integrationIdInt, pdLog.getIncident().getService().getId());
                                if (pdService.isEmpty()) {
                                    return;
                                }
                                var dbIncident = pdIncidentToDbIncidentConverter(pdService.get().getId(), pdLog.getIncident());
                                incidentsService.insert(customer, dbIncident);
                                break;
                        }
                    } catch (SQLException | NullPointerException e) {
                        log.error("[{}] unable to persist the Log Entry '{} - {}'", customer, pdLog.getId(), pdLog.getType(), e);
                    }
                    break;
                case SERVICE: case USER:
                    break;
            }
        });
    }

    private DbPagerDutyAlert pdAlertToDbAlertConverter(UUID serviceId, UUID incidentId, PagerDutyAlert pdAlert) {
        var updatedAt = pdAlert.getUpdatedAt() != null ? Instant.ofEpochMilli(pdAlert.getUpdatedAt()) : null;
        Map<String, Object> body = Map.of();
        
        try {
            body = ObjectUtils.isEmpty(pdAlert.getBody()) ? Map.of()
                    : pdAlert.getBody().startsWith("{")
                            ? this.objectMapper
                                    .readValue(
                                            pdAlert.getBody(),
                                            this.objectMapper.getTypeFactory().constructMapLikeType(HashMap.class,
                                                    String.class, Object.class))
                            : Map.of("details", pdAlert.getBody());
        } catch (JsonProcessingException e) {
            log.error("[{}] Unable to parse the body as json: {}", pdAlert.getId() , pdAlert.getBody(), e);
        }
        return DbPagerDutyAlert.builder()
                .pdId(pdAlert.getId())
                .pdServiceId(serviceId)
                .incidentId(incidentId)
                .summary(pdAlert.getSummary())
                .severity(pdAlert.getSeverity().toString()).status(pdAlert.getStatus()).summary(pdAlert.getSummary())
                .status(pdAlert.getStatus())
                .details(body)
                .createdAt(pdAlert.getCreatedAt().toInstant())
                .updatedAt(updatedAt)
                .lastStatusAt(pdAlert.getResolvedAt() != null ? pdAlert.getResolvedAt().toInstant() : updatedAt)
                .build();
    }

    private DbPagerDutyIncident pdIncidentToDbIncidentConverter(UUID pdServiceId, PagerDutyIncident pdIncident) {
        Set<DbPagerDutyStatus> statuses = pdIncident.getAcknowledgements().stream()
                .map(item -> DbPagerDutyStatus.builder()
                        .status("acknowledged")
                        .timestamp(Instant.parse(item.getAt()))
                        .user(DbPagerDutyUser.builder()
                                .pdId(item.getAcknowledger().getId())
                                .name(item.getAcknowledger().getName())
                                .build())
                        .build())
                .collect(Collectors.toSet());
        if ("resolved".equals(pdIncident.getStatus())) {
            statuses = Set.of(DbPagerDutyStatus.builder()
                    .status("resolved")
                    .timestamp(pdIncident.getLastStatusChangeAt().toInstant())
                    .user(DbPagerDutyUser.builder()
                            .pdId(pdIncident.getLastStatusChangeBy().getId())
                            .name(pdIncident.getLastStatusChangeBy().getName())
                            .build())
                    .build());
        }

        return DbPagerDutyIncident.builder()
                .pdId(pdIncident.getId())
                .pdServiceId(pdServiceId)
                .summary(pdIncident.getTitle())
                // .details(pdIncident.get)
                .priority(pdIncident.getPriority() != null ? pdIncident.getPriority().getSummary() : "")
                .urgency(pdIncident.getUrgency() != null ? pdIncident.getUrgency().toString() : "")
                .status(pdIncident.getStatus())
                .lastStatusAt(pdIncident.getLastStatusChangeAt().toInstant())
                .createdAt(pdIncident.getCreatedAt().toInstant())
                .updatedAt(Instant.ofEpochMilli(pdIncident.getUpdatedAt()))
                .statuses(statuses).build();
    }

    private DbPagerDutyService pdServiceToDbServiceConverter(UUID serviceId, Integer integrationId, PagerDutyService pdService) {
        return DbPagerDutyService.builder()
                .integrationId(integrationId)
                .createdAt(pdService.getCreatedAt())
                .updatedAt(Instant.ofEpochMilli(pdService.getUpdatedAt()))
                .name(pdService.getName())
                .pdId(pdService.getId())
                .serviceId(serviceId)
                .build();
    }

    private DbPagerDutyUser pdUserToDbUserConverter(PagerDutyUser pdUser) {
        return DbPagerDutyUser.builder()
                .pdId(pdUser.getId())
                .name(pdUser.getName())
                .email(pdUser.getEmail())
                .timeZone(pdUser.getTimeZone())
                .build();
    }

    @Cacheable(value = "services", key = "#customer+'_'+#serviceName", unless = "#result == null")
    private UUID getServiceId(String customer, String serviceName) throws SQLException {
        if (Strings.isBlank(serviceName)) {
            return null;
        }
        var service = Service.builder()
            .name(serviceName)
            .createdAt(Instant.now())
            .type(Service.Type.PAGERDUTY)
            .build();
        return UUID.fromString(services.insert(customer, service));
    }

    @Cacheable(value="pd_services", key = "#customer+'_'+#integrationId+'_'+#serviceId", unless = "#result.isEmpty()")
    private Optional<DbPagerDutyService> getPDService(String customer, int integrationId, String serviceId) throws SQLException {
        return pdServices.getByPagerDutyId(customer, integrationId, serviceId);
    }

    @Cacheable(value="pd_incidents", key = "#customer+'_'+#incident.id", unless = "#result.isEmpty()")
    private Optional<DbPagerDutyIncident> getIncident(String customer, PagerDutyIncident incident) throws SQLException {
        return incidentsService.getByPagerDutyId(customer, incident.getId());
    }

    /**
     * return results.
     *
     * @param jobs jobDTOs
     * @return
     */
    public static Stream<PagerDutyEntity> getPagerDutyEntitiesStream(final ObjectMapper objectMapper, final Storage storage, final Stream<JobDTO> jobs) {
        return getPagerDutyEntitiesStream(objectMapper, storage, jobs, supportedIngestionDataTypeInStringsForTempTable);
    }

    /**
     * return results.
     *
     * @param jobs jobDTOs
     * @return
     */
    public static Stream<PagerDutyEntity> getPagerDutyEntitiesStream(final ObjectMapper objectMapper, final Storage storage, final Stream<JobDTO> jobs, final Set<String> supportedIngestionDataTypeInStrings) {
        return jobs.<StorageResult>flatMap(dto ->
            objectMapper.<ListResponse<StorageResult>>convertValue(
                dto.getResult(),
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, StorageResult.class))
            .getRecords().stream()
            )
            .filter(record -> supportedIngestionDataTypeInStrings.contains(record.getStorageMetadata().getDataType()))
            .flatMap(record -> record.getRecords().stream()
                .flatMap(data -> {
                    try {
                        var dataType = record.getStorageMetadata().getDataType();
                        log.info("Parsing records from gcs results... {}", dataType);
                        Class<? extends PagerDutyEntity> clazz = PagerDutyIngestionDataType.fromString(dataType).getIngestionDataTypeClass();
                        log.debug("PD entity for '{}' is  '{}'", record.getStorageMetadata().getDataType(), clazz);
                        ListResponse<PagerDutyEntity> entity = objectMapper.readValue(
                            storage.readAllBytes(
                                BlobId.of(data.getBlobId().getBucket(), data.getBlobId().getName(), data.getBlobId().getGeneration())),
                                objectMapper.getTypeFactory().constructParametricType(
                                    ListResponse.class,
                                    clazz
                                )
                            );
                        log.info("Obtained {} {} ({}) recods", entity.getCount(), clazz.getSimpleName(), dataType);
                        return entity.getRecords().stream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
            )
            .filter(item -> item != null)
            .filter(result -> Strings.isNotBlank(result.getId()));
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.PAGERDUTY;
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