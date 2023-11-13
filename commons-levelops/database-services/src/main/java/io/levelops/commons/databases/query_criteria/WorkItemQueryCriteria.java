package io.levelops.commons.databases.query_criteria;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.IssueMgmtCustomFieldUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.Query;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.utils.IssueMgmtPartialMatchUtil.createPartialMatchFilter;
import static java.util.Map.entry;

@Log4j2
@Component
public class WorkItemQueryCriteria {

    public static final String DEFAULT_TICKET_CATEGORY = "Other";
    private static final Set<String> MULTI_VALUED_ATTRIBUTES = Set.of("teams");
    private static final String WORKITEM_PREFIX = "workitem_";
    public static final Map<String,String> PARTIAL_MATCH_ARRAY_COLUMNS = Map.of(WORKITEM_PREFIX+"components","components", WORKITEM_PREFIX+"labels", "labels",WORKITEM_PREFIX+"versions","versions",WORKITEM_PREFIX+ "fix_versions","fix_versions");
    //TODO : standardize partial_match after LFE-1760 is done
    public static final Map<String,String> PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS = Map.of("teams","teams");
    public static final Map<String,String> PARTIAL_MATCH_ATTRIBUTES_COLUMNS = Map.of("code_area","code_area", "acceptance_criteria","acceptance_criteria","organization","organization");
    public static final Map<String,String> PARTIAL_MATCH_COLUMNS = Map.ofEntries(entry(WORKITEM_PREFIX+"project","project"),
            entry(WORKITEM_PREFIX+"summary","summary"),entry(WORKITEM_PREFIX+"assignee","assignee"),entry(WORKITEM_PREFIX+"epic","epic"),
            entry(WORKITEM_PREFIX+"reporter","reporter"), entry(WORKITEM_PREFIX+"status","status"),entry(WORKITEM_PREFIX+"priority","priority"),
            entry(WORKITEM_PREFIX+"parent_workitem_id","parent_workitem_id"),entry(WORKITEM_PREFIX+"workitem_type","workitem_type"),entry( WORKITEM_PREFIX+"resolution","resolution"), entry(WORKITEM_PREFIX+"status_category","status_category"));
    public static final List<String> WORKITEMS_COLUMNS = List.of("id", "workitem_id",	"integration_id",	"summary",	"sprint_ids",	"priority",	"assignee",	"assignee_id",	"epic",
            "parent_workitem_id",	"reporter",	"reporter_id",	"status",	"workitem_type",	"story_points",	"ingested_at",	"custom_fields",
            "project",	"components",	"labels",	"versions",	"fix_versions",	"resolution",	"status_category",	"original_estimate",
            "desc_size",	"hops",	"bounces",	"num_attachments",	"workitem_created_at",	"workitem_updated_at",	"workitem_resolved_at",
            "workitem_due_at",	"first_attachment_at",	"first_comment_at",	"attributes",	"is_active");
    private static final boolean USE_INGESTED_AT_BY_INTEGRATION = true;

    public static Query getSelectionCriteria(String company, WorkItemsFilter workItemsFilter, List<DbWorkItemField> workItemCustomFields,
                                             String tblQualifier, String paramPrefix, final OUConfiguration ouConfig) {
        return getSelectionCriteria(company, workItemsFilter, workItemCustomFields, null, tblQualifier, paramPrefix, false, ouConfig);
    }

    public static Query getSelectionCriteria(String company, WorkItemsFilter workItemsFilter, List<DbWorkItemField> workItemCustomFields, WorkItemsFilter.DISTINCT stack,
                                             String tblQualifier, String paramPrefix, boolean isDrilldown,  final OUConfiguration ouConfig) {
        WorkItemsFilter.DISTINCT across = workItemsFilter.getAcross();
        if (StringUtils.isEmpty(paramPrefix)) {
            paramPrefix = "";
        }

        if (StringUtils.isEmpty(tblQualifier)) {
            tblQualifier = "";
        }

        ArrayList<Query.SelectField> selectFields = new ArrayList<>();
        Query.QueryConditions queryConditions = getSelectionConditions(company, workItemsFilter,workItemCustomFields, tblQualifier, paramPrefix, ouConfig);

        if (CollectionUtils.isNotEmpty(workItemsFilter.getExtraCriteria())) {
            populateHygieneCondition(workItemsFilter, queryConditions, tblQualifier);
        }

        boolean needTicketCategory = workItemsFilter.getTicketCategorizationSchemeId() != null
                || CollectionUtils.isNotEmpty(workItemsFilter.getTicketCategorizationFilters())
                || across == WorkItemsFilter.DISTINCT.ticket_category
                || stack == WorkItemsFilter.DISTINCT.ticket_category;
        if (needTicketCategory) {
            String ticketCategorySql = getTicketCategorySql(company, queryConditions.getQueryParams(), workItemsFilter,
                    workItemCustomFields,  tblQualifier);
            selectFields.add(Query.selectField(ticketCategorySql, "ticket_category"));
        }

        if (across != null && across.equals(WorkItemsFilter.DISTINCT.trend) && workItemsFilter.getAggInterval() != null
                && !AGG_INTERVAL.day.name().equalsIgnoreCase(workItemsFilter.getAggInterval())
                && ((workItemsFilter.getCalculation() != null && !workItemsFilter.getCalculation().equals(WorkItemsFilter.CALCULATION.age))
                        || isDrilldown)) {
            Query trendEpochForInterval = AggTimeQueryHelper.getAggTimeQueryForTimestampForList(workItemsFilter.getAggInterval(), true);
            String stageInRank = getStageInRank(workItemsFilter, isDrilldown);
            selectFields.add(Query.selectField("ROW_NUMBER() OVER(PARTITION BY "
                    + trendEpochForInterval.getSelectFields().get(0).getField()
                    + ", integration_id, workitem_id" + stageInRank
                    + " ORDER BY ingested_at DESC)", "rank"));
            queryConditions.getConditions().add("rank = 1");
        }

        if (across == WorkItemsFilter.DISTINCT.epic) {
            queryConditions.getConditions().add("epic IS NOT NULL");
        }
        if (across == WorkItemsFilter.DISTINCT.workitem_resolved_at) {
            queryConditions.getConditions().add("workitem_resolved_at IS NOT NULL");
        }
        if (across == WorkItemsFilter.DISTINCT.parent_workitem_id) {
            queryConditions.getConditions().add("parent_workitem_id IS NOT NULL");
        }
        if (across == WorkItemsFilter.DISTINCT.custom_field) {
            queryConditions.getConditions().add("custom_fields->>'" + workItemsFilter.getCustomAcross() + "' IS NOT NULL");
        }
        if (across == WorkItemsFilter.DISTINCT.attribute) {
            queryConditions.getConditions().add("attributes->>'" + workItemsFilter.getAttributeAcross() + "' IS NOT NULL");
        }
        if (across == WorkItemsFilter.DISTINCT.trend && workItemsFilter.getCalculation() == WorkItemsFilter.CALCULATION.age) {
           queryConditions.getConditions().add("date_trunc('day',to_timestamp(ingested_at)) = date_trunc('day',to_timestamp(trend_epoch))");
        }

        return Query.builder().select(selectFields).where(queryConditions, Query.Condition.AND).build();
    }

