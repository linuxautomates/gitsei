package io.levelops.commons.services.business_alignment.es.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.business_alignment.es.models.FTEPartial;
import io.levelops.commons.services.business_alignment.es.models.FTEResult;
import io.levelops.commons.services.business_alignment.es.models.FTEResultMergeRequest;
import io.levelops.commons.services.business_alignment.es.query_builder.BaJiraWIAggsESQueryBuilder;
import io.levelops.commons.services.business_alignment.es.query_builder.SearchRequestAndConverter;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.faceted_search.converters.EsJiraIssueConverter;
import io.levelops.faceted_search.querybuilders.ESAggInterval;
import io.levelops.faceted_search.services.workitems.EsJiraDBHelperService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.commons.services.business_alignment.es.services.BaJiraAggsESServiceUtils.determineAggInterval;
import static io.levelops.commons.services.business_alignment.es.services.BaJiraAggsESServiceUtils.getEffectiveCategoryNames;

@Log4j2
@Service
public class BaJiraAggsESService {
    private static final Comparator<DbJiraIssue> COMPARATOR_FOR_JIRA_ISSUES_BA_CREATED_AT_ASC = Comparator
            .comparing(DbJiraIssue::getIssueCreatedAt).reversed()
            .thenComparing(DbJiraIssue::getKey);
    private final BaJiraWIAggsESQueryBuilder baJiraWIAggsESQueryBuilder;
    private final EsJiraDBHelperService esJiraDBHelperService;
    private final ESClientFactory esClientFactory;
    private final boolean baReadESUseTermAggs;
    @Autowired
    public BaJiraAggsESService(BaJiraWIAggsESQueryBuilder baJiraWIAggsESQueryBuilder, EsJiraDBHelperService esJiraDBHelperService, ESClientFactory esClientFactory,
                               @Value("${BA_READ_ES_USE_TERM_AGGS:false}") Boolean baReadESUseTermAggs) {
        this.baJiraWIAggsESQueryBuilder = baJiraWIAggsESQueryBuilder;
        this.esClientFactory = esClientFactory;
        this.esJiraDBHelperService = esJiraDBHelperService;
        this.baReadESUseTermAggs = baReadESUseTermAggs;
    }

