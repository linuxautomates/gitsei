package io.levelops.api.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.client.ACLClientFactory;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.PermissionCheckDTO;
import io.harness.authz.acl.model.Principal;
import io.harness.authz.acl.model.ResourceScope;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.JiraIssueApiService;
import io.levelops.api.services.JiraSprintMetricsService;
import io.levelops.api.services.JiraSprintMetricsService.SprintMetrics;
import io.levelops.api.services.JiraSprintMetricsService.SprintMetricsSettings;
import io.levelops.api.services.JiraSprintMetricsServiceLegacy;
import io.levelops.api.utils.ForceSourceUtils;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraPrioritySla;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraSprintDistMetric;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.jira.VelocityStageTimesReportPrecalculateWidgetService;
import io.levelops.commons.databases.services.jira.models.VelocityStageTimesReportSubType;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.ListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.business_alignment.es.services.BaJiraAggsESService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.faceted_search.services.workitems.EsJiraIssueQueryService;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getSortedDevStages;
import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getStageStatusesMap;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/jira_issues")
@SuppressWarnings("unused")
public class JiraIssuesController {
    private static final Integer LEGACY_SPRINT_METRICS_PAGE_SIZE_MAX = 25;
    private static final Integer LEGACY_SPRINT_METRICS_PAGE_SIZE_DEFAULT = 10;

    //This is a temporary 'abtest' type variable to be removed when
    //predictions code is verified.
    @Value("${PREDICTIONS_POSTPROCESS:FALSE}")
    private String predictionsPostProcess;

    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final JiraIssueService jiraIssueService;
    private final ConfigTableHelper configTableHelper;
    private final JiraFilterParser jiraFilterParser;
    private final JiraSprintMetricsServiceLegacy jiraSprintMetricsServiceLegacy;
    private final JiraSprintMetricsService jiraSprintMetricsService;
    private final JiraIssueApiService jiraIssueApiService;
    private final EsJiraIssueQueryService esJiraIssueQueryService;
    private final OrgUnitHelper orgUnitHelper;
    private final Executor dbValuesTaskExecutor;
    private final VelocityAggsService velocityAggsService;
    private final OrgUsersDatabaseService orgUsersDatabaseService;
    private final VelocityStageTimesReportPrecalculateWidgetService velocityStageTimesReportPrecalculateWidgetService;


    private Set<String> esAllowedTenants;
    private Map<String, Set<String>> esAllowedTenantsApis;
    private Set<String> dbAllowedTenants;
    private Map<String, Set<String>> dbAllowedTenantsApis;

    @Value("${ES_JIRA:}")
    List<String> jiraCompanies;
    @Value("${ES_JIRA_list:}")
    List<String> jiraListCompanies;
    @Value("${ES_JIRA_list_ba:}")
    List<String> jiraListBACompanies;
    @Value("${ES_JIRA_values:}")
    private List<String> jiraValuesReportCompanies;
    @Value("${ES_JIRA_custom_field_values:}")
    private List<String> jiraCustomFieldValuesReportCompanies;
    @Value("${ES_JIRA_tickets_report:}")
    List<String> jiraTicketsReportCompanies;
    @Value("${ES_JIRA_hygiene_report:}")
    List<String> jiraHygieneReportCompanies;
    @Value("${ES_JIRA_response_time_report:}")
    List<String> jiraResponseTimeReportCompanies;
    @Value("${ES_JIRA_resolution_time_report:}")
    List<String> jiraResolutionTimeReportCompanies;
    @Value("${ES_JIRA_hops_report:}")
    List<String> jiraHopsReportCompanies;
    @Value("${ES_JIRA_bounce_report:}")
    List<String> jiraBounceReportCompanies;
    @Value("${ES_JIRA_age_report:}")
    List<String> jiraAgeReportCompanies;
    @Value("${ES_JIRA_story_point_report:}")
    List<String> jiraStoryPointReportCompanies;
    @Value("${ES_JIRA_state_transition_time_report:}")
    List<String> jiraStateTransitionTimeReportCompanies;
    @Value("${ES_JIRA_first_assignee_report:}")
    List<String> jiraFirstAssigneeReportCompanies;
    @Value("${ES_JIRA_assignee_allocation_report:}")
    List<String> jiraAssigneeAllocationReportCompanies;
    @Value("${ES_JIRA_stage_times_report:}")
    List<String> jiraStageTimesReportCompanies;
    @Value("${ES_JIRA_stage_bounce_report:}")
    List<String> jiraStageBounceReportCompanies;
    @Value("${ES_JIRA_sprint_metrics_report:}")
    List<String> jiraSprintMetricsReportCompanies;

    //DB config
    @Value("${DB_JIRA:}")
    List<String> dbJiraCompanies;
    @Value("${DB_JIRA_list:}")
    List<String> dbJiraListCompanies;
    @Value("${DB_JIRA_list_ba:}")
    List<String> dbJiraListBACompanies;
    @Value("${DB_JIRA_tickets_report:}")
    List<String> dbJiraTicketsReportCompanies;
    @Value("${DB_JIRA_hygiene_report:}")
    List<String> dbJiraHygieneReportCompanies;
    @Value("${DB_JIRA_response_time_report:}")
    List<String> dbJiraResponseTimeReportCompanies;
    @Value("${DB_JIRA_resolution_time_report:}")
    List<String> dbJiraResolutionTimeReportCompanies;
    @Value("${DB_JIRA_hops_report:}")
    List<String> dbJiraHopsReportCompanies;
    @Value("${DB_JIRA_bounce_report:}")
    List<String> dbJiraBounceReportCompanies;
    @Value("${DB_JIRA_age_report:}")
    List<String> dbJiraAgeReportCompanies;
    @Value("${DB_JIRA_story_point_report:}")
    List<String> dbJiraStoryPointReportCompanies;
    @Value("${DB_JIRA_state_transition_time_report:}")
    List<String> dbJiraStateTransitionTimeReportCompanies;
    @Value("${DB_JIRA_first_assignee_report:}")
    List<String> dbJiraFirstAssigneeReportCompanies;
    @Value("${DB_JIRA_assignee_allocation_report:}")
    List<String> dbJiraAssigneeAllocationReportCompanies;
    @Value("${DB_JIRA_stage_times_report:}")
    List<String> dbJiraStageTimesReportCompanies;
    @Value("${DB_JIRA_stage_bounce_report:}")
    List<String> dbJiraStageBounceReportCompanies;
    @Value("${DB_JIRA_sprint_metrics_report:}")
    List<String> dbJiraSprintMetricsReportCompanies;

    @Value("${JIRA_MAX_PAGE_SIZE:10000}")
    private int maxPageSize;

    private final BaJiraAggsESService baJiraAggsESService;

   private ACLClientFactory aclClientFactory;

    @Autowired
    public JiraIssuesController(JiraIssueService issueService,
                                ObjectMapper objectMapper,
                                ConfigTableHelper configTableHelper,
                                JiraFilterParser jiraFilterParser,
                                JiraIssueApiService jiraIssueApiService,
                                AggCacheService cacheService,
                                JiraSprintMetricsServiceLegacy jiraSprintMetricsServiceLegacy,
                                JiraSprintMetricsService jiraSprintMetricsService,
                                final OrgUnitHelper orgUnitHelper,
                                EsJiraIssueQueryService esJiraIssueQueryService,
                                @Qualifier("dbValuesTaskExecutor") final Executor dbValuesTaskExecutor,
                                VelocityAggsService velocityAggsService,
                                OrgUsersDatabaseService orgUsersDatabaseService,
                                VelocityStageTimesReportPrecalculateWidgetService velocityStageTimesReportPrecalculateWidgetService,
                                BaJiraAggsESService baJiraAggsESService) {
        this.mapper = objectMapper;
        this.cacheService = cacheService;
        this.jiraIssueService = issueService;
        this.configTableHelper = configTableHelper;
        this.esJiraIssueQueryService = esJiraIssueQueryService;
        this.jiraFilterParser = jiraFilterParser;
        this.jiraSprintMetricsServiceLegacy = jiraSprintMetricsServiceLegacy;
        this.jiraSprintMetricsService = jiraSprintMetricsService;
        this.orgUnitHelper = orgUnitHelper;
        this.jiraIssueApiService = jiraIssueApiService;
        this.dbValuesTaskExecutor = dbValuesTaskExecutor;
        this.velocityAggsService = velocityAggsService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.velocityStageTimesReportPrecalculateWidgetService = velocityStageTimesReportPrecalculateWidgetService;
        this.baJiraAggsESService = baJiraAggsESService;
    }

