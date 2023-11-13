package io.levelops.api.controllers.issue_mgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import io.jsonwebtoken.lang.Collections;
import io.levelops.api.services.IngestedAtCachingService;
import io.levelops.api.services.IssueMgmtSprintService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.api.utils.ForceSourceUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.utils.IssueMgmtUtil;
import io.levelops.commons.databases.utils.LatestIngestedAt;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.faceted_search.services.workitems.EsWorkItemsQueryService;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.api.controllers.issue_mgmt.WorkItemsController.DbReportResponse.ResponseType.LIST;
import static io.levelops.api.controllers.issue_mgmt.WorkItemsController.DbReportResponse.ResponseType.MAP;
import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController("issueMgmtWorkItemsController")
@Log4j2
@RequestMapping("/v1/issue_mgmt/workitems")
@SuppressWarnings("unused")
public class WorkItemsController {

    private final WorkItemsService workItemService;
    private final EsWorkItemsQueryService esWorkItemsQueryService;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final AggCacheService cacheService; // caching.
    private final ObjectMapper mapper;
    private final IntegrationService integrationService;
    private final IssueMgmtSprintService issueMgmtSprintService;
    private final OrgUnitHelper ouHelper;
    private final OrgUsersDatabaseService orgUsersDatabaseService;
    private final LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache;
    @Value("${ES_IM:}")
    private List<String> imCompanies;
    @Value("${ES_IM_list:}")
    private List<String> issueMgmtListReportCompanies;
    @Value("${ES_IM_values:}")
    private List<String> issueMgmtValuesReportCompanies;
    @Value("${ES_IM_custom_field_values:}")
    private List<String> issueMgmtCustomFieldValuesReportCompanies;
    @Value("${ES_IM_attribute_values:}")
    private List<String> issueMgmtAttributeValuesReportCompanies;
    @Value("${ES_IM_tickets_report:}")
    private List<String> issueMgmtTicketsReportCompanies;
    @Value("${ES_IM_response_time_report:}")
    private List<String> issueMgmtResponseTimeReportCompanies;
    @Value("${ES_IM_resolution_time_report:}")
    private List<String> issueMgmtResolutionTimeReportCompanies;
    @Value("${ES_IM_hops_report:}")
    private List<String> issueMgmtHopsReportCompanies;
    @Value("${ES_IM_bounce_report:}")
    private List<String> issueMgmtBounceReportCompanies;
    @Value("${ES_IM_age_report:}")
    private List<String> issueMgmtAgeReportCompanies;
    @Value("${ES_IM_story_point_report:}")
    private List<String> issueMgmtStoryPointReportCompanies;
    @Value("${ES_IM_effort_report:}")
    private List<String> issueMgmtEffortReportCompanies;
    @Value("${ES_IM_first_assignee_report:}")
    private List<String> issueMgmtFirstAssigneeReportCompanies;
    @Value("${ES_IM_assignee_allocation_report:}")
    List<String> issueMgmtAssigneeAllocationReportCompanies;
    @Value("${ES_IM_stage_times_report:}")
    private List<String> issueMgmtStageTimesReportCompanies;
    @Value("${ES_IM_stage_bounce_report:}")
    private List<String> issueMgmtStageBounceReportCompanies;
    @Value("${ES_IM_sprint_metrics_report:}")
    private List<String> issueMgmtSprintMetricsReportCompanies;
    private Set<String> esAllowedTenants; // ES_IM [ "broadcom,broadridge" ]
    private Map<String, Set<String>> esAllowedTenantsApis; // { resolution_time_report -> [ "broadcom,broadridge" ] }

    //DB config
    @Value("${DB_IM:}")
    private List<String> dbImCompanies;
    @Value("${DB_IM_list:}")
    private List<String> dbIssueMgmtListReportCompanies;
    @Value("${DB_IM_values:}")
    private List<String> dbIssueMgmtValuesReportCompanies;
    @Value("${DB_IM_custom_field_values:}")
    private List<String> dbIssueMgmtCustomFieldValuesReportCompanies;
    @Value("${DB_IM_attribute_values:}")
    private List<String> dbIssueMgmtAttributeValuesReportCompanies;
    @Value("${DB_IM_tickets_report:}")
    private List<String> dbIssueMgmtTicketsReportCompanies;
    @Value("${DB_IM_response_time_report:}")
    private List<String> dbIssueMgmtResponseTimeReportCompanies;
    @Value("${DB_IM_resolution_time_report:}")
    private List<String> dbIssueMgmtResolutionTimeReportCompanies;
    @Value("${DB_IM_hops_report:}")
    private List<String> dbIssueMgmtHopsReportCompanies;
    @Value("${DB_IM_bounce_report:}")
    private List<String> dbIssueMgmtBounceReportCompanies;
    @Value("${DB_IM_age_report:}")
    private List<String> dbIssueMgmtAgeReportCompanies;
    @Value("${DB_IM_story_point_report:}")
    private List<String> dbIssueMgmtStoryPointReportCompanies;
    @Value("${DB_IM_effort_report:}")
    private List<String> dbIssueMgmtEffortReportCompanies;
    @Value("${DB_IM_first_assignee_report:}")
    private List<String> dbIssueMgmtFirstAssigneeReportCompanies;
    @Value("${DB_IM_assignee_allocation_report:}")
    List<String> dbIssueMgmtAssigneeAllocationReportCompanies;
    @Value("${DB_IM_stage_times_report:}")
    private List<String> dbIssueMgmtStageTimesReportCompanies;
    @Value("${DB_IM_stage_bounce_report:}")
    private List<String> dbIssueMgmtStageBounceReportCompanies;
    @Value("${DB_IM_sprint_metrics_report:}")
    private List<String> dbIssueMgmtSprintMetricsReportCompanies;
    private Set<String> dbAllowedTenants;
    private Map<String, Set<String>> dbAllowedTenantsApis;


