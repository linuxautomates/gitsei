package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
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
import io.levelops.commons.databases.models.database.scm.*;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.faceted_search.converters.EsScmCommitsConverter;
import io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder;
import io.levelops.faceted_search.querybuilders.EsScmContributorsQueryBuilder;
import io.levelops.faceted_search.querybuilders.EsScmFilesQueryBuilder;
import io.levelops.faceted_search.querybuilders.EsScmReposQueryBuilder;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

import static io.levelops.commons.databases.services.ScmAggService.COMMITTERS_SORTABLE_COLUMNS;
import static io.levelops.commons.databases.services.ScmAggService.FILE_SORTABLE_COLUMNS;
import static io.levelops.commons.databases.services.ScmAggService.REPOS_SORTABLE_COLUMNS;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCommitsFilterForTrendStack;
import static io.levelops.commons.databases.services.ScmQueryUtils.getScmSortOrder;
import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;
import static io.levelops.faceted_search.converters.EsScmCommitsConverter.ACROSS_USERS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmCommitQueryBuilder.buildQueryConditions;
import static io.levelops.faceted_search.querybuilders.EsScmContributorsQueryBuilder.buildQueryConditionsForContributors;
import static io.levelops.faceted_search.querybuilders.EsScmFilesQueryBuilder.buildQueryConditionsForFiles;
import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;
import static java.util.stream.Collectors.toList;

@Log4j2
@Service
public class EsScmCommitsService {

    private static final int DEFAULT_STACK_PARALLELISM = 30;
    private static final int STACK_COUNT_THRESHOLD = 50;
    private static final int TOP_N_RECORDS = 30;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String SCM_COMMITS = "scm_commits_";
    public static final Set<String> COMMIT_SORTABLE_COLUMNS = Set.of("committed_at", "created_at");
    private static final List<String> UNPREFIXED_SORTABLE_COLUMN = List.of("num_commits", "num_prs", "median", "_doc");
    private static  Map<String, String> userIdMap = new HashMap<>();

    private final Set<ScmCommitFilter.DISTINCT> stackSupportedForCommits = Set.of(
            ScmCommitFilter.DISTINCT.code_change,
            ScmCommitFilter.DISTINCT.project,
            ScmCommitFilter.DISTINCT.author,
            ScmCommitFilter.DISTINCT.file_type,
            ScmCommitFilter.DISTINCT.code_category,
            ScmCommitFilter.DISTINCT.repo_id,
            ScmCommitFilter.DISTINCT.committer,
            ScmCommitFilter.DISTINCT.vcs_type,
            ScmCommitFilter.DISTINCT.commit_branch
    );
    private final ESClientFactory esClientFactory;
    private final NamedParameterJdbcTemplate template;
    private final UserIdentityService userIdentityService;
    private final LoadingCache<String, Map<String,String>> cache;