    //region Calculate FTE
    private List<FTEPartial> calculatePartialFTEForCategory(ElasticsearchClient esClient, String company,
                                                            Calculation calculation, JiraIssuesFilter sanitizedFilter, OUConfiguration ouConfig, BaJiraOptions baJiraOptions, ESAggInterval esAggInterval,
                                                            List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters, String categoryName,
                                                            List<DbJiraField> dbJiraFields, boolean  useCompositeAggs) throws IOException {
        boolean keepFetching = true;
        List<FTEPartial> responses = new ArrayList<>();
        Map<String, String> afterKey = null;

        while (keepFetching) {

            SearchRequestAndConverter forCategory = baJiraWIAggsESQueryBuilder.buildIssuesFTEQuery(company, calculation, sanitizedFilter, ouConfig, baJiraOptions, esAggInterval, ticketCategorizationFilters, categoryName, dbJiraFields, useCompositeAggs, afterKey);
            SearchRequest searchRequestForCategory = forCategory.getSearchRequest();
            SearchResponse<Void> searchResponseWithCategory = esClient.search(searchRequestForCategory, Void.class);
            //ESAggResultUtils.getQueryStringSafe(searchResponseWithCategory);
            List<FTEPartial> currentResponses = forCategory.getConverter().convert(searchResponseWithCategory);
            responses.addAll(currentResponses);
            afterKey = forCategory.getConverter().parseAfterKey(searchResponseWithCategory);
            keepFetching = (afterKey != null);
        }

        return responses;
    }
    public DbListResponse<DbAggregationResult> doCalculateIssueFTE(String company, JiraAcross across, Calculation calculation, JiraIssuesFilter filter, OUConfiguration ouConfig, @Nullable BaJiraOptions baJiraOptions,
                                                                   Integer page, Integer pageSize) throws IOException {
        //Get category name from the filter
        String categoryName = CollectionUtils.emptyIfNull(filter.getTicketCategories()).stream().findFirst().orElse(null);
        //Get reference to Ticket Categorization Filters from the existing JiraIssuesFilter
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = filter.getTicketCategorizationFilters();
        //BA ES Engine handles Ticket Categorization name & Ticket Categorization Filters seperately. We have to remove them from existing JiraIssuesFilter
        //Also age filter gets added & cause query "must_not": [ { "range": { "w_age": {} } }], this messes up the aggs
        JiraIssuesFilter sanitizedFilter = filter.toBuilder().ticketCategories(null).ticketCategorizationFilters(null).age(null).build();
        //Get effective category names
        List<String> effectiveCategoryNames = getEffectiveCategoryNames(categoryName, ticketCategorizationFilters);
        if(CollectionUtils.isEmpty(effectiveCategoryNames)) {
            throw new RuntimeException("Atleast one ticket category name needs to be present");
        }

        if (ouConfig != null) {
            log.info("getAggReport ouConfig != null");
            sanitizedFilter = esJiraDBHelperService.getIntegrationUserIds(company, ouConfig, false, sanitizedFilter);
            log.info("doCalculateIssueFTE filter = {}", sanitizedFilter);
        }

        List<DbJiraField> dbJiraFields = esJiraDBHelperService.getDbJiraFields(company, sanitizedFilter.getIngestedAt(), sanitizedFilter.getIntegrationIds());

        //Determine AggInterval
        ESAggInterval esAggInterval = determineAggInterval(across, filter);

        ElasticsearchClient esClient = esClientFactory.getESClient(company);

        //Use Term Aggs or Composite Aggs - Default is Composite Aggs
        boolean useCompositeAggs = (BooleanUtils.isTrue(baReadESUseTermAggs)) ? BaJiraWIAggsESQueryBuilder.USE_TERMS_AGG : BaJiraWIAggsESQueryBuilder.USE_COMPOSITE_AGG;

        List<FTEPartial> ftePartialAllTickets = calculatePartialFTEForCategory(esClient, company,
                calculation, sanitizedFilter, ouConfig, baJiraOptions, esAggInterval,
                ticketCategorizationFilters, null,
                dbJiraFields, useCompositeAggs);

        List<FTEResult> allFTEResult = new ArrayList<>();

        for(String currentCategoryName : effectiveCategoryNames) {
            List<FTEPartial> ftePartialForCategory = calculatePartialFTEForCategory(esClient, company,
                    calculation, sanitizedFilter, ouConfig, baJiraOptions, esAggInterval,
                    ticketCategorizationFilters, currentCategoryName,
                    dbJiraFields, useCompositeAggs);
            List<FTEResult> fteResultsForCategory = FTEResult.merge(FTEResultMergeRequest.builder(currentCategoryName, ftePartialForCategory, ftePartialAllTickets).build());
            allFTEResult.addAll(fteResultsForCategory);
        }

        List<FTEResult> allFTEResultWithAggInterval = (ESAggInterval.BIWEEKLY == esAggInterval) ? FTEResult.mergeWeeklyDataIntoBiWeekly(allFTEResult) : allFTEResult;

        List<DbAggregationResult> dbAggregationResults = FTEResult.groupFTEResultByAcross(across, allFTEResultWithAggInterval);
        int totalSize = CollectionUtils.size(dbAggregationResults);
        Integer skip = page * pageSize;
        List<DbAggregationResult> dbAggregationResultsPageFiltered = dbAggregationResults.stream()
                .skip(skip).limit(pageSize).collect(Collectors.toList());

        return DbListResponse.of(dbAggregationResultsPageFiltered, totalSize);
    }
    //endregion

