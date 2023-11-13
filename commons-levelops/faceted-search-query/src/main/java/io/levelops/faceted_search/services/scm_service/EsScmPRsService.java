package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmPrSorting;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.converters.EsScmPRsConverter;
import io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.ScmQueryUtils.getFilterWithConfig;
import static io.levelops.commons.databases.services.ScmQueryUtils.getScmSortOrder;
import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;
import static io.levelops.faceted_search.converters.EsScmPRsConverter.ACROSS_USERS;
import static io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder.getPrsCountSortByKey;
import static io.levelops.faceted_search.querybuilders.EsScmPrsQueryBuilder.getSortConfig;
import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;
import static java.util.stream.Collectors.toList;

@Log4j2
@Service
public class EsScmPRsService {

    private final Set<ScmPrFilter.DISTINCT> stackSupported = Set.of(
            ScmPrFilter.DISTINCT.reviewer,
            ScmPrFilter.DISTINCT.approval_status,
            ScmPrFilter.DISTINCT.approver,
            ScmPrFilter.DISTINCT.assignee,
            ScmPrFilter.DISTINCT.repo_id,
            ScmPrFilter.DISTINCT.reviewer_count,
            ScmPrFilter.DISTINCT.approver_count,
            ScmPrFilter.DISTINCT.code_change,
            ScmPrFilter.DISTINCT.comment_density,
            ScmPrFilter.DISTINCT.project,
            ScmPrFilter.DISTINCT.label,
            ScmPrFilter.DISTINCT.branch,
            ScmPrFilter.DISTINCT.source_branch,
            ScmPrFilter.DISTINCT.target_branch,
            ScmPrFilter.DISTINCT.creator,
            ScmPrFilter.DISTINCT.collab_state,
            ScmPrFilter.DISTINCT.state,
            ScmPrFilter.DISTINCT.review_type,
            ScmPrFilter.DISTINCT.pr_closed,
            ScmPrFilter.DISTINCT.pr_merged,
            ScmPrFilter.DISTINCT.pr_created);

    private static final int DEFAULT_STACK_PARALLELISM = 30;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String SCM_PRS = "scm_prs_";
    private static Map<String, String> userIdMap;

    private final ESClientFactory esClientFactory;
    private final NamedParameterJdbcTemplate template;
    private final UserIdentityService userIdentityService;
    private final LoadingCache<String, Map<String,String>> cache;