    @NotNull
    private static String getStageInRank(WorkItemsFilter workItemsFilter, boolean isDrilldown) {
        if (isDrilldown)
            return "";
        String stageInRank;
        if (workItemsFilter.getCalculation().equals(WorkItemsFilter.CALCULATION.stage_bounce_report)) {
            stageInRank = ",stage";
        } else if (workItemsFilter.getCalculation().equals(WorkItemsFilter.CALCULATION.stage_times_report)) {
            stageInRank = ",timeline_field_value";
        } else {
            stageInRank = "";
        }
        return stageInRank;
    }

    public static String getGroupByKey(WorkItemsFilter filter, WorkItemsFilter.DISTINCT acrossOrStack, Boolean isStack) {
        if (WorkItemsFilter.isAcrossTimeField(acrossOrStack)) {
            return acrossOrStack + "_epoch";
        } else if (acrossOrStack.equals(WorkItemsFilter.DISTINCT.attribute)) {
            String attributeColumn = (isStack ? filter.getAttributeStack() : filter.getAttributeAcross());
            return acrossOrStack + "_" + attributeColumn;
        } else if (acrossOrStack.equals(WorkItemsFilter.DISTINCT.custom_field)) {
            String customColumn = (isStack ? filter.getCustomStack() : filter.getCustomAcross());
            customColumn = sanitizeAlias(customColumn, List.of("\\.", "-"));
            return acrossOrStack + "_" + customColumn;
        } else if (acrossOrStack.equals(WorkItemsFilter.DISTINCT.sprint)) {
            return "milestone_full_name";
        } else if (WorkItemsFilter.isAcrossUsers(acrossOrStack)) {
            return acrossOrStack + "_id";
        } else {
            return acrossOrStack.toString();
        }
    }

    public static Query getGroupByCriteria(WorkItemsFilter filter) {
        WorkItemsFilter.DISTINCT across = filter.getAcross();
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();

        Query query;
        if (WorkItemsFilter.isAcrossTimeField(across)) {
            String key = across.toString();
            boolean isBigInt = false;
            if (across == WorkItemsFilter.DISTINCT.trend) {
                key = "ingested_at";
                isBigInt = true;
            }

            query = AggTimeQueryHelper.getAggTimeQueryForTimestamp(key, across.toString(),
                    filter.getAggInterval(), isBigInt, false, false);
        } else {
            query = getGroupByCriteria(filter, across, false);
        }
        query.setCriteria(Query.conditions(queryConditions, queryParams));
        return query;
    }

    public static Query getGroupByCriteria(WorkItemsFilter filter, WorkItemsFilter.DISTINCT acrossOrStack, boolean isStack) {

        List<Query.SelectField> selectFields = new ArrayList<>();
        List<Query.GroupByField> groupByFields = new ArrayList<>();
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();

        switch (acrossOrStack) {
            case none:
                break;
            case label:
            case version:
            case fix_version:
            case component:
                String groupBySelectField = "UNNEST(" + acrossOrStack + "s) ";
                selectFields.add(Query.selectField(groupBySelectField, acrossOrStack.toString()));
                groupByFields.add(Query.groupByField(acrossOrStack.toString()));
                break;
            case custom_field:
                String customColumn = (isStack ? filter.getCustomStack() : filter.getCustomAcross());
                String sanitizedColumn = sanitizeAlias(customColumn, List.of("\\.", "-"));
                selectFields.add(Query.selectField(" custom_fields->> '" + customColumn + "'", acrossOrStack.toString() + "_" +
                        sanitizedColumn));
                groupByFields.add(Query.groupByField(acrossOrStack + "_" + sanitizedColumn));
                queryConditions.add(" custom_fields->> '" + customColumn + "' IS NOT NULL");
                break;
            case attribute:
                String attributeColumn = (isStack ? filter.getAttributeStack() : filter.getAttributeAcross());
                if (MULTI_VALUED_ATTRIBUTES.contains(attributeColumn)) {
                    selectFields.add(Query.selectField(" jsonb_array_elements_text(attributes-> '" +
                            attributeColumn + "')", acrossOrStack.toString() + "_" + attributeColumn));
                    groupByFields.add(Query.groupByField(acrossOrStack.toString() + "_" + attributeColumn));
                } else {
                    selectFields.add(Query.selectField(" attributes->> '" + attributeColumn + "'",
                            acrossOrStack.toString() + "_" + attributeColumn));
                    groupByFields.add(Query.groupByField(acrossOrStack.toString() + "_" + attributeColumn));
                }
                queryConditions.add(" attributes->> '" + attributeColumn + "' IS NOT NULL");
                break;
            case sprint_mapping:
                groupByFields.add(Query.groupByField("sprint_mapping_integration_id"));
                groupByFields.add(Query.groupByField("sprint_mapping_sprint_id"));
                groupByFields.add(Query.groupByField("sprint_mapping_name"));
                groupByFields.add(Query.groupByField("sprint_mapping_start_date"));
                groupByFields.add(Query.groupByField("sprint_mapping_completed_at"));
                break;
            case sprint:
                groupByFields.add(Query.groupByField("milestone_full_name"));
                break;
            case reporter:
            case assignee:
                groupByFields.add(Query.groupByField(acrossOrStack + "_id "));
                groupByFields.add(Query.groupByField(acrossOrStack.toString()));
                break;
            case first_assignee:
            case ticket_category:
            case resolution:
            case workitem_type:
            case status_category:
            case parent_workitem_id:
            case project:
            case status:
            case priority:
            case epic:
            case story_points:
            case stage:
            default:
                groupByFields.add(Query.groupByField(acrossOrStack.toString()));
                break;
        }


        Query query = Query.builder().select(selectFields)
                .groupBy(groupByFields)
                .build();
        query.setCriteria(Query.conditions(queryConditions, queryParams));
        return query;
    }

