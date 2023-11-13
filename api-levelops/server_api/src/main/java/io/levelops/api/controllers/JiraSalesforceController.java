package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.api.utils.SalesforceCaseFilterUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitSalesforce;
import io.levelops.commons.databases.models.database.combined.SalesforceWithJira;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraSalesforceService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.VCS_TYPE.parseFromFilter;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/jira_salesforce")
@SuppressWarnings("unused")
public class JiraSalesforceController {
    private final IntegrationService integService;
    private final JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
    private final JiraSalesforceService jiraSalesforceService;
    private final IntegrationTrackingService integrationTrackingService;
    private final JiraFilterParser jiraFilterParser;
    private final AggCacheService aggCacheService;
    private final ObjectMapper objectMapper;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public JiraSalesforceController(IntegrationService integrationService,
                                    JiraFieldConditionsBuilder jiraFieldConditionsBuilder,
                                    JiraSalesforceService jiraSalesforceService,
                                    IntegrationTrackingService integTService,
                                    JiraFilterParser jiraFilterParser,
                                    AggCacheService aggCacheService,
                                    ObjectMapper objectMapper,
                                    final OrgUnitHelper orgUnitHelper) {
        this.jiraFieldConditionsBuilder = jiraFieldConditionsBuilder;
        this.jiraSalesforceService = jiraSalesforceService;
        this.integService = integrationService;
        this.integrationTrackingService = integTService;
        this.jiraFilterParser = jiraFilterParser;
        this.aggCacheService = aggCacheService;
        this.objectMapper = objectMapper;
        this.orgUnitHelper = orgUnitHelper;
    }

    private Long getIngestedAt(String company, IntegrationType type, DefaultListRequest filter)
            throws SQLException {
        Integration integ = integService.listByFilter(company, null, List.of(type.toString()), null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(x -> NumberUtils.toInt(x))
                        .collect(Collectors.toList()),
                List.of(), 0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/agg", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> filesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/agg' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
//            Map<String, String> sfCreatedRange = filter.getFilterValue("salesforce_created_at", Map.class)
//                    .orElse(Map.of());
//            Map<String, String> sfUpdatedRange = filter.getFilterValue("salesforce_updated_at", Map.class)
//                    .orElse(Map.of());
//            final Long sfCreateStart = sfCreatedRange.get("$gt") != null ? Long.valueOf(sfCreatedRange.get("$gt")) : null;
//            final Long sfCreateEnd = sfCreatedRange.get("$lt") != null ? Long.valueOf(sfCreatedRange.get("$lt")) : null;
//            final Long sfUpdateStart = sfUpdatedRange.get("$gt") != null ? Long.valueOf(sfUpdatedRange.get("$gt")) : null;
//            final Long sfUpdateEnd = sfUpdatedRange.get("$lt") != null ? Long.valueOf(sfUpdatedRange.get("$lt")) : null;
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            Boolean getCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_get_commit",
                    false));
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            DbListResponse<DbAggregationResult> aggResult = null;

