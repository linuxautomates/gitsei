package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCardWithIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import static io.levelops.commons.databases.models.filters.GithubCardFilter.createGithubCardFilter;

@RestController
@Log4j2
@RequestMapping("/v1/github")
@SuppressWarnings("unused")
public class GithubAggsController {

    private final GithubAggService aggService;
    private final AggCacheService aggCacheService;
    private final ObjectMapper mapper;
    private final OrgUnitHelper orgUnitHelper;

    public GithubAggsController(GithubAggService aggService, AggCacheService aggCacheService,
                                ObjectMapper mapper,final OrgUnitHelper orgUnitHelper) {
        this.aggService = aggService;
        this.aggCacheService = aggCacheService;
        this.mapper = mapper;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cards/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> cardResolutionTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws SQLException {
        var request = filter;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), filter);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/github/cards/..getTimeAcrossStagesReport()' for the request: {}", company, filter, e);
        }
        final var finalOuConfig = ouConfig;
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        final List<GithubCardFilter.DISTINCT> stackEnumList = getStacks(request);
        GithubCardFilter.DISTINCT across = GithubCardFilter.DISTINCT.fromString(request.getAcross());
        if (across == null) {
            across = GithubCardFilter.DISTINCT.project;
        }
        GithubCardFilter.GithubCardFilterBuilder githubCardFilterBuilder = createGithubCardFilter(request);
        GithubCardFilter cardFilter = githubCardFilterBuilder
                .across(across)
                .calculation(GithubCardFilter.CALCULATION.resolution_time)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(request.getAggInterval()), AGG_INTERVAL.day))
                .build();
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(
                        page,
                        pageSize,
                        aggService.stackedGroupBy(company, cardFilter, stackEnumList, finalOuConfig))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cards/stage_times_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTimeAcrossStagesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws SQLException {
        var request = filter;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), filter);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/github/cards/..getTimeAcrossStagesReport()' for the request: {}", company, filter, e);
        }
        final var finalOuConfig = ouConfig;
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        GithubCardFilter.DISTINCT across = GithubCardFilter.DISTINCT.fromString(request.getAcross());
        if (across == null) {
            across = GithubCardFilter.DISTINCT.column;
        }
        final List<GithubCardFilter.DISTINCT> stackEnumList = getStacks(request);
        GithubCardFilter.GithubCardFilterBuilder githubCardFilterBuilder = createGithubCardFilter(request);
        GithubCardFilter cardFilter = githubCardFilterBuilder
                .across(across)
                .calculation(GithubCardFilter.CALCULATION.stage_times_report)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(request.getAggInterval()), AGG_INTERVAL.day))
                .build();
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(
                        page,
                        pageSize,
                        aggService.stackedGroupBy(company, cardFilter, stackEnumList, finalOuConfig))));
    }

    @NotNull
    private List<GithubCardFilter.DISTINCT> getStacks(@RequestBody DefaultListRequest filter) {
        final List<GithubCardFilter.DISTINCT> stackEnumList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            stackEnumList.addAll(filter.getStacks().stream()
                    .map(GithubCardFilter.DISTINCT::fromString)
                    .collect(Collectors.toList()));
        }
        return stackEnumList;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cards/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbGithubProjectCardWithIssue>>> projectCardsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        var request = filter;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), filter);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/github/cards/..getTimeAcrossStagesReport()' for the request: {}", company, filter, e);
        }
        final var finalOuConfig = ouConfig;
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        final var sort = request.getSort();
        GithubCardFilter cardFilter = createGithubCardFilter(request).build();
        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of()));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                PaginatedResponse.of(page,
                        pageSize,
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/github/cards/list_" + page + "_" + pageSize + "_" +
                                        sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                cardFilter.generateCacheHash(), cardFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> aggService.list(company,
                                        cardFilter,
                                        finalOuConfig,
                                        sorting,
                                        page,
                                        pageSize)))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cards/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCardValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            var request = filter;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), filter);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/github/cards/..getTimeAcrossStagesReport()' for the request: {}", company, filter, e);
            }
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var sort = request.getSort();
            if (CollectionUtils.isEmpty(request.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : request.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                GithubCardFilter cardFilter = createGithubCardFilter(request)
                        .across(GithubCardFilter.DISTINCT.fromString(value))
                        .calculation(GithubCardFilter.CALCULATION.count)
                        .build();
                map.put(value,
                        AggCacheUtils.cacheOrCall(disableCache, company, "/github/cards/values",
                                cardFilter.generateCacheHash(), cardFilter.getIntegrationIds(), mapper, aggCacheService,
                                () -> aggService.groupByAndCalculateCards(company, cardFilter, page, pageSize)).getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

}