    public WorkItemsController(WorkItemsService workItemService,
                               EsWorkItemsQueryService esWorkItemsQueryService,
                               IssueMgmtSprintService issueMgmtSprintService,
                               TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService,
                               IntegrationService integrationService,
                               IngestedAtCachingService ingestedAtCachingService,
                               AggCacheService cacheService,
                               ObjectMapper mapper,
                               final OrgUnitHelper ouHelper,
                               OrgUsersDatabaseService orgUsersDatabaseService) {
        this.workItemService = workItemService;
        this.esWorkItemsQueryService = esWorkItemsQueryService;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        this.cacheService = cacheService;
        this.mapper = mapper;
        this.issueMgmtSprintService = issueMgmtSprintService;
        this.integrationService = integrationService;
        this.ouHelper = ouHelper;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.ingestedAtCache = ingestedAtCachingService.getIngestedAtCache();
    }

    private static String sanitizeAlias(String column, List<String> characters) {
        String sanitizedColumn = column;
        for (String c : characters) {
            sanitizedColumn = sanitizedColumn.replaceAll(c, "_");
        }
        return sanitizedColumn;
    }

    @PostConstruct
    public void esConfig() {
        esAllowedTenants = new HashSet<>();
        esAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(imCompanies)) {
            esAllowedTenants.addAll(imCompanies);
        }
        if (CollectionUtils.isNotEmpty(issueMgmtListReportCompanies)) {
            esAllowedTenantsApis.put("list", Set.copyOf(issueMgmtListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtValuesReportCompanies)) {
            esAllowedTenantsApis.put("values", Set.copyOf(issueMgmtValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtCustomFieldValuesReportCompanies)) {
            esAllowedTenantsApis.put("custom_field/values", Set.copyOf(issueMgmtCustomFieldValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtAttributeValuesReportCompanies)) {
            esAllowedTenantsApis.put("attribute/values", Set.copyOf(issueMgmtAttributeValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtTicketsReportCompanies)) {
            esAllowedTenantsApis.put("tickets_report", Set.copyOf(issueMgmtTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtResponseTimeReportCompanies)) {
            esAllowedTenantsApis.put("response_time_report", Set.copyOf(issueMgmtResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtResolutionTimeReportCompanies)) {
            esAllowedTenantsApis.put("resolution_time_report", Set.copyOf(issueMgmtResolutionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtHopsReportCompanies)) {
            esAllowedTenantsApis.put("hops_report", Set.copyOf(issueMgmtHopsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtBounceReportCompanies)) {
            esAllowedTenantsApis.put("bounce_report", Set.copyOf(issueMgmtBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtAgeReportCompanies)) {
            esAllowedTenantsApis.put("age_report", Set.copyOf(issueMgmtAgeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtStoryPointReportCompanies)) {
            esAllowedTenantsApis.put("story_point_report", Set.copyOf(issueMgmtStoryPointReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtEffortReportCompanies)) {
            esAllowedTenantsApis.put("effort_report", Set.copyOf(issueMgmtEffortReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtFirstAssigneeReportCompanies)) {
            esAllowedTenantsApis.put("first_assignee_report", Set.copyOf(issueMgmtFirstAssigneeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtAssigneeAllocationReportCompanies)) {
            esAllowedTenantsApis.put("assignee_allocation_report", Set.copyOf(issueMgmtAssigneeAllocationReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtStageTimesReportCompanies)) {
            esAllowedTenantsApis.put("stage_times_report", Set.copyOf(issueMgmtStageTimesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtStageBounceReportCompanies)) {
            esAllowedTenantsApis.put("stage_bounce_report", Set.copyOf(issueMgmtStageBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(issueMgmtSprintMetricsReportCompanies)) {
            esAllowedTenantsApis.put("sprint_metrics_report", Set.copyOf(issueMgmtSprintMetricsReportCompanies));
        }
        dbConfig();
    }

    private void dbConfig() {
        dbAllowedTenants = new HashSet<>();
        dbAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(dbImCompanies)) {
            dbAllowedTenants.addAll(dbImCompanies);
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtListReportCompanies)) {
            dbAllowedTenantsApis.put("list", Set.copyOf(dbIssueMgmtListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtValuesReportCompanies)) {
            dbAllowedTenantsApis.put("values", Set.copyOf(dbIssueMgmtValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtCustomFieldValuesReportCompanies)) {
            dbAllowedTenantsApis.put("custom_field/values", Set.copyOf(dbIssueMgmtCustomFieldValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtAttributeValuesReportCompanies)) {
            dbAllowedTenantsApis.put("attribute/values", Set.copyOf(dbIssueMgmtAttributeValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtTicketsReportCompanies)) {
            dbAllowedTenantsApis.put("tickets_report", Set.copyOf(dbIssueMgmtTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtResponseTimeReportCompanies)) {
            dbAllowedTenantsApis.put("response_time_report", Set.copyOf(dbIssueMgmtResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtResolutionTimeReportCompanies)) {
            dbAllowedTenantsApis.put("resolution_time_report", Set.copyOf(dbIssueMgmtResolutionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtHopsReportCompanies)) {
            dbAllowedTenantsApis.put("hops_report", Set.copyOf(dbIssueMgmtHopsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtBounceReportCompanies)) {
            dbAllowedTenantsApis.put("bounce_report", Set.copyOf(dbIssueMgmtBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtAgeReportCompanies)) {
            dbAllowedTenantsApis.put("age_report", Set.copyOf(dbIssueMgmtAgeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtStoryPointReportCompanies)) {
            dbAllowedTenantsApis.put("story_point_report", Set.copyOf(dbIssueMgmtStoryPointReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtEffortReportCompanies)) {
            dbAllowedTenantsApis.put("effort_report", Set.copyOf(dbIssueMgmtEffortReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtFirstAssigneeReportCompanies)) {
            dbAllowedTenantsApis.put("first_assignee_report", Set.copyOf(dbIssueMgmtFirstAssigneeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtAssigneeAllocationReportCompanies)) {
            dbAllowedTenantsApis.put("assignee_allocation_report", Set.copyOf(dbIssueMgmtAssigneeAllocationReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtStageTimesReportCompanies)) {
            dbAllowedTenantsApis.put("stage_times_report", Set.copyOf(dbIssueMgmtStageTimesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtStageBounceReportCompanies)) {
            dbAllowedTenantsApis.put("stage_bounce_report", Set.copyOf(dbIssueMgmtStageBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbIssueMgmtSprintMetricsReportCompanies)) {
            dbAllowedTenantsApis.put("sprint_metrics_report", Set.copyOf(dbIssueMgmtSprintMetricsReportCompanies));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{workItemId}", produces = "application/json")
    public DeferredResult<ResponseEntity<DbWorkItem>> getWorkItem(
            @SessionAttribute(name = "company") String company,
            @PathVariable String workItemId, @RequestParam String integrationId) {
        log.info("getListOfWorkItems: API being hit");
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(
                        workItemService.get(company, integrationId, workItemId)));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbWorkItem>>> getListOfWorkItemsWithFilters(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company, @RequestBody DefaultListRequest originalRequest) {
        log.info("getListOfWorkItems: API being hit");
        boolean useEs = isUseEs(company, "list", forceSource);
        return SpringUtils.deferResponse(() -> {
                    // OU stuff
                    var request = originalRequest;
                    OUConfiguration ouConfig = null;
                    try {
                        ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                        request = ouConfig.getRequest();
                    } catch (SQLException e) {
                        log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/list' for the request: {}", company, originalRequest, e);
                    }
                    String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
                    WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(request.getAcross()), null);
                    WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
                    LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
                    WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                            .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                    workItemsTempFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService)).build();
                    boolean needVelocityStagesFilter = false;
                    if (request.getFilterValue("velocity_config_id", String.class).isPresent()) {
                        needVelocityStagesFilter = true;
                    }
                    boolean finalUseEs = !needVelocityStagesFilter && useEs && !isTicketCategorySpecified(workItemsFilter, null);
                    WorkItemsFilter finalWorkItemsFilter = workItemsFilter.toBuilder()
                            .ingestedAt(getIngestedAt(workItemsTempFilter, latestIngestedAt, finalUseEs))
                            .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                            .build();
                    var page = request.getPage();
                    var pageSize = request.getPageSize();
                    final var finalOuConfig = ouConfig;
                    return ResponseEntity.ok().body(PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/workItems/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_list_" + sortHash,
                                    workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + finalOuConfig.hashCode(), workItemsFilter.getIntegrationIds(), mapper, cacheService,
                                    () -> {
                                        if (finalUseEs) {
                                            return esWorkItemsQueryService.getWorkItemsList(company, finalWorkItemsFilter, sprintFilter, finalOuConfig, page, pageSize);
                                        } else {
                                            return workItemService.listByFilter(company, finalWorkItemsFilter, sprintFilter, finalOuConfig, page, pageSize, true);
                                        }
                                    })));
                }
        );
    }

    @FunctionalInterface
    protected interface DbReportGenerator {

        // TODO it would be nice to refactor commons to return a standard DbReportGenerator instead

        DbListResponse<DbAggregationResult> get(String company,
                                                WorkItemsFilter filter,
                                                WorkItemsTimelineFilter timelineFilter,
                                                WorkItemsMilestoneFilter milestoneFilter,
                                                WorkItemsFilter.DISTINCT stack,
                                                Boolean valuesOnly,
                                                final OUConfiguration ouConfig,
                                                int page, int pageSize) throws Exception;

        static DbReportGenerator fromNonPaginatedCall(NonPaginatedDbReportGenerator nonPaginatedDbReportGenerator) {
            return (String company, WorkItemsFilter filter,
                    WorkItemsTimelineFilter timelineFilter,
                    WorkItemsMilestoneFilter milestoneFilter,
                    WorkItemsFilter.DISTINCT stack,
                    Boolean valuesOnly, final OUConfiguration ouConfig, int page, int pageSize
            ) -> nonPaginatedDbReportGenerator.get(company, filter, milestoneFilter, stack, valuesOnly, ouConfig);
        }

    }

    @FunctionalInterface
    protected interface NonPaginatedDbReportGenerator {
        DbListResponse<DbAggregationResult> get(String company, WorkItemsFilter filter,
                                                WorkItemsMilestoneFilter milestoneFilter,
                                                WorkItemsFilter.DISTINCT stack,
                                                Boolean valuesOnly,
                                                final OUConfiguration ouConfig) throws Exception;
    }

    protected static class DbReportResponse {

        // TODO do we really want this? it would be nice to return a standard schema for all aggs (the LIST one)
        PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>> map;
        PaginatedResponse<DbAggregationResult> list;

        @Getter
        ResponseType type;

        enum ResponseType {MAP, LIST}

        private DbReportResponse(PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>> map,
                                 PaginatedResponse<DbAggregationResult> list,
                                 ResponseType type) {
            this.map = map;
            this.list = list;
            this.type = type;
        }

        public PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>> asMap() {
            if (type != MAP) {
                throw new IllegalArgumentException("Response is not a map");
            }
            return map;
        }

        public PaginatedResponse<DbAggregationResult> asList() {
            if (type != ResponseType.LIST) {
                throw new IllegalArgumentException("Response is not a list");
            }
            return list;
        }

        public static DbReportResponse buildMap(PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>> map) {
            return new DbReportResponse(map, null, MAP);
        }

        public static DbReportResponse buildList(PaginatedResponse<DbAggregationResult> list) {
            return new DbReportResponse(null, list, ResponseType.LIST);
        }
    }

    public DbReportResponse doGetReport(
            Boolean disableCache, String forceSource, String company, DefaultListRequest originalRequest,
            WorkItemsFilter.CALCULATION calculation, String reportName,
            DbReportGenerator dbReportGenerator, DbReportResponse.ResponseType responseType) throws Exception {
        // OU stuff
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/{}' for the request: {}", company, reportName, originalRequest, e);
        }
        boolean useEs = isUseEs(company, reportName, forceSource);
        String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();

        WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request,
                WorkItemsFilter.DISTINCT.fromString(request.getAcross()),
                calculation);
        LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
        WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                        workItemsTempFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService)).build();
        workItemsFilter = IssueMgmtUtil.resolveAcross(request, workItemsFilter);

        ImmutablePair<WorkItemsFilter, WorkItemsFilter.DISTINCT> immutablePair = IssueMgmtUtil.resolveStack(request, workItemsFilter);
        WorkItemsFilter.DISTINCT stack = immutablePair.getRight();
        workItemsFilter = immutablePair.getLeft();
        boolean finalUseEs = useEs && !isTicketCategorySpecified(workItemsFilter, stack);
        workItemsFilter = workItemsFilter.toBuilder()
                .ingestedAt(getIngestedAt(workItemsTempFilter, latestIngestedAt, finalUseEs))
                .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                .build();

        WorkItemsFilter finalWorkItemsFilter = workItemsFilter;
        final var finalOuConfig = ouConfig;
        WorkItemsTimelineFilter timelineFilter = getWorkItemTimelineFilter(request);
        WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

        int page = request.getPage();
        int pageSize = request.getPageSize();

        DbListResponse<DbAggregationResult> results = AggCacheUtils.cacheOrCall(
                disableCache,
                company,
                "/workItems/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_" + reportName + "_" + sortHash + (stack != null ? "_stack_" + stack : ""),
                workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + (finalOuConfig != null ? finalOuConfig.hashCode() : ""),
                workItemsFilter.getIntegrationIds(),
                mapper, cacheService,
                () -> {
                    if (finalUseEs) {
                        // TODO PROP-1800 pass timeline filter (only when required)
                        return esWorkItemsQueryService.getAggReport(company, finalWorkItemsFilter, sprintFilter, stack, calculation, finalOuConfig, false, page, pageSize);
                    } else {
                        return dbReportGenerator.get(company, finalWorkItemsFilter, timelineFilter, sprintFilter, stack, false, finalOuConfig, page, pageSize);
                    }
                });

        if (responseType == MAP) {
            List<Map<String, DbListResponse<DbAggregationResult>>> response = List.of(Map.of(request.getAcross(), results));
            return DbReportResponse.buildMap(PaginatedResponse.of(0, response.size(), response));
        } else {
            return DbReportResponse.buildList(PaginatedResponse.of(page, pageSize, results));
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/tickets_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getTicketsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws SQLException, ParseException {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.issue_count, "tickets_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsReport), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/age_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getAgeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.age, "age_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsAgeReport), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/assignee_allocation_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getAssigneeAllocationReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.assignees, "assignee_allocation_report",
                (company1, filter, timelineFilter, milestoneFilter, stack, valuesOnly, ouConfig, page, pageSize)
                        -> workItemService.getAssigneeAllocationReport(company1, filter, stack, valuesOnly, ouConfig, page, pageSize), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getResolutionTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.resolution_time, "resolution_time_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsResolutionTimeReport), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBouncesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.bounces, "bounce_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsBouncesReport), LIST).asList()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/hops_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHopsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.hops, "hops_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsHopsReport), LIST).asList()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/stage_times_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStageTimesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws SQLException, ParseException {

        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.stage_times_report, "stage_times_report",
                (company1, filter, timelineFilter, milestoneFilter, stack, valuesOnly, ouConfig, page, pageSize)
                        -> workItemService.getWorkItemsStageTimesReport(company1, filter, timelineFilter, milestoneFilter, stack, valuesOnly, ouConfig), LIST).asList()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getRepsonseTimesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {

        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.response_time, "response_time_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsReponseTimeReport), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/stage_bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStateBounceReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.stage_bounce_report, "stage_bounce_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsStageBounceReport), LIST).asList()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/first_assignee_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getFirstAssigneeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {

        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.assign_to_resolve, "first_assignee_report",
                (company1, filter, timelineFilter, milestoneFilter, stack, valuesOnly, ouConfig, page, pageSize)
                        -> workItemService.getFirstAssigneeReport(company1, filter, timelineFilter, milestoneFilter, valuesOnly, ouConfig), LIST).asList()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/effort_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getEffortReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.effort_report, "effort_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsReport), MAP).asMap()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/story_point_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getStoryPointsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetReport(disableCache, forceSource, company, originalRequest,
                WorkItemsFilter.CALCULATION.story_point_report, "story_point_report",
                DbReportGenerator.fromNonPaginatedCall(workItemService::getWorkItemsReport), MAP).asMap()));
    }


    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/sprint_metrics_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<IssueMgmtSprintService.SprintMetrics>>> getSprintMetricsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws SQLException, ParseException {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/sprint_metrics_report' for the request: {}", company, originalRequest, e);
            }
            HashMap<String, Object> tempFilter = new HashMap<>(MapUtils.emptyIfNull(request.getFilter()));
            Map<String, Object> filterExclude = new HashMap<>((Map<String, Object>) tempFilter.getOrDefault("exclude", Map.of()));
            List<Map<String, DbListResponse<DbAggregationResult>>> response = new ArrayList<>();
            Map<Object, Object> completedAt = request.getFilterValueAsMap("completed_at").orElse(Map.of());
            Map<Object, Object> startedAt = request.getFilterValueAsMap("started_at").orElse(Map.of());
            Map<Object, Object> plannedEndedAt = request.getFilterValueAsMap("planned_ended_at").isEmpty() ? (request.getFilterValueAsMap("ended_at").orElse(Map.of())) :  request.getFilterValueAsMap("planned_ended_at").get();
            String completedAtAfterStr = (String) completedAt.get("$gt");
            String completedAtBeforeStr = (String) completedAt.get("$lt");
            String startedAtAfterStr = (String) startedAt.get("$gt");
            String startedAtBeforeStr = (String) startedAt.get("$lt");
            String plannedEndedAtAfterStr = (String) plannedEndedAt.get("$gt");
            String plannedEndedAtBeforeStr = (String) plannedEndedAt.get("$lt");

            if ((completedAtAfterStr == null || completedAtBeforeStr == null) &&
                    (startedAtAfterStr == null || startedAtBeforeStr == null) &&
                    (plannedEndedAtAfterStr == null || plannedEndedAtBeforeStr == null)) {
                throw new BadRequestException("At least one of the date fiters is required");
            }

            Long completedAtAfter = completedAtAfterStr != null ? Long.valueOf(completedAtAfterStr) :  null;
            Long completedAtBefore = completedAtBeforeStr != null ? Long.valueOf(completedAtBeforeStr) : null;
            Long startedAtAfter = startedAtAfterStr != null ? Long.valueOf(startedAtAfterStr) : null;
            Long startedAtBefore = startedAtBeforeStr != null ? Long.valueOf(startedAtBeforeStr) : null;
            Long plannedEndedAtAfter = plannedEndedAtAfterStr != null ? Long.valueOf(plannedEndedAtAfterStr) : null;
            Long plannedEndedAtBefore = plannedEndedAtBeforeStr != null ? Long.valueOf(plannedEndedAtBeforeStr) : null;
            boolean useEs = isUseEs(company, "sprint_metrics_report", forceSource);
            // -- sprint report (name)
            // sanitizing full names till LFE-1405 is fixed:
            var wiFullNames = request.<String>getFilterValueAsList("workitem_sprint_full_names");
            List<String> fullNames = request.<String>getFilterValueAsList("sprint_report")
                    .or(() -> wiFullNames)
                    .map(list -> list.stream().map(name -> name.replace('/', '\\')).collect(Collectors.toList()))
                    .orElse(null);

            List<String> excludeSprintFullNames = (List<String>) filterExclude.getOrDefault("workitem_sprint_full_names", List.of());
            List<String> excludeFullNames = (List<String>) filterExclude.getOrDefault("sprint_report", excludeSprintFullNames);
            List<String> excludeSprintIds = (List<String>) filterExclude.getOrDefault("sprint_ids", List.of());
            List<String> excludeParentSprints = (List<String>) filterExclude.getOrDefault("parent_sprints", List.of());
            List<String> excludeProjectIds = (List<String>) filterExclude.getOrDefault("project_id", List.of());
            List<String> excludeIntegrationIds = (List<String>) filterExclude.getOrDefault("integration_ids", List.of());
            List<String> excludeSprintStates = (List<String>) filterExclude.getOrDefault("sprint_states", List.of());
            int creepBuffer = (Integer) tempFilter.getOrDefault("creep_buffer", 0);

            // -- include_issue_keys
            // by default, do NOT return the issue keys in the response.
            Boolean includeWorkitemIds = request.getFilterValue("include_workitem_ids", Boolean.class).orElse(false);

            // -- treat_outside_of_sprint_as_planned_and_delivered (false by default)
            boolean treatOutsideOfSprintAsPlannedAndDelivered = request.getFilterValue("treat_outside_of_sprint_as_planned_and_delivered", Boolean.class).orElse(false);

            // -- sanitize request and create issue filter
            tempFilter.remove("completed_at");
            tempFilter.remove("sprint_report");
            tempFilter.remove("workitem_sprint_full_names");
            tempFilter.remove("include_workitem_ids");

            int sprintCount = (Integer) tempFilter.getOrDefault("sprint_count", 0);
            tempFilter.remove("sprint_count");

            // sprintMapping table filter:
            tempFilter.put("sprint_mapping_ignorable_workitem_type", false); // remove ignorable issues (sub-tasks, etc.)

            int page = request.getPage();
            int pageSize = request.getPageSize();
            if (sprintCount > 0) {
                page = 0;
                pageSize = sprintCount;
            }
            DefaultListRequest sanitizedRequest = DefaultListRequest.builder()
                    .page(page) // TODO fix me - not being used yet
                    .pageSize(pageSize) // TODO remove? - only across limit is used
                    .across(WorkItemsFilter.DISTINCT.sprint_mapping.toString())
                    .sort(request.getSort())
                    .filter(tempFilter)
                    .acrossLimit(pageSize)
                    .build();

            WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(sanitizedRequest,
                    WorkItemsFilter.DISTINCT.fromString(sanitizedRequest.getAcross()),
                    WorkItemsFilter.CALCULATION.sprint_mapping);
            LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
            WorkItemsFilter.DISTINCT stack = CollectionUtils.isNotEmpty(sanitizedRequest.getStacks()) ?
                    WorkItemsFilter.DISTINCT.fromString(request.getStacks().get(0)) : null;
            WorkItemsSprintMappingFilter sprintMappingFilter = WorkItemsSprintMappingFilter
                    .fromDefaultListRequest(sanitizedRequest);
            boolean finalUseEs = useEs && !isTicketCategorySpecified(workItemsTempFilter, stack);
            workItemsTempFilter = workItemsTempFilter.toBuilder()
                    .ingestedAt(getIngestedAt(workItemsTempFilter, latestIngestedAt, finalUseEs))
                    .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                    .build();

            // milestone table filters - TODO create separate request for each table
            tempFilter.put("sprint_states", List.of("past")); // only completed sprints
            tempFilter.put("sprint_completed_at", completedAt);
            tempFilter.put("sprint_started_at", startedAt);
            tempFilter.put("sprint_ended_at", plannedEndedAt);
            tempFilter.put("sprint_full_names", fullNames);
            tempFilter.put("exclude", Map.of(
                    "sprint_full_names", excludeFullNames,
                    "sprint_ids", excludeSprintIds,
                    "parent_sprints", excludeParentSprints,
                    "project_id", excludeProjectIds,
                    "integration_ids", excludeIntegrationIds,
                    "sprint_states", excludeSprintStates));
            DefaultListRequest milestoneRequest = sanitizedRequest.toBuilder()
                    .filter(tempFilter)
                    .build();
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(milestoneRequest, "");

            // -- run agg
            DbListResponse<DbAggregationResult> sprintMetricsReport = null;

            if (finalUseEs) {
                sprintMetricsReport = esWorkItemsQueryService.getAggReport(company, workItemsTempFilter,
                        workItemsMilestoneFilter, stack, WorkItemsFilter.CALCULATION.sprint_mapping, ouConfig, false, page, pageSize);
            } else {
                sprintMetricsReport = workItemService.getSprintMetricsReport(company, workItemsTempFilter, workItemsMilestoneFilter, sprintMappingFilter, stack, false, ouConfig);
            }

            Integer totalCount = null;
            List<DbAggregationResult> sprintMetricsRecords = sprintMetricsReport.getRecords();
            if(BooleanUtils.isTrue(finalUseEs)){
                totalCount = sprintMetricsRecords.size();
                sprintMetricsRecords = sprintMetricsRecords.subList((page*pageSize),Math.min(sprintMetricsRecords.size(),(page*pageSize+pageSize)));
            }

            IssueMgmtSprintService.SprintMetricsSettings settings = IssueMgmtSprintService.SprintMetricsSettings.builder()
                    .includeWorkitemIds(includeWorkitemIds)
                    .creepBuffer(creepBuffer)
                    .treatOutsideOfSprintAsPlannedAndDelivered(treatOutsideOfSprintAsPlannedAndDelivered)
                    .build();
            List<IssueMgmtSprintService.SprintMetrics> sprintMetrics = issueMgmtSprintService.generateSprintMetrics(company, sprintMetricsRecords, settings);


            Boolean includeTotalCount = request.getFilterValue("include_total_count", Boolean.class).orElse(false);
            if (BooleanUtils.isTrue(includeTotalCount)) {
                if (sprintCount > 0) {
                    totalCount = CollectionUtils.size(sprintMetrics);
                } else if(BooleanUtils.isNotTrue(finalUseEs)){
                    if (Collections.isEmpty(sprintMetrics)) {
                        totalCount = 0;
                    } else {
                        Map<Pair<String, String>, List<IssueMgmtSprintService.SprintMetrics>> distinctSprintAgg =
                                sprintMetrics.stream().collect(Collectors.groupingBy(sprint -> Pair.of(sprint.getIntegrationId(), sprint.getSprintId())));
                        totalCount = distinctSprintAgg.size();
                    }
                }
            }
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), totalCount, sprintMetrics));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getValuesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws SQLException, ParseException {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/values' for the request: {}", company, originalRequest, e);
            }
            boolean useEs = isUseEs(company, "values", forceSource);
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            List<Map<String, DbListResponse<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : request.getFields()) {
                final WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request,
                                WorkItemsFilter.DISTINCT.fromString(field), WorkItemsFilter.CALCULATION.issue_count)
                        .toBuilder()
                        .sort(Map.of(field, SortingOrder.ASC))
                        .workItemCreatedRange(ImmutablePair.of(null, null))
                        .workItemUpdatedRange(ImmutablePair.of(null, null))
                        .build();
                WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
                LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
                boolean finalUseEs = useEs && !isTicketCategorySpecified(workItemsTempFilter, null);
                WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                        .ingestedAt(getIngestedAt(workItemsTempFilter, latestIngestedAt, finalUseEs))
                        .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                        .build();
                final var finalOuConfig = ouConfig;
                Integer page = request.getPage();
                Integer pageSize = request.getPageSize();
                response.add(Map.of(field, AggCacheUtils.cacheOrCall(disableCache, company,
                        "/workItems/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_list_" + sortHash,
                        workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + finalOuConfig.hashCode(), workItemsFilter.getIntegrationIds(), mapper, cacheService,
                        () -> {
                            if (finalUseEs) {
                                return esWorkItemsQueryService.getAggReport(company, workItemsTempFilter,
                                        sprintFilter, null, WorkItemsFilter.CALCULATION.issue_count, finalOuConfig, true, page, pageSize);
                            } else {
                                return workItemService.getWorkItemsReport(company, workItemsFilter, sprintFilter, null, true, finalOuConfig);
                            }
                        })));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/custom_field/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getCustomValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/custom_field/values' for the request: {}", company, originalRequest, e);
            }
            boolean useEs = isUseEs(company, "custom_field/values", forceSource);
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            List<Map<String, DbListResponse<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : request.getFields()) {
                final WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request,
                                WorkItemsFilter.DISTINCT.fromString(field), WorkItemsFilter.CALCULATION.issue_count)
                        .toBuilder()
                        .across(WorkItemsFilter.DISTINCT.custom_field)
                        .customAcross(field)
                        .sort(Map.of(WorkItemsFilter.DISTINCT.custom_field.toString() + "_" + sanitizeAlias(field, List.of("\\.", "-")), SortingOrder.ASC))
                        .workItemCreatedRange(ImmutablePair.of(null, null))
                        .workItemUpdatedRange(ImmutablePair.of(null, null))
                        .build();
                WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
                LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
                boolean finalUseEs = useEs && !isTicketCategorySpecified(workItemsTempFilter, null);
                WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                        .ingestedAt(getIngestedAt(workItemsTempFilter, latestIngestedAt, finalUseEs))
                        .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                        .build();
                final var finalOuConfig = ouConfig;
                Integer page = request.getPage();
                Integer pageSize = request.getPageSize();
                response.add(Map.of(field, AggCacheUtils.cacheOrCall(disableCache, company,
                        "/workItems/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_list_" + sortHash,
                        workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + finalOuConfig.hashCode(), workItemsFilter.getIntegrationIds(), mapper, cacheService,
                        () -> {
                            if (finalUseEs) {
                                return esWorkItemsQueryService.getAggReport(company, workItemsTempFilter,
                                        sprintFilter, null, WorkItemsFilter.CALCULATION.issue_count, finalOuConfig, true, page, pageSize);
                            } else {
                                return workItemService.getWorkItemsReport(company, workItemsFilter, sprintFilter, null, true, finalOuConfig);
                            }
                        })));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/attribute/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, DbListResponse<DbAggregationResult>>>>> getAttributeValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            boolean useEs = isUseEs(company, "attribute/values", forceSource);
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/attribute/values' for the request: {}", company, originalRequest, e);
            }
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            List<Map<String, DbListResponse<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : request.getFields()) {
                WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request,
                                WorkItemsFilter.DISTINCT.fromString(field), WorkItemsFilter.CALCULATION.issue_count)
                        .toBuilder()
                        .across(WorkItemsFilter.DISTINCT.attribute)
                        .attributeAcross(field)
                        .sort(Map.of(WorkItemsFilter.DISTINCT.attribute.toString() + "_" + field, SortingOrder.ASC))
                        .workItemCreatedRange(ImmutablePair.of(null, null))
                        .workItemUpdatedRange(ImmutablePair.of(null, null))
                        .build();
                WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
                LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsFilter, integrationService, ingestedAtCache);
                boolean finalUseEs = useEs && !isTicketCategorySpecified(workItemsFilter, null);
                workItemsFilter = workItemsFilter.toBuilder()
                        .ingestedAt(getIngestedAt(workItemsFilter, latestIngestedAt, finalUseEs))
                        .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                        .build();
                final var finalOuConfig = ouConfig;
                Integer page = request.getPage();
                Integer pageSize = request.getPageSize();
                WorkItemsFilter finalWorkItemsFilter = workItemsFilter;
                response.add(Map.of(field, AggCacheUtils.cacheOrCall(disableCache, company,
                        "/workItems/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_attribute_values_" + sortHash,
                        workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + finalOuConfig.hashCode(), workItemsFilter.getIntegrationIds(), mapper, cacheService,
                        () -> {
                            if (finalUseEs) {
                                return esWorkItemsQueryService.getAggReport(company, finalWorkItemsFilter,
                                        sprintFilter, null, WorkItemsFilter.CALCULATION.issue_count, finalOuConfig, true, page, pageSize);
                            } else {
                                return workItemService.getWorkItemsReport(company, finalWorkItemsFilter, sprintFilter, null, true, finalOuConfig);
                            }
                        })));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    protected static WorkItemsTimelineFilter getWorkItemTimelineFilter(DefaultListRequest filter) {
        return WorkItemsTimelineFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .fieldTypes(getListOrDefault(filter.getFilter(), "field_types"))
                .fieldValues(getListOrDefault(filter.getFilter(), "field_values"))
                .workItemIds(getListOrDefault(filter.getFilter(), "workitem_ids"))
                .build();
    }

    private Map<String, List<String>> getOuPeople(String company, OUConfiguration ouConfig) {
        Map<String, List<String>> ouUsersMap = new HashMap<>();
        if (OrgUnitHelper.isOuConfigActive(ouConfig)) {
            if (ouConfig.getAdoFields().contains("reporter") && OrgUnitHelper.doesOUConfigHaveWorkItemReporters(ouConfig)) {
                ouUsersMap.put("reporters", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS));
            }
            if (ouConfig.getAdoFields().contains("assignee") && OrgUnitHelper.doesOUConfigHaveWorkItemAssignees(ouConfig)) {
                ouUsersMap.put("assignees", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS));
            }
            if (ouConfig.getAdoFields().contains("first_assignee") && OrgUnitHelper.doesOUConfigHaveWorkItemFirstAssignees(ouConfig)) {
                ouUsersMap.put("first_assignees", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS));
            }
        }
        return ouUsersMap;
    }

    @NotNull
    private Boolean isUseEs(String company, String reportName, String forceSource) {
        Boolean forceSourceUseES = ForceSourceUtils.useES(forceSource);
        if (forceSourceUseES != null) {
            log.info("isUseEs forceSourceUseES={}", forceSourceUseES);
            return forceSourceUseES;
        }
        boolean isEsCompany = this.esAllowedTenants.contains(company) ||
                (this.esAllowedTenantsApis.containsKey(reportName) &&
                        this.esAllowedTenantsApis.get(reportName).contains(company));
        log.info("isUseEs isEsCompany={}", isEsCompany);

        boolean isDbCompany = this.dbAllowedTenants.contains(company) ||
                (this.dbAllowedTenantsApis.containsKey(reportName) &&
                        this.dbAllowedTenantsApis.get(reportName).contains(company));
        log.info("isUseEs isDBCompany={}", isDbCompany);

        //if report or company configured for both ES and DB - ignoring ES and making DB call
        if (isDbCompany) {
            return false;
        }

        return isEsCompany;
    }

    @NotNull
    private Boolean isTicketCategorySpecified(WorkItemsFilter filter, WorkItemsFilter.DISTINCT stack) {
        return filter.getAcross() == WorkItemsFilter.DISTINCT.ticket_category || stack == WorkItemsFilter.DISTINCT.ticket_category ||
                StringUtils.isNotEmpty(filter.getTicketCategorizationSchemeId()) ||
                CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters()) || CollectionUtils.isNotEmpty(filter.getTicketCategories());
    }

    private Long getIngestedAt(WorkItemsFilter workItemsFilter, LatestIngestedAt latestIngestedAt, boolean useEs) {
        return useEs && workItemsFilter.getIngestedAt() != null ?
                workItemsFilter.getIngestedAt() : latestIngestedAt.getLatestIngestedAt();
    }
}
