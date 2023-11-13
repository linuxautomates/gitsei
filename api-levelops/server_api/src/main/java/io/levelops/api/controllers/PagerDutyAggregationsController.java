package io.levelops.api.controllers;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.pagerduty.DbPDIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPdAlert;
import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyAlertsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyIncidentsDatabaseService;
import io.levelops.commons.databases.services.pagerduty.PagerDutyServicesDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
@RestController
@RequestMapping("/v1/pagerduty/aggregations")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@SuppressWarnings("unused")
public class PagerDutyAggregationsController {

    private final CiCdJobsDatabaseService jobsService;
    private final PagerDutyIncidentsDatabaseService incidentsService;
    private final PagerDutyAlertsDatabaseService alertsDatabaseService;
    private final PagerDutyServicesDatabaseService pdServices;
    private final UserService usersService;
    private final OrgUnitHelper orgUnitHelper;
    private final ObjectMapper mapper;
    private final AggCacheService aggCacheService;

    @Autowired
    public PagerDutyAggregationsController(final CiCdJobsDatabaseService jobsService,
                                           final UserService usersService,
                                           final PagerDutyIncidentsDatabaseService incidentsService,
                                           final PagerDutyAlertsDatabaseService alertsDatabaseService,
                                           final PagerDutyServicesDatabaseService pdServices,
                                           ObjectMapper objectMapper,
                                           final OrgUnitHelper orgUnitHelper,
                                           AggCacheService aggCacheService) {
        this.jobsService = jobsService;
        this.usersService = usersService;
        this.incidentsService = incidentsService;
        this.pdServices = pdServices;
        this.alertsDatabaseService = alertsDatabaseService;
        this.orgUnitHelper = orgUnitHelper;
        this.mapper = objectMapper;
        this.aggCacheService = aggCacheService;
    }
    