    //region Get List Of Jira Issues
    private DbListResponse<DbJiraIssue> getListOfJiraIssuesForCategory(ElasticsearchClient esClient, String company,
                                                            JiraIssuesFilter sanitizedFilter, OUConfiguration ouConfig,
                                                            Integer page, Integer pageSize,
                                                            List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters, String categoryName,
                                                            List<DbJiraField> dbJiraFields) throws IOException {
        SearchRequest sr = baJiraWIAggsESQueryBuilder.buildIssuesListQuery(company, sanitizedFilter, ouConfig, page, pageSize, ticketCategorizationFilters, categoryName, dbJiraFields);

        SearchResponse<EsWorkItem> esWorkItemSearchResponse = esClient.search(sr, EsWorkItem.class);
        List<Hit<EsWorkItem>> hits = esWorkItemSearchResponse.hits().hits();
        List<DbJiraIssue> issuesList = new ArrayList<>();
        hits.forEach(hit -> issuesList.add(hit.source() != null ? EsJiraIssueConverter.getIssueFromEsWorkItem(hit.source(), dbJiraFields,
                null, false, false, false, categoryName) : null));
        return DbListResponse.of(issuesList, (int) esWorkItemSearchResponse.hits().total().value());
    }
    public DbListResponse<DbJiraIssue> getListOfJiraIssues(String company, JiraIssuesFilter filter,
                                                              OUConfiguration ouConfig, Integer pageNumber, Integer pageSize) throws IOException {
        //Get category name from the filter
        String categoryName = CollectionUtils.emptyIfNull(filter.getTicketCategories()).stream().findFirst().orElse(null);
        //Get reference to Ticket Categorization Filters from the existing JiraIssuesFilter
        List<JiraIssuesFilter.TicketCategorizationFilter> ticketCategorizationFilters = filter.getTicketCategorizationFilters();
        //BA ES Engine handles Ticket Categorization name & Ticket Categorization Filters seperately. We have to remove them from existing JiraIssuesFilter
        //Also age filter gets added & cause query "must_not": [ { "range": { "w_age": {} } }], this messes up the aggs
        JiraIssuesFilter sanitizedFilter = filter.toBuilder().ticketCategories(null).ticketCategorizationFilters(null).age(null).build();
        //Get effective category names
        List<String> effectiveCategoryNames = getEffectiveCategoryNames(categoryName, ticketCategorizationFilters);
        if(CollectionUtils.isEmpty(effectiveCategoryNames)) {
            throw new RuntimeException("Atleast one ticket category name needs to be present");
        }

        if (ouConfig != null) {
            log.info("getAggReport ouConfig != null");
            sanitizedFilter = esJiraDBHelperService.getIntegrationUserIds(company, ouConfig, false, sanitizedFilter);
            log.info("doCalculateIssueFTE filter = {}", sanitizedFilter);
        }

        List<DbJiraField> dbJiraFields = esJiraDBHelperService.getDbJiraFields(company, sanitizedFilter.getIngestedAt(), sanitizedFilter.getIntegrationIds());

        ElasticsearchClient esClient = esClientFactory.getESClient(company);

        Integer totalCount = 0;
        List<DbJiraIssue> issues =new ArrayList<>();

        for(String currentCategoryName : effectiveCategoryNames) {
            DbListResponse<DbJiraIssue> issuesForCategory = getListOfJiraIssuesForCategory(esClient, company,
                    sanitizedFilter, ouConfig,
                    null, null,
                    ticketCategorizationFilters, currentCategoryName,
                    dbJiraFields);
            totalCount += issuesForCategory.getTotalCount();
            issues.addAll(issuesForCategory.getRecords());
        }

        Integer skip = pageNumber * pageSize;
        List<DbJiraIssue> sortedIssues = CollectionUtils.emptyIfNull(issues).stream()
                .sorted(COMPARATOR_FOR_JIRA_ISSUES_BA_CREATED_AT_ASC)
                .skip(skip).limit(pageSize).collect(Collectors.toList());
        return DbListResponse.of(sortedIssues, totalCount);
    }
    //endregion

}