    private static String getTicketCategorySql(String company, Map<String, Object> params, WorkItemsFilter filter,
                                               List<DbWorkItemField> workItemCustomFields, String tblQualifier) {
        if (!CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters())) {
            return String.format("'%s'", DEFAULT_TICKET_CATEGORY);
        }
        MutableInt nbCategory = new MutableInt(0);
        List<String> categoryCases = ListUtils.emptyIfNull(filter.getTicketCategorizationFilters()).stream()
                .sorted(Comparator.comparingInt(cat -> MoreObjects.firstNonNull(cat.getIndex(), Integer.MAX_VALUE)))
                .map(category -> {
                    String paramPrefix = String.format("ticket_category_%d_", nbCategory.getAndIncrement());
                    Query.QueryConditions workItemQueryConditions = WorkItemQueryCriteria.getSelectionConditions(company, category.getFilter(),
                            workItemCustomFields, tblQualifier, paramPrefix, null /* OU filters will be set on the final table */);
                    String categoryWhereClause = String.join(" AND ", workItemQueryConditions.getConditions());
                    params.putAll(workItemQueryConditions.getQueryParams());
                    if (StringUtils.isEmpty(categoryWhereClause)) {
                        return null;
                    }
                    return String.format(" WHEN (%s) THEN '%s' ", categoryWhereClause, category.getName());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (categoryCases.isEmpty()) {
            return String.format("'%s'", DEFAULT_TICKET_CATEGORY);
        }
        return "CASE " +
                String.join("", categoryCases) +
                String.format(" ELSE '%s' ", DEFAULT_TICKET_CATEGORY) +
                " END";
    }

    private static Query.QueryConditions getSelectionConditions(String company, WorkItemsFilter workItemsFilter,
                                                                List<DbWorkItemField> workItemCustomFields, String tblQualifier,
                                                                String paramPrefix,
                                                                final OUConfiguration ouConfig) {
        List<String> queryConditions = new ArrayList<>();
        Map<String, Object> queryParams = new HashMap<>();
        if (CollectionUtils.isNotEmpty(workItemsFilter.getIntegrationIds())) {
            queryConditions.add(tblQualifier + "integration_id IN (:" + paramPrefix + "integs)");
            queryParams.put(paramPrefix + "integs",
                    workItemsFilter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getPartialMatch())) {
            createPartialMatchFilter(workItemsFilter.getPartialMatch(), queryConditions, queryParams, tblQualifier,
                    PARTIAL_MATCH_COLUMNS, PARTIAL_MATCH_ARRAY_COLUMNS, PARTIAL_MATCH_ARRAY_ATTRIBUTES_COLUMNS, PARTIAL_MATCH_ATTRIBUTES_COLUMNS,
                    true);
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getPriorities())) {
            queryConditions.add(tblQualifier + "priority IN (:" + paramPrefix + "incl_priorities)");
            queryParams.put(paramPrefix + "incl_priorities", workItemsFilter.getPriorities());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludePriorities())) {
            queryConditions.add(tblQualifier + "priority NOT IN (:" + paramPrefix + "excl_priorities)");
            queryParams.put(paramPrefix + "excl_priorities", workItemsFilter.getExcludePriorities());
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getAttributes())) {
            createAttributesConditions(queryParams, workItemsFilter.getAttributes(), queryConditions, paramPrefix,  true);
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getExcludeAttributes())) {
            createAttributesConditions(queryParams, workItemsFilter.getExcludeAttributes(), queryConditions, paramPrefix, false);
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
            createCustomFieldConditions(tblQualifier, paramPrefix,
                    queryParams, workItemsFilter.getCustomFields(), workItemCustomFields, queryConditions, true);
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getExcludeCustomFields())) {
            createCustomFieldConditions(tblQualifier, paramPrefix,
                    queryParams, workItemsFilter.getExcludeCustomFields(),workItemCustomFields, queryConditions, false);
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getEpics())) {
            queryConditions.add(tblQualifier + "epic IN (:" + paramPrefix + "incl_epics)");
            queryParams.put(paramPrefix + "incl_epics", workItemsFilter.getEpics());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeEpics())) {
            queryConditions.add(tblQualifier + "epic NOT IN (:" + paramPrefix + "excl_epics)");
            queryParams.put(paramPrefix + "excl_epics", workItemsFilter.getExcludeEpics());
        }
        if (workItemsFilter.getWorkItemCreatedRange() != null) {
            if (workItemsFilter.getWorkItemCreatedRange().getLeft() != null) {
                queryConditions.add(tblQualifier + " workitem_created_at > to_timestamp(:" + paramPrefix + "workitem_created_gt)");
                queryParams.put(paramPrefix + "workitem_created_gt", workItemsFilter.getWorkItemCreatedRange().getLeft());
            }
            if (workItemsFilter.getWorkItemCreatedRange().getRight() != null) {
                queryConditions.add(tblQualifier + "workitem_created_at < to_timestamp(:" + paramPrefix + "workitem_created_lt)");
                queryParams.put(paramPrefix + "workitem_created_lt", workItemsFilter.getWorkItemCreatedRange().getRight());
            }
        }
        if (workItemsFilter.getWorkItemUpdatedRange() != null) {
            if (workItemsFilter.getWorkItemUpdatedRange().getLeft() != null) {
                queryConditions.add(tblQualifier + "workitem_updated_at > to_timestamp(:" + paramPrefix + "workitem_updated_at_gt)");
                queryParams.put(paramPrefix + "workitem_updated_at_gt", workItemsFilter.getWorkItemUpdatedRange().getLeft());
            }
            if (workItemsFilter.getWorkItemUpdatedRange().getRight() != null) {
                queryConditions.add(tblQualifier + "workitem_updated_at < to_timestamp(:" + paramPrefix + "workitem_updated_at_lt)");
                queryParams.put(paramPrefix + "workitem_updated_at_lt", workItemsFilter.getWorkItemUpdatedRange().getRight());
            }
        }
        if (workItemsFilter.getWorkItemResolvedRange() != null) {
            if (workItemsFilter.getWorkItemResolvedRange().getLeft() != null) {
                queryConditions.add(tblQualifier + "workitem_resolved_at > to_timestamp(:" + paramPrefix + "workitem_resolved_at_gt)");
                queryParams.put(paramPrefix + "workitem_resolved_at_gt", workItemsFilter.getWorkItemResolvedRange().getLeft());
            }
            if (workItemsFilter.getWorkItemResolvedRange().getRight() != null) {
                queryConditions.add(tblQualifier + "workitem_resolved_at < to_timestamp(:" + paramPrefix + "workitem_resolved_at_lt)");
                queryParams.put(paramPrefix + "workitem_resolved_at_lt", workItemsFilter.getWorkItemResolvedRange().getRight());
            }
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getIntegrationIdByIssueUpdatedRange())) {
            List<String> conditions = new ArrayList<>();
            List<Integer> integrationsWithTimeRange = new ArrayList<>();
            for(Map.Entry<Integer, ImmutablePair<Long, Long>> e: workItemsFilter.getIntegrationIdByIssueUpdatedRange().entrySet()) {
                Integer integrationId = e.getKey();
                ImmutablePair<Long, Long> timeRange = e.getValue();
                if(timeRange == null) {
                    continue;
                }
                if ((timeRange.getLeft() != null) || (timeRange.getRight() != null)) {
                    integrationsWithTimeRange.add(integrationId);
                    List<String> innerConditions = new ArrayList<>();
                    innerConditions.add(tblQualifier + "integration_id = :" + paramPrefix + "wi_integration_ids_fs_"+ integrationId);
                    queryParams.put(paramPrefix + "wi_integration_ids_fs_" + integrationId, integrationId);
                    if (timeRange.getLeft() != null) {
                        innerConditions.add(tblQualifier + "workitem_updated_at > to_timestamp(:" + paramPrefix + "workitem_updated_start_fs_" + integrationId + ")");
                        queryParams.put(paramPrefix + "workitem_updated_start_fs_" + integrationId, timeRange.getLeft());
                    }
                    if (timeRange.getRight() != null) {
                        innerConditions.add(tblQualifier + "workitem_updated_at < to_timestamp(:" + paramPrefix + "workitem_updated_end_fs_" + integrationId + ")");
                        queryParams.put(paramPrefix + "workitem_updated_end_fs_" + integrationId, timeRange.getRight());
                    }
                    conditions.add("( " + String.join(" AND ", innerConditions) + " )");
                }
            }
            if (CollectionUtils.isNotEmpty(integrationsWithTimeRange)) {
                conditions.add("( " + tblQualifier + "integration_id NOT IN (:" + paramPrefix + "wi_integration_ids_fs) )");
                queryParams.put(paramPrefix + "wi_integration_ids_fs", integrationsWithTimeRange);
            }
            if(CollectionUtils.isNotEmpty(conditions)) {
                queryConditions.add("( " + String.join(" OR ", conditions) + " )");
            }
        }

        if (CollectionUtils.isNotEmpty(workItemsFilter.getVersions())) {
            queryConditions.add(tblQualifier + "versions && ARRAY[:" + paramPrefix + "incl_versions ]::varchar[] ");
            queryParams.put(paramPrefix + "incl_versions", workItemsFilter.getVersions());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeVersions())) {
            queryConditions.add("NOT " + tblQualifier +  "versions && ARRAY[:" + paramPrefix + "excl_versions ]::varchar[] ");
            queryParams.put(paramPrefix + "excl_versions", workItemsFilter.getExcludeVersions());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getStatuses())) {
            queryConditions.add(tblQualifier + "status IN (:" + paramPrefix + "incl_statuses)");
            queryParams.put(paramPrefix + "incl_statuses", workItemsFilter.getStatuses());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeStatuses())) {
            queryConditions.add(tblQualifier + "status NOT IN (:" + paramPrefix + "excl_statuses)");
            queryParams.put(paramPrefix + "excl_statuses", workItemsFilter.getExcludeStatuses());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getIds())) {
            queryConditions.add(tblQualifier + "id::uuid IN (:" + paramPrefix + "incl_ids)");
            queryParams.put(paramPrefix + "incl_ids", workItemsFilter.getIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getWorkItemIds())) {
            queryConditions.add(tblQualifier + "workitem_id IN (:" + paramPrefix + "incl_workitem_ids)");
            queryParams.put(paramPrefix + "incl_workitem_ids", workItemsFilter.getWorkItemIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeWorkItemIds())) {
            queryConditions.add(tblQualifier + "workitem_id NOT IN (:" + paramPrefix + "excl_workitem_ids)");
            queryParams.put(paramPrefix + "excl_workitem_ids", workItemsFilter.getExcludeWorkItemIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getWorkItemTypes())) {
            queryConditions.add(tblQualifier + "workitem_type IN (:" + paramPrefix + "incl_workitem_types)");
            queryParams.put(paramPrefix + "incl_workitem_types", workItemsFilter.getWorkItemTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeWorkItemTypes())) {
            queryConditions.add(tblQualifier + "workitem_type NOT IN (:" + paramPrefix + "excl_workitem_types)");
            queryParams.put(paramPrefix + "excl_workitem_types", workItemsFilter.getExcludeWorkItemTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getLabels())) {
            queryConditions.add(tblQualifier + "labels && ARRAY[:" + paramPrefix + "incl_labels ]::varchar[] ");
            queryParams.put(paramPrefix + "incl_labels", workItemsFilter.getLabels());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeLabels())) {
            queryConditions.add("NOT " + tblQualifier +  "labels && ARRAY[:" + paramPrefix + "excl_labels ]::varchar[] ");
            queryParams.put(paramPrefix + "excl_labels", workItemsFilter.getExcludeLabels());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getFixVersions())) {
            queryConditions.add(tblQualifier + "fix_versions && ARRAY[:" + paramPrefix + "incl_fix_versions ]::varchar[] ");
            queryParams.put(paramPrefix + "incl_fix_versions", workItemsFilter.getFixVersions());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeFixVersions())) {
            queryConditions.add("NOT " + tblQualifier +  "fix_versions && ARRAY[:" + paramPrefix + "excl_fix_versions ]::varchar[] ");
            queryParams.put(paramPrefix + "excl_fix_versions", workItemsFilter.getExcludeFixVersions());
        }
        //redundant filters applied to workitem filters