    @PostMapping(path = "/values", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> getValues(
        @SessionAttribute("company") String company,
        @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<Map<String, Object>> records = new ArrayList<>();
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var fields = request.getFields();
            fields.forEach(field -> {
                DbListResponse<Object> values = null;
                try {
                    values = incidentsService.getValues(company, field, QueryFilter.fromRequestFilters(request.getFilter()),
                            request.getPage(), request.getPageSize(), orgProductIdsSet);
                } catch (SQLException e) {
                    log.error("Error while getting values ...{0}" + e.getMessage(), e);
                }
                records.add(Map.of(field, values.getRecords()));
            });
            return ResponseEntity.ok(Map.of("records", records));
        });
    }

    @PostMapping(path = "/release_incidents", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<Object, Object>>> getReleaseIncidents(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var jobIds = request.<String>getFilterValueAsList("cicd_job_ids")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "The filter 'cicd_job_ids' is mandatory"));

            List<Map<String, Object>> records = new ArrayList<>();
            if ("levelops".equalsIgnoreCase(company) || "foo".equalsIgnoreCase(company)){
                jobIds.forEach(jobId -> {
                    Optional<CICDJob> optionalJob = null;
                    try {
                        optionalJob = jobsService.get(company, jobIds.get(0));
                    }
                    catch(SQLException e) {
                        return;
                    }
                    if (optionalJob.isEmpty()) {
                        return;
                    }

                    var job = optionalJob.get();
                    if (job.getJobName().equals("Build-Runbook-API")) {
                        records.add(Map.of(
                            "from", 1603168224,
                            "to", 1603172344,
                            "incidents_count", 9,
                            "alerts_count", 1
                        ));
                        records.add(Map.of(
                            "from", 1603166114,
                            "to", 1603168224,
                            "incidents_count", 6,
                            "alerts_count", 5
                        ));
                        records.add(Map.of(
                            "from", 1603156888,
                            "to", 1603166114,
                            "incidents_count", 2,
                            "alerts_count", 1
                        ));
                        records.add(Map.of(
                            "from", 1603156588,
                            "to", 1603156888,
                            "incidents_count", 3,
                            "alerts_count", 3
                        ));
                        records.add(Map.of(
                            "from", 1602890080,
                            "to", 1603156588,
                            "incidents_count", 6,
                            "alerts_count", 6
                        ));
                    }
                    if (job.getJobName().equals("Build-Server-API")) {
                        records.add(Map.of(
                            "from", 1603249334,
                            "to", 1603250976,
                            "incidents_count", 3,
                            "alerts_count", 2
                        ));
                        records.add(Map.of(
                            "from", 1603250976,
                            "to", 1603251524,
                            "incidents_count", 4,
                            "alerts_count", 2
                        ));
                        records.add(Map.of(
                            "from", 1603251524,
                            "to", 1603252385,
                            "incidents_count", 2,
                            "alerts_count", 1
                        ));
                        records.add(Map.of(
                            "from", 1603252385,
                            "to", 1603253763,
                            "incidents_count", 0,
                            "alerts_count", 0
                        ));
                        records.add(Map.of(
                            "from", 1603253763,
                            "to", 1603254431,
                            "incidents_count", 1,
                            "alerts_count", 1
                        ));
                        records.add(Map.of(
                            "from", 1603254431,
                            "to", 1603258001,
                            "incidents_count", 0,
                            "alerts_count", 0
                        ));
                        records.add(Map.of(
                            "from", 1603258001,
                            "to", 1603259546,
                            "incidents_count", 3,
                            "alerts_count", 3
                        ));
                        records.add(Map.of(
                            "from", 1603259546,
                            "to", 1603274682,
                            "incidents_count", 0,
                            "alerts_count", 0
                        ));
                    }
                });
            }
            return ResponseEntity.ok(Map.of(
                "from", request.getFilterValue("from", Long.class).orElse(TimeUnit.DAYS.toSeconds(30)),
                "to", request.getFilterValue("to", Long.class).orElse(Instant.now().getEpochSecond()),
                "records", records
            ));
        });
    }

    @PostMapping(path = "/incidents_rate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<Object, Object>>> getIncidentsTrend(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            String pdServiceId = null;
            var from = request.getFilterValue("from", Long.class).orElse(Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond());
            var to = request.getFilterValue("to", Long.class).orElse(Instant.now().getEpochSecond());
            if(CollectionUtils.isEmpty(orgProductIdsSet)) {
                pdServiceId = request.getFilterValue("pd_service_id", String.class)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "The filter 'pd_service_id' is mandatory"));
            }
            var filters = QueryFilter.fromRequestFilters(request.getFilter()).toBuilder()
                .strictMatch("pd_service_id", StringUtils.isNullOrEmpty(pdServiceId) ? "" : UUID.fromString(pdServiceId))
                .strictMatch("from_created", Instant.now().minus(31, ChronoUnit.DAYS).getEpochSecond())
                .strictMatch("priority", request.getFilter().getOrDefault("incident_priority", List.of()))
                .strictMatch("urgency", request.getFilter().getOrDefault("incident_urgency", List.of()))
                .strictMatch("severity", request.getFilter().getOrDefault("alert_severity", List.of()))
                // .strictMatch("to_created", null) // no filter will do until the current date
                .build();
            var results = incidentsService.getTrend(company, filters, 0, 30, orgProductIdsSet);
            return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "records", results.getRecords()
            ));
        });
    }

    @PostMapping(path = "/ack_trend", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<Object, Object>>> getAcknowledgementTrend(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var from = request.getFilterValue("from", Long.class).orElse(TimeUnit.DAYS.toSeconds(30));
            var to = request.getFilterValue("to", Long.class).orElse(Instant.now().getEpochSecond());
            var filters = QueryFilter.fromRequestFilters(request.getFilter()).toBuilder()
                .strictMatch("priority", request.getFilter().getOrDefault("incident_priority", List.of()))
                .strictMatch("urgency", request.getFilter().getOrDefault("incident_urgency", List.of()))
                .strictMatch("severity", request.getFilter().getOrDefault("alert_severity", List.of()))
                // .strictMatch("to_created", null) // no filter will do until the current date
                .build();
            var records = incidentsService.getAckTrend(company, filters, orgProductIdsSet,request.getPage(), request.getPageSize());
            return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "records", records.getRecords()
            ));
        });
    }

    @PostMapping(path = "/after_hours", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<Object, Object>>> getAfterHours(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var from = Long.valueOf(request.getFilterValue("from", Long.class).orElse(Instant.now().minus(7, ChronoUnit.DAYS).getEpochSecond())).intValue();
            var to = Long.valueOf(request.getFilterValue("to", Long.class).orElse(Instant.now().getEpochSecond())).intValue();
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            var filters = QueryFilter.fromRequestFilters(request.getFilter()).toBuilder()
                .strictMatch("priority", request.getFilter().getOrDefault("incident_priority", List.of()))
                .strictMatch("urgency", request.getFilter().getOrDefault("incident_urgency", List.of()))
                .strictMatch("severity", request.getFilter().getOrDefault("alert_severity", List.of()))
                // .strictMatch("to_created", null) // no filter will do until the current date
                .build();
            var records = incidentsService.getAfterHoursMinutes(company, from, to, filters,
                    orgProductIdsSet ,request.getPage(), request.getPageSize());

            return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "records", records.getRecords()
            ));
        
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolutionReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, PagerDutyFilter.CALCULATION.resolution_time, filter);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResponseReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, PagerDutyFilter.CALCULATION.response_time, filter);
    }


    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggResult(
            Boolean disableCache,
            String company,
            PagerDutyFilter.CALCULATION calc,
            DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(doGetAggResult(disableCache, company, calc, originalRequest));
        });
    }

    private PaginatedResponse<DbAggregationResult> doGetAggResult(
            Boolean disableCache,
            String company,
            PagerDutyFilter.CALCULATION calc,
            DefaultListRequest originalRequest) throws Exception {
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.PAGERDUTY, originalRequest);
        var request = ouConfig.getRequest();
        var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
        Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
        PagerDutyFilter.DISTINCT across = PagerDutyFilter.DISTINCT.fromString(request.getAcross());
        if (across == null) {
            across = PagerDutyFilter.DISTINCT.user_id;
        }
        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
        final List<PagerDutyFilter.DISTINCT> stackEnumList = new ArrayList<>();
        String stackMisc = "";
        if (CollectionUtils.isNotEmpty(request.getStacks())) {
            stackEnumList.addAll(request.getStacks().stream()
                    .map(PagerDutyFilter.DISTINCT::fromString)
                    .collect(Collectors.toList()));
            stackMisc = stackEnumList.stream().map(Enum::toString).sorted().collect(Collectors.joining(","));
        }
        final var finalOuConfig = ouConfig;
        List<DbAggregationResult> aggregationRecords;
        PagerDutyFilter pagerDutyFilter = PagerDutyFilter.fromDefaultListRequest(request, calc, across).build();
            aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/pagerduty/incidents" + stackMisc,
                    pagerDutyFilter.generateCacheRawString(), pagerDutyFilter.getIntegrationIds(), mapper, aggCacheService,
                    () -> incidentsService.stackedGroupBy(company, pagerDutyFilter, orgProductIdsSet, stackEnumList, finalOuConfig, sorting))
                    .getRecords();
        return PaginatedResponse.of(request.getPage(), request.getPageSize(), aggregationRecords);
        }

    @RequestMapping(method = RequestMethod.POST, value = "/incidents/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbPDIncident>>> incidentsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.PAGERDUTY, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in 'pd/incidents/list_' for the request: {}", company, originalRequest, e);
            }
            PagerDutyFilter pagerDutyFilter = PagerDutyFilter.fromDefaultListRequest(request, null, null)
                    .issueType("incident").build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache,
                                    company,
                                    "pd/incidents/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    pagerDutyFilter.generateCacheRawString() + finalOuConfig.hashCode(),
                                    pagerDutyFilter.getIntegrationIds(),
                                    mapper,
                                    aggCacheService,
                                    () -> incidentsService.list(company,
                                            pagerDutyFilter,
                                            sorting,
                                            finalOuConfig,
                                            orgProductIdsSet,
                                            page,
                                            pageSize))));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/alerts/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbPdAlert>>> alertsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.PAGERDUTY, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in 'pd/alerts/list' for the request: {}", company, originalRequest, e);
            }
            PagerDutyFilter pagerDutyFilter = PagerDutyFilter.fromDefaultListRequest(request, null, null)
                    .issueType("alert").build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            var orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache,
                                    company,
                                    "pd/alerts/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    pagerDutyFilter.generateCacheRawString() + finalOuConfig.hashCode(),
                                    pagerDutyFilter.getIntegrationIds(),
                                    mapper,
                                    aggCacheService,
                                    () -> alertsDatabaseService.list(company,
                                            pagerDutyFilter,
                                            sorting,
                                            orgProductIdsSet,
                                            page,
                                            pageSize,
                                            finalOuConfig))));
        });
    }

}
