package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ZendeskQueryService;
import io.levelops.commons.databases.services.ZendeskTicketService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/zendesk_tickets")
@SuppressWarnings("unused")
public class ZendeskTicketsController {

    private final IntegrationService integService;
    private final ZendeskTicketService zendeskTicketService;
    private final IntegrationTrackingService integrationTrackingService;
    private final AggCacheService aggCacheService;
    private final ObjectMapper objectMapper;

    private final ZendeskQueryService zendeskQueryService;

    public ZendeskTicketsController(ZendeskTicketService zendeskTicketService,
                                    IntegrationService integrationService,
                                    IntegrationTrackingService integTService,
                                    AggCacheService aggCacheService,
                                    ObjectMapper objectMapper,
                                    ZendeskQueryService zendeskQueryService) {
        this.zendeskTicketService = zendeskTicketService;
        this.integrationTrackingService = integTService;
        this.integService = integrationService;
        this.aggCacheService = aggCacheService;
        this.objectMapper = objectMapper;
        this.zendeskQueryService = zendeskQueryService;
    }

    private Long getIngestedAt(String company, DefaultListRequest filter) throws SQLException {
        Integration integ = integService.listByFilter(company, null,
                List.of(IntegrationType.ZENDESK.toString()),
                null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(x -> NumberUtils.toInt(x)).collect(Collectors.toList()), List.of(),
                0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbZendeskTicket>>> getListOfTickets(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            ZendeskTicketsFilter zdFilter = getZendeskTicketsBuilderWithFilters(company, filter).build();
            String sortHash = Hashing.sha256().hashBytes(objectMapper.writeValueAsString(filter.getSort()).getBytes()).toString();

            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/zendesk_tickets/list_" + filter.getPage() + "_" + filter.getPageSize() + "_" + sortHash,
                            zdFilter.generateCacheHash(),
                            zdFilter.getIntegrationIds(), objectMapper, aggCacheService,
                            () -> zendeskQueryService.list(company,
                                    getZendeskTicketsBuilderWithFilters(company, filter).build(),
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
                                    filter.getPage(), filter.getPageSize()))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/custom_field/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCustomValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, zendeskTicketService.groupByAndCalculate(company,
                        getZendeskTicketsBuilderWithFilters(company, filter)
                                .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                                .DISTINCT(ZendeskTicketsFilter.DISTINCT.custom_field)
                                .customAcross(field)
                                .build()).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : filter.getFields()) {
                ZendeskTicketsFilter zdFilter = getZendeskTicketsBuilderWithFilters(company, filter)
                        .CALCULATION(ZendeskTicketsFilter.CALCULATION.ticket_count)
                        .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString(field))
                        .build();
                response.add(Map.of(field,
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/zendesk_tickets/values",
                                zdFilter.generateCacheHash(),
                                zdFilter.getIntegrationIds(), objectMapper, aggCacheService,
                                () -> zendeskTicketService.groupByAndCalculate(company, zdFilter)).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBounceReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.bounces);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/hops_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHopsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.hops);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResponseTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.response_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolutionTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.resolution_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/agent_wait_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAgentWaitTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.agent_wait_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/requester_wait_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getRequesterWaitTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.requester_wait_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/reopens_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getReopensReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.reopens);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/replies_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getRepliesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.replies);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/tickets_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.ticket_count);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/hygiene_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHygieneReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        if (CollectionUtils.isEmpty(getListOrDefault(MapUtils.emptyIfNull(filter.getFilter()), "hygiene_types")))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'hygiene_types' provided.");
        return getAggReport(disableCache, company, filter, ZendeskTicketsFilter.CALCULATION.ticket_count);
    }

    @NotNull
    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReport(
            Boolean disableCache, String company, DefaultListRequest filter, ZendeskTicketsFilter.CALCULATION output) {

        List<ZendeskTicketsFilter.DISTINCT> stacks = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(filter.getStacks()))
            stacks.addAll(filter.getStacks().stream()
                    .sorted()
                    .map(ZendeskTicketsFilter.DISTINCT::fromString)
                    .collect(Collectors.toList()));
        return SpringUtils.deferResponse(() -> {
            ZendeskTicketsFilter zdFilter = getZendeskTicketsBuilderWithFilters(company, filter)
                    .CALCULATION(output)
                    .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString(filter.getAcross()))
                    .build();
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/zendesk_tickets/aggs/"+stacks.stream().map(Enum::toString).collect(Collectors.joining(",")),
                                    zdFilter.generateCacheHash(),
                                    zdFilter.getIntegrationIds(), objectMapper, aggCacheService,
                                    () -> zendeskTicketService.stackedGroupBy(company,
                                            getZendeskTicketsBuilderWithFilters(company, filter)
                                                    .CALCULATION(output)
                                                    .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString(filter.getAcross()))
                                                    .build(), stacks))
                    ));
        });
    }

    @SuppressWarnings("unchecked")
    private ZendeskTicketsFilter.ZendeskTicketsFilterBuilder getZendeskTicketsBuilderWithFilters(
            String company,
            DefaultListRequest filter) throws SQLException {
        Map<String, String> createdAtRange = filter.getFilterValue("created_at", Map.class)
                .orElse(Map.of());
        final Map<String, Object> excludedFields = filter.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
        Long zdStart = createdAtRange.get("$gt") != null ? Long.valueOf(createdAtRange.get("$gt")) : null;
        Long zdEnd = createdAtRange.get("$lt") != null ? Long.valueOf(createdAtRange.get("$lt")) : null;
        return ZendeskTicketsFilter.builder()
                .ticketCreatedEnd(zdEnd)
                .ticketCreatedStart(zdStart)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .brands(getListOrDefault(filter.getFilter(), "brands"))
                .extraCriteria(getListOrDefault(filter.getFilter(), "hygiene_types")
                        .stream()
                        .map(String::valueOf)
                        .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .types(getListOrDefault(filter.getFilter(), "types"))
                .ingestedAt(getIngestedAt(company, filter))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .priorities(getListOrDefault(filter.getFilter(), "priorities"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .organizations(getListOrDefault(filter.getFilter(), "organizations"))
                .requesterEmails(getListOrDefault(filter.getFilter(), "requesters"))
                .submitterEmails(getListOrDefault(filter.getFilter(), "submitters"))
                .age(filter.<String, Object>getFilterValueAsMap("age").orElse(Map.of()))
                .assigneeEmails(getListOrDefault(filter.getFilter(), "assignees"))
                .customAcross((String) filter.getFilter().get("custom_across"))
                .customStacks(getListOrDefault(filter.getFilter(), "custom_stacks"))
                .customFields(filter.<String,List<String>>getFilterValueAsMap("custom_fields").orElse(Map.of()))
                .excludeCustomFields(MapUtils.emptyIfNull((Map<String, List<String>>) excludedFields.get("custom_fields")));
    }
}
