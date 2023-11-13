package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.utils.ForceSourceUtils;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabelLite;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/scm")
public class ScmAggsController {
    private final ScmAggService aggService;
    private final AggCacheService aggCacheService;
    private final ObjectMapper mapper;
    private final OrgUnitHelper orgUnitHelper;
    private final Executor dbValuesTaskExecutor;
    private final EsScmCommitsService esScmCommitsService;
    private final EsScmPRsService esScmPRsService;

    private static final String ES_COMMIT_AGG = "commit_agg";
    private static final String ES_COMMIT_VALUES = "commit_values";
    private static final String ES_COMMIT_LIST = "commit_list";
    private static final String ES_COMMIT_REWORK = "commit_rework";
    private static final String ES_COMMIT_CODING_DAY = "commit_coding_day";
    private static final String ES_COMMIT_PER_CODING_DAY = "commit_per_coding_day";
    private static final String ES_COMMITTER_LIST = "committer_list";
    private static final String ES_CONTRIBUTOR_LIST = "contributor_list";
    private static final String ES_FILE_LIST = "file_list";
    private static final String ES_FILE_REPORT = "file_report";
    private static final String ES_FILE_TYPE_LIST = "file_type_list";

    //DB config
    @Value("${DB_SCM:}")
    List<String> dbScmCompanies;
    @Value("${DB_SCM_commit_agg_report:}")
    List<String> dbScmCommitAggReportCompanies;
    @Value("${DB_SCM_commit_values_report:}")
    List<String> dbScmCommitValuesReportCompanies;
    @Value("${DB_SCM_commit_list_report:}")
    List<String> dbScmCommitListReportCompanies;
    @Value("${DB_SCM_commit_rework_report:}")
    List<String> dbScmCommitReworkReportCompanies;
    @Value("${DB_SCM_commit_coding_days_report:}")
    List<String> dbScmCommitCodingDayReportCompanies;
    @Value("${DB_SCM_commit_commits_per_coding_day:}")
    List<String> dbScmCommitPerCodingDayReportCompanies;
    @Value("${DB_SCM_committer_list:}")
    List<String> dbScmCommitterListReportCompanies;
    @Value("${DB_SCM_contributor_list:}")
    List<String> dbScmContributorListReportCompanies;
    @Value("${DB_SCM_files_list:}")
    List<String> dbScmFilesListReportCompanies;
    @Value("${DB_SCM_files_report:}")
    List<String> dbScmFilesReportCompanies;
    @Value("${DB_SCM_files_types_list:}")
    List<String> dbScmFileTypeListReportCompanies;

    @Value("${DB_SCM_pr_agg_report:}")
    List<String> dbScmPRAggReportCompanies;
    @Value("${DB_SCM_pr_values_report:}")
    List<String> dbScmPRValuesReportCompanies;
    @Value("${DB_SCM_pr_list_report:}")
    List<String> dbScmPRListReportCompanies;
    @Value("${DB_SCM_pr_author_response_time:}")
    List<String> dbScmPRAuthorResponseTimeReportCompanies;
    @Value("${DB_SCM_pr_reviewer_response_time:}")
    List<String> dbScmPRReviewerResponseTimeReportCompanies;
    @Value("${DB_SCM_pr_resolution_time:}")
    List<String> dbScmPRResolutionTimeReportCompanies;
    @Value("${DB_SCM_pr_merge_trend:}")
    List<String> dbScmPRMergeTrendReportCompanies;
    @Value("${DB_SCM_pr_first_review_trend:}")
    List<String> dbScmPRFirstReviewTrendReportCompanies;
    @Value("${DB_SCM_pr_first_review_to_merge_trend:}")
    List<String> dbScmPRFirstReviewToMergeTrendReportCompanies;
    @Value("${DB_SCM_pr_collab_report:}")
    List<String> dbScmPRCollabReportCompanies;

    private Set<String> esAllowedTenants;
    private Map<String, Set<String>> esAllowedTenantsApis;
    private Set<String> dbAllowedTenants;
    private Map<String, Set<String>> dbAllowedTenantsApis;

    // ES config
    @Value("${ES_SCM:}")
    List<String> scmCompanies;
    @Value("${ES_SCM_commit_agg_report:}")
    List<String> scmCommitAggReportCompanies;
    @Value("${ES_SCM_commit_values_report:}")
    List<String> scmCommitValuesReportCompanies;
    @Value("${ES_SCM_commit_list_report:}")
    List<String> scmCommitListReportCompanies;
    @Value("${ES_SCM_commit_rework_report:}")
    List<String> scmCommitReworkReportCompanies;
    @Value("${ES_SCM_commit_coding_days_report:}")
    List<String> scmCommitCodingDayReportCompanies;
    @Value("${ES_SCM_commit_commits_per_coding_day:}")
    List<String> scmCommitPerCodingDayReportCompanies;
    @Value("${ES_SCM_committer_list:}")
    List<String> scmCommitterListReportCompanies;
    @Value("${ES_SCM_contributor_list:}")
    List<String> scmContributorListReportCompanies;
    @Value("${ES_SCM_files_list:}")
    List<String> scmFilesListReportCompanies;
    @Value("${ES_SCM_files_report:}")
    List<String> scmFilesReportCompanies;
    @Value("${ES_SCM_files_types_list:}")
    List<String> scmFileTypeListReportCompanies;

    @Value("${ES_SCM_pr_agg_report:}")
    List<String> scmPRAggReportCompanies;
    @Value("${ES_SCM_pr_values_report:}")
    List<String> scmPRValuesReportCompanies;
    @Value("${ES_SCM_pr_list_report:}")
    List<String> scmPRListReportCompanies;
    @Value("${ES_SCM_pr_author_response_time:}")
    List<String> scmPRAuthorResponseTimeReportCompanies;
    @Value("${ES_SCM_pr_reviewer_response_time:}")
    List<String> scmPRReviewerResponseTimeReportCompanies;
    @Value("${ES_SCM_pr_resolution_time:}")
    List<String> scmPRResolutionTimeReportCompanies;
    @Value("${ES_SCM_pr_merge_trend:}")
    List<String> scmPRMergeTrendReportCompanies;
    @Value("${ES_SCM_pr_first_review_trend:}")
    List<String> scmPRFirstReviewTrendReportCompanies;
    @Value("${ES_SCM_pr_first_review_to_merge_trend:}")
    List<String> scmPRFirstReviewToMergeTrendReportCompanies;
    @Value("${ES_SCM_pr_collab_report:}")
    List<String> scmPRCollabReportCompanies;


