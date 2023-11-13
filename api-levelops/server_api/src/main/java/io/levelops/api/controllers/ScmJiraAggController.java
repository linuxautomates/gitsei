package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmIssueMgmtService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/scm_jira")
@SuppressWarnings("unused")
public class ScmJiraAggController {
    public static final boolean USE_INTEGRATION_PREFIX = true;
    
    private final ScmJiraZendeskService aggService;
    private final ScmIssueMgmtService scmIssueMgmtService;
    private final JiraFilterParser jiraFilterParser;
    private final AggCacheService aggCacheService;
    private final ObjectMapper mapper;
    private final OrgUnitHelper ouHelper;

    @Autowired
    public ScmJiraAggController(ScmJiraZendeskService aggService,
                                ScmIssueMgmtService scmIssueMgmtService,
                                JiraFilterParser jiraFilterParser,
                                AggCacheService aggCacheService,
                                ObjectMapper objectMapper,
                                final OrgUnitHelper ouHelper) {
        this.mapper = objectMapper;
        this.aggService = aggService;
        this.aggCacheService = aggCacheService;
        this.jiraFilterParser = jiraFilterParser;
        this.scmIssueMgmtService = scmIssueMgmtService;
        this.ouHelper = ouHelper;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmFile>>> filesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                var types = new HashSet<IntegrationType>(IntegrationType.getIssueManagementIntegrationTypes());
                types.addAll(IntegrationType.getSCMIntegrationTypes());
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, types, originalRequest, USE_INTEGRATION_PREFIX);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/velocity/' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("scm_file_committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap = scmFilesPartialMatchMap(request);
            validatePartialMatchFilter(company, partialMatchMap);
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            var scmRepoIds = getListOrDefault(request.getFilter(), "scm_file_repo_ids");
            if(CollectionUtils.isEmpty(scmRepoIds)) {
                scmRepoIds = getListOrDefault(request.getFilter(), "scm_repo_ids");
            }
            ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(CollectionUtils.isNotEmpty(scmRepoIds) ? scmRepoIds : getListOrDefault(request.getFilter(), "repo_ids")) // 'if' introduced as a hacky fix for OU.. short lived... 
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("scm_file_filename", null))
                    .module((String) request.getFilter().getOrDefault("scm_module", null))
                    .partialMatch(partialMatchMap)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm_jira/files/list_" + request.getPage() + "_" + request.getPageSize() + "_"
                                            + "_" + getHash("sort", sorting),
                                    filesFilter.generateCacheHash() + "_" + jiraFilter.generateCacheHash() + finalOuConfig.hashCode(),
                                    integrationIds, mapper, aggCacheService,
                                    () -> scmIssueMgmtService.listScmFiles(company,
                                            filesFilter,
                                            jiraFilter,
                                            sorting,
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> filesReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                var types = new HashSet<IntegrationType>(IntegrationType.getIssueManagementIntegrationTypes());
                types.addAll(IntegrationType.getSCMIntegrationTypes());
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, types, originalRequest, USE_INTEGRATION_PREFIX);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/velocity/' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("scm_file_committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap = scmFilesPartialMatchMap(request);
            validatePartialMatchFilter(company, partialMatchMap);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            var scmRepoIds = getListOrDefault(request.getFilter(), "scm_file_repo_ids");
            if(CollectionUtils.isEmpty(scmRepoIds)) {
                scmRepoIds = getListOrDefault(request.getFilter(), "scm_repo_ids");
            }
            ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(CollectionUtils.isNotEmpty(scmRepoIds) ? scmRepoIds : getListOrDefault(request.getFilter(), "repo_ids")) // 'if' introduced as a hacky fix for OU.. short lived... 
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("scm_file_filename", null))
                    .module((String) request.getFilter().getOrDefault("scm_module", null))
                    .listFiles((Boolean) request.getFilter().getOrDefault("list_files", true))
                    .partialMatch(partialMatchMap)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm_jira/files/report_" + "_" + getHash("sort", sorting),
                                    filesFilter.generateCacheHash() + "_" + jiraFilter.generateCacheHash() + finalOuConfig.hashCode(),
                                    integrationIds, mapper, aggCacheService,
                                    () -> scmIssueMgmtService.listScmModules(company,
                                            filesFilter,
                                            jiraFilter,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/pr/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmPullRequest>>> prListForSprint(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                var types = new HashSet<IntegrationType>(IntegrationType.getIssueManagementIntegrationTypes());
                types.addAll(IntegrationType.getSCMIntegrationTypes());
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, types, originalRequest, USE_INTEGRATION_PREFIX);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/velocity/' for the request: {}", company, originalRequest, e);
            }

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request,
                    MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.assignee),
                    ScmPrFilter.CALCULATION.count);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company, "scm_jira/pr/list" + "_" + scmPrFilter.generateCacheHash()
                            + "_" + jiraFilter.generateCacheHash(), scmPrFilter.generateCacheHash() + finalOuConfig.hashCode(),
                            scmPrFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> aggService.listPRsForJiraSprint(company,
                                    jiraFilter,
                                    scmPrFilter,
                                    finalOuConfig,
                                    page,
                                    pageSize))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/commits", produces = "application/json")
    public DeferredResult<ResponseEntity<List<DbScmCommit>>> listJiraCommits(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "issue_key", required = true) String issueKey) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    AggCacheUtils.cacheOrCallGeneric(disableCache, company, "scm_jira/commits", issueKey,
                    List.of(), mapper, aggCacheService, List.class, 1l, TimeUnit.DAYS,
                            () -> scmIssueMgmtService.getScmCommitsForJira(company, issueKey)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/prs", produces = "application/json")
    public DeferredResult<ResponseEntity<List<DbScmPullRequest>>> listJiraPRs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "issue_key", required = true) String issueKey) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    AggCacheUtils.cacheOrCallGeneric(disableCache, company, "scm_jira/prs", issueKey,
                            List.of(), mapper, aggCacheService, List.class, 1l, TimeUnit.DAYS,
                            () ->scmIssueMgmtService.getScmPRsForJira(company, issueKey)));
        });
    }

        private void validatePartialMatchFilter(String company,
                                                Map<String, Map<String, String>> partialMatchMap) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> !ScmAggService.FILES_PARTIAL_MATCH_COLUMNS.contains(key))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for scm file partial match based filter");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, String>> scmFilesPartialMatchMap(DefaultListRequest filter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        return partialMatchMap.entrySet().stream()
                .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                .collect(Collectors.toMap(
                        stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                        Map.Entry::getValue));
    }

    public StringBuilder getHash(String fieldName, Map<String, ?> map) {
        StringBuilder dataToHash = new StringBuilder();
        hashDataMapOfStrings(dataToHash, fieldName, map);
        return dataToHash;
    }
}
