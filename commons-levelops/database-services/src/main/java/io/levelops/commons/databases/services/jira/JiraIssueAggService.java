package io.levelops.commons.databases.services.jira;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraPriority;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbJiraSprintDistMetric;
import io.levelops.commons.databases.models.response.JiraSprintDistMetric;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraSprintConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.JiraIssueService.ASSIGNEES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.INNER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_STATUS_METADATA;
import static io.levelops.commons.databases.services.JiraIssueService.LEFT_OUTER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.TIMESTAMP_SORTABLE_COLUMNS;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USER_BASED_COLUMNS;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.computeAndGetPercentileData;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getAssigneeArrayJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getFilterForTrendStack;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getLinkedIssuesWhere;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getLinkedIssuesWhereForJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStackLinkedIssuesWhereForJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getLinksTableJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getParentIssueTypeJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getParentSPjoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getPartialValues;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getPriorityOrderJoinForReports;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSelectStmtForArray;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeColumns;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSprintAuxTable;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSprintJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSprintMappingsJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStageTableJoinForBounce;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStatusTableJoinForStages;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStatusTableJoinForRelease;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStatusTableTransitJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStringIntegerMap;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getUserTableJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getVersionsJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isReleaseStageExcluded;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isLinkedIssuesRequired;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isSprintTblJoinRequired;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.needParentIssueTypeJoin;

@Log4j2
@Service
public class JiraIssueAggService {

    private static final Set<JiraIssuesFilter.DISTINCT> SUPPORTED_STACKS = Set.of(
            JiraIssuesFilter.DISTINCT.fix_version,
            JiraIssuesFilter.DISTINCT.issue_type,
            JiraIssuesFilter.DISTINCT.component,
            JiraIssuesFilter.DISTINCT.assignee,
            JiraIssuesFilter.DISTINCT.reporter,
            JiraIssuesFilter.DISTINCT.priority,
            JiraIssuesFilter.DISTINCT.project,
            JiraIssuesFilter.DISTINCT.version,
            JiraIssuesFilter.DISTINCT.status,
            JiraIssuesFilter.DISTINCT.epic,
            JiraIssuesFilter.DISTINCT.parent,
            JiraIssuesFilter.DISTINCT.label,
            JiraIssuesFilter.DISTINCT.custom_field,
            JiraIssuesFilter.DISTINCT.first_assignee,
            JiraIssuesFilter.DISTINCT.resolution,
            JiraIssuesFilter.DISTINCT.status_category,
            JiraIssuesFilter.DISTINCT.ticket_category,
            JiraIssuesFilter.DISTINCT.issue_created,
            JiraIssuesFilter.DISTINCT.issue_resolved,
            JiraIssuesFilter.DISTINCT.issue_updated,
            JiraIssuesFilter.DISTINCT.trend);
    private static final int DEFAULT_STACK_PARALLELISM = 2;
    public static final int DEFAULT_AGG_PAGE_SIZE = 10;
    public static final String DONE_STATUS_CATEGORY = "DONE";
    public static final String IGNORE_TERMINAL_STAGE = "Ignore_Terminal_Stage";
    private static final String PREFER_RELEASE_DEFAULT = "min";

    private final NamedParameterJdbcTemplate template;
    private final JiraProjectService jiraProjectService;
    private final JiraCustomFieldConditionsBuilder customFieldConditionsBuilder;
    private final JiraConditionsBuilder conditionsBuilder;
    private final JiraIssueQueryBuilder queryBuilder;
    private final JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;
    private final int stackParallelism;