    @Autowired
    public EsScmPRsService(ESClientFactory esClientFactory, DataSource dataSource, UserIdentityService userIdentityService) {
        this.esClientFactory = esClientFactory;
        template = new NamedParameterJdbcTemplate(dataSource);
        this.userIdentityService = userIdentityService;
        cache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getUserIdMapForCompany));
    }


    public DbListResponse<DbAggregationResult> stackedPrsGroupBy(String company, ScmPrFilter filter, List<ScmPrFilter.DISTINCT> stacks, OUConfiguration ouConfig, int pageNumber, int pageSize) throws IOException {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        DbListResponse<DbAggregationResult> result = groupByAndCalculatePrs(company, filter, false, ouConfig, pageNumber, pageSize);
        log.info("[{}] Scm Agg: done across '{}' - results={}", company, filter.getAcross(), result.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))) {
            return result;
        }

        ScmPrFilter.DISTINCT stack = stacks.get(0);
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] Scm Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] Scm Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, filter.getAcross(), result.getCount(), row.getKey());
                    ScmPrFilter newFilter;
                    final ScmPrFilter.ScmPrFilterBuilder newFilterBuilder = filter.toBuilder();
                    OUConfiguration ouConfigForStacks = ouConfig;
                    switch (filter.getAcross()) {
                        case assignee:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).assignees(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "assignees");
                            break;
                        case reviewer:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).reviewers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "reviewers");
                            break;
                        case approver:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).approvers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "approvers");
                            break;
                        case creator:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).creators(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "creators");
                            break;
                        case state:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).states(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case project:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).projects(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case label:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).labels(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case branch:
                        case source_branch:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).sourceBranches(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case target_branch:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).targetBranches(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case repo_id:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).repoIds(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case collab_state:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).collabStates(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case comment_density:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).commentDensities(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case code_change:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).codeChanges(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case review_type:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).reviewTypes(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case approval_status:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).approvalStatuses(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case pr_closed:
                        case pr_created:
                        case pr_updated:
                        case pr_merged:
                            newFilter = ScmQueryUtils.getFilterForTrendStack(
                                    getFilterWithConfig(newFilterBuilder, filter), row, filter.getAcross(), stack,
                                    MoreObjects.firstNonNull(filter.getAggInterval().toString(), ""))
                                    .build();
                            break;
                        case approver_count:
                        case reviewer_count:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).across(stack)
                                    .build();
                            break;
                        default:
                            throw new IOException("This stack is not available for scm queries." + stack);
                    }

                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults = groupByAndCalculatePrs(company, newFilter, false, ouConfigForStacks, pageNumber, pageSize).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (IOException | SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            });
            // -- collecting parallel stream with custom pool
            // (note: the toList collector preserves the encountered order)
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            log.info("[{}] Scm Agg: done processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            throw new IOException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company, ScmPrFilter filter, boolean valuesOnly, OUConfiguration ouConfig) throws IOException {
        return groupByAndCalculatePrs(company, filter, valuesOnly, ouConfig, 0, 500);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company, ScmPrFilter filter, boolean valuesOnly, OUConfiguration ouConfig, int pageNumber, int pageSize) throws IOException {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_PRS + company;

        if(filter.getAcross() == ScmPrFilter.DISTINCT.branch){
            filter = filter.toBuilder()
                    .across(ScmPrFilter.DISTINCT.source_branch)
                    .build();
            Map<String, SortingOrder> sortMap = filter.getSort();
            if (!MapUtils.isEmpty(sortMap)) {
                Map<String, SortingOrder> updatedSortMap = new HashMap<>();
                sortMap.entrySet()
                        .stream().findFirst()
                        .map(entry -> updatedSortMap.put(ScmPrFilter.DISTINCT.source_branch.toString(), entry.getValue()));
                filter = filter.toBuilder()
                        .sort(updatedSortMap)
                        .build();
            }
        }
        String sortByKey = getPrsCountSortByKey(filter.getSort(), filter.getAcross().toString());
        SortingOrder sortingOrder = getScmSortOrder(filter.getSort());
        Pair<String, SortOrder> sortOrder = getSortConfig(Map.of(sortByKey, sortingOrder), "_doc", Set.of());

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);
        Map<String, Aggregation> aggConditions;
        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        if(calculation == null)
            calculation =  ScmPrFilter.CALCULATION.count;

        if(ACROSS_USERS.contains(filter.getAcross())) {
            userIdMap = getUserMap(company);
        }

        if (valuesOnly) {
            aggConditions = EsScmPrsQueryBuilder.buildAggsConditions(filter, pageNumber, pageSize);
        } else {
            aggConditions = EsScmPrsQueryBuilder.buildAggsConditions(filter, calculation, pageNumber, pageSize);
        }

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .from(pageNumber * pageSize)
                .size(pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("ES Query : {}", queryString);
        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);

        List<DbAggregationResult> res = EsScmPRsConverter.getAggResultFromSearchResponse(searchResponse, filter.getAcross(), calculation, filter.getAggInterval(), valuesOnly, userIdMap);

        long total = res.size();
        if(total >= MAX_PAGE_SIZE) {
            Aggregate ag = searchResponse.aggregations().get("across_" + filter.getAcross());
            if (ag._kind().name().equalsIgnoreCase("Sterms")) {
                total += searchResponse.aggregations().get("across_" + filter.getAcross()).sterms().sumOtherDocCount();
            } else if (ag._kind().name().equalsIgnoreCase("Lterms")) {
                total += searchResponse.aggregations().get("across_" + filter.getAcross()).lterms().sumOtherDocCount();
            }
        }

        return DbListResponse.of(res, (int)total);
    }

    public DbListResponse<DbScmPullRequest> list(String company, ScmPrFilter filter, Map<String, SortingOrder> sorting, OUConfiguration ouConfig, int pageNumber, int pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_PRS + company;
        Pair<String, SortOrder> sortOrder = getSortConfig(MapUtils.emptyIfNull(sorting), "pr_pr_updated_at", ScmPrSorting.PR_SORTABLE);

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);

        SearchRequest searchRequest = builder
                .index(index)
                .from(pageNumber * pageSize)
                .size(pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("ES Query : {}", queryString);
        SearchResponse<DbScmPullRequest> searchResponse = elasticsearchClient.search(searchRequest, DbScmPullRequest.class);
        List<DbScmPullRequest> list = searchResponse.hits().hits().stream().map(h -> h.source()).collect(Collectors.toList());

        list = list.stream().map( pr -> {
            return pr.toBuilder()
                    .prCreatedAt(pr.getPrCreatedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(pr.getPrCreatedAt()) : null)
                    .createdAt(pr.getCreatedAt() != null ? TimeUnit.MINUTES.toSeconds(pr.getCreatedAt()) : null)
                    .prUpdatedAt(pr.getPrUpdatedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(pr.getPrUpdatedAt()) : null)
                    .prMergedAt(pr.getPrMergedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(pr.getPrMergedAt()) : null)
                    .prClosedAt(pr.getPrClosedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(pr.getPrClosedAt()) : null)
                    .firstCommittedAt(pr.getFirstCommittedAt() != null ? TimeUnit.MILLISECONDS.toSeconds(pr.getFirstCommittedAt()) : null)
                    .linesAdded(pr.getAdditions() == null ? "0" : String.valueOf(pr.getAdditions()))
                    .linesDeleted(pr.getDeletions() == null ? "0" :  String.valueOf(pr.getDeletions()))
                    .linesChanged(pr.getChange() == null ? "0" :  String.valueOf(pr.getChange()))
                    .build();
        }).collect(toList());

        return DbListResponse.of(list, (int) searchResponse.hits().total().value());

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrsDuration(String company, ScmPrFilter filter, OUConfiguration ouConfig) throws IOException {
        return groupByAndCalculatePrsDuration(company, filter, ouConfig, 0, MAX_PAGE_SIZE);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrsDuration(String company,
                                                                              ScmPrFilter filter, OUConfiguration ouConfig,
                                                                              Integer pageNumber, Integer pageSize) throws IOException {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_PRS + company;

        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = ScmPrFilter.CALCULATION.merge_time;
        }

        if(ACROSS_USERS.contains(filter.getAcross())) {
            userIdMap = getUserMap(company);
        }

        Pair<String, SortOrder> sortOrder = getSortConfig(MapUtils.emptyIfNull(filter.getSort()), "_doc", Set.of());

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);
        Map<String, Aggregation> aggConditions = EsScmPrsQueryBuilder.buildAggsConditionsForTrend(filter, filter.getCalculation());

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .size(MAX_PAGE_SIZE)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("ES Query : {}", queryString);
        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbAggregationResult> res = EsScmPRsConverter.getAggResultFromSearchResponseForTrend(searchResponse, filter.getAcross(), filter.getCalculation(), filter.getAggInterval(), userIdMap);

        return DbListResponse.of(res, res.size());
    }

    public DbListResponse<DbAggregationResult> getStackedCollaborationReport(String company, ScmPrFilter filter, OUConfiguration ouConfig) throws IOException, ExecutionException {
        return getStackedCollaborationReport(company, filter, ouConfig, 0, MAX_PAGE_SIZE);
    }

    public DbListResponse<DbAggregationResult> getStackedCollaborationReport(String company, ScmPrFilter filter,
                                                                             OUConfiguration ouConfig, Integer pageNumber, Integer pageSize) throws IOException, ExecutionException {

        userIdMap = getUserMap(company);
        DbListResponse<DbAggregationResult> result = getCollaborationReport(company, filter, ouConfig, false, userIdMap, true);
        ForkJoinPool threadPool = null;
        try {
            OUConfiguration ouConfigForStacks = newOUConfigForStacks(ouConfig, "approvers");
            final var finalOuConfigForStacks = newOUConfigForStacks(ouConfigForStacks, "creator");

            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                if (Objects.isNull(row.getKey())) {
                    return row.toBuilder().stacks(List.of()).build();
                }
                final ScmPrFilter.ScmPrFilterBuilder newFilterBuilder = filter.toBuilder();
                ScmPrFilter newFilter = getFilterWithConfig(newFilterBuilder, filter)
                        .creators(List.of(row.getKey()))
                        .collabStates(List.of(row.getCollabState()))
                        .sort(Map.of("approvers", SortingOrder.ASC))
                        .build();

                List<DbAggregationResult> currentStackResults = null;
                try {
                    currentStackResults = getCollaborationReport(company, newFilter, finalOuConfigForStacks, true, userIdMap, false).getRecords();

                    if (CollectionUtils.isEmpty(currentStackResults)) {
                        DbAggregationResult res = DbAggregationResult.builder()
                                .key("NONE")
                                .additionalKey("NONE")
                                .count(row.getCount())
                                .build();
                        currentStackResults = new ArrayList<>();
                        currentStackResults.add(res);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return row.toBuilder().stacks(currentStackResults).build();
            });
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {

            throw new IOException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }

    }

    private Map<String, String> getUserMap(String company) {
        try {
            log.info("Getting user mapping for {} from cache", company);
            return cache.get(company);
        } catch (ExecutionException e) {
            new RuntimeException(e);
        }
        return Maps.newHashMap();
    }

    private Map<String, String> getUserIdMapForCompany(String company) throws RuntimeException {

        log.info("Making db call to get user mappings for {} ", company);
        Map<String, String> userMap = new HashMap<>();
        try {
            List<DbScmUser> userList = userIdentityService.list(company, 0, Integer.MAX_VALUE).getRecords();
            userList.forEach(user -> userMap.put(user.getId(), user.getCloudId()));
        } catch (SQLException e) {
            new RuntimeException(e);
        }
        return userMap;
    }

    private DbListResponse<DbAggregationResult> getCollaborationReport(String company, ScmPrFilter filter, OUConfiguration ouConfig, boolean stackValue, Map<String, String> userIdMap, Boolean useOUFilter) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_PRS + company;
        Pair<String, SortOrder> sortOrder = getSortConfig(MapUtils.emptyIfNull(filter.getSort()), "_doc", Set.of());

        if (ouConfig != null && useOUFilter) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);
        Map<String, Aggregation> aggConditions;

        aggConditions = EsScmPrsQueryBuilder.buildAggsConditionsFoCollab(filter, stackValue);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .size(MAX_PAGE_SIZE)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("ES Query : {}", queryString);
        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);

        List<DbAggregationResult> res = EsScmPRsConverter.getAggResultForCollabReport(searchResponse, stackValue, userIdMap);

        return DbListResponse.of(res, res.size());
    }

    private SearchRequest.Builder getSearchBuilderForConditions(ScmPrFilter filter) {

        Map<String, List<Query>> queryConditions = EsScmPrsQueryBuilder.buildQueryConditionsForPrs(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        return builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));
    }

    private ScmPrFilter updateFilterWithOuConfiguration(String company, ScmPrFilter filter, OUConfiguration ouConfig) {
        List<String> ids = mergeOuConfiguration(company, ouConfig);
        if (CollectionUtils.isNotEmpty(ids)) {

            if (OrgUnitHelper.doesOUConfigHavePRCreator(ouConfig)) {
                filter = filter.toBuilder()
                        .creators(ids)
                        .build();
            }
            if (OrgUnitHelper.doesOUConfigHavePRAssignee(ouConfig)) {
                filter = filter.toBuilder()
                        .assignees(ids)
                        .build();
            }
            if (OrgUnitHelper.doesOUConfigHavePRReviewer(ouConfig)) {
                filter = filter.toBuilder()
                        .reviewerIds(ids)
                        .build();
            }
            if (OrgUnitHelper.doesOUConfigHavePRApprover(ouConfig)) {
                filter = filter.toBuilder()
                        .approvers(ids)
                        .build();
            }
        }
        return filter;
    }

    private List<String> mergeOuConfiguration(String company, OUConfiguration ouConfig) {

        Map<String, Object> params = new HashMap<>();

        if (OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig) || OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) {

            String usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());

            if (StringUtils.isNotEmpty(usersSelect)) {
                String sql = "SELECT id FROM ( " + usersSelect + ") a";
                List<UUID> ids = template.queryForList(sql, params, UUID.class);

                if (CollectionUtils.isNotEmpty(ids)) {
                    return ids.stream().map(id -> id.toString()).collect(toList());
                }
            }
        }
        return List.of();
    }

    private String getPrsDurationSortByKey(Map<String, SortingOrder> sortBy, String across) {
        if (org.apache.commons.collections4.MapUtils.isEmpty(sortBy)) {
            return "_doc";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                            if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                                if (!across.equals(entry.getKey())) {
                                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                                }
                                String[] acrossArrayFields = {"assignee"};
                                if (Arrays.stream(acrossArrayFields).anyMatch(ac -> ac.equalsIgnoreCase(entry.getKey()))) {
                                    return entry.getKey() + "s ";
                                }
                                return entry.getKey();
                            }
                            return "_doc";
                        }
                )
                .orElse("_doc");
    }
}
