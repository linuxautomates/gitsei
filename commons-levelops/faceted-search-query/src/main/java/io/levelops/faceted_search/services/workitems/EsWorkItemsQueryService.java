package io.levelops.faceted_search.services.workitems;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.faceted_search.db.models.workitems.EsWorkItem;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.converters.EsWorkItemConverter;
import io.levelops.faceted_search.querybuilders.workitems.EsWorkItemQueryBuilder;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static io.levelops.faceted_search.utils.ESAggResultUtils.getQueryString;

@Service
@Log4j2
public class EsWorkItemsQueryService {
    public static final String PRIORITY_ORDER = "priority";
    private static final Set<String> SORTABLE_COLUMNS = Set.of("bounces", "hops", "workitem_created_at",
            "workitem_due_at", "desc_size", "num_attachments", "first_attachment_at", " workitem_resolved_at",
            "workitem_updated_at", "first_comment_at", "versions", "fix_versions", "ingested_at", "story_points",
            PRIORITY_ORDER);
    private static final Map<String, String> SORTABLE_COLUMNS_MAPPING = Map.ofEntries(Map.entry("bounces", "w_bounces"),
            Map.entry("hops", "w_hops"), Map.entry("workitem_created_at", "w_created_at"),
            Map.entry("workitem_due_at", "w_due_at"), Map.entry("desc_size", "w_desc_size"),
            Map.entry("num_attachments", "w_num_attachments"), Map.entry("first_attachment_at", "w_first_attachment_at"),
            Map.entry("workitem_resolved_at", "w_resolved_at"), Map.entry("workitem_updated_at", "w_updated_at"),
            Map.entry("first_comment_at", "w_first_comment_at"), Map.entry("versions", "w_versions.name"),
            Map.entry("fix_versions", "w_fix_versions.name"), Map.entry("ingested_at", "w_ingested_at"),
            Map.entry("story_points", "w_story_points"), Map.entry(PRIORITY_ORDER, "w_priority"));

    private final ESClientFactory esClientFactory;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final OrgUsersDatabaseService orgUsersDatabaseService;

    @Autowired
    public EsWorkItemsQueryService(ESClientFactory esClientFactory, WorkItemFieldsMetaService workItemFieldsMetaService, OrgUsersDatabaseService orgUsersDatabaseService) {
        this.esClientFactory = esClientFactory;
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
    }