    @Autowired
    public EsScmCommitsService(ESClientFactory esClientFactory, DataSource dataSource, UserIdentityService userIdentityService) {
        this.esClientFactory = esClientFactory;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.userIdentityService = userIdentityService;
        cache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(this::getUserIdMapForCompany));
    }


    public DbListResponse<DbAggregationResult> groupByAndCalculateCommits(String company,
                                                                          ScmCommitFilter filter,
                                                                          boolean valuesOnly,
                                                                          OUConfiguration ouConfig,
                                                                          Integer pageNumber,
                                                                          Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        String sortByKey = getCommitsSortByKey(filter.getSort(), filter.getAcross().name(), filter.getCalculation());
        SortingOrder sortingOrder =  getScmSortOrder(filter.getSort());
        Pair<String, SortOrder> sortOrder = getSortConfig(Map.of(sortByKey, sortingOrder), "_doc", Set.of());
        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);

        Map<String, Aggregation> aggConditions;

        if (valuesOnly) {
            aggConditions = EsScmCommitQueryBuilder.buildAggsConditions(filter);
        } else {
            aggConditions = EsScmCommitQueryBuilder.buildAggsConditions(filter, filter.getCalculation(), pageNumber, pageSize);
        }

        if (ACROSS_USERS.contains(filter.getAcross())) {
           userIdMap = getUserMap(company);
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
        log.info("ES commit Query : {}", queryString);
        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbAggregationResult> aggResultFromSearchResponse = EsScmCommitsConverter.getAggResultFromSearchResponse(searchResponse, filter, valuesOnly, userIdMap);

        return DbListResponse.of(aggResultFromSearchResponse, aggResultFromSearchResponse.size());

    }

    public DbListResponse<DbAggregationResult> stackedCommitsGroupBy(String company,
                                                                     ScmCommitFilter filter,
                                                                     List<ScmCommitFilter.DISTINCT> stacks,
                                                                     OUConfiguration ouConfig) throws IOException {

        return stackedCommitsGroupBy(company, filter, stacks, ouConfig, 0, MAX_PAGE_SIZE);
    }

    public DbListResponse<DbAggregationResult> stackedCommitsGroupBy(String company,
                                                                     ScmCommitFilter filter,
                                                                     List<ScmCommitFilter.DISTINCT> stacks,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize) throws IOException {

        DbListResponse<DbAggregationResult> tempResult = groupByAndCalculateCommits(company, filter, false, ouConfig, pageNumber, pageSize);

        log.info("[{}] Scm Agg: done across '{}' - results={}", company, filter.getAcross(), tempResult.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupportedForCommits.contains(stacks.get(0))) {
            return tempResult;
        }

        if (tempResult.getCount() > STACK_COUNT_THRESHOLD) {
            log.info("[{}] Scm Agg: Total number of buckets to be processed are {}, will pick only top {} records ", company, tempResult.getCount(), TOP_N_RECORDS);
            tempResult = getTopNResults(tempResult.getRecords());
        }

        DbListResponse<DbAggregationResult> result = tempResult;

        ScmCommitFilter.DISTINCT stack = stacks.get(0);
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] Scm Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] Scm Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, filter.getAcross(), result.getCount(), row.getKey());
                    ScmCommitFilter newFilter;
                    OUConfiguration ouConfigForStacks = ouConfig;
                    final ScmCommitFilter.ScmCommitFilterBuilder newFilterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case committer:
                            newFilter = newFilterBuilder.committers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "committers");
                            break;
                        case author:
                            newFilter = newFilterBuilder.authors(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "authors");
                            break;
                        case commit_branch:
                            newFilter = newFilterBuilder.commitBranches(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "commit_branch");
                            break;
                        case repo_id:
                            newFilter = newFilterBuilder.repoIds(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case project:
                            newFilter = newFilterBuilder.projects(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case vcs_type:
                            newFilter = newFilterBuilder.vcsTypes(List.of(VCS_TYPE.fromString(row.getKey())))
                                    .across(stack).build();
                            break;
                        case file_type:
                            newFilter = newFilterBuilder.fileTypes(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case trend:
                            newFilter = getCommitsFilterForTrendStack(
                                    newFilterBuilder, row, filter.getAcross(), stack,
                                    MoreObjects.firstNonNull(filter.getAggInterval().toString(), "")).build();
                            break;
                        default:
                            throw new SQLException("This stack is not available for scm queries." + stack);
                    }

                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults = groupByAndCalculateCommits(company, newFilter, false, ouConfig, pageNumber, pageSize).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (SQLException | IOException e) {
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

            throw new RuntimeException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }
    }

    public DbListResponse<DbScmCommit> listCommits(String company,
                                                   ScmCommitFilter filter,
                                                   Map<String, SortingOrder> sortBy,
                                                   OUConfiguration ouConfig,
                                                   Integer pageNumber,
                                                   Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        Pair<String, SortOrder> sortOrder = getSortConfig(sortBy, "committed_at", COMMIT_SORTABLE_COLUMNS);
        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);

        SearchRequest searchRequest = builder
                .index(index)
                .from(pageNumber * pageSize)
                .size(pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        String queryString = getQueryString(searchRequest);
        log.info("ES commit Query : {}", queryString);
        SearchResponse<DbScmCommit> searchResponse = elasticsearchClient.search(searchRequest, DbScmCommit.class);
        List<DbScmCommit> list = searchResponse.hits().hits().stream().map(h -> h.source()).collect(Collectors.toList());

        ScmCommitFilter finalFilter = filter;
        list = list.stream().map(commit -> {
            return getEnrichedCommits(finalFilter, commit);
        }).collect(toList());
        return DbListResponse.of(list, (int) searchResponse.hits().total().value());
    }

    private DbScmCommit getEnrichedCommits(ScmCommitFilter filter, DbScmCommit commit) {
        long legacyTime = filter.getLegacyCodeConfig() != null ? TimeUnit.SECONDS.toMillis(filter.getLegacyCodeConfig()) :
                TimeUnit.SECONDS.toMillis(Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());

        long totalLines = 0;
        long legacyLines = 0;
        long refactoredLines = 0;
        long totalNewLines = 0;

        if(CollectionUtils.isNotEmpty(commit.getFileCommitList())) {
            for (DbScmFileCommitDetails details : commit.getFileCommitList()) {

                totalLines += details.getTotalChange();

                if (details.getPreviousCommittedAt() == null) {
                    totalNewLines += totalNewLines;
                } else if (details.getPreviousCommittedAt() <= legacyTime) {
                    legacyLines += details.getTotalChange();
                } else if (details.getPreviousCommittedAt() >= legacyTime) {
                    refactoredLines += details.getTotalChange();
                }
            }
        }

        double pctNewLines = 0d;
        double pctRefactoredLine = 0d;
        double pctLegacyLine = 0d;

        if(totalLines != 0 ){
            pctNewLines = totalNewLines * 100 / totalLines;
            pctRefactoredLine = refactoredLines * 100 / totalLines;
            pctLegacyLine = legacyLines * 100 / totalLines;
        }

        return commit.toBuilder()
                .totalLinesAdded(commit.getTotalLinesAdded() == null ? 0 : commit.getTotalLinesAdded())
                .totalLinesChanged(commit.getTotalLinesChanged() == null ? 0 : commit.getTotalLinesChanged())
                .totalLinesRemoved(commit.getTotalLinesRemoved() == null ? 0 : commit.getTotalLinesRemoved())
                .legacyLinesCount(legacyLines)
                .linesRefactoredCount(refactoredLines)
                .pctNewLines(pctNewLines)
                .pctRefactoredLines(pctRefactoredLine)
                .pctLegacyLines(pctLegacyLine)
                .build();
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCodingDays(String company,
                                                                             ScmCommitFilter filter,
                                                                             OUConfiguration ouConfig) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        String sortByKey = getCommitsSortByKey(filter.getSort(), filter.getAcross().toString(), filter.getCalculation());
        SortingOrder sortingOrder =  getScmSortOrder(filter.getSort());
        Pair<String, SortOrder> sortOrder = getSortConfig(Map.of(sortByKey, sortingOrder), "_doc", Set.of());
        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);

        Map<String, Aggregation> aggConditions = EsScmCommitQueryBuilder.buildAggsConditions(filter, filter.getCalculation(), 0, MAX_PAGE_SIZE);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .size(MAX_PAGE_SIZE)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        if (ACROSS_USERS.contains(filter.getAcross())) {
            userIdMap = getUserMap(company);
        }
        String queryString = getQueryString(searchRequest);
        log.info("ES commit Query : {}", queryString);
        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbAggregationResult> aggResultFromSearchResponse;
        if ("commit_days".equals(filter.getCalculation().name())) {
            aggResultFromSearchResponse = EsScmCommitsConverter.getAggResultForCodingDaysReport(searchResponse, filter, userIdMap);
        } else {
            aggResultFromSearchResponse = EsScmCommitsConverter.getAggResultForCodingDays(searchResponse, filter, userIdMap);
        }
        DbListResponse<DbAggregationResult> result = DbListResponse.of(aggResultFromSearchResponse, aggResultFromSearchResponse.size());

        return result;
    }

    public DbListResponse<DbAggregationResult> listModules(String company,
                                                           ScmFilesFilter filter,
                                                           Map<String, SortingOrder> sortBy,
                                                           Integer page,
                                                           Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        Map<String, List<Query>> queryConditions = buildQueryConditionsForFiles(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder = builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));

        String sortByKey = "_doc";
        SortingOrder sortingOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        Pair<String, SortOrder> sortOrder = getSortConfig(Map.of(sortByKey, sortingOrder), "_doc", Set.of());
        Map<String, Aggregation> aggConditions = EsScmFilesQueryBuilder.buildAggsConditionForFiles(filter, page, pageSize);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .from(page)
                .size(page * pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        String queryString = getQueryString(searchRequest);
        log.info("ES commit  Query : {}", queryString);

        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbAggregationResult> list = EsScmCommitsConverter.esScmModuleMapper(searchResponse, filter);

        long totalCount = 0;
        if (StringUtils.isNotEmpty(filter.getModule())) {
            totalCount = searchResponse.aggregations().get("across_files").nested().aggregations().get("wildcard_filter").filter().aggregations().get("total_count").cardinality().value();
        } else {
            totalCount = searchResponse.aggregations().get("across_files").nested().aggregations().get("total_count").cardinality().value();
        }
        return DbListResponse.of(list, (int) totalCount);
    }

    public DbListResponse<DbScmFile> listFile(String company,
                                              ScmFilesFilter filter,
                                              Map<String, SortingOrder> sortBy,
                                              Integer pageNumber,
                                              Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        Map<String, List<Query>> queryConditions = buildQueryConditionsForFiles(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder = builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));

        Pair<String, SortOrder> sortOrder = getSortConfig(sortBy, "num_commits", FILE_SORTABLE_COLUMNS);
        Map<String, Aggregation> aggConditions = EsScmFilesQueryBuilder.buildAggsConditionForListFiles(filter, sortOrder, pageNumber, pageSize);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        String queryString = getQueryString(searchRequest);
        log.info("ES commit  Query : {}", queryString);

        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbScmFile> list = EsScmCommitsConverter.esScmFileMapper(searchResponse);
        long total = searchResponse.aggregations().get("across_files").nested().aggregations().get("total_count").cardinality().value();

        return DbListResponse.of(list, (int) total);

    }

    public DbListResponse<DbScmContributorAgg> list(String company,
                                                    ScmContributorsFilter filter,
                                                    Map<String, SortingOrder> sortBy,
                                                    OUConfiguration ouConfig,
                                                    Integer pageNumber,
                                                    Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        if (ouConfig != null) {

            List<String> ids = mergeOuConfiguration(company, ouConfig);
            if (CollectionUtils.isNotEmpty(ids)) {
                if (OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig)) {
                    filter = filter.toBuilder()
                            .committers(ids.stream().map(id -> id.toString()).collect(toList()))
                            .build();
                } else if (OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) {
                    filter = filter.toBuilder()
                            .authors(ids)
                            .build();
                }
            }
        }

        Map<String, List<Query>> queryConditions = buildQueryConditionsForContributors(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder = builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));

        Pair<String, SortOrder> sortOrder = getSortConfig(sortBy, "num_commits", COMMITTERS_SORTABLE_COLUMNS);
        Map<String, Aggregation> aggConditions = EsScmContributorsQueryBuilder.buildAggsConditionsForContributor(filter, sortOrder, pageNumber, pageSize);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        String queryString = getQueryString(searchRequest);
        log.info("ES contributors  Query : {}", queryString);

        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbScmContributorAgg> list = EsScmCommitsConverter.esScmContributorMapper(searchResponse, filter.getAcross().name());
        long totalCount = searchResponse.aggregations().get("total_count").cardinality().value();

        return DbListResponse.of(list,  (int) totalCount);

    }

    public DbListResponse<DbScmRepoAgg> listFileTypes(String company,
                                                      ScmReposFilter filter,
                                                      Map<String, SortingOrder> sortBy,
                                                      OUConfiguration ouConfig,
                                                      Integer pageNumber,
                                                      Integer pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_COMMITS + company;

        if (ouConfig != null) {

            List<String> ids = mergeOuConfiguration(company, ouConfig);
            if (CollectionUtils.isNotEmpty(ids)) {
                if (OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig)) {
                    filter = filter.toBuilder()
                            .committers(ids.stream().map(id -> id.toString()).collect(toList()))
                            .build();
                } else if (OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) {
                    filter = filter.toBuilder()
                            .authors(ids)
                            .build();
                }
            }
        }

        Map<String, List<Query>> queryConditions = EsScmReposQueryBuilder.buildQueryConditionsForRepos(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder = builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));

        Pair<String, SortOrder> sortOrder = getSortConfig(sortBy, "num_commits", REPOS_SORTABLE_COLUMNS);
        Map<String, Aggregation> aggConditions = EsScmReposQueryBuilder.buildAggsConditionsForRepos(filter, sortOrder, pageNumber, pageSize);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        String queryString = getQueryString(searchRequest);
        log.info("ES filetypes  Query : {}", queryString);

        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);
        List<DbScmRepoAgg> list = EsScmCommitsConverter.esScmFileTypeMapper(searchResponse);
        long totalCount = searchResponse.aggregations().get("total_count").cardinality().value();

        return DbListResponse.of(list, (int) totalCount);
    }

    private SearchRequest.Builder getSearchBuilderForConditions(ScmCommitFilter filter) {

        Map<String, List<Query>> queryConditions = buildQueryConditions(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        return builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS))))));

    }

    private Pair<String, SortOrder> getSortConfig(Map<String, SortingOrder> sortBy, String defaultSort, Set<String> sortableColumns) {

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (sortableColumns.contains(entry.getKey())) {
                            return entry.getKey();
                    }
                    return defaultSort;
                })
                .orElse(defaultSort);

        SortOrder esSortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC)
                .toString().equalsIgnoreCase(SortOrder.Asc.toString()) ? SortOrder.Asc : SortOrder.Desc;

        if(sortableColumns.contains(sortByKey) && !UNPREFIXED_SORTABLE_COLUMN.contains(sortByKey)) {
            sortByKey = "c_" + sortByKey;
        }

        return Pair.of(sortByKey, esSortOrder);
    }

    private DbListResponse<DbAggregationResult> getTopNResults(List<DbAggregationResult> result) {

        List<DbAggregationResult> topNResults = result.stream().
                sorted(Comparator.comparingLong(dbAgg -> dbAgg.getLinesAddedCount() + dbAgg.getLinesRemovedCount() + dbAgg.getLinesChangedCount()))
                .collect(toList());
        int topNRecords = TOP_N_RECORDS / 2;
        topNResults = Stream.concat(topNResults.subList((topNResults.size() - topNRecords), topNResults.size()).stream(),
                topNResults.subList(0, topNRecords).stream())
                .distinct()
                .collect(toList());

        return DbListResponse.of(topNResults, topNResults.size());

    }

    private ScmCommitFilter updateFilterWithOuConfiguration(String company, ScmCommitFilter filter, OUConfiguration ouConfig) {
        List<String> ids = mergeOuConfiguration(company, ouConfig);
        if (CollectionUtils.isNotEmpty(ids)) {
            if (OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig)) {
                filter = filter.toBuilder()
                        .committers(ids.stream().map(id -> id.toString()).collect(toList()))
                        .build();
            } else if (OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) {
                filter = filter.toBuilder()
                        .authors(ids)
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

    private String getCommitsSortByKey(Map<String, SortingOrder> sortBy, String across, ScmCommitFilter.CALCULATION calculation) {
        if (org.apache.commons.collections4.MapUtils.isEmpty(sortBy)) {
            return "_doc";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmCommitFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (Objects.isNull(ScmCommitFilter.CALCULATION.fromString(entry.getKey())) || !entry.getKey().equals(calculation.toString())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                    } else {
                        if (calculation.equals(ScmCommitFilter.CALCULATION.count)) {
                            return "_doc";
                        } else if (List.of(ScmCommitFilter.CALCULATION.commit_count, ScmCommitFilter.CALCULATION.commit_days).contains(calculation)) {
                            return "median";
                        }
                        return "_doc";
                    }
                })
                .orElse("_doc");
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
}
