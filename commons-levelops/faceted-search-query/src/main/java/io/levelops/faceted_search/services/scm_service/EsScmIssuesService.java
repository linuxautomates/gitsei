package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.converters.EsScmIssuesConverter;
import io.levelops.faceted_search.querybuilders.EsScmIssuesQueryBuilder;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.querybuilders.EsScmIssuesQueryBuilder.EXCLUDE_CONDITIONS;
import static io.levelops.faceted_search.querybuilders.EsScmIssuesQueryBuilder.INCLUDE_CONDITIONS;
import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;
import static java.util.stream.Collectors.toList;

@Log4j2
public class EsScmIssuesService {

    private static final String SCM_ISSUES = "scm_issues_";
    private final ESClientFactory esClientFactory;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public EsScmIssuesService(ESClientFactory esClientFactory, DataSource dataSource) {
        this.esClientFactory = esClientFactory;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public DbListResponse<DbScmIssue> list(String company, ScmIssueFilter filter, Map<String, SortingOrder> sorting, OUConfiguration ouConfig, int pageNumber, int pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_ISSUES + company;

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        Pair<String, SortOrder> sortOrder = getSortConfig(MapUtils.emptyIfNull(sorting), "_doc");
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
        SearchResponse<DbScmIssue> searchResponse = elasticsearchClient.search(searchRequest, DbScmIssue.class);
        List<DbScmIssue> list = searchResponse.hits().hits().stream().map(h -> h.source()).collect(Collectors.toList());

        return DbListResponse.of(list, (int) searchResponse.hits().total().value());

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateIssues(String company, ScmIssueFilter filter, OUConfiguration ouConfig, int pageNumber, int pageSize) throws IOException {

        ElasticsearchClient elasticsearchClient = esClientFactory.getESClient(company);
        String index = SCM_ISSUES + company;

        if (ouConfig != null) {
            filter = updateFilterWithOuConfiguration(company, filter, ouConfig);
        }

        Pair<String, SortOrder> sortOrder = getSortConfig(MapUtils.emptyIfNull(filter.getSort()), "_doc");
        SearchRequest.Builder builder = getSearchBuilderForConditions(filter);

        Map<String, Aggregation> aggConditions = EsScmIssuesQueryBuilder.buildAggsConditions(filter, filter.getCalculation(), pageNumber, pageSize);

        SearchRequest searchRequest = builder
                .index(index)
                .aggregations(aggConditions)
                .from(pageNumber * pageSize)
                .size(pageSize)
                .sort(s -> s.field(v -> v.field(sortOrder.getLeft()).order(sortOrder.getRight())))
                .trackTotalHits(TrackHits.of(t -> t.enabled(true)))
                .build();

        SearchResponse<Void> searchResponse = elasticsearchClient.search(searchRequest, Void.class);

        List<DbAggregationResult> res = EsScmIssuesConverter.getAggResultFromSearchResponse(searchResponse, filter);

        return DbListResponse.of(res, res.size());
    }

    private ScmIssueFilter updateFilterWithOuConfiguration(String company, ScmIssueFilter filter, OUConfiguration ouConfig) {
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


    private SearchRequest.Builder getSearchBuilderForConditions(ScmIssueFilter filter) {

        Map<String, List<Query>> queryConditions = EsScmIssuesQueryBuilder.buildQueryConditionsForIssues(filter);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        return builder
                .query(Query.of(q -> q
                        .bool(BoolQuery.of(b -> b
                                .must(queryConditions.get(INCLUDE_CONDITIONS))
                                .mustNot(queryConditions.get(EXCLUDE_CONDITIONS)))
                        )));
    }

    private Pair<String, SortOrder> getSortConfig(Map<String, SortingOrder> sortBy, String defaultSort) {

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    return entry.getKey();
                })
                .orElse(defaultSort);

        SortOrder esSortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC)
                .toString().equalsIgnoreCase(SortOrder.Asc.toString()) ? SortOrder.Asc : SortOrder.Desc;

        return Pair.of(sortByKey, esSortOrder);
    }
}