    public DbListResponse<DbAggregationResult> getAggReport(String company, WorkItemsFilter workItemsFilter,
                                                            WorkItemsMilestoneFilter milestoneFilter,
                                                            WorkItemsFilter.DISTINCT stack,
                                                            WorkItemsFilter.CALCULATION calculation,
                                                            OUConfiguration ouConfig,
                                                            Boolean valuesOnly, Integer page, Integer pageSize) throws IOException, SQLException {
        WorkItemsFilter.DISTINCT across = workItemsFilter.getAcross();
        Validate.notNull(across, "Across can't be missing for groupby query.");
        Validate.notNull(calculation, "Calculation cannot be empty");
        String indexNameOrAlias;
        if (across == WorkItemsFilter.DISTINCT.trend || calculation == WorkItemsFilter.CALCULATION.age) {
            indexNameOrAlias = IndexType.WORK_ITEMS.getCombinedIndexAlias(company);
        } else {
            indexNameOrAlias = IndexType.WORK_ITEMS.getPartitionedIndexName(company, workItemsFilter.getIngestedAt());
        }
        if (ouConfig != null) {
            workItemsFilter = getIntegrationUserIds(company, ouConfig, workItemsFilter);
        }

        if (across == WorkItemsFilter.DISTINCT.custom_field
                && StringUtils.isEmpty(workItemsFilter.getCustomAcross())) {
            throw new SQLException("Invalid custom field name provided. will not execute query. " +
                    "Provided field: " + workItemsFilter.getCustomAcross());
        }
        stack = stack != null ? stack : WorkItemsFilter.DISTINCT.none;
        List<DbWorkItemField> dbWorkItemsFields = List.of();
        if (across == WorkItemsFilter.DISTINCT.custom_field || stack == WorkItemsFilter.DISTINCT.custom_field) {
            dbWorkItemsFields = getDbWorkItemsFields(company, workItemsFilter);
        }
        if (calculation == WorkItemsFilter.CALCULATION.sprint_mapping) {
            milestoneFilter = milestoneFilter.toBuilder().states(List.of("past")).build();
        }
        if (calculation == WorkItemsFilter.CALCULATION.stage_bounce_report &&
                (CollectionUtils.isEmpty(workItemsFilter.getStages()) && CollectionUtils.isEmpty(workItemsFilter.getExcludeStages()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Stage Bounce Report must need stages filter. Missing or empty value of 'workitem_stages'.");
        }
        SearchRequest searchRequest = EsWorkItemQueryBuilder.buildSearchRequest(workItemsFilter, milestoneFilter,
                        List.of(), List.of(), null, dbWorkItemsFields, false, stack, calculation, indexNameOrAlias, valuesOnly, page, pageSize, false)
                .size(workItemsFilter.getCalculation() == WorkItemsFilter.CALCULATION.sprint_mapping ? Integer.MAX_VALUE : 0)
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("Index name {} and ES Query : {} ", indexNameOrAlias, queryString);
        SearchResponse<EsWorkItem> searchResponse = esClientFactory.getESClient(company).search(searchRequest, EsWorkItem.class);
        List<DbAggregationResult> aggResultFromSearchResponse = EsWorkItemConverter.getAggForWorkItemFromSearchResponse(searchResponse,
                workItemsFilter, milestoneFilter, workItemsFilter.getAcross(), stack, calculation, valuesOnly);
        return DbListResponse.of(aggResultFromSearchResponse, aggResultFromSearchResponse.size());
    }

    public DbListResponse<DbWorkItem> getWorkItemsList(String company, WorkItemsFilter workItemsFilter,
                                                       WorkItemsMilestoneFilter milestoneFilter,
                                                       OUConfiguration ouConfig,
                                                       int pageNumber, int pageSize) throws IOException {
        String index = IndexType.WORK_ITEMS.getPartitionedIndexName(company, workItemsFilter.getIngestedAt());
        Map<String, SortingOrder> sortBy = MapUtils.emptyIfNull(workItemsFilter.getSort());
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
        if (ouConfig != null) {
            workItemsFilter = getIntegrationUserIds(company, ouConfig, workItemsFilter);
        }
        SortOrder esSortOrder = sortingOrder.get().equals(SortingOrder.ASC) ? SortOrder.Asc : SortOrder.Desc;
        List<DbWorkItemField> dbWorkItemFields = getDbWorkItemsFields(company, workItemsFilter);
        SearchRequest searchRequest = EsWorkItemQueryBuilder.buildSearchRequest(workItemsFilter, milestoneFilter,
                        List.of(), List.of(), null, dbWorkItemFields, false, null, null, index, false, null, null, true)
                .size(pageSize)
                .from(pageNumber * pageSize)
                .sort(List.of(SortOptions.of(s -> s.field(v -> v.field(sortByKey).order(esSortOrder))),
                        SortOptions.of(s -> s.field(v -> v.field("w_workitem_id").order(SortOrder.Asc)))))
                .build();
        String queryString = getQueryString(searchRequest);
        log.info("Index name {} and ES Query : {} ", index, queryString);
        SearchResponse<EsWorkItem> esWorkItemSearchResponse = esClientFactory.getESClient(company).search(searchRequest, EsWorkItem.class);
        List<Hit<EsWorkItem>> hits = esWorkItemSearchResponse.hits().hits();
        List<DbWorkItem> issuesList = new ArrayList<>();
        hits.forEach(hit -> issuesList.add(hit.source() != null ? EsWorkItemConverter.getWorkItemsFromEsWorkItem(hit.source(), dbWorkItemFields,
                null, false, false, false) : null));
        return DbListResponse.of(issuesList, issuesList.size());
    }

    private List<DbWorkItemField> getDbWorkItemsFields(String company, WorkItemsFilter workItemsFilter) {
        List<DbWorkItemField> workItemFields = new ArrayList<>();
        try {
            workItemFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(),
                            true, null, null, null, null,
                            null, 0, Integer.MAX_VALUE)
                    .getRecords();
        } catch (SQLException e) {
            log.error("Error for company: " + company + " , ingestedAt : " + workItemsFilter.getIngestedAt() +
                    "while getting jiraFields : " + e.getMessage());
        }
        return workItemFields;
    }

    private WorkItemsFilter getIntegrationUserIds(String company, OUConfiguration ouConfig, WorkItemsFilter workItemsFilter) {
        if (OrgUnitHelper.isOuConfigActive(ouConfig)) {
            if (ouConfig.getAdoFields().contains("reporter") && OrgUnitHelper.doesOUConfigHaveWorkItemReporters(ouConfig)) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .reporters(orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS))
                        .build();
            }
            if (ouConfig.getAdoFields().contains("assignee") && OrgUnitHelper.doesOUConfigHaveWorkItemAssignees(ouConfig)) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .assignees(orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS))
                        .build();
            }
            if (ouConfig.getAdoFields().contains("first_assignee") && OrgUnitHelper.doesOUConfigHaveWorkItemFirstAssignees(ouConfig)) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .firstAssignees(orgUsersDatabaseService.getOuUsers(company, ouConfig, IntegrationType.AZURE_DEVOPS))
                        .build();
            }
        }
        return workItemsFilter;
    }
}