    public JiraIssueAggService(DataSource dataSource,
                               JiraProjectService jiraProjectService,
                               JiraCustomFieldConditionsBuilder customFieldConditionsBuilder,
                               JiraConditionsBuilder conditionsBuilder,
                               JiraIssueQueryBuilder queryBuilder,
                               JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService,
                               @Nullable @Value("${jira.stack_parallelism:}") Integer stackParallelism) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.jiraProjectService = jiraProjectService;
        this.customFieldConditionsBuilder = customFieldConditionsBuilder;
        this.conditionsBuilder = conditionsBuilder;
        this.queryBuilder = queryBuilder;
        this.jiraStatusMetadataDatabaseService = jiraStatusMetadataDatabaseService;
        this.stackParallelism = Math.max(1, MoreObjects.firstNonNull(stackParallelism, DEFAULT_STACK_PARALLELISM));
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              JiraIssuesFilter filter,
                                                              List<JiraIssuesFilter.DISTINCT> stacks,
                                                              String configTableKey,
                                                              OUConfiguration ouConfig,
                                                              Map<String, List<String>> velocityStageStatusesMap) throws SQLException {
        return stackedGroupBy(company, filter, stacks, configTableKey, ouConfig, velocityStageStatusesMap, null);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              JiraIssuesFilter filter,
                                                              List<JiraIssuesFilter.DISTINCT> stacks,
                                                              String configTableKey,
                                                              OUConfiguration ouConfig,
                                                              Map<String, List<String>> velocityStageStatusesMap,
                                                              VelocityConfigDTO velocityConfigDTO)
            throws SQLException {
        String acrossLogString = filter.getAcross() + (filter.getCustomAcross() != null ? " (" + filter.getCustomAcross() + ")" : "");
        log.info("[{}] Jira Agg: started across '{}'", company, acrossLogString);
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, false, configTableKey, ouConfig, velocityStageStatusesMap, velocityConfigDTO);
        log.info("[{}] Jira Agg: done across '{}' - results={}", company, acrossLogString, result.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !SUPPORTED_STACKS.contains(stacks.get(0))) {
            return result;
        }
        JiraIssuesFilter.DISTINCT stack = stacks.get(0);
        final String customStack = (stack == JiraIssuesFilter.DISTINCT.custom_field) ? IterableUtils.getFirst(filter.getCustomStacks())
                .orElseThrow(() -> new SQLException("custom_stacks field must be present with custom_field as stack")) : null;
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] Jira Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, acrossLogString, result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] Jira Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, acrossLogString, result.getCount(), row.getKey());
                    JiraIssuesFilter newFilter;
                    boolean linkedIssuesRequired = isLinkedIssuesRequired(filter);
                    if (StringUtils.isNotEmpty(configTableKey)) {
                        newFilter = filter.toBuilder().across(stack).acrossLimit(null).build();
                    } else {
                        final JiraIssuesFilter.JiraIssuesFilterBuilder newFilterBuilder = filter.toBuilder();
                        newFilterBuilder.acrossLimit(null);
                        if (customStack != null) {
                            newFilterBuilder.customAcross(customStack);
                        }
                        List<String> filterValues = (row.getKey() != null) ? List.of(row.getKey()) : List.of();
                        switch (filter.getAcross()) {
                            case assignee:
                                if (CollectionUtils.isEmpty(filterValues)) {
                                    newFilter = newFilterBuilder.unAssigned(true).across(stack).ignoreOU(true).build();
                                } else {
                                    newFilter = newFilterBuilder.assignees(filterValues).across(stack).ignoreOU(true).build();
                                }
                                if (ListUtils.isEmpty(filter.getAssignees()) && (filter.getUnAssigned()!=null && !filter.getUnAssigned()))
                                    linkedIssuesRequired = false;
                                break;
                            case issue_type:
                                newFilter = newFilterBuilder.issueTypes(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getIssueTypes()))
                                    linkedIssuesRequired = false;
                                break;
                            case component:
                                newFilter = newFilterBuilder.components(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getComponents()))
                                    linkedIssuesRequired = false;
                                break;
                            case fix_version:
                                newFilter = newFilterBuilder.fixVersions(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getFixVersions()))
                                    linkedIssuesRequired = false;
                                break;
                            case label:
                                newFilter = newFilterBuilder.labels(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getLabels()))
                                    linkedIssuesRequired = false;
                                break;
                            case priority:
                                newFilter = newFilterBuilder.priorities(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getPriorities()))
                                    linkedIssuesRequired = false;
                                break;
                            case project:
                                newFilter = newFilterBuilder.projects(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getProjects()))
                                    linkedIssuesRequired = false;
                                break;
                            case reporter:
                                newFilter = newFilterBuilder.reporters(filterValues).across(stack).ignoreOU(true).build();
                                if (ListUtils.isEmpty(filter.getReporters()))
                                    linkedIssuesRequired = false;
                                break;
                            case status:
                                newFilter = newFilterBuilder.statuses(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getStatuses()))
                                    linkedIssuesRequired = false;
                                break;
                            case version:
                                newFilter = newFilterBuilder.versions(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getVersions()))
                                    linkedIssuesRequired = false;
                                break;
                            case epic:
                                newFilter = newFilterBuilder.epics(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getEpics()))
                                    linkedIssuesRequired = false;
                                break;
                            case parent:
                                newFilter = newFilterBuilder.parentKeys(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getParentKeys()))
                                    linkedIssuesRequired = false;
                                break;
                            case first_assignee:
                                newFilter = newFilterBuilder.firstAssignees(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getFirstAssignees()))
                                    linkedIssuesRequired = false;
                                break;
                            case custom_field:
                                if (filter.getCustomFields() != null) {
                                    Map<String, Object> newCustomFields = new HashMap<>();
                                    newCustomFields.putAll(filter.getCustomFields());
                                    newCustomFields.put(filter.getCustomAcross(), filterValues);
                                    newFilter = newFilterBuilder.customFields(newCustomFields).across(stack).build();
                                } else {
                                    Validate.notBlank(filter.getCustomAcross(), "filter.getCustomAcross() cannot be null or empty.");
                                    newFilter = newFilterBuilder.customFields(Map.of(filter.getCustomAcross(), filterValues)).across(stack).build();
                                }
                                if (filter.getCustomFields() == null || filter.getCustomFields().isEmpty())
                                    linkedIssuesRequired = false;
                                break;
                            case resolution:
                                newFilter = newFilterBuilder.resolutions(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getResolutions()))
                                    linkedIssuesRequired = false;
                                break;
                            case status_category:
                                newFilter = newFilterBuilder.statusCategories(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getStatusCategories()))
                                    linkedIssuesRequired = false;
                                break;
                            case ticket_category:
                                newFilter = newFilterBuilder.ticketCategories(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getTicketCategories()))
                                    linkedIssuesRequired = false;
                                break;
                            case sprint:
                                newFilter = newFilterBuilder.sprintNames(filterValues).across(stack).build();
                                if (ListUtils.isEmpty(filter.getSprintNames()))
                                    linkedIssuesRequired = false;
                                break;
                            case issue_created:
                            case issue_updated:
                            case issue_due:
                            case issue_due_relative:
                            case issue_resolved:
                            case trend:
                                newFilter = getFilterForTrendStack(
                                        newFilterBuilder, row, filter.getAcross(), stack, MoreObjects.firstNonNull(filter.getAggInterval(), "")).build();
                                if (filter.getAggInterval()==null || filter.getAcross()==null)
                                    linkedIssuesRequired = false;
                                break;
                            default:
                                throw new SQLException("Could not calculate stack for jira issues '" + stack + "': stacks not available across '" + filter.getAcross() + "'");
                        }
                    }
                    if (filter.getCalculation() == JiraIssuesFilter.CALCULATION.stage_bounce_report) {
                        newFilter = newFilter.toBuilder().stages(List.of(row.getStage())).build();
                    }
                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults =linkedIssuesRequired?
                            groupByAndCalculate(company, filter,true,newFilter, false, null, ouConfig, Map.of(), velocityConfigDTO).getRecords() :
                            groupByAndCalculate(company,newFilter, false, null, ouConfig, Map.of(), velocityConfigDTO).getRecords();
                    log.info("[{}] Jira Agg: --- done processing current stack for '{}' across '{}' - buckets={}, current='{}', results={}", company, stack, acrossLogString, result.getCount(), row.getKey(), CollectionUtils.size(currentStackResults));

                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            });
            // -- collecting parallel stream with custom pool
            // (note: the toList collector preserves the encountered order)
            threadPool = new ForkJoinPool(stackParallelism);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            log.info("[{}] Jira Agg: done processing stacks for '{}' across '{}' - buckets={}", company, stack, acrossLogString, result.getCount());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   JiraIssuesFilter filter,
                                                                   @Nullable Boolean valuesOnly,
                                                                   @Nullable String configTableKey,
                                                                   @Nullable OUConfiguration ouConfig,
                                                                   @Nullable Map<String, List<String>> velocityStageStatusesMap) throws SQLException {
        return groupByAndCalculate(company, filter, valuesOnly, configTableKey, ouConfig, velocityStageStatusesMap, null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                  JiraIssuesFilter filter,
                                                                  @Nullable Boolean valuesOnly,
                                                                  @Nullable String configTableKey,
                                                                  @Nullable OUConfiguration ouConfig,
                                                                  @Nullable Map<String, List<String>> velocityStageStatusesMap,
                                                                  VelocityConfigDTO velocityConfigDTO) throws SQLException {
        return groupByAndCalculate(company, null,false,filter, valuesOnly, configTableKey, ouConfig, velocityStageStatusesMap, velocityConfigDTO);

    }

        public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   JiraIssuesFilter originalFilter,
                                                                   @Nullable Boolean stackingDependencyFilter,
                                                                   JiraIssuesFilter filter,
                                                                   @Nullable Boolean valuesOnly,
                                                                   @Nullable String configTableKey,
                                                                   @Nullable OUConfiguration ouConfig,
                                                                   @Nullable Map<String, List<String>> velocityStageStatusesMap,
                                                                   VelocityConfigDTO velocityConfigDTO) throws SQLException {
        Map<String, SortingOrder> sortBy = filter.getSort();
        Boolean filterByLastSprint = false;
        if (filter.getFilterByLastSprint() != null) {
            filterByLastSprint = filter.getFilterByLastSprint();
        }
        JiraIssuesFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupby query.");

        if (StringUtils.isNotEmpty(configTableKey)) {
            across = JiraIssuesFilter.DISTINCT.none;
        }
        //PROP-1269: This block of code is meant to return all statuses from jira_status_metadata table for provided integration ids.
        //It will ignore rest of the filters since values endpoint is currently invoked without filters (except integration id filter).
        if (valuesOnly != null && valuesOnly && across == JiraIssuesFilter.DISTINCT.status) {
            return getResultsForAcrossStatusValues(company, filter);
        }
        //endregion
        boolean needVelocityStageCategory = JiraIssuesFilter.CALCULATION.velocity_stage_times_report.equals(filter.getCalculation());
        if (needVelocityStageCategory && MapUtils.isNotEmpty(velocityStageStatusesMap)) {
            across = JiraIssuesFilter.DISTINCT.velocity_stage;
            List<String> excludeVelocityStages = filter.getExcludeVelocityStages();
            if (CollectionUtils.isNotEmpty(excludeVelocityStages)) {
                excludeVelocityStages.add(IGNORE_TERMINAL_STAGE);
                filter = filter.toBuilder().excludeVelocityStages(excludeVelocityStages).build();
            } else {
                filter = filter.toBuilder().excludeVelocityStages(List.of(IGNORE_TERMINAL_STAGE)).build();
            }
        }
        if (across == JiraIssuesFilter.DISTINCT.custom_field
                && (StringUtils.isEmpty(filter.getCustomAcross()) ||
                !DbJiraField.CUSTOM_FIELD_KEY_PATTERN.matcher(filter.getCustomAcross()).matches())) {
            throw new SQLException("Invalid custom field name provided. will not execute query. " +
                    "Provided field: " + filter.getCustomAcross());
        }
        JiraIssuesFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = JiraIssuesFilter.CALCULATION.ticket_count;
        }
        Map<String, Object> params = new HashMap<>();
        boolean needUserTableStuff = false;
        Long latestIngestedDate = null;
        long currentTime = Instant.now().getEpochSecond();
        String sql, calculationComponent, optimizeLinkedIssueCalComp = null, outerCalculationComponent = null;
        String selectDistinctString, groupByString, orderByString, outerSelectDistinctString = null, outerGroupByString = null, selectDistinctStringSingleState = null, groupByStringSingleState = null;

        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        needSlaTimeStuff = needSlaTimeStuff || (filter.getOrFilter() != null && filter.getOrFilter().getExtraCriteria() != null &&
                (filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time)));
        boolean needTimeAcrossStages = false; //The ordering for this is important, this needs to be determined before parsing across
        boolean needAssignTimeStuff = false;
        boolean needStatusTransitTime = (filter.getFromState() != null && filter.getToState() != null);
        if (JiraIssuesFilter.CALCULATION.state_transition_time.equals(calculation) && !needStatusTransitTime) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'from_state' and 'to_state' must be present to generate report for state transition time");
        }
        boolean needStageBounce = (CollectionUtils.isNotEmpty(filter.getStages())
                || CollectionUtils.isNotEmpty(filter.getExcludeStages()));
        if (JiraIssuesFilter.CALCULATION.stage_bounce_report.equals(calculation) && !needStageBounce) {
            throw new IllegalArgumentException("'stages' must be present to generate report for stage bounce report");
        }
        boolean needAge = false;
        boolean needTicketCategory = CollectionUtils.isNotEmpty(filter.getTicketCategories());
        boolean needStage = false;
        boolean needAssigneesArray = false;
        boolean needSprintMappings = false;
        boolean needPagination = false;
        boolean needPriorityOrderJoin = false;
        boolean needJiraFieldsJoin = false;
        boolean requireSprints = isSprintTblJoinRequired(filter);
        boolean requireLinkedIssues = isLinkedIssuesRequired(filter);
        boolean optimizeLinkedIssueQuery = false;
        boolean needRelease = isReleaseStageExcluded(velocityStageStatusesMap, filter);
        String limitString = null;
        boolean needReleaseStagesGroupBy = false;

        if(needRelease && velocityConfigDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found");
        }

        String statusInRank = "";
        List<String> metrics = new ArrayList<>();
        Optional<String> additionalKey = Optional.empty();
        switch (calculation) {
            case hops:
                calculationComponent =
                        "MIN(hops) AS mn,MAX(hops) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY hops) AS median";
                orderByString = "mx DESC";
                break;
            case bounces:
                calculationComponent =
                        "MIN(bounces) AS mn,MAX(bounces) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY bounces) AS median";
                orderByString = "mx DESC";
                break;
            case resolution_time:
                calculationComponent =
                        "MIN(solve_time) AS mn,MAX(solve_time) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY solve_time) AS median, PERCENTILE_DISC(0.9) " +
                                "WITHIN GROUP(ORDER BY solve_time) AS p90, AVG(solve_time) AS mean";
                orderByString = "mx DESC";
                needSlaTimeStuff = true;
                if (ouConfig != null &&
                        ouConfig.getRequest() != null &&
                        ouConfig.getRequest().getFilter() != null &&
                        !ouConfig.getRequest().getFilter().isEmpty() &&
                        ouConfig.getRequest().getFilter().containsKey("metric") &&
                        ouConfig.getRequest().getFilter().get("metric") instanceof List<?> &&
                        !((List<?>) ouConfig.getRequest().getFilter().get("metric")).isEmpty()) {
                    metrics = (List<String>) ouConfig.getRequest().getFilter().get("metric");
                }
                break;
            case response_time:
                calculationComponent =
                        "MIN(resp_time) AS mn,MAX(resp_time) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY resp_time) AS median ";
                orderByString = "mx DESC";
                needSlaTimeStuff = true;
                break;
            case stage_bounce_report:
                calculationComponent = " count(DISTINCT id) AS ct,  avg(count) as mean, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY count) AS median";
                orderByString = "ct DESC";
                needStage = true;
                statusInRank = ", state";
                break;
            case assign_to_resolve:
                calculationComponent =
                        "MIN(assign) AS mn,MAX(assign) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY assign) AS median";
                orderByString = "mx DESC";
                needAssignTimeStuff = true;
                break;
            case state_transition_time:
                calculationComponent =
                        "MIN(state_transition_time) AS mn,MAX(state_transition_time) AS mx,COUNT(DISTINCT id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY state_transition_time) AS median";
                orderByString = "mx DESC";
                break;
            case stage_times_report:
                calculationComponent =
                        "MIN(time_spent) AS mn, MAX(time_spent) AS mx,COUNT(DISTINCT id) AS ct, AVG(time_spent) AS mean_time, " +
                                "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY time_spent) AS median,  " +
                                "PERCENTILE_CONT(0.90) WITHIN GROUP(ORDER BY time_spent) AS p90," +
                                "PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY time_spent) AS p95";
                orderByString = "mean_time DESC";
                needTimeAcrossStages = true;
                statusInRank = ", state";
                break;
            case velocity_stage_times_report:
                calculationComponent = " SUM(time_spent) AS total_time_spent";
                outerCalculationComponent =
                        "MIN(total_time_spent) AS mn, MAX(total_time_spent) AS mx,COUNT(DISTINCT id) AS ct, AVG(total_time_spent) AS mean_time, " +
                                "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY total_time_spent) AS median,  " +
                                "PERCENTILE_CONT(0.90) WITHIN GROUP(ORDER BY total_time_spent) AS p90," +
                                "PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY total_time_spent) AS p95";
                orderByString = "mean_time DESC";
                needTimeAcrossStages = true;
                break;
            case age:
                needAge = true;
                calculationComponent = "AVG(age) AS mean, MAX(age) AS max, " +
                        "MIN(age) AS min, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY age) " +
                        "AS median, COUNT(DISTINCT id) AS count, PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY age) AS p90, sum(story_point) as total_story_points";
                orderByString = "max DESC";
                break;
            case story_points:
                calculationComponent = "" +
                        "COUNT(DISTINCT id) AS count," +
                        "SUM(story_points) AS story_points_sum," +
                        "SUM(CASE WHEN (story_points = 0 OR story_points IS NULL) THEN 1 ELSE 0 END) AS unestimated_tickets_count";
                orderByString = "story_points_sum DESC NULLS LAST, count DESC NULLS LAST";
                break;
            case assignees:
                calculationComponent = "ARRAY_AGG(DISTINCT assignee_item) AS assignees";
                orderByString = "assignees DESC NULLS LAST";
                needAssigneesArray = true;
                break;
            case sprint_mapping:
                Validate.isTrue(across == JiraIssuesFilter.DISTINCT.sprint_mapping, "Sprint_mapping calculation is only supported across sprint_mapping");
                calculationComponent = "sprint_mapping_integration_id, sprint_mapping_sprint_id, sprint_mapping_name, sprint_mapping_goal,sprint_mapping_start_date, sprint_mapping_completed_at";
                orderByString = "sprint_mapping_completed_at DESC NULLS LAST";
                needSprintMappings = true;
                needPagination = true;
                requireSprints = false; // we never want to join the sprint mappings (historic values) with the current sprints
                filterByLastSprint = false;
                break;
            case sprint_mapping_count:
                Validate.isTrue(across == JiraIssuesFilter.DISTINCT.none, "Sprint_mapping calculation is only supported with no across");
                calculationComponent = "count(distinct (sprint_mapping_integration_id, sprint_mapping_sprint_id)) as ct";
                orderByString = "";
                needSprintMappings = true;
                limitString = ""; // no limit
                requireSprints = false; // we never want to join the sprint mappings (historic values) with the current sprints
                filterByLastSprint = false;
                break;
            case priority:
                calculationComponent = "(array_agg(priority))[1] as priority, (array_agg(priority_order))[1] as priority_order";
                orderByString = "";
                needPriorityOrderJoin = true;
                break;
            default:
                orderByString = "ct DESC";
                calculationComponent = "COUNT(DISTINCT id) as ct, sum(COALESCE(story_points,0)) as total_story_points, avg(COALESCE(story_points,0)) as mean_story_points";
                optimizeLinkedIssueQuery = MapUtils.isEmpty(filter.getParentStoryPoints())
                        && CollectionUtils.isEmpty(filter.getSprintIds())
                        && CollectionUtils.isEmpty(filter.getSprintNames());
                if (requireLinkedIssues && optimizeLinkedIssueQuery)
                    optimizeLinkedIssueCalComp = "COUNT(DISTINCT I.id) as ct, sum(COALESCE(I.story_points,0)) as total_story_points, avg(COALESCE(I.story_points,0)) as mean_story_points";
                // needSprintMappings = isSprintTblJoinRequired(filter);
                break;
        }
        String intervalColumn = "";
        String intervalColumnForTrend = "";
        String rankFilterForTrend = "";
        String rankColumnForTrend = "";
        String rankColumnForTrendLinkedIssues = "";
        String rankFilterForTrendLinkedIssues = "";
        boolean needIngestedAtFilterForTrendLinkedIssues = true;
        String ageFilter = "";
        boolean optimizableLinkedAcross = false;
        switch (across) {
            //unnest array aggs
            case component:
                groupByString = across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = getSelectStmtForArray("components", across.toString(), params,
                        filter, filter.getComponents(), "components");
                if (requireLinkedIssues && optimizeLinkedIssueQuery) {
                    optimizableLinkedAcross = true;
                    selectDistinctString = getSelectStmtForArray("I.components", across.toString(), params,
                            filter, filter.getComponents(), "components");
                }
                break;
            case label:
                groupByString = across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = getSelectStmtForArray("labels", across.toString(), params,
                        filter, filter.getLabels(), "labels");
                if (requireLinkedIssues && optimizeLinkedIssueQuery) {
                    optimizableLinkedAcross = true;
                    selectDistinctString = getSelectStmtForArray("I.labels", across.toString(), params,
                            filter, filter.getLabels(), "labels");
                }
                break;
            case version:
            case fix_version:
                groupByString = across + "," + across + "_end_date";
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = across + "," + across + "_end_date";
                break;
            case trend:
                AggTimeQueryHelper.AggTimeQuery trendAggQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName("ingested_at")
                        .across(across.toString())
                        .interval(filter.getAggInterval())
                        .isBigInt(true)
                        .sortAscending(false)
                        .isRelative(false)
                        .build());
                String aggInterval = filter.getAggInterval();
                if (StringUtils.isEmpty(aggInterval) || !AggTimeQueryHelper.isValid(aggInterval)) {
                    aggInterval = "day";
                }
                intervalColumnForTrend = trendAggQuery.getHelperColumn();
                groupByString = trendAggQuery.getGroupBy();
                orderByString = trendAggQuery.getOrderBy();
                selectDistinctString = trendAggQuery.getSelect();
                additionalKey = Optional.of(trendAggQuery.getIntervalKey());
                if (!calculation.equals(JiraIssuesFilter.CALCULATION.age)) {
                    rankColumnForTrend = ",ROW_NUMBER() OVER(PARTITION BY trend_interval, integration_id, key" + statusInRank
                            + " ORDER BY ingested_at DESC) as rank";
                    rankFilterForTrend = "rank = 1";
                }
                if (requireLinkedIssues) {
                    if (!calculation.equals(JiraIssuesFilter.CALCULATION.age)) {
                        rankColumnForTrendLinkedIssues = ",ROW_NUMBER() OVER(PARTITION BY Date_trunc('" + aggInterval
                                + "', To_timestamp(ingested_at)), integration_id, key" + statusInRank
                                + " ORDER BY ingested_at DESC) as li_rank";
                        rankFilterForTrendLinkedIssues = " WHERE li_rank = 1 ";
                    }
                    needIngestedAtFilterForTrendLinkedIssues = false;
                }
                ageFilter = "to_timestamp(ingested_at)::date=" + across + "_interval" + "::date";
                break;
            case issue_created:
            case issue_updated:
            case issue_due:
            case issue_resolved:
                latestIngestedDate = filter.getIngestedAt();
                AggTimeQueryHelper.AggTimeQuery issueModAggQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName(across.toString() + "_at")
                        .across(across.toString())
                        .interval(filter.getAggInterval())
                        .isBigInt(true)
                        .sortAscending(false)
                        .isRelative(false)
                        .build());
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = issueModAggQuery.getGroupBy();
                orderByString = issueModAggQuery.getOrderBy();
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                break;
            case issue_due_relative:
                latestIngestedDate = filter.getIngestedAt();
                AggTimeQueryHelper.AggTimeQuery issueDueRelativeAggQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName("issue_due_at")
                        .across(across.toString())
                        .interval(filter.getAggInterval())
                        .isBigInt(true)
                        .isRelative(true)
                        .sortAscending(true)
                        .build());
                intervalColumn = issueDueRelativeAggQuery.getHelperColumn();
                groupByString = issueDueRelativeAggQuery.getGroupBy();
                orderByString = issueDueRelativeAggQuery.getOrderBy();
                selectDistinctString = issueDueRelativeAggQuery.getSelect();
                additionalKey = Optional.of(issueDueRelativeAggQuery.getIntervalKey());
                break;
            case custom_field:
                if (customFieldConditionsBuilder.getCustomFieldType(company, filter.getCustomAcross(), filter.getIntegrationIds())
                        .equalsIgnoreCase("array")) {
                    selectDistinctString = "jsonb_array_elements_text(custom_fields->'" + filter.getCustomAcross() + "')" +
                            " AS custom_field";
                    if (BooleanUtils.isTrue(filter.getFilterAcrossValues())) {
                        ArrayList<String> filterValues = new ArrayList<>();
                        if (MapUtils.isNotEmpty(filter.getCustomFields())
                                && filter.getCustomFields().get(filter.getCustomAcross()) instanceof List) {
                            List<String> customAcrossValueList
                                    = List.class.cast(filter.getCustomFields().get(filter.getCustomAcross()));
                            if (CollectionUtils.isNotEmpty(customAcrossValueList)) {
                                filterValues.addAll(customAcrossValueList);
                            }
                        }

                        if (filter.getPartialMatch() != null && filter.getPartialMatch().containsKey(filter.getCustomAcross())) {
                            filterValues.addAll(getPartialValues(filter.getPartialMatch().get(filter.getCustomAcross())));
                        }
                        if (CollectionUtils.isNotEmpty(filterValues)) {
                            selectDistinctString = "UNNEST(array_intersect(" +
                                    "ARRAY(select jsonb_array_elements_text(custom_fields->'" + filter.getCustomAcross() + "')),  " +
                                    "ARRAY[ :across_filter_values ]::text[])) as custom_field";
                            params.put("across_filter_values", filterValues);
                        }
                    }
                } else {
                    selectDistinctString = "custom_fields->>'" + filter.getCustomAcross() + "' AS custom_field";
                }
                groupByString = across.toString();
                latestIngestedDate = filter.getIngestedAt();
                if (MapUtils.isNotEmpty(sortBy) && groupByString.equals(sortBy.keySet().stream().findFirst().get())) {
                    needJiraFieldsJoin = true;
                    groupByString += ", customFieldType";
                    selectDistinctString += ", customFieldType";
                }
                break;
            case none:
                groupByString = "";
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = "";
                break;
            case first_assignee:
                groupByString = " first_assignee_id, first_assignee ";
                selectDistinctString = groupByString;
                latestIngestedDate = filter.getIngestedAt();
                if (requireLinkedIssues && optimizeLinkedIssueQuery) {
                    optimizableLinkedAcross = true;
                    groupByString = " I.first_assignee_id, I.first_assignee ";
                    selectDistinctString = groupByString;
                }
                break;
            case parent:
                groupByString = " parent_key ";
                selectDistinctString = " parent_key as parent";
                latestIngestedDate = filter.getIngestedAt();
                break;
            case ticket_category:
                groupByString = " ticket_category ";
                selectDistinctString = " ticket_category ";
                latestIngestedDate = filter.getIngestedAt();
                needTicketCategory = true;
                break;
            case status:
                groupByString = (needTimeAcrossStages) ? "state " : "status ";
                selectDistinctString = (needTimeAcrossStages) ? "state " : "status ";
                latestIngestedDate = filter.getIngestedAt();
                //needTimeAcrossStages = true; => This is not correct. Do not enable this line.
                if (requireLinkedIssues && optimizeLinkedIssueQuery && !needTimeAcrossStages) {
                    optimizableLinkedAcross = true;
                    groupByString = " I." + groupByString;
                    selectDistinctString = " I." + selectDistinctString;
                }
                break;
            case stage:
                groupByString = "stage";
                selectDistinctString = (needStatusTransitTime) ? "status as stage" : "state as stage ";
                latestIngestedDate = filter.getIngestedAt();
                needStage = !needStatusTransitTime;
                break;
            case sprint_mapping:
                Validate.isTrue(calculation == JiraIssuesFilter.CALCULATION.sprint_mapping, "Only sprint_mapping calculation is supported across sprint_mapping");
                groupByString = " sprint_mapping_integration_id, sprint_mapping_sprint_id, sprint_mapping_name, sprint_mapping_goal, sprint_mapping_start_date, sprint_mapping_completed_at ";
                selectDistinctString = "array_agg(json_build_object('sprint_mapping', to_jsonb(sprint_mapping_json), 'issue_type', issue_type)) as sprint_mappings";
                latestIngestedDate = filter.getIngestedAt();
                needSprintMappings = true;
                break;
            case reporter:
            case assignee:
                groupByString = across.toString() + "_id " + ", " + across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = groupByString;
                if (requireLinkedIssues && optimizeLinkedIssueQuery) {
                    optimizableLinkedAcross = true;
                    groupByString = "I." + across.toString() + "_id " + ", I." + across.toString();
                    selectDistinctString = groupByString;
                }
                break;
            case velocity_stage:
                outerGroupByString = across.name();
                groupByString = "id," + across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = groupByString;
                outerSelectDistinctString = outerGroupByString + ",";
                selectDistinctStringSingleState = "id";
                groupByStringSingleState = "id";
                break;
            default:
                groupByString = across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = across.toString();
                if (requireLinkedIssues && optimizeLinkedIssueQuery
                        && ("project".equals(across.toString().toLowerCase())
                        || "priority".equals(across.toString().toLowerCase())
                        || "resolution".equals(across.toString().toLowerCase())
                        || (stackingDependencyFilter && "issue_type".equals(across.toString().toLowerCase())))) {
                    optimizableLinkedAcross = true;
                    selectDistinctString = "I." + selectDistinctString;
                    groupByString = "I." + groupByString;
                }
                break;
        }
        optimizeLinkedIssueQuery = optimizableLinkedAcross;
        if(optimizeLinkedIssueQuery)
            calculationComponent = optimizeLinkedIssueCalComp;
        if (calculation == JiraIssuesFilter.CALCULATION.stage_times_report) {
            if (across != JiraIssuesFilter.DISTINCT.none && !across.equals(JiraIssuesFilter.DISTINCT.status)) {
                groupByString = groupByString + ",state ";
                selectDistinctString = selectDistinctString + ",state ";
            } else {
                groupByString = "state ";
                selectDistinctString = "state ";
            }
            if (StringUtils.isNotEmpty(configTableKey)) {
                additionalKey = Optional.of("state");
            }
        }
        if (calculation == JiraIssuesFilter.CALCULATION.stage_bounce_report) {
            if (across != JiraIssuesFilter.DISTINCT.none) {
                groupByString = groupByString + ",state ";
                selectDistinctString = selectDistinctString + ",state ";
            } else {
                groupByString = "state ";
                selectDistinctString = "state ";
            }
        }
        if (calculation == JiraIssuesFilter.CALCULATION.ticket_count &&
                across == JiraIssuesFilter.DISTINCT.status && valuesOnly
                && CollectionUtils.isEmpty(filter.getStatuses()) && CollectionUtils.isEmpty(filter.getStatusCategories())) {
            selectDistinctString = (needTimeAcrossStages) ? "state " : "DISTINCT status FROM " + company + "." + ISSUES_TABLE + " UNION SELECT status ";
        }

        // -- limit
        if (limitString == null) {
            if (needPagination) {
                int pageSize = MoreObjects.firstNonNull(filter.getPageSize(), DEFAULT_AGG_PAGE_SIZE);
                int offset = pageSize * MoreObjects.firstNonNull(filter.getPage(), 0);
                limitString = " OFFSET :skip LIMIT :limit ";
                params.put("skip", offset);
                params.put("limit", pageSize);
            } else {
                Integer acrossLimit = filter.getAcrossLimit();
                if (acrossLimit != null && acrossLimit > 0) {
                    limitString = " LIMIT " + acrossLimit;
                } else {
                    limitString = "";
                }
            }
        }

        final ImmutablePair<Long, Long> snapshotRange = MoreObjects.firstNonNull(filter.getSnapshotRange(),
                ImmutablePair.nullPair());
        if (snapshotRange.getLeft() != null || snapshotRange.getRight() != null) {
            latestIngestedDate = null;
        }
        Map<String, List<String>> conditions=new HashMap<>();
        if(stackingDependencyFilter && originalFilter!=null){
           conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                            originalFilter, currentTime, latestIngestedDate, ouConfig);
        }
        else {
             conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                    filter, currentTime, latestIngestedDate, ouConfig);
        }

        if (requireLinkedIssues && !optimizeLinkedIssueQuery) {
            conditions.get(JIRA_ISSUE_LINKS).add("from_issue_key IN ( select key from issues )");
        }

        if (calculation.equals(JiraIssuesFilter.CALCULATION.age) && StringUtils.isNotEmpty(ageFilter)) {
            conditions.get(FINAL_TABLE).add(ageFilter);
        }
        if (StringUtils.isNotEmpty(rankFilterForTrend)) {
            conditions.get(FINAL_TABLE).add(rankFilterForTrend);
        }

        if (needAssignTimeStuff) {
            conditions.get(ISSUES_TABLE).add("first_assigned_at IS NOT NULL");
        }
        if (across == JiraIssuesFilter.DISTINCT.epic) {
            conditions.get(ISSUES_TABLE).add("epic IS NOT NULL");
            conditions.get(JIRA_ISSUE_LINKS).add("epic IS NOT NULL");
        }
        if (across == JiraIssuesFilter.DISTINCT.issue_due || across == JiraIssuesFilter.DISTINCT.issue_due_relative) {
            conditions.get(ISSUES_TABLE).add("issue_due_at IS NOT NULL");
            conditions.get(JIRA_ISSUE_LINKS).add("issue_due_at IS NOT NULL");
        }
        if (across == JiraIssuesFilter.DISTINCT.issue_resolved) {
            conditions.get(ISSUES_TABLE).add("issue_resolved_at IS NOT NULL");
            conditions.get(JIRA_ISSUE_LINKS).add("issue_resolved_at IS NOT NULL");
        }
        if (across == JiraIssuesFilter.DISTINCT.parent) {
            conditions.get(ISSUES_TABLE).add("parent_key IS NOT NULL");
            conditions.get(JIRA_ISSUE_LINKS).add("parent_key IS NOT NULL");
        }

        String finalWhere = "";
        if (conditions.get(FINAL_TABLE).size() > 0) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        }
        if (across == JiraIssuesFilter.DISTINCT.custom_field) {
            conditions.get(ISSUES_TABLE).add("custom_fields ?? :custom_field_name");
            params.put("custom_field_name", filter.getCustomAcross());
        }
        String ageColumn = "";
        if (needAge) {
            ageColumn = ",(ingested_at-issue_created_at)/86400 AS age, COALESCE(story_points, 0) as story_point";

            //If we need age column and across is trend then we need list of ingested_ats (snapshots)
            //If we need age column and are inside stack i.e. across is specific (assignee, custom_field etc.) and NOT trend, then we run query only on one ingested_at which is set in getFilterForTrendStack
            if ((across == JiraIssuesFilter.DISTINCT.trend) && (snapshotRange != null && StringUtils.isNotBlank(filter.getAggInterval()))) {
                // get age intervals
                var now = Instant.now();
                var rangeFrom = snapshotRange.getLeft() != null ? snapshotRange.getLeft() : now.minus(Duration.ofDays(90)).getEpochSecond();
                var rangeTo = snapshotRange.getRight() != null ? snapshotRange.getRight() : now.getEpochSecond();
                var intervals = AggTimeQueryHelper.getIngestedAtSetForInterval(rangeFrom, rangeTo, filter.getAggInterval());
                if (CollectionUtils.isNotEmpty(intervals)) {
                    conditions.get(ISSUES_TABLE).add("ingested_at IN (:age_range_ingested_at_list)");
                    params.put("age_range_ingested_at_list", intervals.stream().collect(Collectors.toList()));
                }
            }
        }
        String usersWhere = "";
        if (conditions.get(USERS_TABLE).size() > 0) {
            usersWhere = " WHERE " + String.join(" AND ", conditions.get(USERS_TABLE));
            needUserTableStuff = true;
        }
        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }

        String statusWhere = "";
        if (conditions.get(STATUSES_TABLE).size() > 0) {
            statusWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
        }

        String sprintWhere = "";
        if (conditions.get(JIRA_ISSUE_SPRINTS).size() > 0) {
            sprintWhere = " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_SPRINTS));
        }

        String versionWhere = "";
        if (conditions.get(JIRA_ISSUE_VERSIONS).size() > 0) {
            versionWhere = " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_VERSIONS));
        }

        String jiraIssueLinkWhere = "";
        if (conditions.get(JIRA_ISSUE_LINKS).size() > 0) {
            jiraIssueLinkWhere = requireLinkedIssues && optimizeLinkedIssueQuery ? " AND L." + String.join(" AND L.", conditions.get(JIRA_ISSUE_LINKS)) : " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_LINKS));
        }

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String slaTimeColumnsForLinkedIssues = "";
        String slaTimeJoinForLinkedIssues = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time";
            if (requireLinkedIssues) {
                slaTimeColumnsForLinkedIssues = slaTimeColumns;
            }
            if (CollectionUtils.isNotEmpty(filter.getExcludeStages())) {
                slaTimeColumns = getSlaTimeColumns(company, currentTime, "issues");
                slaTimeColumnsForLinkedIssues = getSlaTimeColumns(company, currentTime, "linked_issues");
            }
            slaTimeJoin = getSlaTimeJoin(company);
            if (requireLinkedIssues) {
                slaTimeJoinForLinkedIssues = slaTimeJoin;
            }
        }
        String assigneeDurationColumn = "";
        if (needAssignTimeStuff) {
            assigneeDurationColumn = ",(COALESCE(issue_resolved_at," + currentTime + ")-first_assigned_at) AS assign";
        }

        String userTableJoin = "";
        if (needUserTableStuff) {
            userTableJoin = getUserTableJoin(company, usersWhere, "issues");
        }
        String parentSPjoin = "";
        boolean needParentSPStuff = filter.getParentStoryPoints() != null || (filter.getOrFilter() != null && filter.getOrFilter().getParentStoryPoints() != null);
        if (needParentSPStuff) {
            parentSPjoin = getParentSPjoin(company);
        }
        String statusTableJoin = "";
        String statusTableJoinForLinkedIssues = "";
        if (needStatusTransitTime) {
            statusTableJoin = getStatusTableTransitJoin(company, "issues");
            if (requireLinkedIssues) {
                statusTableJoinForLinkedIssues = getStatusTableTransitJoin(company, "linked_issues");
            }
            params.put("from_status", filter.getFromState().toUpperCase());
            params.put("to_status", filter.getToState().toUpperCase());
        }
        if (needTimeAcrossStages || needStage) {
            statusTableJoin = getStatusTableJoinForStages(company, statusWhere, "issues");
            if (requireLinkedIssues) {
                statusTableJoinForLinkedIssues = getStatusTableJoinForStages(company, "", "linked_issues");
            }
        }

        String assigneeArrayJoin = "";
        String assigneeArrayJoinForLinkedIssues = "";
        if (needAssigneesArray) {
            List<String> assigneesTableConditions = conditions.get(ASSIGNEES_TABLE);
            assigneesTableConditions.add("assignee != '_UNASSIGNED_'");
            String assigneesWhere = (assigneesTableConditions.isEmpty()) ? "" : " WHERE " + String.join(" AND ", assigneesTableConditions) + " ";
            assigneeArrayJoin = getAssigneeArrayJoin(company, assigneesWhere, "issues");
            if (requireLinkedIssues) {
                assigneeArrayJoinForLinkedIssues = getAssigneeArrayJoin(company, "", "linked_issues");
            }
        }

        String ticketCategoryColumn = "";
        if (needTicketCategory) {
            String ticketCategorySql = queryBuilder.generateTicketCategorySql(company, params, filter, currentTime);
            ticketCategoryColumn = String.format(", (%s) as ticket_category ", ticketCategorySql);
            log.debug("ticketCategoryColumn={}", ticketCategoryColumn);
        }
        String velocityStageCategoryColumn = "";
        if (needVelocityStageCategory) {
            List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> statusCategoryMetadataList = jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
            String velocityStageSql = queryBuilder.generateVelocityStageSql(velocityStageStatusesMap, params, statusCategoryMetadataList);
            velocityStageCategoryColumn = StringUtils.isNotEmpty(velocityStageSql) ?
                    String.format(", (%s) as velocity_stage ", velocityStageSql) : StringUtils.EMPTY;
            log.debug("velocityStageCategoryColumn={}", velocityStageCategoryColumn);
        }

        String stageBounceJoin = "";
        if (JiraIssuesFilter.CALCULATION.stage_bounce_report.equals(calculation)
                && needStageBounce) {
            String stageBounceWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
            stageBounceJoin = getStageTableJoinForBounce(company, stageBounceWhere, false);
        }

        String sprintMappingsJoin = "";
        String sprintMappingsJoinForLinkedIssues = "";
        boolean isSprintMapping = false;
        if (needSprintMappings) {
            List<String> sprintMappingsConditions = new ArrayList<>();
            if (filter.getSprintMappingIgnorableIssueType() != null) {
                sprintMappingsConditions.add("ignorable_issue_type = :sprint_mapping_ignorable_issue_type");
                params.put("sprint_mapping_ignorable_issue_type", filter.getSprintMappingIgnorableIssueType());
            }
            JiraSprintFilter.JiraSprintFilterBuilder sprintFilterBuilder = JiraSprintFilter.builder();
            if (filter.getSprintMappingSprintIds() != null) {
                sprintFilterBuilder.sprintIds(filter.getSprintMappingSprintIds());
            }
            if (filter.getSprintMappingSprintCompletedAtAfter() != null) {
                sprintFilterBuilder.completedAtAfter(filter.getSprintMappingSprintCompletedAtAfter());
            }
            if (filter.getSprintMappingSprintCompletedAtBefore() != null) {
                sprintFilterBuilder.completedAtBefore(filter.getSprintMappingSprintCompletedAtBefore());
            }
            if (filter.getSprintMappingSprintStartedAtAfter() != null) {
                sprintFilterBuilder.startDateAfter(filter.getSprintMappingSprintStartedAtAfter());
            }
            if (filter.getSprintMappingSprintStartedAtBefore() != null) {
                sprintFilterBuilder.startDateBefore(filter.getSprintMappingSprintStartedAtBefore());
            }
            if (filter.getSprintMappingSprintPlannedCompletedAtAfter() != null) {
                sprintFilterBuilder.endDateAfter(filter.getSprintMappingSprintPlannedCompletedAtAfter());
            }
            if (filter.getSprintMappingSprintPlannedCompletedAtBefore() != null) {
                sprintFilterBuilder.endDateBefore(filter.getSprintMappingSprintPlannedCompletedAtBefore());
            }
            if (filter.getSprintMappingSprintNames() != null) {
                sprintFilterBuilder.names(filter.getSprintMappingSprintNames());
            }
            if (filter.getSprintMappingSprintNameStartsWith() != null) {
                sprintFilterBuilder.nameStartsWith(filter.getSprintMappingSprintNameStartsWith());
            }
            if (filter.getSprintMappingSprintNameEndsWith() != null) {
                sprintFilterBuilder.nameEndsWith(filter.getSprintMappingSprintNameEndsWith());
            }
            if (filter.getSprintMappingSprintNameContains() != null) {
                sprintFilterBuilder.nameContains(filter.getSprintMappingSprintNameContains());
            }
            if (filter.getSprintMappingExcludeSprintNames() != null) {
                sprintFilterBuilder.excludeNames(filter.getSprintMappingExcludeSprintNames());
            }
            if (filter.getSprintMappingSprintState() != null) {
                sprintFilterBuilder.state(filter.getSprintMappingSprintState());
            }
            if (filter.getIntegrationIds() != null) {
                sprintFilterBuilder.integrationIds(filter.getIntegrationIds());
            }
            List<String> sprintConditions = new ArrayList<>();
            JiraSprintConditionsBuilder.generateSprintsConditions(sprintConditions, "sprint_mapping_", params, sprintFilterBuilder.build());
            String sprintMappingSprintWhere = sprintConditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", sprintConditions);
            sprintWhere = sprintMappingSprintWhere; // LEV-3251 - missing conditions in sprintTableJoin so, replacing with the sprintMappingSprintWhere
            String sprintMappingsWhere = sprintMappingsConditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", sprintMappingsConditions);

            String sprintOrderBy = "";
            if (MapUtils.isNotEmpty(sortBy)) {
                sprintOrderBy = " ORDER BY " + getOrderBy(metrics, sortBy, across, calculation);
                isSprintMapping = true;
            }
            if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
                // If sprint count is specified, the sorting specification from the request is overridden
                sprintOrderBy = " order by end_date desc nulls last ";
            }

            sprintMappingsJoin = getSprintMappingsJoin(company, sprintMappingSprintWhere, sprintMappingsWhere, sprintOrderBy, "issues");
            if (requireLinkedIssues) {
                sprintMappingsJoinForLinkedIssues = getSprintMappingsJoin(company, "", "", sprintOrderBy, "linked_issues");
            }
        }
        String sprintTableJoin = requireSprints ? getSprintJoinStmt(company, filter, INNER_JOIN, sprintWhere, "issues") : "";
        String sprintTableJoinForLinkedIssues = (requireSprints && requireLinkedIssues) ? getSprintJoinStmt(company, filter,
                LEFT_OUTER_JOIN, "", "linked_issues") : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, filter, sprintWhere) : "";
        if (across == JiraIssuesFilter.DISTINCT.sprint) {
            orderByString = "sprint_creation_date DESC ";
            groupByString = groupByString + ",sprint_creation_date ";
        }

        String priorityOrderJoin = "";
        String priorityOrderJoinForLinkedIssues = "";
        if (needPriorityOrderJoin) {
            priorityOrderJoin = getPriorityOrderJoinForReports(company, "issues");
            if (requireLinkedIssues) {
                priorityOrderJoinForLinkedIssues = getPriorityOrderJoinForReports(company, "linked_issues");
            }
        }

        String sortAcrossString = "";
        boolean isAcrossSort = false;
        if (MapUtils.isNotEmpty(sortBy) && (!isSprintMapping) && (!(across.equals(JiraIssuesFilter.DISTINCT.none) &&
                (sortBy.keySet().stream().findFirst().get().equals(JiraIssuesFilter.DISTINCT.none.toString()))))) {
            if (across.toString().equals(sortBy.keySet().stream().findFirst().get()) && (!TIMESTAMP_SORTABLE_COLUMNS.contains(across))) {
                sortAcrossString = "SELECT * FROM (";
                isAcrossSort = true;
            }
            orderByString = getOrderBy(metrics, sortBy, across, calculation);
        }

        boolean requireVersionTableJoin = ((MapUtils.isNotEmpty(sortBy)
                && (sortBy.keySet().stream().findFirst().get().equals(JiraIssuesFilter.DISTINCT.version.toString())
                || sortBy.keySet().stream().findFirst().get().equals(JiraIssuesFilter.DISTINCT.fix_version.toString())))
                || JiraIssuesFilter.DISTINCT.version.equals(filter.getAcross())
                || JiraIssuesFilter.DISTINCT.fix_version.equals(filter.getAcross()));
        String versionTableJoin = "";
        String statusTableJoinForRelease = "";
        String versionTableJoinForRelease = "";
        String versionTableJoinForLinkedIssues = "";
        if (requireVersionTableJoin) {
            versionTableJoin = getVersionsJoinStmt(company, across.toString(), INNER_JOIN, versionWhere, "issues");
            if (requireLinkedIssues) {
                versionTableJoinForLinkedIssues = getVersionsJoinStmt(company, across.toString(), LEFT_OUTER_JOIN, "", "linked_issues");
            }
        }
        String releaseStageName = "";
        if (needRelease) {
            needReleaseStagesGroupBy = true;
            List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            List<String> terminationStageValues = preDevSortedStages.get(preDevSortedStages.size() - 2).getEvent().getValues();

            VelocityConfigDTO.Stage releaseStage = preDevSortedStages.get(preDevSortedStages.size() - 1);
            releaseStageName = releaseStage.getName();
            Map<String, List<String>> stageParam = releaseStage.getEvent().getParams();
            String preferRelease = (stageParam == null ? PREFER_RELEASE_DEFAULT : stageParam.getOrDefault("prefer_release", List.of(PREFER_RELEASE_DEFAULT)).get(0));
            statusTableJoinForRelease = getStatusTableJoinForRelease(company, statusWhere, "issues", terminationStageValues, preferRelease);
            versionTableJoinForRelease = getVersionsJoinStmt(company, "fix_version", INNER_JOIN, versionWhere, "issues", needRelease);
        }

        boolean needParentIssueTypeJoin = needParentIssueTypeJoin(filter);
        String parentIssueTypeJoinStmt = "";
        if (needParentIssueTypeJoin) {
            parentIssueTypeJoinStmt = getParentIssueTypeJoinStmt(company, filter, "issues");
        }

        String orderByAndLimitStatements = "";
        if (valuesOnly && MapUtils.isNotEmpty(sortBy) && StringUtils.isNotEmpty(orderByString) && !across.equals(JiraIssuesFilter.DISTINCT.none)) {
            orderByAndLimitStatements += (isAcrossSort) ? orderByString : " ORDER BY " + orderByString + " ";
        } else if (!valuesOnly) {
            if (StringUtils.isNotEmpty(orderByString) && !isAcrossSort) {
                orderByAndLimitStatements += " ORDER BY " + orderByString;
            }
            if (isAcrossSort) {
                orderByAndLimitStatements += orderByString;
            }
            orderByAndLimitStatements += limitString;
        }

            List<String> customTblConditions = new ArrayList<>();
            String linkedIssuesWhere="";
            if(stackingDependencyFilter) {
                if (filter.getCustomFields() != null && !filter.getCustomFields().isEmpty() && CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
                    customFieldConditionsBuilder.createCustomFieldConditions(company, params, "", filter.getIntegrationIds(), filter.getCustomFields(), customTblConditions, true, "I.", new ArrayList<>(),true);
                }
                linkedIssuesWhere = requireLinkedIssues ? (optimizeLinkedIssueQuery ? (getStackLinkedIssuesWhereForJoinStmt(originalFilter, filter, params, needIngestedAtFilterForTrendLinkedIssues, customTblConditions)) : (getLinkedIssuesWhere(filter, params, needIngestedAtFilterForTrendLinkedIssues))) : "";
            }
           else{
                linkedIssuesWhere = requireLinkedIssues ? (optimizeLinkedIssueQuery ? (getLinkedIssuesWhereForJoinStmt(filter, params, needIngestedAtFilterForTrendLinkedIssues)) : (getLinkedIssuesWhere(filter, params, needIngestedAtFilterForTrendLinkedIssues))) : "";
            }
        String linkTableJoin = requireLinkedIssues ? (optimizeLinkedIssueQuery ? (getLinksTableJoinStmt(company, jiraIssueLinkWhere, linkedIssuesWhere)) : (getLinksTableJoinStmt(company, jiraIssueLinkWhere))) : "";

        String baseWithClause = StringUtils.isNotEmpty(sprintsWithClause) ? " , issues AS " : "WITH issues AS ";
        String baseWithSql = baseWithClause
                + "( SELECT * " + intervalColumnForTrend + " FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + parentIssueTypeJoinStmt
                + issuesWhere
                + " )";
            if (filter.getIssueReleasedRange() != null
                    && filter.getIssueReleasedRange().getLeft() != null
                    && filter.getIssueReleasedRange().getRight() != null
                    && StringUtils.isEmpty(parentIssueTypeJoinStmt)
                    && StringUtils.isEmpty(intervalColumnForTrend)
            ) {
                baseWithSql = baseWithClause
                        + "( SELECT distinct(key) as jira_issue_key, issues.* " + " FROM "
                        + company + "." + ISSUES_TABLE + " AS issues"
                        + queryBuilder.releaseTableJoinToBaseSql(company, filter)
                        + issuesWhere
                        + " )";
            }
        if (needJiraFieldsJoin) {
            baseWithSql = baseWithClause + "( SELECT i.*, jf.fieldtype as customFieldType "
                    + intervalColumnForTrend + " FROM " + company + "." + ISSUES_TABLE + " AS i "
                    + INNER_JOIN + " " + company + ".jirafields AS jf ON jf.fieldkey = '"
                    + filter.getCustomAcross() + "'  AND integrationid IN (:jira_integration_ids)"
                    + parentIssueTypeJoinStmt
                    + issuesWhere + " )";
        }

        List<DbAggregationResult> results;
        String selectStmt = (needSlaTimeStuff ? " FROM ( SELECT issco.*,p.*" : "")
                + " FROM ( SELECT *"
                + assigneeDurationColumn
                + ageColumn
                + intervalColumn
                + ticketCategoryColumn
                + velocityStageCategoryColumn;
        String outerVelocityCategorySelectStmt = needVelocityStageCategory ? "SELECT " + outerSelectDistinctString + outerCalculationComponent + " FROM (" : StringUtils.EMPTY;
        String outerVelocityCategoryGroupByOrderStmt = needVelocityStageCategory ? ") AS final_table_1 GROUP BY " + outerGroupByString + orderByAndLimitStatements : orderByAndLimitStatements;
        String outerVelocityCategorySelectStmtSingleState = needVelocityStageCategory ? "SELECT 'SingleState' as velocity_stage," + outerCalculationComponent + " FROM (" : StringUtils.EMPTY;
        String outerVelocityCategoryGroupByOrderStmtSingleState = needVelocityStageCategory ? ") AS final_table_1 " + orderByAndLimitStatements : orderByAndLimitStatements;
        if (StringUtils.isNotEmpty(configTableKey)) {
            //Not checking for empty selectDistinctString because the across value cannot be none
            String upperSelectStmt = "SELECT " + (needTimeAcrossStages ? selectDistinctString + "," : "") + "'" + configTableKey + "'" + " as config_key "
                    + (valuesOnly ? "" : ("," + calculationComponent));
            String upperSelectStmtForIssues = requireLinkedIssues ? "SELECT * " : upperSelectStmt;
            String selectStmtForIssues = selectStmt
                    + slaTimeColumns
                    + rankColumnForTrend;
            String selectStmtForLinkedIssues = selectStmt
                    + slaTimeColumnsForLinkedIssues
                    + intervalColumnForTrend
                    + rankColumnForTrendLinkedIssues;
            String linkedIssuesSql = "";
            if (requireLinkedIssues) {
                var selectLinkedIssue = "";
                var findLinkedIssue = "";
                if (!optimizeLinkedIssueQuery) {
                    selectLinkedIssue = selectStmtForLinkedIssues
                            + " FROM " + company + "." + ISSUES_TABLE + " linked_issues";
                    findLinkedIssue = " WHERE key IN (SELECT to_issue_key FROM (";
                }
                linkedIssuesSql = upperSelectStmt
                        + selectLinkedIssue
                        + statusTableJoinForLinkedIssues
                        + sprintTableJoinForLinkedIssues
                        + versionTableJoinForLinkedIssues
                        + assigneeArrayJoinForLinkedIssues
                        + sprintMappingsJoinForLinkedIssues
                        + priorityOrderJoinForLinkedIssues
                        + slaTimeJoinForLinkedIssues
                        + findLinkedIssue;
            }
            String intermediateSql = upperSelectStmtForIssues + selectStmtForIssues
                    + " FROM issues "
                    + userTableJoin
                    + linkTableJoin
                    + statusTableJoin
                    + sprintTableJoin
                    + versionTableJoin
                    + assigneeArrayJoin
                    + sprintMappingsJoin
                    + stageBounceJoin
                    + priorityOrderJoin
                    + slaTimeJoin
                    + " ) as finaltable"
                    + parentSPjoin
                    + finalWhere;
            String linkedSubQuery = requireLinkedIssues ? (optimizeLinkedIssueQuery ? linkTableJoin : (intermediateSql + " ) li ) " + linkedIssuesWhere + " ) req_linked_issues " + rankFilterForTrendLinkedIssues)) : intermediateSql;
            sql = outerVelocityCategorySelectStmt
                    + sprintsWithClause
                    + baseWithSql
                    + sortAcrossString
                    + linkedIssuesSql
                    + linkedSubQuery
                    + (needTimeAcrossStages ? " GROUP BY " + groupByString : "")
                    + outerVelocityCategoryGroupByOrderStmt;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params,
                    DbJiraIssueConverters.distinctRowMapper("config_key", valuesOnly ? null : calculation, additionalKey));
        } else {
            String upperSelectStmt = "SELECT " + selectDistinctString
                    + (valuesOnly ? "" : (StringUtils.isNotEmpty(selectDistinctString) ? "," : "") + calculationComponent);
            String upperSelectStmtForIssues = requireLinkedIssues ? "SELECT * " : upperSelectStmt;
            String selectStmtForIssues = selectStmt
                    + slaTimeColumns
                    + rankColumnForTrend;
            String selectStmtForLinkedIssues = selectStmt
                    + slaTimeColumnsForLinkedIssues
                    + intervalColumnForTrend
                    + rankColumnForTrendLinkedIssues;
            String linkedIssuesSql = "";
            String sqlSingleState = "";
            if (requireLinkedIssues) {
                var selectLinkedIssue = "";
                var findLinkedIssue = "";
                if (!optimizeLinkedIssueQuery) {
                    selectLinkedIssue = selectStmtForLinkedIssues
                            + " FROM " + company + "." + ISSUES_TABLE + " linked_issues";
                    findLinkedIssue = " WHERE key IN (SELECT to_issue_key FROM (";
                }
                linkedIssuesSql = upperSelectStmt
                        + selectLinkedIssue
                        + statusTableJoinForLinkedIssues
                        + sprintTableJoinForLinkedIssues
                        + versionTableJoinForLinkedIssues
                        + assigneeArrayJoinForLinkedIssues
                        + sprintMappingsJoinForLinkedIssues
                        + priorityOrderJoinForLinkedIssues
                        + slaTimeJoinForLinkedIssues
                        + findLinkedIssue;
            }
            String intermediateSql = upperSelectStmtForIssues + selectStmtForIssues
                    + " FROM issues "
                    + userTableJoin
                    + linkTableJoin
                    + statusTableJoin
                    + sprintTableJoin
                    + versionTableJoin
                    + assigneeArrayJoin
                    + sprintMappingsJoin
                    + priorityOrderJoin
                    + stageBounceJoin
                    + slaTimeJoin
                    + " ) as finaltable"
                    + parentSPjoin
                    + finalWhere;
            String linkedSubQuery = requireLinkedIssues ? (optimizeLinkedIssueQuery ? linkTableJoin : (intermediateSql + " ) li ) " + linkedIssuesWhere + " ) req_linked_issues " + rankFilterForTrendLinkedIssues)) : intermediateSql;
            sql = outerVelocityCategorySelectStmt
                    + sprintsWithClause
                    + baseWithSql
                    + sortAcrossString
                    + linkedIssuesSql
                    + linkedSubQuery
                    + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY " + groupByString : "")
                    + String.format(statusTableJoinForRelease, " '" + releaseStageName + "' as velocity_stage, ")
                    + versionTableJoinForRelease
                    + (needReleaseStagesGroupBy ? " GROUP BY " + groupByString : "")
                    + outerVelocityCategoryGroupByOrderStmt;
            if (BooleanUtils.isTrue(filter.getCalculateSingleState())) {
                String upperSelectStmtSingleState = "SELECT " + selectDistinctStringSingleState
                        + (valuesOnly ? "" : (StringUtils.isNotEmpty(selectDistinctStringSingleState) ? "," : "") + calculationComponent);
                String intermediateSqlSingleState = upperSelectStmtSingleState + selectStmtForIssues
                        + " FROM issues "
                        + userTableJoin
                        + linkTableJoin
                        + statusTableJoin
                        + sprintTableJoin
                        + versionTableJoin
                        + assigneeArrayJoin
                        + sprintMappingsJoin
                        + priorityOrderJoin
                        + stageBounceJoin
                        + slaTimeJoin
                        + " ) as finaltable"
                        + parentSPjoin
                        + finalWhere;
                if (!needRelease) {
                    sqlSingleState = outerVelocityCategorySelectStmtSingleState
                            + sprintsWithClause
                            + baseWithSql
                            + sortAcrossString
                            + linkedIssuesSql
                            + intermediateSqlSingleState
                            + (requireLinkedIssues ? " ) li ) " + linkedIssuesWhere + " ) req_linked_issues " + rankFilterForTrendLinkedIssues : "")
                            + (StringUtils.isNotEmpty(groupByStringSingleState) ? " GROUP BY " + groupByStringSingleState : "")
                            + outerVelocityCategoryGroupByOrderStmtSingleState;
                } else {
                    sqlSingleState = outerVelocityCategorySelectStmtSingleState
                            + "SELECT id, sum(total_time_spent) as total_time_spent FROM ( "
                            + sprintsWithClause
                            + baseWithSql
                            + sortAcrossString
                            + linkedIssuesSql
                            + intermediateSqlSingleState
                            + (requireLinkedIssues ? " ) li ) " + linkedIssuesWhere + " ) req_linked_issues " + rankFilterForTrendLinkedIssues : "")
                            + "GROUP BY id"
                            + String.format(statusTableJoinForRelease, "")
                            + versionTableJoinForRelease
                            + (needReleaseStagesGroupBy ? " GROUP BY id ": "")
                            + ") as final_table_2 "
                            + (StringUtils.isNotEmpty(groupByStringSingleState) ? " GROUP BY " + groupByStringSingleState : "")
                            + outerVelocityCategoryGroupByOrderStmtSingleState;
                }
                sql = "( " + sql + " ) UNION ALL ( " + sqlSingleState + " )";
            }
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            String key = across.toString();
            if ((JiraIssuesFilter.DISTINCT.status.name().equals(key)) && (needTimeAcrossStages)) {
                key = "state";
            }
            if (USER_BASED_COLUMNS.contains(across)) {
                key = across.toString() + "_id";
                additionalKey = Optional.of(across.toString());
            }

            results = template.query(sql, params,
                    DbJiraIssueConverters.distinctRowMapper(key, valuesOnly ? null : calculation, additionalKey));
        }
        if (across == JiraIssuesFilter.DISTINCT.priority && results.size() > 0 &&
                filter.getIntegrationIds() != null) {
            // Maxime: this code doesn't look good enough to me - we should revisit it.
            // Adding an arbitrary limit as a work-around for LEV-3063.
            List<DbJiraPriority> jiraPriorityList = jiraProjectService.getPriorities(company, filter.getIntegrationIds(), 0, 1000);
            Map<String, Integer> namePriorityOrderMap = getStringIntegerMap(jiraPriorityList);
            results.sort((o1, o2) -> {
                if (namePriorityOrderMap.containsKey(o1.getKey()) && namePriorityOrderMap.containsKey(o2.getKey())) {
                    return namePriorityOrderMap.get(o1.getKey()).compareTo(namePriorityOrderMap.get(o2.getKey()));
                }
                return -1;
            });
        }
        return DbListResponse.of(results, results.size());
    }

    private static String metricToKeyMapping(List<String> metricValue) {
        Map<String, String> metricToKeyMap = new HashMap<>();
        metricToKeyMap.put("median_resolution_time", "median");
        metricToKeyMap.put("average_resolution_time", "mean");
        metricToKeyMap.put("90th_percentile_resolution_time", "p90");
        metricToKeyMap.put("number_of_tickets_closed", "ct");
        String key=metricToKeyMap.get(metricValue.get(0));
        return key;
    }

    public DbListResponse<DbAggregationResult> getResultsForAcrossStatusValues(String company, JiraIssuesFilter filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("integration_id IN (:jira_integration_ids)");
            params.put("jira_integration_ids",
                    filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeIntegrationIds())) {
            conditions.add("integration_id NOT IN (:not_jira_integration_ids)");
            params.put("not_jira_integration_ids",
                    filter.getExcludeIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getStatusCategories())) {
            conditions.add("status_category IN (:status_categories)");
            params.put("status_categories",
                    filter.getStatusCategories());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeStatusCategories())) {
            conditions.add("status_category NOT IN (:not_status_categories)");
            params.put("not_status_categories",
                    filter.getExcludeStatusCategories());
        }
        String whereClause = "";
        if (conditions.size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        String sql = "SELECT DISTINCT status FROM " + company + "." + JIRA_STATUS_METADATA + whereClause + " ORDER BY status ASC";
        List<DbAggregationResult> results = template.query(sql, params, DbJiraIssueConverters.statusMetadataRowMapperForValues());
        return DbListResponse.of(results, results.size());
    }

    private static String getOrderBy(List<String> metrics, Map<String, SortingOrder> sort, JiraIssuesFilter.DISTINCT across,
                                     JiraIssuesFilter.CALCULATION calculation) {
        String groupByField = sort.keySet().stream().findFirst().get();
        SortingOrder sortOrder = sort.values().stream().findFirst().get();
        if (JiraIssuesFilter.DISTINCT.custom_field.equals(across) && String.valueOf(across).equals(groupByField)) {
            String numberSort = "CASE customFieldType WHEN 'number' THEN " + groupByField + " :: numeric ELSE null END " + sortOrder + " NULLS LAST";
            String otherSort = ", CASE customFieldType WHEN 'text' THEN " + groupByField + " :: text ELSE null END " + sortOrder + " NULLS LAST";
            return ") sort ORDER BY " + numberSort + " " + otherSort;
        }
        if (!across.toString().equals(groupByField)) {
            if (!calculation.toString().equals(groupByField) &&
                    !(List.of(JiraIssuesFilter.CALCULATION.sprint_mapping, JiraIssuesFilter.CALCULATION.sprint_mapping_count).contains(calculation))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + groupByField);
            }
            switch (calculation) {
                case age:
                case hops:
                case bounces:
                case response_time:
                case assign_to_resolve:
                case stage_times_report:
                case stage_bounce_report:
                case state_transition_time:
                    return "median " + sortOrder + " NULLS LAST";
                case resolution_time:
                    String orderByStringMetricKey = "median";
                    if (CollectionUtils.isNotEmpty(metrics)) {
                        String metricKey = metricToKeyMapping(metrics);
                        if (metricKey != null) {
                            orderByStringMetricKey = metricKey;
                        }
                    }
                    return orderByStringMetricKey + " " + sortOrder + " NULLS LAST";
                case sprint_mapping_count:
                case sprint_mapping:
                    if (!(List.of("start_date", "end_date").contains(groupByField))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + groupByField);
                    }
                    if (groupByField.equals("start_date") || groupByField.equals("end_date")) {
                        return groupByField + " " + sortOrder;
                    }
                case story_points:
                    return "story_points_sum " + sortOrder + " NULLS LAST";
                case assignees:
                    return "assignees " + sortOrder + " NULLS LAST";
                case priority:
                    // lower priority order implies higher the priority
                    sortOrder = SortingOrder.DESC.equals(sortOrder) ? SortingOrder.ASC : SortingOrder.DESC;
                    return "priority_order " + sortOrder + " NULLS LAST";
                case ticket_count:
                    return "ct " + sortOrder + " NULLS LAST";
            }
        }
        if (TIMESTAMP_SORTABLE_COLUMNS.contains(across)) {
            return across + "_interval " + sortOrder + " NULLS LAST";
        }
        if (across.equals(JiraIssuesFilter.DISTINCT.version) || across.equals(JiraIssuesFilter.DISTINCT.fix_version)) {
            sortOrder = SortingOrder.DESC;
            if (MapUtils.isNotEmpty(sort)) {
                Optional<SortingOrder> sortOrderOptional = sort.values().stream().findFirst();
                if (sortOrderOptional.isPresent()
                        && (sort.keySet().stream().findFirst().get().equals(JiraIssuesFilter.DISTINCT.version.toString())
                        || sort.keySet().stream().findFirst().get().equals(JiraIssuesFilter.DISTINCT.fix_version.toString()))) {
                    sortOrder = sortOrderOptional.get();
                }
            }
            String versionColumn;
            versionColumn = across.toString();
            groupByField = versionColumn + "_end_date ";
            return ") sort ORDER BY " + groupByField + sortOrder + " NULLS LAST";
        }
        if (calculation.equals(JiraIssuesFilter.CALCULATION.stage_times_report) &&
                across.equals(JiraIssuesFilter.DISTINCT.status) &&
                JiraIssuesFilter.DISTINCT.status.toString().equalsIgnoreCase(groupByField)) {
            return ") sort ORDER BY LOWER(state) " + sortOrder + " NULLS LAST";
        }
        return ") sort ORDER BY LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST";
    }

    public DbListResponse<JiraSprintDistMetric> getSprintDistributionReport(String company, JiraSprintFilter.CALCULATION calculation,
                                                                            JiraSprintFilter sprintFilter) {

        List<String> sprintConditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        JiraSprintConditionsBuilder.generateSprintsConditions(sprintConditions, "", params, sprintFilter);
        String sprintMappingSprintWhere = sprintConditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", sprintConditions);

        List<String> jiraStatusCondition = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sprintFilter.getDistributionStages())) {
            jiraStatusCondition.add("UPPER(status) IN (:dist_stages)");
            params.put("dist_stages", sprintFilter.getDistributionStages().stream().map(StringUtils::upperCase).collect(Collectors.toList()));
        }
        String jiraStatusesWhere = jiraStatusCondition.isEmpty() ? "" : " WHERE " + String.join(" AND ", jiraStatusCondition);

        String sprintLimit = "";
        Integer sprintCount = sprintFilter.getSprintCount();
        if (sprintCount != null && sprintCount > 0) {
            sprintLimit = " Limit " + sprintCount + " ";
        }

        String sprintKeysStmt = "  WITH sprint_dist AS (SELECT sj.sprint_id, sj.sprint_full_name, sj.issue_key, sj.planned, sj.story_points, sj.start_date AS sprint_start, jis.start_time AS key_status_start " +
                "         FROM   (SELECT jisp.*, jism.issue_key, jism.planned, jism.story_points " +
                "                 FROM   (SELECT integration_id, issue_key, sprint_id, planned, story_points_delivered as story_points " +
                "                         FROM   " + company + ".jira_issue_sprint_mappings) jism " +
                "                        INNER JOIN (SELECT integration_id, name as sprint_full_name, sprint_id, start_date, completed_at " +
                "                                    FROM   " + company + ".jira_issue_sprints" + sprintMappingSprintWhere + " order by end_date desc " + sprintLimit + " ) jisp " +
                "                                ON jisp.integration_id = jism.integration_id " +
                "                                   AND jisp.sprint_id = jism.sprint_id) sj " +
                "                INNER JOIN (SELECT * " +
                "                            FROM   " + company + ".jira_issue_statuses " + jiraStatusesWhere + ") jis " +
                "                        ON sj.issue_key = jis.issue_key " +
                "                           AND jis.start_time > sj.start_date " +
                "                           AND jis.start_time < sj.completed_at) ";

        String totalStoryPoints = "sum(story_points) over(partition by sprint_id)";
        String cumulativeCalcComp;
        String totalCalcComp;
        if (calculation == JiraSprintFilter.CALCULATION.sprint_story_points_report) {
            cumulativeCalcComp = "sum(story_points) over(partition by sprint_id order by key_status_start)";
            totalCalcComp = totalStoryPoints;
        } else {
            cumulativeCalcComp = "count(*) over(partition by sprint_id order by key_status_start)";
            totalCalcComp = "count(*) over(partition by sprint_id)";
        }

        String sprintDistStmt = sprintKeysStmt + " select *, " + cumulativeCalcComp + " as cumulative_comp, " +
                totalCalcComp + " as total_comp, " + totalStoryPoints + " as tot_story_points " +
                " FROM sprint_dist ";

        log.info("Sprint distribution sql: {}", sprintDistStmt);
        log.info("Sprint distribution params: {}", params);
        List<DbJiraSprintDistMetric> results = template.query(sprintDistStmt, params, (rs, rowNumber) -> DbJiraSprintDistMetric.builder()
                .sprint(rs.getString("sprint_full_name"))
                .key(rs.getString("issue_key"))
                .planned(rs.getBoolean("planned"))
                .storyPoints(rs.getInt("story_points"))
                .percentile(rs.getInt("cumulative_comp") * 100f / rs.getInt("total_comp"))
                .totalTimeTaken(rs.getInt("key_status_start") - rs.getInt("sprint_start"))
                .deliveredStoryPoints(rs.getInt("tot_story_points"))
                .build());

        Map<String, List<DbJiraSprintDistMetric>> metricsPerSprint = results.stream()
                .collect(Collectors.groupingBy(DbJiraSprintDistMetric::getSprint, Collectors.toList()));

        List<Integer> completionPercentiles = sprintFilter.getCompletionPercentiles();
        if (completionPercentiles == null || completionPercentiles.isEmpty()) {
            completionPercentiles = List.of(25, 50, 75, 100);
        }

        List<JiraSprintDistMetric> finalResult = new ArrayList<>();
        for (var entry : metricsPerSprint.entrySet()) {
            List<DbJiraSprintDistMetric> sprintMetrics = entry.getValue();
            sprintMetrics.sort(Comparator.comparing(DbJiraSprintDistMetric::getPercentile));
            finalResult.addAll(computeAndGetPercentileData(completionPercentiles, calculation, entry.getKey(), sprintMetrics));
        }
        return DbListResponse.of(finalResult, finalResult.size());
    }
}
