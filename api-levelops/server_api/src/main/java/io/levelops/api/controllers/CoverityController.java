package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.coverity.DbCoverityDefect;
import io.levelops.commons.databases.models.database.coverity.DbCoveritySnapshot;
import io.levelops.commons.databases.models.database.coverity.DbCoverityStream;
import io.levelops.commons.databases.models.filters.CoverityDefectFilter;
import io.levelops.commons.databases.models.filters.CoveritySnapshotFilter;
import io.levelops.commons.databases.models.filters.CoverityStreamFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CoverityDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
@RequestMapping("/v1/coverity")
public class CoverityController {

    private final CoverityDatabaseService coverityDatabaseService;
    private final AggCacheService aggCacheService;
    private final ObjectMapper mapper;

    @Autowired
    public CoverityController(CoverityDatabaseService coverityDatabaseService,
                              AggCacheService aggCacheService,
                              ObjectMapper mapper) {
        this.coverityDatabaseService = coverityDatabaseService;
        this.aggCacheService = aggCacheService;
        this.mapper = mapper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/streams/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbCoverityStream>>> streamsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        CoverityStreamFilter coverityStreamFilter = CoverityStreamFilter.fromDefaultListRequest(filter, null, null);
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(filter.getPage(),
                        filter.getPageSize(),
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/coverity/streams/list_" + filter.getPage() + "_" + filter.getPageSize() + "_" + sort,
                                coverityStreamFilter.generateCacheHash(), coverityStreamFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> coverityDatabaseService.listStreams(company,
                                        coverityStreamFilter,
                                        sort,
                                        filter.getPage(),
                                        filter.getPageSize())))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/defects/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbCoverityDefect>>> defectsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws SQLException, BadRequestException {
        CoverityDefectFilter coverityDefectFilter = CoverityDefectFilter.fromDefaultListRequest(filter, null, null, null);
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(filter.getPage(),
                        filter.getPageSize(),
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/coverity/defects/list_" + filter.getPage() + "_" + filter.getPageSize() + "_" + sort,
                                coverityDefectFilter.generateCacheHash(), coverityDefectFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> coverityDatabaseService.list(company,
                                        coverityDefectFilter,
                                        sort,
                                        filter.getPage(),
                                        filter.getPageSize())))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/snapshots/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbCoveritySnapshot>>> snapshotsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        CoveritySnapshotFilter snapshotFilter = CoveritySnapshotFilter.fromDefaultListRequest(filter, null, null);
        Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(filter.getPage(),
                        filter.getPageSize(),
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/coverity/snapshots/list_" + filter.getPage() + "_" + filter.getPageSize() + "_" + sort,
                                snapshotFilter.generateCacheHash(), snapshotFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> coverityDatabaseService.listSnapshots(company,
                                        snapshotFilter,
                                        sort,
                                        filter.getPage(),
                                        filter.getPageSize())))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/defects/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getDefectsValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : filter.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                CoverityDefectFilter coverityDefectFilter = CoverityDefectFilter.fromDefaultListRequest(filter, CoverityDefectFilter.DISTINCT.fromString(value),
                        CoverityDefectFilter.CALCULATION.count, filter.getAggInterval());
                map.put(value,
                        AggCacheUtils.cacheOrCall(disableCache, company, "/coverity/defects/values",
                                coverityDefectFilter.generateCacheHash(), coverityDefectFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> coverityDatabaseService.groupByAndCalculateDefectsCount(company, coverityDefectFilter.toBuilder()
                                        .build(), Map.of(value, SortingOrder.ASC), true))
                                .getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/snapshots/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getSnapshotsValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : filter.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                CoveritySnapshotFilter snapshotFilter = CoveritySnapshotFilter.fromDefaultListRequest(filter, CoveritySnapshotFilter.DISTINCT.fromString(value),
                        CoveritySnapshotFilter.CALCULATION.count);
                map.put(value,
                        AggCacheUtils.cacheOrCall(disableCache, company, "/coverity/snapshots/values",
                                snapshotFilter.generateCacheHash(), snapshotFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> coverityDatabaseService.groupByAndCalculateSnapshotsCount(company, snapshotFilter.toBuilder()
                                        .build(), Map.of(value, SortingOrder.ASC)))
                                .getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/snapshots/analysis_time_report", produces = "application/json")
    public ResponseEntity<PaginatedResponse<DbAggregationResult>> getAnalysisTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        try {
            CoveritySnapshotFilter.DISTINCT across = CoveritySnapshotFilter.DISTINCT.fromString(filter.getAcross());
            CoveritySnapshotFilter snapshotFilter = CoveritySnapshotFilter.fromDefaultListRequest(filter, across, CoveritySnapshotFilter.CALCULATION.analysis_time);
            Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
            String stackMisc = "";
            final List<CoveritySnapshotFilter.DISTINCT> stackEnumList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(filter.getStacks())) {
                stackEnumList.addAll(filter.getStacks().stream()
                        .map(CoveritySnapshotFilter.DISTINCT::fromString)
                        .collect(Collectors.toList()));
                stackMisc = stackEnumList.stream().map(Enum::toString).sorted().collect(Collectors.joining(","));
            }
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/coverity/snapshots/analysis_time_" + stackMisc + "_" + sort,
                                    snapshotFilter.generateCacheHash(), snapshotFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> coverityDatabaseService.snapshotsStackedGroupBy(company, snapshotFilter, stackEnumList,
                                            sort)).getRecords()));
        } catch (Exception e) {
            log.info(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/defects/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDefectsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> handleDefectsQuery(disableCache, company, CoverityDefectFilter.CALCULATION.count, filter));
    }

    private ResponseEntity<PaginatedResponse<DbAggregationResult>> handleDefectsQuery(
            Boolean disableCache,
            String company,
            CoverityDefectFilter.CALCULATION calculation,
            DefaultListRequest filter) {
        try {
            CoverityDefectFilter.DISTINCT across = filter.getAcross() == null ? CoverityDefectFilter.DISTINCT.last_detected :
                    CoverityDefectFilter.DISTINCT.fromString(filter.getAcross());
            final List<CoverityDefectFilter.DISTINCT> stackEnumList = new ArrayList<>();
            Map<String, SortingOrder> sort = SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of()));
            String stackMisc = "";
            if (CollectionUtils.isNotEmpty(filter.getStacks())) {
                stackEnumList.addAll(filter.getStacks().stream()
                        .map(CoverityDefectFilter.DISTINCT::fromString)
                        .collect(Collectors.toList()));
                stackMisc = stackEnumList.stream().map(Enum::toString).sorted().collect(Collectors.joining(","));
            }
            CoverityDefectFilter defectFilter = CoverityDefectFilter.fromDefaultListRequest(filter, across, calculation, filter.getAggInterval());
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/coverity/defects/report_" + stackMisc + "_" + sort,
                                    defectFilter.generateCacheHash(), defectFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> coverityDatabaseService.defectsStackedGroupBy(company, defectFilter, stackEnumList,
                                            sort)).getRecords()));
        } catch (Exception e) {
            log.info(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
