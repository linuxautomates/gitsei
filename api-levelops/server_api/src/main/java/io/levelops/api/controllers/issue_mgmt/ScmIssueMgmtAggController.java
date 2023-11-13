package io.levelops.api.controllers.issue_mgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import io.levelops.api.services.IngestedAtCachingService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmIssueMgmtService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.utils.IssueMgmtUtil;
import io.levelops.commons.databases.utils.LatestIngestedAt;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/scm_issue_mgmt")
@SuppressWarnings("unused")
public class ScmIssueMgmtAggController {
    private final ScmIssueMgmtService aggService;
    private final AggCacheService aggCacheService;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final ObjectMapper mapper;
    private final IntegrationService integrationService;
    private final LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache;
    private final OrgUnitHelper ouHelper;

    private final ScmIssueMgmtService scmIssueMgmtService;

    @Autowired
    public ScmIssueMgmtAggController(ScmIssueMgmtService aggService,
                                     AggCacheService aggCacheService,
                                     TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService,
                                     ObjectMapper objectMapper, IntegrationService integrationService, ScmIssueMgmtService scmIssueMgmtService,
                                     IngestedAtCachingService ingestedAtCachingService,
                                     final OrgUnitHelper ouHelper) {
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        this.mapper = objectMapper;
        this.aggService = aggService;
        this.aggCacheService = aggCacheService;
        this.integrationService = integrationService;
        this.scmIssueMgmtService = scmIssueMgmtService;
        this.ingestedAtCache = ingestedAtCachingService.getIngestedAtCache();
        this.ouHelper = ouHelper;
    }

    @SuppressWarnings("unchecked")
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
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/' for the request: {}", company, originalRequest, e);
            }
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("scm_file_committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap = IssueMgmtUtil.scmFilesPartialMatchMap(request);
            IssueMgmtUtil.validatePartialMatchFilter(company, partialMatchMap);
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
                    .excludeRepoIds(getListOrDefault(excludedFields, "scm_file_repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("scm_file_filename", null))
                    .module((String) request.getFilter().getOrDefault("scm_module", null))
                    .partialMatch(partialMatchMap)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request,
                    WorkItemsFilter.DISTINCT.fromString(request.getAcross()),
                    WorkItemsFilter.CALCULATION.story_point_report);
            LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
            WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                    .ingestedAt(latestIngestedAt.getLatestIngestedAt())
                    .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                    .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                            workItemsTempFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService)).build();
            workItemsFilter = IssueMgmtUtil.resolveAcross(request, workItemsFilter);
            WorkItemsFilter finalWorkItemsFilter = workItemsFilter;

            var page = request.getPage();
            var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm_issue_mgmt/files/list_" + request.getPage() + "_" + request.getPageSize() + "_"
                                            + "_" + sortHash,
                                    filesFilter.generateCacheHash() + "_" + finalWorkItemsFilter.generateCacheHash() + "_" + sprintFilter.generateCacheHash(),
                                    integrationIds, mapper, aggCacheService,
                                    () -> aggService.listIssueMgmtScmFiles(company,
                                            filesFilter, finalWorkItemsFilter,
                                            sprintFilter,
                                            sorting,
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
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
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/' for the request: {}", company, originalRequest, e);
            }
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("scm_file_committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap = IssueMgmtUtil.scmFilesPartialMatchMap(request);
            IssueMgmtUtil.validatePartialMatchFilter(company, partialMatchMap);
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var scmRepoIds = getListOrDefault(request.getFilter(), "scm_file_repo_ids");
            if(CollectionUtils.isEmpty(scmRepoIds)) {
                scmRepoIds = getListOrDefault(request.getFilter(), "scm_repo_ids");
            }
            ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(CollectionUtils.isNotEmpty(scmRepoIds) ? scmRepoIds : getListOrDefault(request.getFilter(), "repo_ids")) // 'if' introduced as a hacky fix for OU.. short lived... 
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "scm_file_repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("scm_file_filename", null))
                    .module((String) request.getFilter().getOrDefault("scm_module", null))
                    .listFiles((Boolean) request.getFilter().getOrDefault("list_files", true))
                    .partialMatch(partialMatchMap)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            WorkItemsFilter workItemsTempFilter = WorkItemsFilter.fromDefaultListRequest(request,
                    WorkItemsFilter.DISTINCT.fromString(request.getAcross()),
                    WorkItemsFilter.CALCULATION.story_point_report);
            LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsTempFilter, integrationService, ingestedAtCache);
            WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            WorkItemsFilter workItemsFilter = workItemsTempFilter.toBuilder()
                    .ingestedAt(latestIngestedAt.getLatestIngestedAt())
                    .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                    .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                            workItemsTempFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService)).build();
            workItemsFilter = IssueMgmtUtil.resolveAcross(request, workItemsFilter);
            WorkItemsFilter finalWorkItemsFilter = workItemsFilter;
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm_issue_mgmt/files/report_" + "_" + sortHash,
                                    filesFilter.generateCacheHash() + "_" + finalWorkItemsFilter.generateCacheHash() + "_" + sprintFilter.generateCacheHash(),
                                    integrationIds, mapper, aggCacheService,
                                    () -> aggService.listIssueMgmtScmModules(company,
                                            filesFilter,
                                            finalWorkItemsFilter,
                                            sprintFilter,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/commits", produces = "application/json")
    public DeferredResult<ResponseEntity<List<DbScmCommit>>> listJiraCommits(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "issue_key", required = true) String issueKey) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    AggCacheUtils.cacheOrCallGeneric(disableCache, company, "scm_issue_mgmt/commits", issueKey,
                            List.of(), mapper, aggCacheService, List.class, 1l, TimeUnit.DAYS,
                            () -> scmIssueMgmtService.getScmCommitsForWorkItem(company, issueKey)));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/prs", produces = "application/json")
    public DeferredResult<ResponseEntity<List<DbScmPullRequest>>> listJiraPRs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "issue_key", required = true) String issueKey) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    AggCacheUtils.cacheOrCallGeneric(disableCache, company, "scm_issue_mgmt/prs", issueKey,
                            List.of(), mapper, aggCacheService, List.class, 1l, TimeUnit.DAYS,
                            () -> scmIssueMgmtService.getScmPRsForWorkItem(company, issueKey)));
        });
    }
}