    private static final String ES_PR_AGG = "pr_agg";
    private static final String ES_PR_VALUES = "pr_values";
    private static final String ES_PR_LIST = "pr_list";
    private static final String ES_PR_AUTHOR_RESP_TIME = "pr_author_response_time";
    private static final String ES_PR_REVIEWER_RESP_TIME = "pr_reviewer_response_time";
    private static final String ES_PR_RESOLUTION_TIME_REPORT = "pr_resolution_time_report";
    private static final String ES_PR_MERGE_TREND = "pr_merge_trend";
    private static final String ES_PR_FIRST_REVIEW_TREND = "pr_first_review_trend";
    private static final String ES_PR_FIRST_REVIEW_TO_MERGE_TREND = "pr_first_review_to_merge_trend";
    private static final String ES_PR_COLLAB_REPORT = "pr_collab_report";
    @Value("${SCM_LONGER_CACHE_TENANTS:}")
    List<String> longerCacheTimesForScmTenants;

    private static final Long LONGER_CACHE_EXPIRY_VALUE = 480L;
    private static final TimeUnit LONGER_CACHE_EXPIRY_UNIT= TimeUnit.MINUTES;


    @Autowired
    public ScmAggsController(ScmAggService aggService,
                             EsScmCommitsService esScmCommitsService,
                             EsScmPRsService esScmPRsService,
                             AggCacheService aggCacheService,
                             ObjectMapper objectMapper,
                             final OrgUnitHelper orgUnitHelper,
                             @Qualifier("dbValuesTaskExecutor") final Executor dbValuesTaskExecutor) {
        this.aggService = aggService;
        this.esScmCommitsService = esScmCommitsService;
        this.esScmPRsService = esScmPRsService;
        this.aggCacheService = aggCacheService;
        this.mapper = objectMapper;
        this.orgUnitHelper = orgUnitHelper;
        this.dbValuesTaskExecutor = dbValuesTaskExecutor;
    }