//        if (CollectionUtils.isNotEmpty(workItemsFilter.getSprintIds())) {
//            queryConditions.add(tblQualifier + "sprint_ids && ARRAY[:" + paramPrefix + "incl_sprint_ids ]::uuid[] ");
//            queryParams.put(paramPrefix + "incl_sprint_ids", workItemsFilter.getSprintIds());
//        }
//        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeSprintIds())) {
//            queryConditions.add("NOT " + tblQualifier +  "sprint_ids && ARRAY[:" + paramPrefix + "excl_sprint_ids ]::uuid[] ");
//            queryParams.put(paramPrefix + "excl_sprint_ids", workItemsFilter.getExcludeSprintIds());
//        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getStatusCategories())) {
            queryConditions.add(tblQualifier + "status_category IN (:" + paramPrefix + "incl_status_categories)");
            queryParams.put(paramPrefix + "incl_status_categories", workItemsFilter.getStatusCategories());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeStatusCategories())) {
            queryConditions.add(tblQualifier + "status_category NOT IN (:" + paramPrefix + "excl_status_categories)");
            queryParams.put(paramPrefix + "excl_status_categories", workItemsFilter.getExcludeStatusCategories());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getAssignees()) || (OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.getAdoFields().contains("assignee"))) { // OU: assignee
            var columnName = tblQualifier + "assignee_id::text";
            if(OrgUnitHelper.doesOUConfigHaveWorkItemAssignees(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, queryParams, IntegrationType.AZURE_DEVOPS);
                if (StringUtils.isNotBlank(usersSelect)) {
                    queryConditions.add(MessageFormat.format("{0} IN (SELECT id::text FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else if (CollectionUtils.isNotEmpty(workItemsFilter.getAssignees())){
                queryConditions.add(columnName + " IN (:" + paramPrefix + "incl_assignees)");
                queryParams.put(paramPrefix + "incl_assignees", workItemsFilter.getAssignees());
            }
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeAssignees())) {
            queryConditions.add(tblQualifier + "assignee_id::text NOT IN (:" + paramPrefix + "excl_assignees)");
            queryParams.put(paramPrefix + "excl_assignees", workItemsFilter.getExcludeAssignees());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getParentWorkItemIds())) {
            queryConditions.add(tblQualifier + "parent_workitem_id IN (:" + paramPrefix + "incl_parent_workitem_ids)");
            queryParams.put(paramPrefix + "incl_parent_workitem_ids", workItemsFilter.getParentWorkItemIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeParentWorkItemIds())) {
            queryConditions.add(tblQualifier + "parent_workitem_id NOT IN (:" + paramPrefix + "excl_parent_workitem_ids)");
            queryParams.put(paramPrefix + "excl_parent_workitem_ids", workItemsFilter.getExcludeParentWorkItemIds());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getParentWorkItemTypes())) {
            queryConditions.add(tblQualifier + "parent_workitem_type IN (:" + paramPrefix + "incl_parent_workitem_types)");
            queryParams.put(paramPrefix + "incl_parent_workitem_types", workItemsFilter.getParentWorkItemTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeParentWorkItemTypes())) {
            queryConditions.add(tblQualifier + "parent_workitem_type NOT IN (:" + paramPrefix + "excl_parent_workitem_types)");
            queryParams.put(paramPrefix + "excl_parent_workitem_types", workItemsFilter.getExcludeParentWorkItemTypes());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getProjects())) {
            queryConditions.add(tblQualifier + "project IN (:" + paramPrefix + "incl_projects)");
            queryParams.put(paramPrefix + "incl_projects", workItemsFilter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeProjects())) {
            queryConditions.add(tblQualifier + "project NOT IN (:" + paramPrefix + "excl_projects)");
            queryParams.put(paramPrefix + "excl_projects", workItemsFilter.getExcludeProjects());
        }
        if (workItemsFilter.getStoryPointsRange() != null) {
            if (workItemsFilter.getStoryPointsRange().getLeft() != null) {
                queryConditions.add(tblQualifier + "story_points > :" + paramPrefix + "story_points_gt");
                queryParams.put(paramPrefix + "story_points_gt", workItemsFilter.getStoryPointsRange().getLeft());
            }
            if (workItemsFilter.getStoryPointsRange().getRight() != null) {
                queryConditions.add(tblQualifier + "story_points < :" + paramPrefix + "story_points_lt");
                queryParams.put(paramPrefix + "story_points_lt", workItemsFilter.getStoryPointsRange().getRight());
            }
        }
        if (workItemsFilter.getSnapshotRange() != null) {
            if (workItemsFilter.getSnapshotRange().getLeft() != null) {
                queryConditions.add(tblQualifier + "ingested_at > :" + paramPrefix + "snapshot_start");
                queryParams.put(paramPrefix + "snapshot_start", workItemsFilter.getSnapshotRange().getLeft());
            }
            if (workItemsFilter.getSnapshotRange().getRight() != null) {
                queryConditions.add(tblQualifier + "ingested_at < :" + paramPrefix + "snapshot_end");
                queryParams.put(paramPrefix + "snapshot_end", workItemsFilter.getSnapshotRange().getRight());
            }
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getReporters()) || (OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.getAdoFields().contains("reporter"))) { // OU: reporter
            var columnName = tblQualifier + "reporter_id::text";
            if(OrgUnitHelper.doesOUConfigHaveWorkItemReporters(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, queryParams, IntegrationType.AZURE_DEVOPS);
                if (StringUtils.isNotBlank(usersSelect)) {
                    queryConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else if (CollectionUtils.isNotEmpty(workItemsFilter.getReporters())){
                queryConditions.add(columnName + " IN (:" + paramPrefix + "incl_reporters)");
                queryParams.put(paramPrefix + "incl_reporters", workItemsFilter.getReporters());
            }
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeReporters())) {
            queryConditions.add(tblQualifier + "reporter_id::text NOT IN (:" + paramPrefix + "excl_reporters)");
            queryParams.put(paramPrefix + "excl_reporters", workItemsFilter.getExcludeReporters());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getFirstAssignees()) || (OrgUnitHelper.isOuConfigActive(ouConfig) && ouConfig.getAdoFields().contains("first_assignee"))) { // OU: first assignee
            var columnName = tblQualifier + "first_assignee";
            if(OrgUnitHelper.doesOUConfigHaveWorkItemFirstAssignees(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, queryParams, IntegrationType.AZURE_DEVOPS);
                if (StringUtils.isNotBlank(usersSelect)) {
                    queryConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else if (CollectionUtils.isNotEmpty(workItemsFilter.getAssignees())){
                queryConditions.add(columnName + " IN (:" + paramPrefix + "incl_first_assignees)");
                queryParams.put(paramPrefix + "incl_first_assignees", workItemsFilter.getFirstAssignees());
            }
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getExcludeFirstAssignees())) {
            queryConditions.add(tblQualifier + "first_assignee NOT IN (:" + paramPrefix + "excl_first_assignees)");
            queryParams.put(paramPrefix + "excl_first_assignees", workItemsFilter.getExcludeFirstAssignees());
        }
        if (CollectionUtils.isNotEmpty(workItemsFilter.getTicketCategories())) {
            queryConditions.add(tblQualifier + "ticket_category IN (:" + paramPrefix + "ticket_categories)");
            queryParams.put(paramPrefix + "ticket_categories", workItemsFilter.getTicketCategories());
        }
        if (MapUtils.isNotEmpty(workItemsFilter.getMissingFields())) {
            Map<String, Boolean> missingCustomFields = new HashMap<>();
            Map<String, Boolean> missingAttributeFields = new HashMap<>();
            Map<WorkItemsFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    WorkItemsFilter.MISSING_BUILTIN_FIELD.class);
            workItemsFilter.getMissingFields().forEach((field, shouldBeMissing) -> {
                if (Optional.ofNullable(WorkItemsFilter.MISSING_BUILTIN_FIELD.fromString(field)).isPresent()) {
                    Optional.ofNullable(WorkItemsFilter.MISSING_BUILTIN_FIELD.fromString(field))
                            .map(builtInField -> missingBuiltinFields.put(builtInField, shouldBeMissing));
                } else if (IssueMgmtCustomFieldUtils.isCustomField(field)) {
                    missingCustomFields.put(field, shouldBeMissing);
                } else {
                    missingAttributeFields.put(field, shouldBeMissing);
                }
            });
            queryConditions.addAll(getMissingFieldsClause(missingBuiltinFields,
                    missingCustomFields, missingAttributeFields, tblQualifier));
        }
        if (workItemsFilter.getIngestedAt() != null) {
            // NOTE: if ingested_at is null, we do not want to use useIngestedAtByIntegration either
            if (USE_INGESTED_AT_BY_INTEGRATION && MapUtils.isNotEmpty(workItemsFilter.getIngestedAtByIntegrationId())) {
                List<String> ingestedAtConditions = new ArrayList<>();
                final String finalParamPrefix = paramPrefix;
                workItemsFilter.getIngestedAtByIntegrationId().forEach((integrationId, latestIngestedAt) -> {
                    String param = StringUtils.trimToEmpty(finalParamPrefix) + "workitem_ingested_at_" + integrationId;
                    ingestedAtConditions.add(String.format("(" + tblQualifier + "ingested_at = :%s AND " + tblQualifier + "integration_id = '%s') ", param, integrationId));
                    queryParams.put(param, latestIngestedAt);
                });
                queryConditions.add("(" + String.join(" OR ", ingestedAtConditions) + ")");
            } else {
                queryConditions.add(tblQualifier + "ingested_at = (:" + paramPrefix + "ingested_at)");
                queryParams.put(paramPrefix + "ingested_at", workItemsFilter.getIngestedAt());
            }
        }
        return Query.conditions(queryConditions, queryParams);
    }

    private static List<String> getMissingFieldsClause(
            Map<WorkItemsFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
            Map<String, Boolean> missingCustomFields, Map<String, Boolean> missingAttributeFields, String tblQualifier) {
        List<String> missingFieldConditions = new ArrayList<>();
        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingFieldConditions.addAll(missingBuiltinFields.entrySet().stream()
                    .map(missingBuiltinField -> {
                        String clause;
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingBuiltinField.getValue());
                        switch (missingBuiltinField.getKey()) {
                            case priority:
                                clause = shouldBeMissing ? " " + tblQualifier + "priority IN ('_UNPRIORITIZED_') " : " " + tblQualifier + "priority NOT IN ('_UNPRIORITIZED_') ";
                                break;
                            case status:
                                clause = shouldBeMissing ? " " + tblQualifier + "status IN ('_UNKNOWN_') " : " " + tblQualifier + "status NOT IN ('_UNKNOWN_') ";
                                break;
                            case resolution:
                                clause = shouldBeMissing ? " " + tblQualifier + "resolution IN ('_UNKNOWN_') " : " " + tblQualifier + "resolution NOT IN ('_UNKNOWN_') ";
                                break;
                            case assignee:
                                clause = shouldBeMissing ? " " + tblQualifier + "assignee IN ('_UNASSIGNED_') " : " " + tblQualifier + "assignee NOT IN ('_UNASSIGNED_') ";
                                break;
                            case reporter:
                                clause = shouldBeMissing ? " " + tblQualifier + "reporter IN ('_UNKNOWN_') " : " " + tblQualifier + "reporter NOT IN ('_UNKNOWN_') ";
                                break;
                            case epic:
                                clause = shouldBeMissing ? " " + tblQualifier + "epic IN ('_UNKNOWN_') " : " " + tblQualifier + "epic NOT IN ('_UNKNOWN_') ";
                                break;
                            case component:
                                clause = " array_length(" + tblQualifier + "components, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case label:
                                clause = " array_length(" + tblQualifier + "labels, 1) IS " + (shouldBeMissing ? " NULL " : "NOT NULL ");
                                break;
                            case fix_version:
                                clause = " array_length(" + tblQualifier + "fix_versions, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case version:
                                clause = " array_length(" + tblQualifier + "versions, 1) IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case parent_workitem_id:
                                clause = " " + tblQualifier + "parent_workitem_id IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case status_category:
                                clause = " " + tblQualifier + "status_category IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case workitem_resolved_at:
                                clause = " " + tblQualifier + "workitem_resolved_at IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case workitem_due_at:
                                clause = " " + tblQualifier + "workitem_due_at IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case first_attachment_at:
                                clause = " " + tblQualifier + "first_attachment_at IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case first_comment_at:
                                clause = " " + tblQualifier + "first_comment_at IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            case story_points:
                                clause = " " + tblQualifier + "story_points " + (shouldBeMissing ? " IN (0) " : "NOT IN (0) ");
                                break;
                            case project:
                                clause = " " + tblQualifier + "project IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            default:
                                return null;
                        }
                        return clause;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        missingFieldConditions.addAll(getMissingFieldConditions(missingCustomFields, tblQualifier, "custom_fields"));
        missingFieldConditions.addAll(getMissingFieldConditions(missingAttributeFields, tblQualifier, "attributes"));
        return missingFieldConditions;
    }

    private static ArrayList<String> getMissingFieldConditions(Map<String, Boolean> missingFields,
                                                               String tblQualifier, String column) {
        ArrayList<String> missingFieldConditions = new ArrayList<>();
        if (MapUtils.isNotEmpty(missingFields)) {
            List<String> emptyFields = new ArrayList<>();
            List<String> nonEmptyFields = new ArrayList<>();
            missingFields.forEach((field, shouldBeMissing) -> {
                final String fieldStr = "'" + field + "'";
                if (shouldBeMissing) {
                    emptyFields.add(fieldStr);
                } else {
                    nonEmptyFields.add(fieldStr);
                }
            });
            if (CollectionUtils.isNotEmpty(emptyFields)) {
                missingFieldConditions.add("NOT " + tblQualifier + column + " ??| array[" + String.join(",", emptyFields) + "]");
            }
            if (CollectionUtils.isNotEmpty(nonEmptyFields)) {
                missingFieldConditions.add(tblQualifier + column + " ??| array[" + String.join(",", nonEmptyFields) + "]");
            }
        }
        return missingFieldConditions;
    }

    private static void populateHygieneCondition(WorkItemsFilter filter, Query.QueryConditions queryConditions,
                                                 String tblQualifier) {
        List<String> conditions = queryConditions.getConditions();
        Map<String, Object> params = queryConditions.getQueryParams();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<WorkItemsFilter.EXTRA_CRITERIA, Object> hygieneSpecs = filter.getHygieneCriteriaSpecs();
        for (WorkItemsFilter.EXTRA_CRITERIA hygieneType : filter.getExtraCriteria()) {
            switch (hygieneType) {
                case idle:
                    conditions.add(tblQualifier + "workitem_updated_at < to_timestamp(:workitem_idletime)");
                    params.put("workitem_idletime", (currentTime - NumberUtils.toInt(
                            String.valueOf(hygieneSpecs.get(WorkItemsFilter.EXTRA_CRITERIA.idle)), 30) * 86400L));
                    break;
                case no_assignee:
                    conditions.add(tblQualifier + "assignee = :workitem_no_assignee");
                    params.put("workitem_no_assignee", DbWorkItem.UNASSIGNED);
                    break;
                case no_due_date:
                    conditions.add(tblQualifier + "workitem_due_at IS NULL");
                    break;
                case poor_description:
                    conditions.add(tblQualifier + "desc_size < " + NumberUtils.toInt(String.valueOf(
                            hygieneSpecs.get(WorkItemsFilter.EXTRA_CRITERIA.poor_description)), 10));
                    break;
                case no_components:
                    conditions.add(tblQualifier + "components = '{}'");
                    break;
                case missed_response_time:
                    conditions.add("resp_time > resp_sla");
                    break;
                case missed_resolution_time:
                    conditions.add("solve_time > solve_sla");
                    break;
            }
        }
    }

    private static void createAttributesConditions(Map<String, Object> queryParams,
                                                   Map<String, List<String>> attributes,
                                                   List<String> queryConditions, String paramPrefix, Boolean include) {
        int prefix = 0;
        for (var es : attributes.entrySet()) {
            ++prefix;
            String key = es.getKey();
            List<String> values = ListUtils.emptyIfNull(attributes.get(key));
            if (CollectionUtils.isNotEmpty(values)) {
                if (include) {
                    if (MULTI_VALUED_ATTRIBUTES.contains(key)) {
                        queryConditions.add("attributes -> '" + key + "' @> ANY(ARRAY[ :"+paramPrefix+"incl_attribute_" + prefix + " ]::jsonb[])");
                        queryParams.put(paramPrefix+"incl_attribute_" + prefix, values.stream().map(val -> "\"" + StringEscapeUtils.escapeJson(val) + "\"")
                                .collect(Collectors.toList()));
                    } else {
                        queryConditions.add("attributes ->> '" + key + "' IN (:"+paramPrefix+"incl_attribute_" + prefix + ")");
                        queryParams.put(paramPrefix+"incl_attribute_" + prefix, attributes.get(key));
                    }
                } else {
                    if (MULTI_VALUED_ATTRIBUTES.contains(key)) {
                        queryConditions.add(" NOT attributes -> '" + key + "' @> ANY(ARRAY[ :"+paramPrefix+"excl_attribute_" + prefix + " ]::jsonb[])");
                        queryParams.put(paramPrefix+"excl_attribute_" + prefix, values.stream().map(val -> "\"" + StringEscapeUtils.escapeJson(val) + "\"")
                                .collect(Collectors.toList()));
                    } else {
                        queryConditions.add("attributes ->> '" + key + "' NOT IN (:"+paramPrefix+"excl_attribute_" + prefix + ")");
                        queryParams.put(paramPrefix+"excl_attribute_" + prefix, attributes.get(key));
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void createCustomFieldConditions(String tblQualifier, String paramPrefix, Map<String, Object> queryParams,
                                                    Map<String, Object> customFields, List<DbWorkItemField> workItemCustomFields,
                                                    List<String> queryConditions, Boolean include) {
        int i = 0;
        for (var es : customFields.entrySet()) {
            int prefix = i++;
            String key = es.getKey();
            Optional<DbWorkItemField> customField = Optional.empty();
            if(CollectionUtils.isNotEmpty(workItemCustomFields)) {
                customField = workItemCustomFields.stream()
                        .filter(dbWorkItemField -> dbWorkItemField.getFieldKey().equalsIgnoreCase(key)).findFirst();
            }
            String customFieldType = "";
            if(customField.isPresent()) {
                customFieldType = customField.get().getFieldType();
            }

            String fieldRef = StringUtils.defaultString(paramPrefix) + "custom_field_" + prefix;
            if (customFields.get(key) instanceof Map) {
                Map<String, String> timeRange = Map.class.cast(customFields.get(key));
                final Long rangeStart = timeRange.get("$gt") != null ? Long.valueOf(timeRange.get("$gt")) : null;
                final Long rangeEnd = timeRange.get("$lt") != null ? Long.valueOf(timeRange.get("$lt")) : null;

                if(rangeStart != null) {
                    fieldRef = fieldRef + "_start";
                    String condition = "(" + tblQualifier + "custom_fields->>'" + key + "')::float8 >= :" + fieldRef;
                    queryConditions.add(condition);
                    queryParams.put(fieldRef, DateUtils.toEpochSecond(rangeStart));
                }
                if(rangeEnd != null) {
                    fieldRef = fieldRef + "_end";
                    String condition = "(" + tblQualifier + "custom_fields->>'" + key + "')::float8 <= :" + fieldRef;
                    queryConditions.add(condition);
                    queryParams.put(fieldRef, DateUtils.toEpochSecond(rangeEnd));
                }
            } else {
                List<String> values = (List) customFields.get(key);
                if (CollectionUtils.isNotEmpty(values)) {
                    if (include) {
                        queryConditions.add("custom_fields ->> '" + key + "' IN (:" + fieldRef + ")");
                        queryParams.put(fieldRef, customFields.get(key));
                    } else {
                        queryConditions.add("custom_fields ->> '" + key + "' NOT IN (:" + fieldRef + ")");
                        queryParams.put(fieldRef, customFields.get(key));
                    }
                }
            }
        }
    }

    private static boolean isDateCustomField(String customFieldType) {
        return customFieldType.equalsIgnoreCase("datetime")
                || customFieldType.equalsIgnoreCase("date");
    }

    private static String sanitizeAlias(String column, List<String> characters) {
        String sanitizedColumn = column;
        for (String c : characters) {
            sanitizedColumn = sanitizedColumn.replaceAll(c, "_");
        }
        return sanitizedColumn;
    }

    public static List<Query.SelectField> getFieldNames(List<Query.SelectField> selectFields) {
        List<Query.SelectField> selectFieldWithoutAlias = new ArrayList<>();

        selectFields.forEach(selectField -> {
            if(StringUtils.isNotEmpty(selectField.getAlias())) {
                selectFieldWithoutAlias.add(Query.selectField(selectField.getAlias(), null));
            } else {
                selectFieldWithoutAlias.add(Query.selectField(selectField.getField(), null));
            }
        });
        return selectFieldWithoutAlias;
    }

    public static List<Query.SelectField> getAllWorkItemsFields(){
       return WORKITEMS_COLUMNS.stream().map(f -> Query.selectField(f)).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
