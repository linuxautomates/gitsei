package io.levelops.faceted_search.services.workitems;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.levelops.commons.databases.models.database.SortingOrder;
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
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.IssueLinkageType;
import io.levelops.faceted_search.converters.EsJiraIssueConverter;
import io.levelops.faceted_search.querybuilders.workitems.EsJiraQueryBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;

@Service
@Log4j2
public class EsJiraIssueQueryService {
    private final ESClientFactory esClientFactory;
    private final EsJiraDBHelperService esJiraDBHelperService;
    public static final String STATE_TRANSITION_TIME = "state_transition_time";
    public static final String PARENT_STORY_POINTS = "parent_story_points";
    public static final String PRIORITY_ORDER = "priority";

    private  final Integer ES_DEFAULT_HITS_SIZE = 10000;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("bounces", "hops", "resp_time",
            "solve_time", "issue_created_at", "issue_due_at", "issue_due_relative_at", "desc_size", "num_attachments",
            "first_attachment_at", "issue_resolved_at", "issue_updated_at", "first_comment_at",
            "version", "fix_version", "ingested_at", "story_points",
            PRIORITY_ORDER, PARENT_STORY_POINTS, STATE_TRANSITION_TIME);

    private static final List<JiraIssuesFilter.DISTINCT> TIMESTAMP_SORTABLE_COLUMNS = List.of(JiraIssuesFilter.DISTINCT.trend,
            JiraIssuesFilter.DISTINCT.issue_created, JiraIssuesFilter.DISTINCT.issue_updated, JiraIssuesFilter.DISTINCT.issue_due,
            JiraIssuesFilter.DISTINCT.issue_due_relative, JiraIssuesFilter.DISTINCT.issue_resolved, JiraIssuesFilter.DISTINCT.issue_updated);

    private static final Map<String, String> SORTABLE_COLUMNS_MAPPING = Map.ofEntries(Map.entry("bounces", "w_bounces"),
            Map.entry("hops", "w_hops"), Map.entry("issue_created_at", "w_created_at"),
            Map.entry("issue_due_at", "w_due_at"), Map.entry("desc_size", "w_desc_size"),
            Map.entry("num_attachments", "w_num_attachments"), Map.entry("first_attachment_at", "w_first_attachment_at"),
            Map.entry("issue_resolved_at", "w_resolved_at"), Map.entry("issue_updated_at", "w_updated_at"),
            Map.entry("first_comment_at", "w_first_comment_at"), Map.entry("versions", "w_versions.name"),
            Map.entry("fix_versions", "w_fix_versions.name"), Map.entry("ingested_at", "w_ingested_at"),
            Map.entry("story_points", "w_story_points"), Map.entry(PRIORITY_ORDER, "w_priority"),
            Map.entry(STATE_TRANSITION_TIME, "w_state_transition_time"));

    @Autowired
    public EsJiraIssueQueryService(ESClientFactory esClientFactory, EsJiraDBHelperService esJiraDBHelperService) {
        this.esClientFactory = esClientFactory;
        this.esJiraDBHelperService = esJiraDBHelperService;
    }

    public DbListResponse<DbAggregationResult> getAggReport(String company, JiraIssuesFilter jiraIssuesFilter,
                                                            List<JiraIssuesFilter.DISTINCT> stacks, OUConfiguration ouConfig,
                                                            Integer pageNumber, Integer pageSize, Boolean valuesOnly) throws IOException, SQLException {
        String indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, jiraIssuesFilter.getIngestedAt());
        JiraIssuesFilter.DISTINCT across = jiraIssuesFilter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        if (across == JiraIssuesFilter.DISTINCT.trend || ( across == JiraIssuesFilter.DISTINCT.status && valuesOnly)) {
            indexNameOrAlias = IndexType.WORK_ITEMS.getCombinedIndexAlias(company);
        }
        log.info("getAggReport indexNameOrAlias = {}", indexNameOrAlias);
        if (across == JiraIssuesFilter.DISTINCT.custom_field
                && (StringUtils.isEmpty(jiraIssuesFilter.getCustomAcross()) ||
                !DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(jiraIssuesFilter.getCustomAcross()).matches())) {
            throw new SQLException("Invalid custom field name provided. will not execute query. " +
                    "Provided field: " + jiraIssuesFilter.getCustomAcross());
        }
        if (ouConfig != null) {
            log.info("getAggReport ouConfig != null");
            jiraIssuesFilter = esJiraDBHelperService.getIntegrationUserIds(company, ouConfig, false, jiraIssuesFilter);
            log.info("getAggReport jiraIssuesFilter = {}", jiraIssuesFilter);
        }