    @PostConstruct
    public void esConfig() {
        this.esAllowedTenants = new HashSet<>();
        this.esAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(scmCompanies)) {
            this.esAllowedTenants.addAll( scmCompanies);
        }
        if (CollectionUtils.isNotEmpty(scmCommitAggReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_AGG, Set.copyOf(scmCommitAggReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitValuesReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_VALUES, Set.copyOf(scmCommitValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitListReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_LIST, Set.copyOf(scmCommitListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitReworkReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_REWORK, Set.copyOf(scmCommitReworkReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitCodingDayReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_CODING_DAY, Set.copyOf(scmCommitCodingDayReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitPerCodingDayReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMIT_PER_CODING_DAY, Set.copyOf(scmCommitPerCodingDayReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmCommitterListReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_COMMITTER_LIST, Set.copyOf(scmCommitterListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmContributorListReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_CONTRIBUTOR_LIST, Set.copyOf(scmContributorListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmFilesListReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_FILE_LIST, Set.copyOf(scmFilesListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmFilesReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_FILE_REPORT, Set.copyOf(scmFilesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(scmFileTypeListReportCompanies)) {
            this.esAllowedTenantsApis.put(ES_FILE_TYPE_LIST, Set.copyOf(scmFileTypeListReportCompanies));
        }
        //prs config
        if(CollectionUtils.isNotEmpty(scmPRAggReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_AGG, Set.copyOf(scmPRAggReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRValuesReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_VALUES, Set.copyOf(scmPRValuesReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRListReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_LIST, Set.copyOf(scmPRListReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRAuthorResponseTimeReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_AUTHOR_RESP_TIME, Set.copyOf(scmPRAuthorResponseTimeReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRReviewerResponseTimeReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_REVIEWER_RESP_TIME, Set.copyOf(scmPRReviewerResponseTimeReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRResolutionTimeReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_RESOLUTION_TIME_REPORT, Set.copyOf(scmPRResolutionTimeReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRMergeTrendReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_MERGE_TREND, Set.copyOf(scmPRMergeTrendReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRFirstReviewTrendReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_FIRST_REVIEW_TREND, Set.copyOf(scmPRFirstReviewTrendReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRFirstReviewToMergeTrendReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_FIRST_REVIEW_TO_MERGE_TREND, Set.copyOf(scmPRFirstReviewToMergeTrendReportCompanies));
        }
        if(CollectionUtils.isNotEmpty(scmPRCollabReportCompanies)){
            this.esAllowedTenantsApis.put(ES_PR_COLLAB_REPORT, Set.copyOf(scmPRCollabReportCompanies));
        }

        dbScmConfig();
    }

    private void dbScmConfig() {
        this.dbAllowedTenants = new HashSet<>();
        this.dbAllowedTenantsApis = new HashMap<>();

        if (CollectionUtils.isNotEmpty(dbScmCompanies)) {
            this.dbAllowedTenants.addAll(dbScmCompanies);
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitAggReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_AGG, Set.copyOf(dbScmCommitAggReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitValuesReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_VALUES, Set.copyOf(dbScmCommitValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_LIST, Set.copyOf(dbScmCommitListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitReworkReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_REWORK, Set.copyOf(dbScmCommitReworkReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitCodingDayReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_CODING_DAY, Set.copyOf(dbScmCommitCodingDayReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitPerCodingDayReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMIT_PER_CODING_DAY, Set.copyOf(dbScmCommitPerCodingDayReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmCommitterListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_COMMITTER_LIST, Set.copyOf(dbScmCommitterListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmContributorListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_CONTRIBUTOR_LIST, Set.copyOf(dbScmContributorListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmFilesListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_FILE_LIST, Set.copyOf(dbScmFilesListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmFilesReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_FILE_REPORT, Set.copyOf(dbScmFilesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmFileTypeListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_FILE_TYPE_LIST, Set.copyOf(dbScmFileTypeListReportCompanies));
        }
        //prs config
        if (CollectionUtils.isNotEmpty(dbScmPRAggReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_AGG, Set.copyOf(dbScmPRAggReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRValuesReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_VALUES, Set.copyOf(dbScmPRValuesReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRListReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_LIST, Set.copyOf(dbScmPRListReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRAuthorResponseTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_AUTHOR_RESP_TIME, Set.copyOf(dbScmPRAuthorResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRReviewerResponseTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_REVIEWER_RESP_TIME, Set.copyOf(dbScmPRReviewerResponseTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRResolutionTimeReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_RESOLUTION_TIME_REPORT, Set.copyOf(dbScmPRResolutionTimeReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRMergeTrendReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_MERGE_TREND, Set.copyOf(dbScmPRMergeTrendReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRFirstReviewTrendReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_FIRST_REVIEW_TREND, Set.copyOf(dbScmPRFirstReviewTrendReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRFirstReviewToMergeTrendReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_FIRST_REVIEW_TO_MERGE_TREND, Set.copyOf(dbScmPRFirstReviewToMergeTrendReportCompanies));
        }
        if (CollectionUtils.isNotEmpty(dbScmPRCollabReportCompanies)) {
            this.dbAllowedTenantsApis.put(ES_PR_COLLAB_REPORT, Set.copyOf(dbScmPRCollabReportCompanies));
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmPullRequest>>> prsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {

        return SpringUtils.deferResponse(() -> {
            Boolean useEs = isUseEs(company, ES_PR_LIST, forceSource);
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/prs/list' for the request: {}", company, originalRequest, e);
            }

            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, null, null);
            validatePartialMatchFilter(company, scmPrFilter.getPartialMatch(), ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));

            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            DbListResponse<DbScmPullRequest> dbListResponse = AggCacheUtils.cacheOrCall(disableCache,
                    company,
                    "/scm/prs/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                    scmPrFilter.generateCacheHash() + finalOuConfig.hashCode(),
                    scmPrFilter.getIntegrationIds(),
                    mapper,
                    aggCacheService,
                    () -> useEs ? esScmPRsService.list(company, scmPrFilter, sorting, finalOuConfig, page, pageSize)
                            : aggService.list(company,
                            scmPrFilter,
                            sorting,
                            finalOuConfig,
                            page,
                            pageSize));
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), dbListResponse));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/labels/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmPRLabelLite>>> prLabelsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/prs/labels/list' for the request: {}", company, originalRequest, e);
            }

            List<Integer> integrationIds = getListOrDefault(request.getFilter(), "integration_ids").stream().map(Integer::parseInt).collect(Collectors.toList());
            log.info("integrationIds = {}", integrationIds);
            List<UUID> scmPRIds = getListOrDefault(request.getFilter(), "scm_pullrequest_ids").stream().map(UUID::fromString).collect(Collectors.toList());
            log.info("scmPRIds = {}", scmPRIds);
            Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            log.info("partialMatchMap = {}", partialMatchMap);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            log.info("sorting = {}", sorting);

            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            aggService.listUniquePRLabelsByFilter(company, request.getPage(), request.getPageSize(), integrationIds, scmPRIds, partialMatchMap, sorting)
                    ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/collab_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCollaborationReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Boolean useEs = isUseEs(company, ES_PR_COLLAB_REPORT, forceSource);
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/prs/collab_report' for the request: {}", company, originalRequest, e);
            }

            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, null, null);
            validatePartialMatchFilter(company, scmPrFilter.getPartialMatch(), ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();

            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(
                                    disableCache,
                                    company,
                                    "/scm/prs/collab_report" + scmPrFilter.getSort().entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    Hashing.sha256().hashBytes((scmPrFilter.generateCacheHash() + request.getPageSize() + request.getPage()  + finalOuConfig.hashCode()).getBytes()).toString(),
                                    scmPrFilter.getIntegrationIds(),
                                    mapper,
                                    aggCacheService,
                                    () -> useEs ? esScmPRsService.getStackedCollaborationReport(company, scmPrFilter, finalOuConfig, page, pageSize) :
                                            aggService.getStackedCollaborationReport(company, scmPrFilter, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/commits/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmCommit>>> commitsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_LIST, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commits/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS);
            ScmCommitFilter commitsFilter = ScmCommitFilter.fromDefaultListRequest(request,
                    null,
                    null, partialMatchMap);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var sort = request.getSort();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/commits/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    commitsFilter.generateCacheHash() + finalOuConfig.hashCode(), commitsFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () ->  useEs ? esScmCommitsService.listCommits(company, commitsFilter, SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                            finalOuConfig, page, pageSize) :
                                            aggService.listCommits(company,
                                            commitsFilter,
                                            SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issues/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmIssue>>> issuesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/issues/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            ScmIssueFilter issueFilter = ScmIssueFilter.fromDefaultListRequest(request, null, null, partialMatchMap, sorting);
            final var finalOuConfig = ouConfig;
            final var sort = request.getSort();
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/issues/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    issueFilter.generateCacheHash() + finalOuConfig.hashCode(), issueFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> aggService.list(company,
                                            issueFilter,
                                            SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmFile>>> filesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_FILE_LIST, forceSource);
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/files/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.FILES_PARTIAL_MATCH_COLUMNS);
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .orgProductIds(orgProductIdsSet)
                    .partialMatch(partialMatchMap)
                    .build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));

            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/files/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    filesFilter.generateCacheHash(), filesFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.listFile(company, filesFilter, sorting, page, pageSize)
                                            : aggService.list(company,
                                            filesFilter,
                                            sorting,
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
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_FILE_REPORT, forceSource);

            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/files/report' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.FILES_PARTIAL_MATCH_COLUMNS);
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .listFiles((Boolean) request.getFilter().getOrDefault("list_files", true))
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .orgProductIds(orgProductIdsSet)
                    .partialMatch(partialMatchMap)
                    .build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));

            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(
                                    disableCache,
                                    company,
                                    "/scm/files/report_" + sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    Hashing.sha256().hashBytes((filesFilter.generateCacheHash() + request.getPageSize() + request.getPage()).getBytes()).toString(),
                                    filesFilter.getIntegrationIds(),
                                    mapper,
                                    aggCacheService,
                                    () -> useEs ? esScmCommitsService.listModules(company, filesFilter, sorting, page, pageSize)
                                            : aggService.listModules(company, filesFilter, sorting, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getFileValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/files/values' for the request: {}", company, originalRequest, e);
            }

            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.FILES_PARTIAL_MATCH_COLUMNS);
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            var totalCount = 0;
            for (String value : request.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                ScmFilesFilter filesFilter = ScmFilesFilter.builder()
                        .across(ScmFilesFilter.DISTINCT.fromString(value))
                        .calculation(ScmFilesFilter.CALCULATION.count)
                        .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                        .projects(getListOrDefault(request.getFilter(), "projects"))
                        .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                        .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                        .excludeProjects(getListOrDefault(excludedFields, "projects"))
                        .filename((String) request.getFilter().getOrDefault("filename", null))
                        .commitStartTime(commitStart)
                        .commitEndTime(commitEnd)
                        .partialMatch(partialMatchMap)
                        .orgProductIds(orgProductIdsSet)
                        .sort(Map.of(value, SortingOrder.ASC))
                        .build();

                final var page = request.getPage();
                final var pageSize = Math.max(5000, request.getPageSize());
                var results = AggCacheUtils.cacheOrCall(
                        disableCache,
                        company,
                        "/scm/files/values",
                        Hashing.sha256().hashBytes((filesFilter.generateCacheHash() + request.getPageSize() + request.getPage()).getBytes()).toString(),
                        filesFilter.getIntegrationIds(),
                        mapper,
                        aggCacheService,
                        () -> aggService.groupByAndCalculateFiles(company, filesFilter, page, pageSize)
                );
                map.put(value, results.getRecords());
                response.add(map);
                if (totalCount < results.getTotalCount()) {
                    totalCount = results.getTotalCount();
                }
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(request.getPage(), request.getPageSize(), totalCount, response));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getPrValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Boolean useEs = isUseEs(company, ES_PR_VALUES, forceSource);
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/prs/values' for the request: {}", company, originalRequest, e);
            }

            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);

            var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = Math.max(5000, request.getPageSize());
            var totalCount = 0;
            for (String value : request.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                ScmPrFilter prsFilter = ScmPrFilter.fromDefaultListRequest(request, ScmPrFilter.DISTINCT.fromString(value), ScmPrFilter.CALCULATION.count);
                DbListResponse<DbAggregationResult> results = AggCacheUtils.cacheOrCall(disableCache, company, "/scm/prs/values",
                        prsFilter.generateCacheHash() + finalOuConfig.hashCode(), prsFilter.getIntegrationIds(), mapper, aggCacheService,
                        () -> useEs ? esScmPRsService.groupByAndCalculatePrs(company, prsFilter.toBuilder()
                                        .sort(Map.of(value, SortingOrder.ASC))
                                        .build(), true, finalOuConfig, page, pageSize) :
                                aggService.groupByAndCalculatePrs(company, prsFilter.toBuilder()
                                        .sort(Map.of(value, SortingOrder.ASC))
                                        .build(),
                                true,
                                finalOuConfig, page, pageSize));
                map.put(value, results.getRecords());
                response.add(map);
                if (totalCount < results.getTotalCount()) {
                    totalCount = results.getTotalCount();
                }
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(page, pageSize, totalCount, response));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/commits/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCommitValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_LIST, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commits/values' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>)
                    request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS);
            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }

            List<CompletableFuture<Map<String, DbListResponse<DbAggregationResult>>>> futures = new ArrayList<>();
            for (String value : request.getFields()) {
                futures.add(calculateScmCommitsValuesAsync(company, disableCache, request, partialMatchMap, value, ouConfig, useEs));
            }

            List<Map<String, DbListResponse<DbAggregationResult>>> response = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            Integer totalCount = response.stream()
                    .flatMap(m -> m.entrySet().stream())
                    .max(Comparator.comparing(m -> m.getValue().getTotalCount())).get().getValue().getTotalCount();

            List<Map<String, List<DbAggregationResult>>> listResponse = response.stream()
                    .map(m -> m.keySet().stream()
                            .collect(Collectors.toMap(k -> k, k -> m.get(k).getRecords())))
                    .collect(Collectors.toList());

            return ResponseEntity.ok().body(PaginatedResponse.of(request.getPage(), request.getPageSize(), totalCount, listResponse));
        });
    }

    public CompletableFuture<Map<String, DbListResponse<DbAggregationResult>>> calculateScmCommitsValuesAsync(String company, final Boolean disableCache, DefaultListRequest request, Map<String, Map<String, String>> partialMatchMap, String value, OUConfiguration ouConfig, Boolean useEs) {

        return CompletableFuture.supplyAsync(() -> {
            ScmCommitFilter commitFilter = ScmCommitFilter.builder().build();
            try {
                commitFilter = ScmCommitFilter.fromDefaultListRequest(request, ScmCommitFilter.DISTINCT.fromString(value),
                        ScmCommitFilter.CALCULATION.count, partialMatchMap);
            } catch (BadRequestException e) {
                log.error("Failed to parse the request. " + e);
            }
            DbListResponse<DbAggregationResult> dbAggregationResults = null;
            final var page = request.getPage();
            final var pageSize = Math.max(5000, request.getPageSize());
            try {
                ScmCommitFilter finalCommitFilter = commitFilter;
                dbAggregationResults = AggCacheUtils.cacheOrCall(disableCache, company, "/scm/commits/values",
                        commitFilter.generateCacheHash() + ouConfig.hashCode(), commitFilter.getIntegrationIds(), mapper, aggCacheService,
                        () -> useEs ? esScmCommitsService.groupByAndCalculateCommits(company, finalCommitFilter, true, ouConfig, page, pageSize)
                               : aggService.groupByAndCalculateCommits(company, finalCommitFilter, true, ouConfig, page, pageSize));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.debug("value = {}, dbAggregationResults = {}", value, dbAggregationResults);
            return Map.of(value, dbAggregationResults);
        }, dbValuesTaskExecutor);
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/issues/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getIssuesValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/issues/values' for the request: {}", company, originalRequest, e);
            }

            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = Math.max(5000, request.getPageSize());
            var totalCount = 0;
            for (String value : request.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                ScmIssueFilter issueFilter = ScmIssueFilter.fromDefaultListRequest(request, ScmIssueFilter.DISTINCT.fromString(value),
                        ScmIssueFilter.CALCULATION.count, partialMatchMap, Map.of(value, SortingOrder.ASC));
                DbListResponse<DbAggregationResult> results = AggCacheUtils.cacheOrCall(disableCache, company, "/scm/issues/values",
                        issueFilter.generateCacheHash() + finalOuConfig.hashCode(), issueFilter.getIntegrationIds(), mapper, aggCacheService,
                        () -> aggService.groupByAndCalculateIssues(company, issueFilter, finalOuConfig, page, pageSize));

                map.put(value, results.getRecords());
                response.add(map);
                if (totalCount < results.getTotalCount()) {
                    totalCount = results.getTotalCount();
                }
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(page, pageSize, totalCount, response));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/commits/aggregate", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCommitAggs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_AGG, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commits/aggregate' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            List<ScmCommitFilter.DISTINCT> stacks = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(request.getStacks())) {
                stacks.addAll(request.getStacks().stream()
                        .sorted()
                        .map(ScmCommitFilter.DISTINCT::fromString)
                        .collect(Collectors.toList()));
            }
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS, ScmAggService.COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS);
            ScmCommitFilter commitFilter = ScmCommitFilter.fromDefaultListRequest(request,
                    MoreObjects.firstNonNull(ScmCommitFilter.DISTINCT.fromString(request.getAcross()), ScmCommitFilter.DISTINCT.committer),
                    ScmCommitFilter.CALCULATION.count, partialMatchMap);
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/commits/aggregate",
                                    commitFilter.generateCacheHash() + finalOuConfig.hashCode(), commitFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ?  esScmCommitsService.stackedCommitsGroupBy(company, commitFilter, stacks, finalOuConfig, page, pageSize)
                                            : aggService.stackedCommitsGroupBy(company, commitFilter, stacks, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/rework_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getReworkAggs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_REWORK, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/rework_report' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS, ScmAggService.COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS);
            ScmCommitFilter commitFilter = ScmCommitFilter.fromDefaultListRequest(request,
                    MoreObjects.firstNonNull(ScmCommitFilter.DISTINCT.fromString(request.getAcross()), ScmCommitFilter.DISTINCT.committer),
                    ScmCommitFilter.CALCULATION.count, partialMatchMap);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/rework_report",
                                    commitFilter.generateCacheHash() + finalOuConfig.hashCode(), commitFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.stackedCommitsGroupBy(company, commitFilter.toBuilder().codeChangeUnit("files").build(),
                                            List.of(ScmCommitFilter.DISTINCT.code_category), finalOuConfig)
                                            : aggService.stackedCommitsGroupBy(company, commitFilter.toBuilder().codeChangeUnit("files").build(),
                                            List.of(ScmCommitFilter.DISTINCT.code_category), finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/commits/coding_days_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCodingDaysReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_CODING_DAY, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commits/coding_days_report' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS, ScmAggService.COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS);
            ScmCommitFilter commitFilter = ScmCommitFilter.fromDefaultListRequest(request,
                    MoreObjects.firstNonNull(ScmCommitFilter.DISTINCT.fromString(request.getAcross()), ScmCommitFilter.DISTINCT.committer),
                    ScmCommitFilter.CALCULATION.commit_days, partialMatchMap);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/commits/coding_days_report",
                                    commitFilter.generateCacheHash() + finalOuConfig.hashCode(), commitFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ?  esScmCommitsService.groupByAndCalculateCodingDays(company, commitFilter, finalOuConfig)
                                            : aggService.groupByAndCalculateCodingDays(company, commitFilter, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/commits/commits_per_coding_day", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCommitsPerCodingDay(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMIT_PER_CODING_DAY, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commits/commits_per_cofing_day' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.COMMITS_PARTIAL_MATCH_COLUMNS, ScmAggService.COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS);
            ScmCommitFilter commitFilter = ScmCommitFilter.fromDefaultListRequest(request,
                    MoreObjects.firstNonNull(ScmCommitFilter.DISTINCT.fromString(request.getAcross()), ScmCommitFilter.DISTINCT.committer),
                    ScmCommitFilter.CALCULATION.commit_count, partialMatchMap);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/commits/commits_per_coding_day",
                                    commitFilter.generateCacheHash() + finalOuConfig.hashCode(), commitFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.groupByAndCalculateCodingDays(company, commitFilter, finalOuConfig)
                            : aggService.groupByAndCalculateCodingDays(company, commitFilter, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/committers/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmContributorAgg>>> getCommittersList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_COMMITTER_LIST, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/commters/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> rnge = request.getFilterValue("time_range", Map.class)
                    .orElse(Map.of());
            final Long committedAtStart = rnge.get("$gt") != null ? Long.valueOf(rnge.get("$gt")) : null;
            final Long committedAtEnd = rnge.get("$lt") != null ? Long.valueOf(rnge.get("$lt")) : null;
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmContributorsFilter contributorsFilter = ScmContributorsFilter.builder()
                    .across(ScmContributorsFilter.DISTINCT.committer)
                    .dataTimeRange(ImmutablePair.of(committedAtStart, committedAtEnd))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .committers(getListOrDefault(request.getFilter(), "committers"))
                    .authors(getListOrDefault(request.getFilter(), "authors"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .includeIssues(true)
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                    .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                    .orgProductIds(orgProductIdsSet)
                    .partialMatch(partialMatchMap)
                    .build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/committers/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    contributorsFilter.generateCacheHash() + finalOuConfig.hashCode(), contributorsFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.list(company, contributorsFilter, sorting, finalOuConfig, page, pageSize)
                                            : aggService.list(company, contributorsFilter, sorting, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/contributors/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmContributorAgg>>> getContributorsList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_CONTRIBUTOR_LIST, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/contributors/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> rnge = request.getFilterValue("time_range", Map.class)
                    .orElse(Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            final Long committedAtStart = rnge.get("$gt") != null ? Long.valueOf(rnge.get("$gt")) : null;
            final Long committedAtEnd = rnge.get("$lt") != null ? Long.valueOf(rnge.get("$lt")) : null;
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmContributorsFilter contributorsFilter = ScmContributorsFilter.builder()
                    .across(ScmContributorsFilter.DISTINCT.fromString(request.getAcross()))
                    .dataTimeRange(ImmutablePair.of(committedAtStart, committedAtEnd))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .committers(getListOrDefault(request.getFilter(), "committers"))
                    .authors(getListOrDefault(request.getFilter(), "authors"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .includeIssues(true)
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                    .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                    .partialMatch(partialMatchMap)
                    .orgProductIds(orgProductIdsSet)
                    .build();
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/committers/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    contributorsFilter.generateCacheHash() + finalOuConfig.hashCode(), contributorsFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.list(company, contributorsFilter, sorting,
                                            finalOuConfig, page, pageSize)
                                            : aggService.list(
                                            company,
                                            contributorsFilter,
                                            sorting,
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/filetypes/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmRepoAgg>>> getFileTypesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {

            Boolean useEs = isUseEs(company, ES_FILE_TYPE_LIST, forceSource);

            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/filetypes/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> rnge = request.getFilterValue("time_range", Map.class)
                    .orElse(Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            final Long committedAtStart = rnge.get("$gt") != null ? Long.valueOf(rnge.get("$gt")) : null;
            final Long committedAtEnd = rnge.get("$lt") != null ? Long.valueOf(rnge.get("$lt")) : null;
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            ScmReposFilter scmReposFilter = ScmReposFilter.builder()
                    .dataTimeRange(ImmutablePair.of(committedAtStart, committedAtEnd))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .committers(getListOrDefault(request.getFilter(), "committers"))
                    .authors(getListOrDefault(request.getFilter(), "authors"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .fileTypes(getListOrDefault(request.getFilter(), "file_types"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                    .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                    .excludeFileTypes(getListOrDefault(excludedFields, "file_types"))
                    .partialMatch(partialMatchMap)
                    .orgProductIds(orgProductIdsSet)
                    .build();

            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var sort = request.getSort();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/scm/filetypes/list_" + request.getPage() + "_" + request.getPageSize() + "_" +
                                            sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")),
                                    scmReposFilter.generateCacheHash() + finalOuConfig.hashCode(), scmReposFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmCommitsService.listFileTypes(company, scmReposFilter, SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                            finalOuConfig, page, pageSize)
                                    : aggService.listFileTypes(company,
                                            scmReposFilter,
                                            SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                                            finalOuConfig,
                                            page,
                                            pageSize))));
        });
    }

    //TODO add caching.
    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/repos/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmRepoAgg>>> getReposList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/repos/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> rnge = request.getFilterValue("time_range", Map.class)
                    .orElse(Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            final Long committedAtStart = rnge.get("$gt") != null ? Long.valueOf(rnge.get("$gt")) : null;
            final Long committedAtEnd = rnge.get("$lt") != null ? Long.valueOf(rnge.get("$lt")) : null;
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            aggService.list(
                                    company,
                                    ScmReposFilter.builder()
                                            .dataTimeRange(ImmutablePair.of(committedAtStart, committedAtEnd))
                                            .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                                            .projects(getListOrDefault(request.getFilter(), "projects"))
                                            .committers(getListOrDefault(request.getFilter(), "committers"))
                                            .authors(getListOrDefault(request.getFilter(), "authors"))
                                            .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                            .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                                            .excludeProjects(getListOrDefault(excludedFields, "projects"))
                                            .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                                            .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                                            .partialMatch(partialMatchMap)
                                            .orgProductIds(orgProductIdsSet)
                                            .build(),
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())),
                                    finalOuConfig,
                                    request.getPage(),
                                    request.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/repo_names/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> getRepoNamesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/repo_names/list' for the request: {}", company, originalRequest, e);
            }

            Map<String, String> rnge = request.getFilterValue("time_range", Map.class)
                    .orElse(Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            final Long committedAtStart = rnge.get("$gt") != null ? Long.valueOf(rnge.get("$gt")) : null;
            final Long committedAtEnd = rnge.get("$lt") != null ? Long.valueOf(rnge.get("$lt")) : null;
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            aggService.listAllRepoNames(
                                    company,
                                    ScmReposFilter.builder()
                                            .dataTimeRange(ImmutablePair.of(committedAtStart, committedAtEnd))
                                            .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                                            .projects(getListOrDefault(request.getFilter(), "projects"))
                                            .committers(getListOrDefault(request.getFilter(), "committers"))
                                            .authors(getListOrDefault(request.getFilter(), "authors"))
                                            .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                            .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                                            .excludeProjects(getListOrDefault(excludedFields, "projects"))
                                            .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                                            .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                                            .partialMatch(partialMatchMap)
                                            .orgProductIds(orgProductIdsSet)
                                            .build(),
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())),
                                    finalOuConfig,
                                    request.getPage(),
                                    request.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/aggregate", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPrAggs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        Boolean useEs = isUseEs(company, ES_PR_AGG, forceSource);
        return getPRAggResult(useEs, disableCache, company, filter, ScmPrFilter.CALCULATION.count);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/author_response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAuthorResponseTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        Boolean useEs = isUseEs(company, ES_PR_AUTHOR_RESP_TIME, forceSource);
        return getPRAggResult(useEs, disableCache, company, filter, ScmPrFilter.CALCULATION.author_response_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/reviewer_response_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getReviewerResponseTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        Boolean useEs = isUseEs(company, ES_PR_REVIEWER_RESP_TIME, forceSource);
        return getPRAggResult(useEs, disableCache, company, filter, ScmPrFilter.CALCULATION.reviewer_response_time);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolutionTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        Boolean useEs = isUseEs(company, ES_PR_RESOLUTION_TIME_REPORT, forceSource);
        return getPRAggResult(useEs, disableCache, company, filter, ScmPrFilter.CALCULATION.merge_time);
    }

    @NotNull
    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPRAggResult(Boolean useEs, Boolean disableCache,
                                                                                                  String company,
                                                                                                  DefaultListRequest originalRequest,
                                                                                                  ScmPrFilter.CALCULATION calculation) throws BadRequestException {
        // OU stuff
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/prs/..getPRAggResult()' for the request: {}", company, originalRequest, e);
        }

        List<ScmPrFilter.DISTINCT> stacks = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(request.getStacks())) {
            stacks.addAll(request.getStacks().stream()
                    .sorted()
                    .map(ScmPrFilter.DISTINCT::fromString)
                    .collect(Collectors.toList()));
        }
        ScmPrFilter prsFilter = ScmPrFilter.fromDefaultListRequest(request,
                MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.assignee),
                calculation);
        validatePartialMatchFilter(company, prsFilter.getPartialMatch(), ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        final var finalOuConfig = ouConfig;
        Long cacheTTLValue = getCacheTTLValue(company, calculation);
        TimeUnit cacheTTLUnit = getCacheTTLUnit(company, calculation);
        log.info("company {}, cacheTTLValue = {}",company,cacheTTLValue);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        page,
                        pageSize,
                        AggCacheUtils.cacheOrCallGeneric(disableCache, company, "/scm/prs/aggregate/" + stacks.stream().map(Enum::toString)
                                        .sorted().collect(Collectors.joining(",")),
                                prsFilter.generateCacheHash() + finalOuConfig.hashCode(), prsFilter.getIntegrationIds(), mapper, aggCacheService,
                                DbListResponse.class,cacheTTLValue, cacheTTLUnit,
                                () -> useEs ? esScmPRsService.stackedPrsGroupBy(company, prsFilter, stacks, finalOuConfig, page, pageSize)
                                        : aggService.stackedPrsGroupBy(company, prsFilter, stacks, finalOuConfig, false, page, pageSize)))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/merge_trend", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPrMergeTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, ES_PR_MERGE_TREND, forceSource);
        return SpringUtils.deferResponse(() -> handleDurationQuery(useEs, disableCache, company, ScmPrFilter.CALCULATION.merge_time, filter));
    }


    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/first_review_trend", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPrFirstReview(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, ES_PR_FIRST_REVIEW_TREND, forceSource);
        return SpringUtils.deferResponse(() -> handleDurationQuery(useEs, disableCache, company, ScmPrFilter.CALCULATION.first_review_time, filter));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/prs/first_review_to_merge_trend", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPrFirstReviewToMerge(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        Boolean useEs = isUseEs(company, ES_PR_FIRST_REVIEW_TO_MERGE_TREND, forceSource);
        return SpringUtils.deferResponse(() -> handleDurationQuery(useEs, disableCache, company, ScmPrFilter.CALCULATION.first_review_to_merge_time, filter));
    }

    private ResponseEntity<PaginatedResponse<DbAggregationResult>> handleDurationQuery(Boolean useEs,
            Boolean disableCache,
            String company,
            ScmPrFilter.CALCULATION calculation,
            DefaultListRequest originalRequest) {
        // OU stuff
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/prs/..handleDurationQuery()' for the request: {}", company, originalRequest, e);
        }
        try {
            ScmPrFilter.DISTINCT across = MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.pr_created);
            if (across != ScmPrFilter.DISTINCT.pr_updated
                    && across != ScmPrFilter.DISTINCT.pr_merged
                    && across != ScmPrFilter.DISTINCT.pr_created
                    && across != ScmPrFilter.DISTINCT.pr_closed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "this endpoint does not support Across: " + across);
            }

            ScmPrFilter prsFilter = ScmPrFilter.fromDefaultListRequest(request, across, calculation);
            validatePartialMatchFilter(company, prsFilter.getPartialMatch(), ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/prs/durations",
                                    prsFilter.generateCacheHash() + finalOuConfig.hashCode(), prsFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> useEs ? esScmPRsService.groupByAndCalculatePrsDuration(company, prsFilter, finalOuConfig, page, pageSize) :
                                            aggService.groupByAndCalculatePrsDuration(company, prsFilter, finalOuConfig, page, pageSize))));
        } catch (SQLException throwables) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, throwables.getMessage());
        } catch (Exception e) {
            log.info(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issues/aggregate", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssueAggs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/issues/aggregate' for the request: {}", company, originalRequest, e);
            }

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            ScmIssueFilter issueFilter = ScmIssueFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(
                            ScmIssueFilter.DISTINCT.fromString(
                                    request.getAcross()),
                            ScmIssueFilter.DISTINCT.creator),
                    ScmIssueFilter.CALCULATION.count, partialMatchMap, sorting);
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/issues/aggregate",
                                    issueFilter.generateCacheHash() + finalOuConfig.hashCode(), issueFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> aggService.groupByAndCalculateIssues(company, issueFilter, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issues/first_response_time", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssueResponseTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/issues/first_response_time' for the request: {}", company, originalRequest, e);
            }

            ImmutablePair<Long, Long> issueCreatedAtRange = getTimeRange(request, "issue_created_at");
            ImmutablePair<Long, Long> issueClosedAtRange = getTimeRange(request, "issue_closed_at");
            ImmutablePair<Long, Long> issueUpdatedAtRange = getTimeRange(request, "issue_updated_at");
            ImmutablePair<Long, Long> firstCommentAtRange = getTimeRange(request, "first_comment_at");
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) request.getFilter().get("exclude"));
            List<String> excludeStates = (List<String>) filterExclude.getOrDefault("states", List.of());
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmIssueFilter issueFilter = ScmIssueFilter.builder()
                    .issueCreatedRange(issueCreatedAtRange)
                    .across(MoreObjects.firstNonNull(
                            ScmIssueFilter.DISTINCT.fromString(
                                    request.getAcross()),
                            ScmIssueFilter.DISTINCT.creator))
                    .calculation(ScmIssueFilter.CALCULATION.response_time)
                    .extraCriteria(MoreObjects.firstNonNull(
                                    getListOrDefault(request.getFilter(), "hygiene_types"),
                                    List.of())
                            .stream()
                            .map(String::valueOf)
                            .map(ScmIssueFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .creators(getListOrDefault(request.getFilter(), "creators"))
                    .assignees(getListOrDefault(request.getFilter(), "assignees"))
                    .labels(getListOrDefault(request.getFilter(), "labels"))
                    .states(getListOrDefault(request.getFilter(), "states"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeCreators(getListOrDefault(excludedFields, "creators"))
                    .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                    .excludeStates(getListOrDefault(excludedFields, "states"))
                    .excludeLabels(getListOrDefault(excludedFields, "labels"))
                    .title((String) request.getFilter().getOrDefault("title", null))
                    .orgProductIds(orgProductIdsSet)
                    .partialMatch(partialMatchMap)
                    .excludeStates(excludeStates)
                    .issueClosedRange(issueClosedAtRange)
                    .issueUpdatedRange(issueUpdatedAtRange)
                    .firstCommentAtRange(firstCommentAtRange)
                    .aggInterval(MoreObjects.firstNonNull(
                            AGG_INTERVAL.fromString(request.getAggInterval()), AGG_INTERVAL.day))
                    .sort(sorting)
                    .build();
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/issues/first_response_time",
                                    issueFilter.generateCacheHash() + finalOuConfig.hashCode(), issueFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> aggService.groupByAndCalculateIssues(company, issueFilter, finalOuConfig, page, pageSize))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issues/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssueResolutionReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/prs/values' for the request: {}", company, originalRequest, e);
            }

            ImmutablePair<Long, Long> issueCreatedAtRange = getTimeRange(request, "issue_created_at");
            ImmutablePair<Long, Long> issueClosedAtRange = getTimeRange(request, "issue_closed_at");
            ImmutablePair<Long, Long> issueUpdatedAtRange = getTimeRange(request, "issue_updated_at");
            ImmutablePair<Long, Long> firstCommentAtRange = getTimeRange(request, "first_comment_at");
            List<String> orgProductIdsList = getListOrDefault(request.getFilter(), "org_product_ids");
            Set<UUID> orgProductIdsSet = orgProductIdsList.stream().map(UUID::fromString).collect(Collectors.toSet());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) request.getFilter().get("exclude"));
            List<String> excludeStates = (List<String>) filterExclude.getOrDefault("states", List.of());
            validatePartialMatchFilter(company, partialMatchMap, ScmAggService.ISSUES_PARTIAL_MATCH_COLUMNS);
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            Map<String, Object> excludedFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            ScmIssueFilter issueFilter = ScmIssueFilter.builder()
                    .orgProductIds(orgProductIdsSet)
                    .issueCreatedRange(issueCreatedAtRange)
                    .across(MoreObjects.firstNonNull(
                            ScmIssueFilter.DISTINCT.fromString(
                                    request.getAcross()),
                            ScmIssueFilter.DISTINCT.creator))
                    .calculation(ScmIssueFilter.CALCULATION.resolution_time)
                    .extraCriteria(MoreObjects.firstNonNull(
                                    getListOrDefault(request.getFilter(), "hygiene_types"),
                                    List.of())
                            .stream()
                            .map(String::valueOf)
                            .map(ScmIssueFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .creators(getListOrDefault(request.getFilter(), "creators"))
                    .assignees(getListOrDefault(request.getFilter(), "assignees"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .labels(getListOrDefault(request.getFilter(), "labels"))
                    .states(getListOrDefault(request.getFilter(), "states"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeCreators(getListOrDefault(excludedFields, "creators"))
                    .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                    .excludeStates(getListOrDefault(excludedFields, "states"))
                    .excludeLabels(getListOrDefault(excludedFields, "labels"))
                    .title((String) request.getFilter().getOrDefault("title", null))
                    .partialMatch(partialMatchMap)
                    .excludeStates(excludeStates)
                    .aggInterval(MoreObjects.firstNonNull(
                            AGG_INTERVAL.fromString(request.getAggInterval()), AGG_INTERVAL.day))
                    .issueClosedRange(issueClosedAtRange)
                    .issueUpdatedRange(issueUpdatedAtRange)
                    .firstCommentAtRange(firstCommentAtRange)
                    .sort(sorting)
                    .build();
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company, "/scm/issues/resolution_time_report",
                                    issueFilter.generateCacheHash() + finalOuConfig.hashCode(), issueFilter.getIntegrationIds(), mapper, aggCacheService,
                                    () -> aggService.groupByAndCalculateIssues(company, issueFilter, finalOuConfig, page, pageSize))));
        });
    }

    private void validatePartialMatchFilter(String company,
                                            Map<String, Map<String, String>> partialMatchMap,
                                            Set<String> partialMatchColumns, Set<String> partialMatchArrayColumns) {
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> (!partialMatchColumns.contains(key)) && (!partialMatchArrayColumns.contains(key)))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for scm file partial match based filter");
        }
    }

    private void validatePartialMatchFilter(String company,
                                            Map<String, Map<String, String>> partialMatchMap,
                                            Set<String> partialMatchColumns) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> !partialMatchColumns.contains(key))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for scm file partial match based filter");
        }
    }

    private Boolean isUseEs(String company, String type, String forceSource) {
        Boolean forceSourceUseES = ForceSourceUtils.useES(forceSource);
        if(forceSourceUseES != null) {
            log.info("isUseEs forceSourceUseES={}", forceSourceUseES);
            return forceSourceUseES;
        }

        Boolean isEsCompany = this.esAllowedTenants.contains(company) ||
                (this.esAllowedTenantsApis.containsKey(type) &&
                        this.esAllowedTenantsApis.get(type).contains(company));
        log.info("isUseEs isEsCompany={}", isEsCompany);

        Boolean isDbCompany = this.dbAllowedTenants.contains(company) ||
                (this.dbAllowedTenantsApis.containsKey(type) &&
                        this.dbAllowedTenantsApis.get(type).contains(company));
        log.info("isUseEs isDBCompany={}", isDbCompany);

        //if report or company configured for both ES and DB - ignoring ES and making DB call
        if(isDbCompany){
            return false;
        }

        return isEsCompany;
    }

    private Long getCacheTTLValue(String company, ScmPrFilter.CALCULATION calculation) {
        return longerCacheTimesForScmTenants.contains(company) && calculation.equals(ScmPrFilter.CALCULATION.count) ? LONGER_CACHE_EXPIRY_VALUE : null;
    }

    private TimeUnit getCacheTTLUnit(String company, ScmPrFilter.CALCULATION calculation) {
        return longerCacheTimesForScmTenants.contains(company) && calculation.equals(ScmPrFilter.CALCULATION.count) ? LONGER_CACHE_EXPIRY_UNIT : null;
    }
}