    @PostConstruct
    public void esConfig() {
        this.esAllowedTenants = new HashSet<>();
        this.esAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(jiraCompanies)) {
            this.esAllowedTenants.addAll(jiraCompanies);
        }
        if (CollectionUtils.isNotEmpty(jiraListCompanies)) {
            this.esAllowedTenantsApis.put("list", Set.copyOf(jiraListCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraListBACompanies)) {
            this.esAllowedTenantsApis.put("list_ba", Set.copyOf(jiraListBACompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraValuesReportCompanies)) {
            this.esAllowedTenantsApis.put("values", Set.copyOf(jiraValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraCustomFieldValuesReportCompanies)) {
            this.esAllowedTenantsApis.put("custom_field/values", Set.copyOf(jiraCustomFieldValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraTicketsReportCompanies)) {
            this.esAllowedTenantsApis.put("tickets_report", Set.copyOf(jiraTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraHygieneReportCompanies)) {
            this.esAllowedTenantsApis.put("hygiene_report", Set.copyOf(jiraTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraResponseTimeReportCompanies)) {
            this.esAllowedTenantsApis.put("response_time_report", Set.copyOf(jiraResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraResolutionTimeReportCompanies)) {
            this.esAllowedTenantsApis.put("resolution_time_report", Set.copyOf(jiraResolutionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraHopsReportCompanies)) {
            this.esAllowedTenantsApis.put("hops_report", Set.copyOf(jiraHopsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraBounceReportCompanies)) {
            this.esAllowedTenantsApis.put("bounce_report", Set.copyOf(jiraBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraAgeReportCompanies)) {
            this.esAllowedTenantsApis.put("age_report", Set.copyOf(jiraAgeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraStoryPointReportCompanies)) {
            this.esAllowedTenantsApis.put("story_point_report", Set.copyOf(jiraStoryPointReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraStateTransitionTimeReportCompanies)) {
            this.esAllowedTenantsApis.put("state_transition_time_report", Set.copyOf(jiraStateTransitionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraFirstAssigneeReportCompanies)) {
            this.esAllowedTenantsApis.put("first_assignee_report", Set.copyOf(jiraFirstAssigneeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraAssigneeAllocationReportCompanies)) {
            this.esAllowedTenantsApis.put("assignee_allocation_report", Set.copyOf(jiraAssigneeAllocationReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraStageTimesReportCompanies)) {
            this.esAllowedTenantsApis.put("stage_times_report", Set.copyOf(jiraStageTimesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraStageBounceReportCompanies)) {
            this.esAllowedTenantsApis.put("stage_bounce_report", Set.copyOf(jiraStageBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(jiraSprintMetricsReportCompanies)) {
            this.esAllowedTenantsApis.put("sprint_metrics_report", Set.copyOf(jiraSprintMetricsReportCompanies));
        }

        dbConfig();
    }

    private void dbConfig() {

        this.dbAllowedTenants = new HashSet<>();
        this.dbAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(dbJiraCompanies)) {
            this.dbAllowedTenants.addAll(dbJiraCompanies);
        }
        if (CollectionUtils.isNotEmpty(dbJiraListCompanies)) {
            this.dbAllowedTenantsApis.put("list", Set.copyOf(dbJiraListCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraListBACompanies)) {
            this.dbAllowedTenantsApis.put("list_ba", Set.copyOf(dbJiraListBACompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraTicketsReportCompanies)) {
            this.dbAllowedTenantsApis.put("tickets_report", Set.copyOf(dbJiraTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraHygieneReportCompanies)) {
            this.dbAllowedTenantsApis.put("hygiene_report", Set.copyOf(dbJiraTicketsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraResponseTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put("response_time_report", Set.copyOf(dbJiraResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraResolutionTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put("resolution_time_report", Set.copyOf(dbJiraResolutionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraHopsReportCompanies)) {
            this.dbAllowedTenantsApis.put("hops_report", Set.copyOf(dbJiraHopsReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraBounceReportCompanies)) {
            this.dbAllowedTenantsApis.put("bounce_report", Set.copyOf(dbJiraBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraAgeReportCompanies)) {
            this.dbAllowedTenantsApis.put("age_report", Set.copyOf(dbJiraAgeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraStoryPointReportCompanies)) {
            this.dbAllowedTenantsApis.put("story_point_report", Set.copyOf(dbJiraStoryPointReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraStateTransitionTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put("state_transition_time_report", Set.copyOf(dbJiraStateTransitionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraFirstAssigneeReportCompanies)) {
            this.dbAllowedTenantsApis.put("first_assignee_report", Set.copyOf(dbJiraFirstAssigneeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraAssigneeAllocationReportCompanies)) {
            this.dbAllowedTenantsApis.put("assignee_allocation_report", Set.copyOf(dbJiraAssigneeAllocationReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraStageTimesReportCompanies)) {
            this.dbAllowedTenantsApis.put("stage_times_report", Set.copyOf(dbJiraStageTimesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraStageBounceReportCompanies)) {
            this.dbAllowedTenantsApis.put("stage_bounce_report", Set.copyOf(dbJiraStageBounceReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbJiraSprintMetricsReportCompanies)) {
            this.dbAllowedTenantsApis.put("sprint_metrics_report", Set.copyOf(dbJiraSprintMetricsReportCompanies));
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraIssue>>> getListOfIssues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {

            Optional<DbListResponse<DbJiraIssue>> preCalculatedValues = velocityStageTimesReportPrecalculateWidgetService.getVelocityStageTimeReportPreCalculation(company, originalRequest, VelocityStageTimesReportSubType.VELOCITY_STAGE_TIME_REPORT_VALUES, disablePrecalculatedResult);
            if (preCalculatedValues.isPresent()) {
                return ResponseEntity.ok(
                        PaginatedResponse.of(originalRequest.getPage(), originalRequest.getPageSize(), preCalculatedValues.get())
                );
            }

            if (originalRequest.getPageSize() > maxPageSize) {
                throw new BadRequestException("page_size cannot exceed " + maxPageSize);
            }
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            // horrible hack for drilldowns...
            if (request.getFilter().containsKey("keys") && request.getFilter().containsKey("projects") && !originalRequest.getFilter().containsKey("projects")) {
                var sanitizedFilters = new HashMap<>(request.getFilter());
                sanitizedFilters.remove("projects");
                request = request.toBuilder().filter(sanitizedFilters).build();
            }
            VelocityConfigDTO velocityConfigDTO = null;
            boolean needVelocityStagesFilter = false;
            if (request.getFilterValue("velocity_config_id", String.class).isPresent()) {
                velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
                needVelocityStagesFilter = true;
            }
            Boolean shouldFetchEpicSummary = (Boolean) request.getFilter().getOrDefault("fetch_epic_summary",false);
            JiraIssuesFilter.DISTINCT across = JiraIssuesFilter.DISTINCT.fromString(request.getAcross());
            String interval = StringUtils.defaultString(request.getAggInterval()).trim().toLowerCase();
            JiraIssuesFilter issueFilter;
            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, request);
                String configTableRowId = request.getFilter().get("config_table_row_id").toString();
                issueFilter = jiraFilterParser.createFilterFromConfig(company, request, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, across, null, interval, false);
            } else {
                issueFilter = jiraFilterParser.createFilter(company, request, null, across, null, interval, "", false, needVelocityStagesFilter);
            }
            velocityConfigDTO = checkStagePresentInWorkflowProfile(company, originalRequest, velocityConfigDTO, needVelocityStagesFilter, issueFilter);
            Map<String, List<String>> velocityStageStatusesMap = Map.of();
            List<VelocityConfigDTO.Stage> developmentCustomStages = new ArrayList<>();
            Optional<JiraIssuesFilter> linkedIssuesFilter;
            if (JiraIssueReadUtils.isLinkedIssuesRequired(issueFilter)) {
                linkedIssuesFilter = Optional.ofNullable(jiraFilterParser.createFilter(company, request, null, null, null, null, "linked_", false));
            } else {
                linkedIssuesFilter = Optional.empty();
            }
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            JiraSprintFilter jiraSprintFilter = JiraSprintFilter.fromDefaultListRequest(request);
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var sort = request.getSort();
            final var finalOuConfig = ouConfig;
            JiraIssuesFilter finalIssueFilter = issueFilter;
            Optional<VelocityConfigDTO> optionalVelocityConfigDTO = velocityConfigDTO != null ? Optional.of(velocityConfigDTO) : Optional.empty();
            boolean finalNeedVelocityStagesFilter = needVelocityStagesFilter;
            //PROP-3316 : If velocity filter is involved in the drill-down, ensure it hits DB

            //PROP-3432 - exclude stages filter not working correctly on ES
            boolean isExcludeStagesDrilldown = false;
            if (CollectionUtils.isNotEmpty(issueFilter.getExcludeStages())) {
                isExcludeStagesDrilldown = true;
                log.info("isExcludeStagesDrilldown = {}, company = {}", isExcludeStagesDrilldown, company);
            }

            boolean issuesListIsForTicketCategory = isIssuesListForTicketCategory(finalIssueFilter);
            boolean issuesListIsForBAAndUseES = issuesListIsForTicketCategory && isUseEs(company, "list_ba", forceSource);;

            //TODO : Populate velocity related filters to the original filter
            boolean useEs = !needVelocityStagesFilter && isUseEs(company, "list", forceSource) && !issuesListIsForTicketCategory
                    && !isExcludeStagesDrilldown;

            log.info("useEs = {}, company = {}", useEs, company);

            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_list_" + sortHash,
                                    issueFilter.generateCacheHash() + finalOuConfig.hashCode() + shouldFetchEpicSummary.hashCode(), issueFilter.getIntegrationIds(), mapper, cacheService,
                                    () -> {
                                        DbListResponse<DbJiraIssue> issues = null;
                                        if (issuesListIsForBAAndUseES) {
                                            issues = baJiraAggsESService.getListOfJiraIssues(company, finalIssueFilter, ouConfig, page, pageSize);
                                        } else {
                                            if (useEs) {
                                                JiraIssuesFilter.JiraIssuesFilterBuilder issuesFilterBuilder = issueFilter.toBuilder();
                                                List<String> velocityStages = issueFilter.getVelocityStages();
                                                if (CollectionUtils.isNotEmpty(velocityStages) && optionalVelocityConfigDTO.isPresent()) {
                                                    LinkedHashMap<String, List<String>> stageStatusesMap = getStageStatusesMap(getSortedDevStages(optionalVelocityConfigDTO.get()));
                                                    if (velocityStages.get(0).equalsIgnoreCase("Other")) {
                                                        List<String> allStageStatuses = new ArrayList<>();
                                                        stageStatusesMap.forEach((k, v) -> allStageStatuses.addAll(v));
                                                        List<String> finalList = new ArrayList<>(issueFilter.getExcludeStages());
                                                        finalList.addAll(allStageStatuses);
                                                        issuesFilterBuilder = issueFilter.toBuilder().excludeStages(finalList);
                                                    } else {
                                                        List<String> statuses = stageStatusesMap.get(velocityStages.get(0));
                                                        List<String> finalList = new ArrayList<>(issueFilter.getStages());
                                                        finalList.addAll(statuses);
                                                        issuesFilterBuilder = issueFilter.toBuilder().stages(finalList);
                                                    }
                                                }
                                                JiraIssuesFilter linkedFilter = linkedIssuesFilter.isPresent() ? linkedIssuesFilter.get() : JiraIssuesFilter.builder().build();
                                                issues = esJiraIssueQueryService.getJiraIssuesList(company, issuesFilterBuilder
                                                                .sort(SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())))
                                                                .build(),
                                                        linkedFilter,
                                                        finalOuConfig,
                                                        page,
                                                        pageSize,
                                                        optionalVelocityConfigDTO);
                                            } else {
                                                issues = jiraIssueService.list(company, jiraSprintFilter, finalIssueFilter, linkedIssuesFilter,
                                                        finalOuConfig, optionalVelocityConfigDTO,
                                                        SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                                        page,
                                                        pageSize);
                                            }
                                        }
                                        return postProcessAddEpicSummary(company, finalIssueFilter, issues, shouldFetchEpicSummary);
                                    })));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/release_table_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraReleaseResponse>>> getListOfRelease(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult) throws SQLException, NotFoundException, BadRequestException {

        VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();

        final var page = request.getPage();
        final var pageSize = request.getPageSize();

        final JiraIssuesFilter finalIssueFilter = jiraFilterParser.createFilter(
                company, request, null, null, null, StringUtils.EMPTY, "", false, true
        );
        final VelocityConfigDTO finalVelocityConfigDTO = velocityConfigDTO;
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page, pageSize, jiraIssueApiService.getListOfRelease(
                                    company, originalRequest, finalIssueFilter, finalVelocityConfigDTO, disablePrecalculatedResult
                            )
                    )
            );
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/release_table_report/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraIssue>>> getListOfJiraRelease(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult) throws SQLException, NotFoundException, BadRequestException {
        VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();

        final var page = request.getPage();
        final var pageSize = request.getPageSize();

        final JiraIssuesFilter finalIssueFilter = jiraFilterParser.createFilter(
                company, request, null, null, null, StringUtils.EMPTY, "", false, true
        );

        final VelocityConfigDTO finalVelocityConfigDTO = velocityConfigDTO;
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page, pageSize, jiraIssueApiService.getListOfReleaseForDrillDown(
                                    company, originalRequest, finalIssueFilter, finalVelocityConfigDTO, disablePrecalculatedResult
                            )
                    )
            );
        });
    }

    private VelocityConfigDTO checkStagePresentInWorkflowProfile(String company, DefaultListRequest originalRequest, VelocityConfigDTO velocityConfigDTO,
                                                                 boolean needVelocityStagesFilter,
                                                                 JiraIssuesFilter issueFilter) throws SQLException, NotFoundException {
        List<VelocityConfigDTO.Stage> developmentStages = new ArrayList<>();
        if (needVelocityStagesFilter) {
            velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
            if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
                developmentStages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
            }
            if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
                developmentStages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
            }
            List<String> workFlowStageNames = new ArrayList<>(getStageStatusesMap(developmentStages).keySet());
            workFlowStageNames.add("Other");
            workFlowStageNames.add("$$ALL_STAGES$$");
            issueFilter.getVelocityStages().forEach(velocityStage -> {
                if (!workFlowStageNames.contains(velocityStage)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The velocity stage: " + velocityStage + "" +
                            " is not present in the provided workflow profile");
                }
            });
        }
        return velocityConfigDTO;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/priorities/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraPrioritySla>>> getListOfPriorities(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/priorities/list' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            List<String> projects = getListOrDefault(request.getFilter(), "projects");
            List<String> issueTypes = getListOrDefault(request.getFilter(), "issue_types");
            List<String> priorities = getListOrDefault(request.getFilter(), "priorities");

            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, request);
                String configTableRowId = request.getFilter().get("config_table_row_id").toString();
                ConfigTable.Row row = configTable.getRows().get(configTableRowId);

                projects = getCommonFilterValue(configTable, row, "jira_projects", projects);
                issueTypes = getCommonFilterValue(configTable, row, "jira_issue_types", issueTypes);
                priorities = getCommonFilterValue(configTable, row, "jira_priorities", priorities);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            jiraIssueService.listPrioritiesSla(company, integrationIds, projects, issueTypes, priorities,
                                    request.getPage(), request.getPageSize()
                            )));

        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/priorities/bulk", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> bulkUpdatePriorities(
            @SessionAttribute(name = "company") String company,
            @RequestBody Map<String, Object> requestBody) {
        DefaultListRequest filter = mapper.convertValue(requestBody, DefaultListRequest.class);
        DbJiraPrioritySla slaObj = mapper.convertValue(requestBody.get("update"), DbJiraPrioritySla.class);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(mapper.writeValueAsString(
                Map.of("update_count",
                        jiraIssueService.bulkUpdatePrioritySla(
                                company,
                                null,
                                getListOrDefault(filter.getFilter(), "integration_ids"),
                                getListOrDefault(filter.getFilter(), "projects"),
                                getListOrDefault(filter.getFilter(), "issue_types"),
                                getListOrDefault(filter.getFilter(), "priorities"),
                                slaObj.getRespSla(),
                                slaObj.getSolveSla()
                        )))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/priorities/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> updatePriority(
            @PathVariable("id") final UUID id,
            @SessionAttribute(name = "company") String company,
            @RequestBody DbJiraPrioritySla prioritySla) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok("{\"ok\":\"" + jiraIssueService.updatePrioritySla(company,
                        prioritySla.toBuilder().id(id.toString()).build()) + "\"}"));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/custom_field/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCustomValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            JiraIssuesFilter issuesFilterBase = jiraFilterParser.createFilter(company, request, null, JiraIssuesFilter.DISTINCT.custom_field, null, request.getAggInterval(), false, false)
                    .toBuilder()
                    .sort(Map.of(JiraIssuesFilter.DISTINCT.custom_field.toString(), SortingOrder.ASC))
                    .build();

            boolean useEs = isUseEs(company, "custom_field/values", forceSource) && !isTicketCategorySpecified(issuesFilterBase, List.of());
            List<CompletableFuture<Map<String, List<DbAggregationResult>>>> futures = new ArrayList<>();
            for (String value : request.getFields()) {
                futures.add(calculateJiraCustomFieldValuesAsync(company, disableCache, issuesFilterBase, value, ouConfig, useEs));
            }

            List<Map<String, List<DbAggregationResult>>> response = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            log.debug("response = {}", response);
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    public CompletableFuture<Map<String, List<DbAggregationResult>>> calculateJiraCustomFieldValuesAsync(String company, final Boolean disableCache, final JiraIssuesFilter issuesFilterBase, String value, OUConfiguration ouConfig, Boolean useEs) {
        return CompletableFuture.supplyAsync(() -> {
            JiraIssuesFilter issuesFilter = issuesFilterBase.toBuilder().customAcross(value).build();
            List<DbAggregationResult> dbAggregationResults = null;
            try {
                final var finalOuConfig = ouConfig;
                dbAggregationResults = AggCacheUtils.cacheOrCall(disableCache, company, "/jira/custom/values",
                        issuesFilter.generateCacheHash() + finalOuConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                        () -> {
                            if (useEs) {
                                return esJiraIssueQueryService.getAggReport(
                                        company, issuesFilter, List.of(), finalOuConfig, null, null, true);
                            } else {
                                return jiraIssueService.groupByAndCalculate(
                                        company,
                                        issuesFilter,
                                        true,
                                        null, finalOuConfig, Map.of());
                            }
                        }).getRecords();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.debug("CustomFieldValue = {}, dbAggregationResults = {}", value, dbAggregationResults);
            return Map.of(value, dbAggregationResults);
        }, dbValuesTaskExecutor);
    }


    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            JiraIssuesFilter issuesFilterBase = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false, false)
                    .toBuilder()
                    .issueCreatedRange(ImmutablePair.of(null, null))
                    .issueUpdatedRange(ImmutablePair.of(null, null))
                    .build();

            boolean useEs = isUseEs(company, "values", forceSource) && !isTicketCategorySpecified(issuesFilterBase, List.of());
            List<CompletableFuture<Map<String, List<DbAggregationResult>>>> futures = new ArrayList<>();
            for (String value : request.getFields()) {
                futures.add(calculateJiraValuesAsync(company, disableCache, issuesFilterBase, value, ouConfig, useEs));
            }

            List<Map<String, List<DbAggregationResult>>> response = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            log.debug("response = {}", response);
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    public CompletableFuture<Map<String, List<DbAggregationResult>>> calculateJiraValuesAsync(String company, final Boolean disableCache, final JiraIssuesFilter issuesFilterBase, String value, OUConfiguration ouConfig, Boolean useEs) {
        return CompletableFuture.supplyAsync(() -> {
            JiraIssuesFilter issuesFilter = issuesFilterBase.toBuilder().across(JiraIssuesFilter.DISTINCT.fromString(value)).sort(Map.of(value, SortingOrder.ASC)).build();
            List<DbAggregationResult> dbAggregationResults = null;
            try {
                final var finalOuConfig = ouConfig;
                dbAggregationResults = AggCacheUtils.cacheOrCall(disableCache, company, "/jira/values",
                        issuesFilter.generateCacheHash() + finalOuConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                        () -> {
                            if (useEs) {
                                return esJiraIssueQueryService.getAggReport(
                                        company, issuesFilter, List.of(), finalOuConfig, null, null, true);
                            } else {
                                return jiraIssueService.groupByAndCalculate(
                                        company,
                                        issuesFilter,
                                        true,
                                        null,
                                        finalOuConfig, Map.of());
                            }
                        }).getRecords();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.debug("value = {}, dbAggregationResults = {}", value, dbAggregationResults);
            return Map.of(value, dbAggregationResults);
        }, dbValuesTaskExecutor);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBounceReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "bounce_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.bounces, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/hops_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHopsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "hops_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.hops, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResponseReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "response_time_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.response_time, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/stage_times_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTimeAcrossStagesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "stage_times_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.stage_times_report, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/velocity_stage_times_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTimeAcrossStagesLeadReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalFilter) {
        OUConfiguration ouConfig;
        List<DbAggregationResult> dbResults;
        List<DbAggregationResult> calculatedAggResults;

        try {
            Optional<DbListResponse<DbAggregationResult>> preCalculatedResult = velocityStageTimesReportPrecalculateWidgetService.getVelocityStageTimeReportPreCalculation(company, originalFilter, VelocityStageTimesReportSubType.VELOCITY_STAGE_TIME_REPORT, disablePrecalculatedResult);
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalFilter);
            final DefaultListRequest filter = ouConfig.getRequest();
            if (filter.getFilterValue("velocity_config_id", String.class).isEmpty()) {
                throw new BadRequestException("Velocity config id must be present for this report.");
            }
            VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, filter);
            Validate.notNull(velocityConfigDTO, "velocityConfigDTO cant be missing for velocity_stage_times_report.");
            List<VelocityConfigDTO.Stage> developmentStages = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
                List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                        .stream()
                        .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                        .collect(Collectors.toList());
                developmentStages.addAll(preDevSortedStages);
            }
            if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
                List<VelocityConfigDTO.Stage> postDevSortedStages = velocityConfigDTO.getPostDevelopmentCustomStages()
                        .stream()
                        .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                        .collect(Collectors.toList());
                developmentStages.addAll(postDevSortedStages);
            }

            // PROP-95: only Jira stages are supported at this time (need to filter them so that parseAggResults doesn't return zeros for non-Jira steps)
            developmentStages = developmentStages.stream()
                    .filter(stage ->
                            stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_STATUS)
                                    || stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_RELEASE)
                    )
                    .collect(Collectors.toList());

            var calculateSingleState = MapUtils.getBoolean(filter.getFilter(), "calculateSingleState", false);
            List<DbAggregationResult> dbAggregationResults;
            if (preCalculatedResult.isPresent()) {
                dbAggregationResults = preCalculatedResult.get().getRecords();
            }
            else {
                Map<String, List<String>> velocityStageStatusesMap = getStageStatusesMap(developmentStages);
                if (MapUtils.isEmpty(velocityStageStatusesMap)) {
                    return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.stage_times_report, filter, Map.of(), false);
                }

                dbAggregationResults = doGetAggResultAndDeserializeResponse(disableCache, company, JiraIssuesFilter.CALCULATION.velocity_stage_times_report,
                        filter, ouConfig, velocityStageStatusesMap, velocityConfigDTO, false);
            }
            dbAggregationResults = parseAggResults(dbAggregationResults, developmentStages, calculateSingleState);
            List<DbAggregationResult> finalDbAggregationResults = dbAggregationResults;

            if (preCalculatedResult.isPresent()) {
                return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), CollectionUtils.size(finalDbAggregationResults), preCalculatedResult.get().getCalculatedAt(), finalDbAggregationResults)));
            } else {
                return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), finalDbAggregationResults)));
            }
        } catch (Exception e) {
            log.warn("Failed to get velocity_stage_times_report", e);
            throw new RuntimeStreamException(e);
        }
    }


    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/assignee_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraAssigneeTime>>> getAssigneeTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            JiraIssuesFilter issuesFilter;
            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, request);
                String configTableRowId = request.getFilter().get("config_table_row_id").toString();
                issuesFilter = jiraFilterParser.createFilterFromConfig(company, request, configTable.getRows().get(configTableRowId)
                        , configTable.getSchema().getColumns(), null, null, null,
                        request.getAggInterval(), false);
            } else {
                issuesFilter = jiraFilterParser.createFilter(company, request, null, null, null,
                        request.getAggInterval(), false, false);
            }


            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira/assignee_list_pg_" + request.getPage() + "_sz_" + request.getPageSize(),
                                    issuesFilter.generateCacheHash() + ouConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                                    () -> jiraIssueService.listIssueAssigneesByTime(company,
                                            issuesFilter,
                                            ouConfig,
                                            request.getPage(),
                                            request.getPageSize()))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/status_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraStatusTime>>> getStatusTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            JiraIssuesFilter issuesFilter;
            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, request);
                String configTableRowId = request.getFilter().get("config_table_row_id").toString();
                issuesFilter = jiraFilterParser.createFilterFromConfig(company, request, configTable.getRows().get(configTableRowId)
                        , configTable.getSchema().getColumns(), null, null, null,
                        request.getAggInterval(), false);
            } else {
                issuesFilter = jiraFilterParser.createFilter(company, request, null, null, null,
                        request.getAggInterval(), false, false);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira/statuses_list_pg_" + request.getPage() + "_sz_" + request.getPageSize(),
                                    issuesFilter.generateCacheHash() + ouConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                                    () -> jiraIssueService.listIssueStatusesByTime(company,
                                            issuesFilter,
                                            ouConfig,
                                            request.getPage(),
                                            request.getPageSize()))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolutionReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "resolution_time_report", forceSource);
        if ("broadridge".equals(company)) {
            useEs = false;
            log.info("useEs = {}, company = {}", useEs, company);
        }
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.resolution_time, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/first_assignee_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getFirstAssigneeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "first_assignee_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.assign_to_resolve, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/tickets_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "tickets_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.ticket_count, filter, Map.of(), useEs);
    }

    private boolean hasAccess(String company, Principal principal, String token, String permission) {
        try {
            ACLClient aclClient = aclClientFactory.get(token);
            AccessCheckRequestDTO checkRequestDTO = getAccessCheckRequestDTO(company, principal, permission);
            AccessCheckResponseDTO response = aclClient.checkAccess(checkRequestDTO);
            log.info("response {}", response);
            return response.getAccessCheckDataResponse().getAccessControlList().stream().anyMatch(o -> o.isPermitted());
        } catch (ACLClientException e) {
            throw new RuntimeException(e);
        }
    }

    private AccessCheckRequestDTO getAccessCheckRequestDTO(String company, Principal principal, String permission) {
        return AccessCheckRequestDTO.builder()
                .principal(principal)
                .permissions(List.of(
                        PermissionCheckDTO.builder()
                                .resourceScope(ResourceScope.builder()
                                        .accountIdentifier(company)
                                        .build())
                                .resourceType("ACCOUNT")
                                .resourceIdentifier(company)
                                .permission(permission)
                                .build()))
                .build();
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/age_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBacklogsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "age_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.age, filter, Map.of(), useEs);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/hygiene_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHygieneReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "hygiene_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.ticket_count, filter, Map.of(), useEs);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/state_transition_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStateTransitionTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "state_transition_time_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.state_transition_time, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/stage_bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStageBouncesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "stage_bounce_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.stage_bounce_report, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/story_point_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStoryPointReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, "story_point_report", forceSource);
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.story_points, filter, Map.of(), useEs);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/assignee_allocation_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAssigneeAllocationReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        Boolean useEs = isUseEs(company, "assignee_allocation_report", forceSource);
        // -- normal agg
        if (!JiraIssuesFilter.DISTINCT.trend.toString().equalsIgnoreCase(originalRequest.getAcross())) {
            return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.assignees, originalRequest, Map.of(), useEs);
        }
        // -- trend agg -> we will be generating the time partition client-side and do 1 query per time window
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            String interval = StringUtils.defaultString(request.getAggInterval()).trim().toLowerCase();
            ImmutablePair<Long, Long> assigneesRange = DefaultListRequestUtils.getTimeRange(request, "assignees_range");
            Instant lowerBound = DateUtils.fromEpochSecond(assigneesRange.getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(assigneesRange.getRight());
            if (lowerBound == null || upperBound == null) {
                throw new BadRequestException("assignees_range required");
            }
            List<ImmutablePair<Long, Long>> timePartition;
            switch (interval) {
                case "week":
                    timePartition = DateUtils.getWeeklyPartition(lowerBound, upperBound);
                    break;
                case "month":
                    timePartition = DateUtils.getMonthlyPartition(lowerBound, upperBound);
                    break;
                default:
                    throw new BadRequestException("Interval not supported: " + interval);
            }
            log.info("Getting assignee allocation from {} to {} ({}) -> time_partition={}", lowerBound, upperBound, assigneesRange, timePartition);
            return ResponseEntity.ok(getAssigneeAllocationForTimePartition(company, request, disableCache, timePartition, ouConfig));
        });
    }

    private PaginatedResponse<DbAggregationResult> getAssigneeAllocationForTimePartition(String company, DefaultListRequest request, Boolean disableCache, List<ImmutablePair<Long, Long>> timePartition, OUConfiguration ouConfig) {
        int page = MoreObjects.firstNonNull(request.getPage(), 0);
        int pageSize = MoreObjects.firstNonNull(request.getPageSize(), 25);
        return PaginatedResponse.of(page, pageSize, timePartition.stream()
                .skip((long) page * pageSize)
                .limit(pageSize)
                .map(pair -> {
                    try {
                        return getAssigneeAllocationForDateRange(company, request, disableCache, pair.getLeft(), pair.getRight(), ouConfig);
                    } catch (Exception e) {
                        log.warn("Failed to get assignee allocation", e);
                        throw new RuntimeStreamException(e);
                    }
                })
                .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private DbAggregationResult getAssigneeAllocationForDateRange(String company, DefaultListRequest request, Boolean disableCache, @Nonnull Long from, @Nonnull Long to, OUConfiguration ouConfig) throws Exception {
        Map<String, Object> filter = request.getFilter();
        filter.put("assignees_range", Map.of(
                "$lt", String.valueOf(to),
                "$gt", String.valueOf(from)));
        var updatedRequest = request.toBuilder()
                .across("none")
                .aggInterval("")
                .filter(filter)
                .build();
        log.info("Getting assignee allocation for date range: {}-{}, filters: {}", from, to, updatedRequest);
        PaginatedResponse<?> results = doGetAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.assignees, updatedRequest, ouConfig, Map.of(), null, false);
        List<String> assignees = null;
        if (results != null && results.getResponse() != null) {
            // needed to do some jackson gymnastic due to caching/templating/etc.
            ListResponse<?> response = results.getResponse();
            Map<?, ?> listResponse = mapper.convertValue(response, Map.class);
            List<Object> records = (List<Object>) listResponse.get("records");
            assignees = IterableUtils.getFirst(records)
                    .map(obj -> mapper.convertValue(obj, Map.class))
                    .map(result -> (List<String>) result.get("assignees"))
                    .orElse(List.of());
        }
        return DbAggregationResult.builder()
                .key(String.valueOf(from)) // epoch s
                .assignees(assignees)
                .total((long) CollectionUtils.size(assignees))
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/priority_trend_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPriorityTrendReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {
        // OU stuff
        var request = originalRequest;
        try {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/priority_trend_report' for the request: {}", company, originalRequest, e);
        }

        String key = request.getFilterValue("key", String.class)
                .orElseThrow(() -> new BadRequestException("'key' filter is required"));
        String integrationId = request.getFilterValue("integration_id", String.class)
                .orElseThrow(() -> new BadRequestException("'integration_id' filter is required"));
        Map<String, Object> updatedFilter = new HashMap<>(request.getFilter());
        updatedFilter.put("keys", List.of(key));
        updatedFilter.put("integration_ids", List.of(integrationId));
        request = request.toBuilder()
                .across("trend")
                .filter(updatedFilter)
                .build();
        return getAggResult(disableCache, company, JiraIssuesFilter.CALCULATION.priority, request, Map.of(), false);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/sprints/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraSprint>>> listSprints(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();

            // -- completed at
            Map<Object, Object> completedAt = request.getFilterValueAsMap("completed_at").orElse(Map.of());
            Map<Object, Object> startedAt = request.getFilterValueAsMap("started_at").orElse(Map.of());
            Map<Object, Object> plannedEndedAt = request.getFilterValueAsMap("planned_ended_at").orElse(Map.of());
            String completedAtAfterStr = (String) completedAt.get("$gt");
            String completedAtBeforeStr = (String) completedAt.get("$lt");
            String startedAtAfterStr = (String) startedAt.get("$gt");
            String startedAtBeforeStr = (String) startedAt.get("$lt");
            String plannedEndedAtAfterStr = (String) plannedEndedAt.get("$gt");
            String plannedEndedAtBeforeStr = (String) plannedEndedAt.get("$lt");

            Long completedAtAfter = completedAtAfterStr != null ? Long.valueOf(completedAtAfterStr) :  null;
            Long completedAtBefore = completedAtBeforeStr != null ? Long.valueOf(completedAtBeforeStr) : null;
            Long startedAtAfter = startedAtAfterStr != null ? Long.valueOf(startedAtAfterStr) : null;
            Long startedAtBefore = startedAtBeforeStr != null ? Long.valueOf(startedAtBeforeStr) : null;
            Long plannedEndedAtAfter = plannedEndedAtAfterStr != null ? Long.valueOf(plannedEndedAtAfterStr) : null;
            Long plannedEndedAtBefore = plannedEndedAtBeforeStr != null ? Long.valueOf(plannedEndedAtBeforeStr) : null;

            // -- sprint report (name)
            String nameContains = request.<String, Object>getFilterValueAsMap("partial_match")
                    .map(m -> (Map<String, Object>) m.get("name"))
                    .map(m -> (String) m.get("$contains")).orElse(null);

            DbListResponse<DbJiraSprint> dbJiraSprints = jiraIssueService.filterSprints(company, request.getPage(), request.getPageSize(), JiraSprintFilter.builder()
                    .integrationIds(request.<Object>getFilterValueAsList("integration_ids").orElse(List.of()).stream().filter(i -> i != null).map(Object::toString).collect(Collectors.toList()))
                    .sprintIds(request.<String>getFilterValueAsList("sprint_ids").orElse(null))
                    .completedAtAfter(completedAtAfter)
                    .completedAtBefore(completedAtBefore)
                    .startDateAfter(startedAtAfter)
                    .startDateBefore(startedAtAfter)
                    .endDateBefore(plannedEndedAtBefore)
                    .endDateAfter(plannedEndedAtAfter)
                    .nameContains(nameContains)
                    .state(request.getFilterValue("state", String.class).orElse(null))
                    .build());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), dbJiraSprints));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/sprint_metrics_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<SprintMetrics>>> getSprintMetricsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(AggCacheUtils.cacheOrCallGeneric(
                disableCache,
                company,
                "/jira/sprint_metrics",
                CacheHashUtils.generateCacheHash(forceSource, originalRequest),
                List.of(), mapper, cacheService, PaginatedResponse.class,
                null, null,
                () -> doGetSprintMetricsReport(disableCache, forceSource, company, originalRequest))));
    }

    @SuppressWarnings("unchecked")
    public PaginatedResponse<SprintMetrics> doGetSprintMetricsReport(Boolean disableCache, String forceSource, String company, DefaultListRequest originalRequest) throws Exception {
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        HashMap<String, Object> filter = new HashMap<>(MapUtils.emptyIfNull(request.getFilter()));
        Map<String, Object> filterPartialMatch = new HashMap<>((Map<String, Object>) filter.getOrDefault("partial_match", Map.of()));
        Map<String, Object> filterExclude = new HashMap<>((Map<String, Object>) filter.getOrDefault("exclude", Map.of()));

        // -- completed at
        Map<Object, Object> completedAt = request.getFilterValueAsMap("completed_at").orElse(Map.of());
        Map<Object, Object> startedAt = request.getFilterValueAsMap("started_at").orElse(Map.of());
        Map<Object, Object> plannedEndedAt = request.getFilterValueAsMap("planned_ended_at").orElse(Map.of());
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

        // -- sprint report (name)
        List<String> fullNames = request.<String>getFilterValueAsList("sprint_full_names").orElse(null);
        List<String> names = request.<String>getFilterValueAsList("sprint_report").orElse(fullNames);
        List<String> excludeFullNames = (List<String>) filterExclude.getOrDefault("sprint_full_names", List.of());
        List<String> excludeNames = (List<String>) filterExclude.getOrDefault("sprint_report", excludeFullNames);
        Map<String, Object> sprintFullNamePartialMatchObj = (Map<String, Object>) filterPartialMatch.getOrDefault("sprint_full_names", Map.of());
        Map<String, Object> sprintReportPartialMatchObj = (Map<String, Object>) filterPartialMatch.getOrDefault("sprint_report", sprintFullNamePartialMatchObj);
        String nameContains = (String) sprintReportPartialMatchObj.get("$contains");
        String nameStartsWith = (String) sprintReportPartialMatchObj.get("$begins");
        String nameEndsWith = (String) sprintReportPartialMatchObj.get("$ends");

        // -- include_issue_keys
        // by default, do NOT return the issue keys in the response.
        boolean includeIssueKeys = request.getFilterValue("include_issue_keys", Boolean.class).orElse(false);

        // -- creep buffer
        int creepBuffer = (Integer) filter.getOrDefault("creep_buffer", 0);

        // -- additional_done_statuses
        List<String> additionalDoneStatuses = request.<String>getFilterValueAsList("additional_done_statuses").orElse(List.of());

        // -- treat_outside_of_sprint_as_planned_and_delivered (false by default)
        boolean treatOutsideOfSprintAsPlannedAndDelivered = request.getFilterValue("treat_outside_of_sprint_as_planned_and_delivered", Boolean.class).orElse(false);

        // -- sanitize request and create issue filter
        filter.remove("completed_at");
        filter.remove("started_at");
        filter.remove("planned_ended_at");
        filter.remove("sprint_report");
        filter.remove("sprint_full_names");
        filter.remove("include_issue_keys");
        filterPartialMatch.remove("sprint_report");
        filterPartialMatch.remove("sprint_full_name");
        filter.put("partial_match", filterPartialMatch);
        filterExclude.remove("sprint_report");
        filterExclude.remove("sprint_full_names");
        filter.put("exclude", filterExclude);

        int sprintCount = (Integer) filter.getOrDefault("sprint_count", 0);
        filter.remove("sprint_count");

        filter.put("sprint_mapping_ignorable_issue_type", false); // remove ignorable issues (sub-tasks, etc.)
        filter.put("sprint_mapping_sprint_state", "closed"); // only completed sprints
        filter.put("sprint_mapping_sprint_completed_at_after", completedAtAfter);
        filter.put("sprint_mapping_sprint_completed_at_before", completedAtBefore);
        filter.put("sprint_mapping_sprint_started_at_after", startedAtAfter);
        filter.put("sprint_mapping_sprint_started_at_before", startedAtBefore);
        filter.put("sprint_mapping_sprint_planned_completed_at_after", plannedEndedAtAfter);
        filter.put("sprint_mapping_sprint_planned_completed_at_before", plannedEndedAtBefore);
        filter.put("sprint_mapping_sprint_names", names);
        filter.put("sprint_mapping_sprint_name_starts_with", nameStartsWith);
        filter.put("sprint_mapping_sprint_name_ends_with", nameEndsWith);
        filter.put("sprint_mapping_sprint_name_contains", nameContains);
        filter.put("sprint_mapping_exclude_sprint_names", excludeNames);

        int page = request.getPage();
        int pageSize = request.getPageSize();
        if (sprintCount > 0) {
            page = 0;
            pageSize = sprintCount;
        }
        DefaultListRequest sanitizedRequest = DefaultListRequest.builder()
                .page(page)
                .pageSize(pageSize)
                .across(JiraIssuesFilter.DISTINCT.sprint_mapping.toString())
                .sort(request.getSort())
                .filter(filter)
                .build();

        // -- run agg
        Boolean useEs = isUseEs(company, "sprint_metrics_report", forceSource);
        List<DbAggregationResult> aggResults = doGetAggResultAndDeserializeResponse(disableCache, company, JiraIssuesFilter.CALCULATION.sprint_mapping, sanitizedRequest, ouConfig, Map.of(), null, useEs);
        Integer totalCount = null;
        //PROP-3431 : In case of ES, handle the pagination aftwer results are fetched
        //TODO : Come up with pagination strategy within ES Query
        if(BooleanUtils.isTrue(useEs)){
            totalCount = aggResults.size();
            aggResults = aggResults.subList((page*pageSize),Math.min(aggResults.size(),(page*pageSize+pageSize)));
        }
        List<SprintMetrics> sprintMetrics = jiraSprintMetricsService.generateSprintMetrics(company, aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(includeIssueKeys)
                .creepBuffer(creepBuffer)
                .additionalDoneStatuses(additionalDoneStatuses)
                .treatOutsideOfSprintAsPlannedAndDelivered(treatOutsideOfSprintAsPlannedAndDelivered)
                .build());


        Boolean includeTotalCount = request.getFilterValue("include_total_count", Boolean.class).orElse(false);
        if (BooleanUtils.isTrue(includeTotalCount)) {
            if (sprintCount > 0) {
                totalCount = CollectionUtils.size(sprintMetrics);
            } else if(BooleanUtils.isNotTrue(useEs)){
                DefaultListRequest countRequest = sanitizedRequest.toBuilder()
                        .across(JiraIssuesFilter.DISTINCT.none.toString())
                        .page(null)
                        .pageSize(null)
                        .build();
                List<DbAggregationResult> dbAggregationResults = doGetAggResultAndDeserializeResponse(disableCache, company, JiraIssuesFilter.CALCULATION.sprint_mapping_count, countRequest, ouConfig, Map.of(), null, useEs);
                totalCount = IterableUtils.getFirst(dbAggregationResults).map(DbAggregationResult::getTotal).orElse(0L).intValue();
            }
        }

        return PaginatedResponse.of(request.getPage(), request.getPageSize(), totalCount, sprintMetrics);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/sprint_distribution_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraSprintDistMetric>>> getSprintDistributionReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            HashMap<String, Object> filter = new HashMap<>(MapUtils.emptyIfNull(request.getFilter()));
            Map<String, Object> filterPartialMatch = new HashMap<>((Map<String, Object>) filter.getOrDefault("partial_match", Map.of()));
            Map<String, Object> filterExclude = new HashMap<>((Map<String, Object>) filter.getOrDefault("exclude", Map.of()));

            Map<Object, Object> completedAt = request.getFilterValueAsMap("completed_at").orElse(Map.of());
            Map<Object, Object> startedAt = request.getFilterValueAsMap("started_at").orElse(Map.of());
            Map<Object, Object> plannedEndedAt = request.getFilterValueAsMap("planned_ended_at").orElse(Map.of());
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

            // -- sprint report (name)
            List<String> fullNames = request.<String>getFilterValueAsList("sprint_full_names").orElse(null);
            List<String> names = request.<String>getFilterValueAsList("sprint_report").orElse(fullNames);
            List<String> excludeFullNames = (List<String>) filterExclude.getOrDefault("sprint_full_names", List.of());
            List<String> excludeNames = (List<String>) filterExclude.getOrDefault("sprint_report", excludeFullNames);
            Map<String, Object> sprintFullNamePartialMatchObj = (Map<String, Object>) filterPartialMatch.getOrDefault("sprint_full_names", Map.of());
            Map<String, Object> sprintReportPartialMatchObj = (Map<String, Object>) filterPartialMatch.getOrDefault("sprint_report", sprintFullNamePartialMatchObj);
            String nameContains = (String) sprintReportPartialMatchObj.get("$contains");
            String nameStartsWith = (String) sprintReportPartialMatchObj.get("$begins");
            String nameEndsWith = (String) sprintReportPartialMatchObj.get("$ends");

            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");

            JiraSprintFilter.CALCULATION calculation;
            String calc = (String) request.getFilter().getOrDefault("agg_metric", "ticket_count");
            if (calc.equalsIgnoreCase("story_points")) {
                calculation = JiraSprintFilter.CALCULATION.sprint_story_points_report;
            } else if (calc.equalsIgnoreCase("ticket_count")) {
                calculation = JiraSprintFilter.CALCULATION.sprint_ticket_count_report;
            } else {
                throw new BadRequestException("Invalid option for agg_metric provided. Given value: " + calc);
            }

            JiraSprintFilter jiraSprintFilter = JiraSprintFilter.builder()
                    .integrationIds(integrationIds)
                    .sprintIds(ListUtils.emptyIfNull((List<String>) request.getFilter().get("sprint_ids")))
                    .sprintCount((Integer) request.getFilter().get("sprint_count"))
                    .names(names)
                    .excludeNames(excludeNames)
                    .completionPercentiles(ListUtils.emptyIfNull((List<Integer>) request.getFilter().get("percentiles")))
                    .distributionStages(ListUtils.emptyIfNull((List<String>) request.getFilter().get("distribution_stages")))
                    .nameStartsWith(nameStartsWith)
                    .nameEndsWith(nameEndsWith)
                    .nameContains(nameContains)
                    .completedAtAfter(completedAtAfter)
                    .completedAtBefore(completedAtBefore)
                    .startDateAfter(startedAtAfter)
                    .startDateBefore(startedAtBefore)
                    .endDateAfter(plannedEndedAtAfter)
                    .endDateBefore(plannedEndedAtBefore)
                    .build();

            DbListResponse<JiraSprintDistMetric> response = jiraIssueService.getSprintDistributionReport(company, calculation, jiraSprintFilter);

            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.getRecords().size(), response));
        });
    }

    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggResult(
            Boolean disableCache,
            String company,
            JiraIssuesFilter.CALCULATION calc,
            DefaultListRequest originalRequest,
            Map<String, List<String>> velocityStageStatusesMap,
            Boolean useEs) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
            var request = ouConfig.getRequest();
            return ResponseEntity.ok(doGetAggResult(disableCache, company, calc, request, ouConfig, velocityStageStatusesMap, null, useEs));
        });
    }

    private List<DbAggregationResult> doGetAggResultAndDeserializeResponse(Boolean disableCache,
                                                                           String company,
                                                                           JiraIssuesFilter.CALCULATION calc,
                                                                           DefaultListRequest filter,
                                                                           OUConfiguration ouConfig,
                                                                           Map<String, List<String>> velocityStageStatusesMap,
                                                                           VelocityConfigDTO velocityConfigDTO,
                                                                           Boolean useEs) throws Exception {
        // doing this Jackson magic to be able to use the response regardless of where it came from (db or cache)
        // otherwise, it throws runtime deserialization errors
        PaginatedResponse<?> results = doGetAggResult(disableCache, company, calc, filter, ouConfig, velocityStageStatusesMap, velocityConfigDTO, useEs);
        if (results == null || results.getResponse() == null || results.getResponse().getRecords() == null) {
            return List.of();
        }
        List<?> records = results.getResponse().getRecords();
        return mapper.convertValue(records, mapper.getTypeFactory().constructCollectionType(List.class, DbAggregationResult.class));
    }

    private PaginatedResponse<DbAggregationResult> doGetAggResult(
            Boolean disableCache,
            String company,
            JiraIssuesFilter.CALCULATION calc,
            DefaultListRequest request,
            OUConfiguration ouConfig,
            Map<String, List<String>> velocityStageStatusesMap,
            VelocityConfigDTO velocityConfigDTO,
            Boolean useEs) throws Exception {
        String customAcross = null;
        JiraIssuesFilter.DISTINCT across = JiraIssuesFilter.DISTINCT.fromString(request.getAcross());
        if (across == null) {
            across = JiraIssuesFilter.DISTINCT.assignee;
            if (StringUtils.isNotEmpty(request.getAcross()) && request.getAcross().startsWith("customfield_")) {
                across = JiraIssuesFilter.DISTINCT.custom_field;
                customAcross = request.getAcross();
            }
        }
        final List<JiraIssuesFilter.DISTINCT> stackEnumList = new ArrayList<>();
        String stackMisc = "";
        if (CollectionUtils.isNotEmpty(request.getStacks())) {
            stackEnumList.addAll(request.getStacks().stream()
                    .map(JiraIssuesFilter.DISTINCT::fromString)
                    .collect(Collectors.toList()));
            stackMisc = stackEnumList.stream().map(Enum::toString).sorted().collect(Collectors.joining(","));
        }
        Boolean shouldFetchEpicName = (Boolean) request.getFilter().getOrDefault("fetch_epic_summary",false);
        JiraIssuesFilter issuesFilter = jiraFilterParser.createFilter(company, request, calc, across, customAcross,
                request.getAggInterval(), false, false);

        List<DbAggregationResult> aggregationRecords = new ArrayList<>();
        if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, request);
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    issuesFilter = jiraFilterParser.createFilterFromConfig(company, request, row.getValue(),
                            configTable.getSchema().getColumns(), calc, across, customAcross,
                            request.getAggInterval(), false);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, request.getAcross()).getId());
                    final var finalOuConfig = ouConfig;
                    JiraIssuesFilter finalIssuesFilter = issuesFilter;
                    List<DbAggregationResult> records =
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                            "/jira/config_agg_" + stackMisc,
                                            issuesFilter.generateCacheHash() + finalOuConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                                            () -> jiraIssueService.stackedGroupBy(company, finalIssuesFilter, stackEnumList, rowValue, finalOuConfig, velocityConfigDTO, velocityStageStatusesMap))
                                    .getRecords();
                    aggregationRecords.addAll(records);
                }
        } else {
            JiraIssuesFilter finalIssuesFilter1 = issuesFilter;
            final var finalOuConfig = ouConfig;
            DbListResponse<DbAggregationResult> aggs = null;
            if (useEs && !isTicketCategorySpecified(issuesFilter, stackEnumList)) {
                Integer page = null;
                Integer pageSize = null;
                if (request.getPageSize() != 0) {
                    page = request.getPage();
                    pageSize = request.getPageSize();
                }
                log.info("doGetAggResult : Effective request after OU merge - " + issuesFilter);
                aggs = esJiraIssueQueryService.getAggReport(company, issuesFilter, stackEnumList, ouConfig, page, pageSize, false);
            } else{
                aggs = jiraIssueService.stackedGroupBy(company, finalIssuesFilter1, stackEnumList, null, finalOuConfig, velocityConfigDTO, velocityStageStatusesMap);
            }
            final DbListResponse<DbAggregationResult> aggsFinal = aggs;
            aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                                "/jira/nonconfig_agg_" + stackMisc,
                                issuesFilter.generateCacheHash() + finalOuConfig.hashCode(), issuesFilter.getIntegrationIds(), mapper, cacheService,
                                () -> {
                                    return postProcessStacks(company, finalIssuesFilter1, aggsFinal, shouldFetchEpicName);
                                })
                        .getRecords();
        }
        if ("TRUE".equals(predictionsPostProcess)) {
            log.warn("JiraIssuesController: predictionsPostProcess set to true!");
                aggregationRecords  = mapper.convertValue(aggregationRecords, new TypeReference<List<DbAggregationResult>>() {
                });
            aggregationRecords = PredictionsService.postProcess(aggregationRecords, request);
        } else {
            log.warn("JiraIssuesController: predictionsPostProcess set to false!");
        }
        return PaginatedResponse.of(request.getPage(), request.getPageSize(), aggregationRecords);
    }

    private DbListResponse<DbAggregationResult> postProcessStacks(String company, JiraIssuesFilter issuesFilter, DbListResponse<DbAggregationResult> aggs, Boolean shouldFetchEpicName) throws SQLException {
        //SEI-1108 : If shouldFetchEpicName is true, set epic name in additional_key
        List<DbAggregationResult> aggregationRecords = aggs.getRecords();
        if(BooleanUtils.isTrue(shouldFetchEpicName)){
            Set<String> epicKeys = new HashSet<>();
            aggregationRecords.stream().forEach(r -> {
                epicKeys.addAll(r.getStacks().stream().map(DbAggregationResult::getKey).filter(Objects::nonNull).collect(Collectors.toList()));
            });
            Map<String,String> epicKeyEpicNameMap = jiraIssueService.listJiraIssues(company, JiraIssuesFilter.builder()
                            .integrationIds(issuesFilter.getIntegrationIds())
                            .ingestedAtByIntegrationId(issuesFilter.getIngestedAtByIntegrationId())
                            .ingestedAt(issuesFilter.getIngestedAt())
                            .keys(epicKeys.stream().collect(Collectors.toList()))
                            .build(),null,0,10000)
                    .stream().collect(Collectors.toMap(DbJiraIssue::getKey, DbJiraIssue::getSummary,
                            (i1,i2)->i1));
            aggregationRecords = aggregationRecords.stream().map(r ->
                    r.toBuilder().stacks(r.getStacks().stream().map(s -> s.toBuilder().additionalKey(epicKeyEpicNameMap.get(s.getKey())).build()).collect(Collectors.toList())).build()
            ).collect(Collectors.toList());
        }
        return DbListResponse.of(aggregationRecords, aggregationRecords.size());
    }

    private DbListResponse<DbJiraIssue> postProcessAddEpicSummary(String company, JiraIssuesFilter issuesFilter, DbListResponse<DbJiraIssue> issues, Boolean shouldFetchEpicName) throws SQLException {
        List<DbJiraIssue> dbJiraIssues = issues.getRecords();
        if(BooleanUtils.isTrue(shouldFetchEpicName)){
            List<String> epicKeys = dbJiraIssues.stream().map(DbJiraIssue::getEpic).filter(Objects::nonNull).collect(Collectors.toList());
            Map<String,String> epicKeyEpicNameMap = jiraIssueService.listJiraIssues(company, JiraIssuesFilter.builder()
                            .integrationIds(issuesFilter.getIntegrationIds())
                            .ingestedAtByIntegrationId(issuesFilter.getIngestedAtByIntegrationId())
                            .ingestedAt(issuesFilter.getIngestedAt())
                            .keys(epicKeys.stream().collect(Collectors.toList()))
                            .build(),null,0,10000)
                    .stream().collect(Collectors.toMap(DbJiraIssue::getKey, DbJiraIssue::getSummary,
                            (i1,i2)->i1));
            dbJiraIssues = dbJiraIssues.stream().map(i -> {
                if(StringUtils.isNotEmpty(i.getEpic()) && epicKeyEpicNameMap.containsKey(i.getEpic())){
                    return i.toBuilder().epicSummary(epicKeyEpicNameMap.get(i.getEpic())).build();
                }
                return i;
            }).collect(Collectors.toList());
        }
        return DbListResponse.of(dbJiraIssues, issues.getTotalCount());
    }

    private List<DbAggregationResult> parseAggResults(List<DbAggregationResult> aggregationRecords,
                                                      List<VelocityConfigDTO.Stage> developmentCustomStages, boolean calculateSingleState) {
        if (CollectionUtils.isEmpty(developmentCustomStages) || CollectionUtils.isEmpty(aggregationRecords)) {
            return aggregationRecords;
        }
        List<DbAggregationResult> finalAggResults = new ArrayList<>();
        List<String> stageNames = aggregationRecords.stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        developmentCustomStages.forEach(stage -> {
            String aggStageName = stage.getName();
            if (stageNames.contains(aggStageName)) {
                aggregationRecords.stream()
                        .filter(aggResult -> aggResult.getKey().equalsIgnoreCase(aggStageName)).findFirst().ifPresent(agg -> finalAggResults.add(agg.toBuilder()
                                .velocityStageResult(getVelocityStageResult(stage, agg.getMedian())).build()));
            } else {
                finalAggResults.add(DbAggregationResult.builder()
                        .key(aggStageName)
                        .p95(0L).mean(0.0).median(0L).totalTickets(0L).p90(0L).min(0L).max(0L)
                        .velocityStageResult(getVelocityStageResult(stage, 0L)).build());
            }
        });
        aggregationRecords.stream()
                .filter(aggResult -> aggResult.getKey().equalsIgnoreCase("other")).findFirst()
                .ifPresent(finalAggResults::add);
        if (calculateSingleState)
            aggregationRecords.stream()
                    .filter(aggResult -> aggResult.getKey().equalsIgnoreCase("SingleState")).findFirst()
                    .ifPresent(finalAggResults::add);
        return finalAggResults;

    }

    private VelocityStageResult getVelocityStageResult(VelocityConfigDTO.Stage stage, Long median) {
        return VelocityStageResult.builder()
                .lowerLimitUnit(stage.getLowerLimitUnit())
                .lowerLimitValue(stage.getLowerLimitValue())
                .upperLimitUnit(stage.getUpperLimitUnit())
                .upperLimitValue(stage.getUpperLimitValue())
                .rating(stage.calculateRating(median))
                .build();
    }

    private ConfigTable.Column getColumn(ConfigTable configTable, String key) {
        return configTable.getSchema().getColumns().values().stream()
                .filter(column -> (column.getKey().equalsIgnoreCase(key)))
                .findAny().orElse(null);
    }

    private List<String> getCommonFilterValue(ConfigTable configTable, ConfigTable.Row row, String columnKey, List<String> filterValue) {
        ConfigTable.Column jiraProjectColumn = getColumn(configTable, columnKey);
        if (jiraProjectColumn != null) {
            return getIntersection(getRowValue(row, jiraProjectColumn), filterValue);
        }
        return filterValue;
    }

    private List<String> getIntersection(List<String> rowValues, List<String> filterValues) {
        if (rowValues.size() == 0) {
            return filterValues;
        } else if (filterValues.size() == 0) {
            return rowValues;
        } else {
            return new ArrayList<>(CollectionUtils.intersection(rowValues, filterValues));
        }
    }

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "").replaceAll("\"", "");
            return Arrays.asList(sanitizedRowValue.split(","));
        }
        return Collections.singletonList(rowValue);
    }

    private Map<String, List<String>> getOuPeople(String company, OUConfiguration ouConfig, Boolean ignoreOU) {
        Map<String, List<String>> ouUsersMap = new HashMap<>();
        if (OrgUnitHelper.isOuConfigActive(ouConfig) && BooleanUtils.isFalse(ignoreOU)) {
            if (ouConfig.getJiraFields().contains("reporter") && OrgUnitHelper.doesOUConfigHaveJiraReporters(ouConfig)) {
                ouUsersMap.put("reporters", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.JIRA));
            }
            if (ouConfig.getJiraFields().contains("assignee") && OrgUnitHelper.doesOUConfigHaveJiraAssignees(ouConfig)) {
                ouUsersMap.put("assignees", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.JIRA));
            }
            if (ouConfig.getJiraFields().contains("first_assignee") && OrgUnitHelper.doesOUConfigHaveJiraFirstAssignees(ouConfig)) {
                ouUsersMap.put("first_assignees", orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.JIRA));
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

        Boolean isDbCompany = this.dbAllowedTenants.contains(company) ||
                (this.dbAllowedTenantsApis.containsKey(reportName) &&
                        this.dbAllowedTenantsApis.get(reportName).contains(company));
        log.info("isUseEs isDBCompany={}", isDbCompany);

        //if report or company configured for both ES and DB - ignoring ES and making DB call
        if (isDbCompany) {
            return false;
        }
        return isEsCompany;
    }

    private Boolean isIssuesListForTicketCategory(JiraIssuesFilter filter) {
        return StringUtils.isNotEmpty(filter.getTicketCategorizationSchemeId()) &&
                (CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters()) || CollectionUtils.isNotEmpty(filter.getTicketCategories()));
    }
    @NotNull
    private Boolean isTicketCategorySpecified(JiraIssuesFilter filter, List<JiraIssuesFilter.DISTINCT> stackList) {
        return filter.getAcross() == JiraIssuesFilter.DISTINCT.ticket_category || stackList.contains(JiraIssuesFilter.DISTINCT.ticket_category) ||
                StringUtils.isNotEmpty(filter.getTicketCategorizationSchemeId()) ||
                CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters()) || CollectionUtils.isNotEmpty(filter.getTicketCategories());
    }

}