        JiraIssuesFilter.CALCULATION calculation = jiraIssuesFilter.getCalculation();
        if (calculation == null) {
            calculation = JiraIssuesFilter.CALCULATION.ticket_count;
        }
        log.info("getAggReport calculation = {}", calculation);
        JiraIssuesFilter.DISTINCT stack = CollectionUtils.isNotEmpty(stacks) ?
                stacks.get(0) : JiraIssuesFilter.DISTINCT.none;
        log.info("getAggReport stack = {}", stack);
        List<DbJiraField> dbJiraFields = esJiraDBHelperService.getDbJiraFields(company, jiraIssuesFilter.getIngestedAt(), jiraIssuesFilter.getIntegrationIds());
        log.info("getAggReport dbJiraFields = {}", dbJiraFields);

        if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getLinks())){

            List<String> originalLinkRequest = jiraIssuesFilter.getLinks();
            List<String> dbJiraIssueList = getJiraIssueKeyList(company, jiraIssuesFilter, null, ouConfig);

            if(CollectionUtils.isEmpty(dbJiraIssueList)){
                return DbListResponse.of(List.of(), 0);
            }

            jiraIssuesFilter = JiraIssuesFilter.builder()
                    .integrationIds(jiraIssuesFilter.getIntegrationIds())
                    .links(originalLinkRequest)
                    .aggInterval(jiraIssuesFilter.getAggInterval())
                    .linkedIssueKeys(dbJiraIssueList)
                    .across(across)
                    .customAcross(jiraIssuesFilter.getCustomAcross())
                    .calculation(calculation)
                    .acrossLimit(jiraIssuesFilter.getAcrossLimit())
                    .customStacks(jiraIssuesFilter.getCustomStacks())
                    .filterAcrossValues(jiraIssuesFilter.getFilterAcrossValues())
                    .build();
        }

        SearchRequest searchRequest = EsJiraQueryBuilder.buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, dbJiraFields, false, stack, indexNameOrAlias, valuesOnly, pageNumber, pageSize, false)
                .size(jiraIssuesFilter.getCalculation() == JiraIssuesFilter.CALCULATION.sprint_mapping ? ES_DEFAULT_HITS_SIZE : 0)
                .build();
        log.info("getAggReport searchRequest generated");
        String queryString = getQueryString(searchRequest);
        log.info("Index name {} and ES Query : {} ", indexNameOrAlias, queryString);
        SearchResponse<EsWorkItem> searchResponse = esClientFactory.getESClient(company).search(searchRequest, EsWorkItem.class);
        List<DbAggregationResult> aggResultFromSearchResponse = EsJiraIssueConverter.getAggResultFromSearchResponse(searchResponse, jiraIssuesFilter.getAcross(), stack, calculation, jiraIssuesFilter, valuesOnly);
        return DbListResponse.of(aggResultFromSearchResponse, aggResultFromSearchResponse.size());

    }

    private List<String> getJiraIssueKeyList(String company, JiraIssuesFilter jiraIssuesFilter, JiraIssuesFilter jiraIssueLinkedFilter, OUConfiguration ouConfig) throws SQLException, IOException {
        int pNumber = 0;
        List<String> dbJiraIssueList = new ArrayList<>();
        while(true) {
            DbListResponse<DbJiraIssue> dbResponseJiraIssueList = getJiraIssuesList(company, jiraIssuesFilter, jiraIssueLinkedFilter, ouConfig, pNumber, ES_DEFAULT_HITS_SIZE, Optional.empty());
            dbJiraIssueList.addAll(dbResponseJiraIssueList.getRecords().stream().map(i -> i.getKey()).collect(Collectors.toList()));
            if(dbResponseJiraIssueList.getTotalCount() == dbJiraIssueList.size()) {
                break;
            }
            pNumber++;
        }
        return dbJiraIssueList;
    }


    public DbListResponse<DbJiraIssue> getJiraIssuesList(String company, JiraIssuesFilter jiraIssuesFilter, JiraIssuesFilter jiraIssueLinkedFilter,
                                                         OUConfiguration ouConfig, Integer pageNumber,
                                                         Integer pageSize,
                                                         Optional<VelocityConfigDTO> velocityConfigDTO) throws IOException, SQLException {
        String index = IndexType.WORK_ITEMS.getPartitionedIndexName(company, jiraIssuesFilter.getIngestedAt());
        Map<String, SortingOrder> sortBy = MapUtils.emptyIfNull(jiraIssuesFilter.getSort());
        AtomicReference<SortingOrder> sortingOrder = new AtomicReference<>(SortingOrder.DESC);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        sortingOrder.set(sortBy.getOrDefault(entry.getKey(), SortingOrder.DESC));
                        return SORTABLE_COLUMNS_MAPPING.get(entry.getKey());
                    }
                    return "w_created_at";
                })
                .orElse("w_created_at");
        SortOrder esSortOrder = sortingOrder.get().equals(SortingOrder.ASC) ? SortOrder.Asc : SortOrder.Desc;
        boolean needStatusTransitTime = (jiraIssuesFilter.getFromState() != null && jiraIssuesFilter.getToState() != null);
        if (STATE_TRANSITION_TIME.equals(sortByKey) && !needStatusTransitTime) {
            throw new SQLException("'from_state' and 'to_state' must be present to sort by state transition time");
        }
        if (ouConfig != null) {
            jiraIssuesFilter = esJiraDBHelperService.getIntegrationUserIds(company, ouConfig, false, jiraIssuesFilter);
        }

        List<DbJiraField> dbJiraFields = esJiraDBHelperService.getDbJiraFields(company, jiraIssuesFilter.getIngestedAt(), jiraIssuesFilter.getIntegrationIds());

        if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getLinks())){
            if(jiraIssueLinkedFilter != null){
                List<String> originalLinkRequest = jiraIssuesFilter.getLinks();
                List<String> dbJiraIssueList = getJiraIssueKeyList(company, jiraIssuesFilter, null, ouConfig);

                if(CollectionUtils.isEmpty(dbJiraIssueList)){
                    return DbListResponse.of(List.of(), 0);
                }
                jiraIssuesFilter = jiraIssueLinkedFilter.toBuilder()
                        .links(originalLinkRequest)
                        .linkedIssueKeys(dbJiraIssueList)
                        .build();
            }else {
                List<IssueLinkageType> issueLinkageTypes = IssueLinkageType.fromStringList(jiraIssuesFilter.getLinks());
                List<String> newLinkRequest = issueLinkageTypes.stream().flatMap(i -> i.getRelatedStringTypes().stream()).collect(Collectors.toList());
                jiraIssuesFilter = jiraIssuesFilter.toBuilder()
                        .links(newLinkRequest)
                        .build();
            }
        }
        SearchRequest searchRequest = EsJiraQueryBuilder.buildSearchRequest(jiraIssuesFilter, List.of(), List.of(), null, dbJiraFields, false, null, index, false, null, null, true)
                .size(pageSize)
                .from(pageNumber * pageSize)
                .sort(List.of(SortOptions.of(s -> s.field(v -> v.field(sortByKey).order(esSortOrder))),
                        SortOptions.of(s -> s.field(v -> v.field("w_workitem_id").order(SortOrder.Asc)))))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("Index name {} and ES Query : {} ", index, queryString);
        SearchResponse<EsWorkItem> esWorkItemSearchResponse = esClientFactory.getESClient(company).search(searchRequest, EsWorkItem.class);
        List<Hit<EsWorkItem>> hits = esWorkItemSearchResponse.hits().hits();
        List<DbJiraIssue> issuesList = new ArrayList<>();
        hits.forEach(hit -> issuesList.add(hit.source() != null ? EsJiraIssueConverter.getIssueFromEsWorkItem(hit.source(), dbJiraFields,
                null, false, false, false, null) : null));
        assert esWorkItemSearchResponse.hits().total() != null;
        if (velocityConfigDTO.isPresent()) {
            List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadata = esJiraDBHelperService.getIntegStatusCategoryMetadata(company, jiraIssuesFilter);
            List<DbJiraIssue> esJiraIssuesWithVelocityStages = JiraConditionUtils.getDbJiraIssuesWithVelocityStages(jiraIssuesFilter, issuesList,
                    velocityConfigDTO.get(), integStatusCategoryMetadata, true);
            JiraIssuesFilter finalJiraIssuesFilter = jiraIssuesFilter;
            List<DbJiraIssue> filteredJiraIssues = esJiraIssuesWithVelocityStages.stream()
                    .filter(issue -> finalJiraIssuesFilter.getVelocityStages()
                            .contains(issue.getVelocityStage()))
                    .collect(Collectors.toList());
            return DbListResponse.of(filteredJiraIssues,
                    (int) esWorkItemSearchResponse.hits().total().value());
        }
        return DbListResponse.of(issuesList, (int) esWorkItemSearchResponse.hits().total().value());
    }

}