            String across = ObjectUtils.firstNonNull(request.getAcross(), "trend");
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));

            var finalOuConfig = ouConfig;
            if (across.equalsIgnoreCase("jira_status"))
                aggResult = AggCacheUtils.cacheOrCall(disableCache, company,
                        "/jira_salesforce/files_" + request.getAcross() + "_" + (withCommits || getCommits)
                                + "_" + getCommits + "_" + "_" + getHash("sort", sorting),
                        jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                        integrationIds, objectMapper, aggCacheService,
                        () -> jiraSalesforceService.groupJiraTicketsByStatus(
                                company,
                                jiraFilter,
                                salesforceCaseFilter,
                                integrationIds,
                                withCommits || getCommits,
                                getCommits,
                                sorting,
                                finalOuConfig));
            else
                aggResult = AggCacheUtils.cacheOrCall(disableCache, company,
                        "/jira_salesforce/files_" + across + "_" + getHash("sort", sorting),
                        jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                        integrationIds, objectMapper, aggCacheService,
                        () -> jiraSalesforceService.groupSalesforceCasesWithJiraLinks(
                                company,
                                jiraFilter,
                                salesforceCaseFilter,
                                sorting,
                                finalOuConfig));

            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            aggResult));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/escalation_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketEscalationTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/escalation_time_report' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request,
                    getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/jira_salesforce/escalation_time_report" + "_" + getHash("sort", sorting),
                            jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                            integrationIds, objectMapper, aggCacheService,
                            () -> jiraSalesforceService.getCaseEscalationTimeReport(company, jiraFilter, salesforceCaseFilter, sorting, finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getRootModuleReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Map<String, String>> scmFilePartialmatch = partialMatchMap.entrySet().stream()
                    .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                    .collect(Collectors.toMap(
                            stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                            Map.Entry::getValue));
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmFilesFilter scmFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .listFiles((Boolean) request.getFilter().getOrDefault("list_files", true))
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .partialMatch(scmFilePartialmatch)
                    .build();
            JiraIssuesFilter jiraFilter =
                    jiraFilterParser.createFilter(company, request, null, null, null, null, true);

            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/jira_salesforce/files_report" + "_" + getHash("sort", sorting),
                            scmFilter.generateCacheHash() + "_" + jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                            integrationIds, objectMapper, aggCacheService,
                            () -> jiraSalesforceService.filesReport(
                                    company, scmFilter, jiraFilter, salesforceCaseFilter, sorting, finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/resolved_tickets_trend", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolvedTicketsTrend(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/resolved_tickets_trend' for the request: {}", company, originalRequest, e);
            }
            Instant now = Instant.now();
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            SalesforceCaseFilter salesforceCaseFilterTmp = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            if (salesforceCaseFilterTmp.getSFCreatedRange() == null || salesforceCaseFilterTmp.getSFCreatedRange().getLeft() == null
                    || salesforceCaseFilterTmp.getSFUpdatedRange() == null || salesforceCaseFilterTmp.getSFUpdatedRange().getLeft() == null) {
                salesforceCaseFilterTmp = salesforceCaseFilterTmp.toBuilder()
                        .SFCreatedRange(ImmutablePair.of(now.minus(Duration.ofDays(7)).getEpochSecond(), ((salesforceCaseFilterTmp.getSFCreatedRange() != null) && (salesforceCaseFilterTmp.getSFCreatedRange().getRight() != null)) ? salesforceCaseFilterTmp.getSFCreatedRange().getRight() : now.getEpochSecond()))
                        .SFUpdatedRange(ImmutablePair.of(now.minus(Duration.ofDays(7)).getEpochSecond(), ((salesforceCaseFilterTmp.getSFUpdatedRange() != null) && (salesforceCaseFilterTmp.getSFUpdatedRange().getRight() != null)) ? salesforceCaseFilterTmp.getSFUpdatedRange().getRight() : now.getEpochSecond()))
                        .build();
            }
            final SalesforceCaseFilter sfFilter = salesforceCaseFilterTmp;
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/jira_salesforce/resolved_tickets_trend_" + withCommits + "_" + getHash("sort", sorting),
                            jiraFilter.generateCacheHash() + "_" + sfFilter.generateCacheHash(),
                            integrationIds, objectMapper, aggCacheService,
                            () -> jiraSalesforceService.resolvedTicketsTrendReport(
                                    company, jiraFilter, sfFilter, integrationIds, withCommits, sorting, finalOuConfig))
            ));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_salesforce", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<SalesforceWithJira>>> listJiraTicketsForSFGroup(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/list_salesforce' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            if (sorting.containsKey("escalation_time")) {
                return ResponseEntity.ok(
                        PaginatedResponse.of(
                                request.getPage(),
                                request.getPageSize(),
                                AggCacheUtils.cacheOrCall(disableCache, company,
                                        "/jira_salesforce/list_salesforce/escalation_" + sorting.get("escalation_time")
                                                + "_" + request.getPage() + "_" + request.getPageSize() + "_" + getHash("sort", sorting),
                                        jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                                        integrationIds, objectMapper, aggCacheService,
                                        () -> jiraSalesforceService.listCasesWithEscalationTime(company,
                                                jiraFilter,
                                                salesforceCaseFilter,
                                                page,
                                                pageSize,
                                                sorting,
                                                finalOuConfig))));
            } else {
                return ResponseEntity.ok(
                        PaginatedResponse.of(
                                page,
                                pageSize,
                                AggCacheUtils.cacheOrCall(disableCache, company,
                                        "/jira_salesforce/list_salesforce/non_esc_"
                                                + page + "_" + pageSize + "_" + getHash("sort", sorting),
                                        jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                                        integrationIds, objectMapper, aggCacheService,
                                        () -> jiraSalesforceService.listSalesforceCases(
                                                company, jiraFilter, salesforceCaseFilter, page, pageSize, sorting, finalOuConfig))));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_jira", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraWithGitSalesforce>>> listJiraTickets(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/list_jira' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_salesforce/list_jira_"
                                            + withCommits + "_" + page + "_" + pageSize + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> jiraSalesforceService.listJiraTickets(company,
                                            jiraFilter,
                                            salesforceCaseFilter,
                                            integrationIds,
                                            withCommits,
                                            page,
                                            pageSize, sorting, finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_commit", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CommitWithJira>>> listCommits(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/list_commit' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_salesforce/list_commit_" + page
                                            + "_" + pageSize + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> jiraSalesforceService.listCommits(company,
                                            jiraFilter,
                                            salesforceCaseFilter,
                                            integrationIds,
                                            page,
                                            pageSize,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmFile>>> listFiles(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/files/list' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Map<String, String>> scmFilePartialmatch = partialMatchMap.entrySet().stream()
                    .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                    .collect(Collectors.toMap(
                            stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                            Map.Entry::getValue));
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmFilesFilter scmFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .partialMatch(scmFilePartialmatch)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_salesforce/list_files_" + page
                                            + "_" + pageSize + "_" + getHash("sort", sorting),
                                    scmFilter.generateCacheHash() + "_" + jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> jiraSalesforceService.listFiles(company,
                                            scmFilter,
                                            jiraFilter,
                                            salesforceCaseFilter,
                                            page,
                                            pageSize,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/top_committers", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> topCommitters(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.SALESFORCE), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_salesforce/top_committers' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilterUtils.buildFilter(company, request, getIngestedAt(company, IntegrationType.SALESFORCE, request));
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder()
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .vcsTypes(parseFromFilter(request.getFilter()))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .authors(getListOrDefault(request.getFilter(), "authors"))
                    .committers(getListOrDefault(request.getFilter(), "committers"))
                    .commitShas(getListOrDefault(request.getFilter(), "commit_shas"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                    .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                    .excludeCommitShas(getListOrDefault(excludedFields, "commit_shas"))
                    .build();
            ScmFilesFilter scmFilesFilter = ScmFilesFilter.builder()
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_salesforce/top_committers" + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + salesforceCaseFilter.generateCacheHash() + "_" + scmCommitFilter.generateCacheHash() + "_" + scmFilesFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> jiraSalesforceService.getTopCommitters(company,
                                            jiraFilter,
                                            salesforceCaseFilter,
                                            scmCommitFilter,
                                            scmFilesFilter,
                                            integrationIds,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    public void validateFieldSizeFilter(String company, List<String> integrationIds, Map<String, Map<String, String>> fieldSizes) {
        if (MapUtils.isEmpty(fieldSizes))
            return;
        ArrayList<String> fieldSizeKeys = new ArrayList<>(fieldSizes.keySet());
        try {
            String unknownField = jiraFieldConditionsBuilder.checkFieldsPresent(company, integrationIds, fieldSizeKeys);
            if (unknownField != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unknownField + " is not valid field for size based filter");
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while fetching list of jira fields");
        }
    }

    public StringBuilder getHash(String fieldName, Map<String, ?> map) {
        StringBuilder dataToHash = new StringBuilder();
        hashDataMapOfStrings(dataToHash, fieldName, map);
        return dataToHash;
    }
}
