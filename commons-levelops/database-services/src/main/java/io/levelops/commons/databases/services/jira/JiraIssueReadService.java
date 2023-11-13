package io.levelops.commons.databases.services.jira;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraLink;
import io.levelops.commons.databases.models.database.jira.DbJiraSalesforceCase;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.JiraIssueService.ASSIGNEES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.INNER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SALESFORCE_CASES;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.LEFT_OUTER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.PARENT_STORY_POINTS;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITY_ORDER;
import static io.levelops.commons.databases.services.JiraIssueService.SORTABLE_COLUMNS;
import static io.levelops.commons.databases.services.JiraIssueService.STATE_TRANSITION_TIME;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.STORY_POINTS_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;
import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getSortedDevStages;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.*;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeColumns;

@Log4j2
@Service
public class JiraIssueReadService {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final String PREFER_RELEASE_DEFAULT = "min";
    private final NamedParameterJdbcTemplate template;
    private final JiraIssueSprintService sprintService;
    private final JiraIssueStatusService statusService;
    private final JiraConditionsBuilder conditionsBuilder;
    private final JiraIssueQueryBuilder queryBuilder;
    private final JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;

    public JiraIssueReadService(DataSource dataSource,
                                JiraIssueSprintService sprintService,
                                JiraIssueStatusService statusService,
                                JiraConditionsBuilder conditionsBuilder,
                                JiraIssueQueryBuilder queryBuilder,
                                JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.sprintService = sprintService;
        this.statusService = statusService;
        this.conditionsBuilder = conditionsBuilder;
        this.queryBuilder = queryBuilder;
        this.jiraStatusMetadataDatabaseService = jiraStatusMetadataDatabaseService;
    }

    public Optional<DbJiraIssue> get(String company, String key, String integrationId, Long ingestedAt)
            throws SQLException {
        Validate.notNull(key, "Missing key.");
        Validate.notNull(ingestedAt, "Missing ingestedAt.");
        Validate.notNull(integrationId, "Missing integrationId.");
        List<DbJiraIssue> data = template.query(
                "SELECT * FROM " + company + "." + ISSUES_TABLE
                        + " WHERE jira_issues.key = :key"
                        + " AND jira_issues.ingested_at = :ingestedat"
                        + " AND jira_issues.integration_id = :integid",
                Map.of("key", key, "integid", NumberUtils.toInt(integrationId), "ingestedat", ingestedAt),
                DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));
        return data.stream().findFirst();
    }

    public Stream<DbJiraIssue> stream(@Nonnull String company,
                                      @Nonnull JiraIssuesFilter filter,
                                      @Nullable JiraSprintFilter jiraSprintFilter,
                                      @Nullable JiraIssuesFilter linkedJiraIssuesFilter,
                                      @Nullable OUConfiguration ouConfig,
                                      @Nullable VelocityConfigDTO velocityConfigDTO,
                                      @Nullable Map<String, SortingOrder> sortBy) {
        return PaginationUtils.streamThrowingRuntime(0, 1, pageNumber ->
                list(company, filter, jiraSprintFilter, linkedJiraIssuesFilter, ouConfig, velocityConfigDTO, sortBy, pageNumber, DEFAULT_PAGE_SIZE).getRecords());
    }

    public Stream<DbJiraIssue> stream(@Nonnull String company,
                                      @Nonnull JiraIssuesFilter filter,
                                      @Nullable JiraSprintFilter jiraSprintFilter,
                                      @Nullable JiraIssuesFilter linkedJiraIssuesFilter,
                                      @Nullable OUConfiguration ouConfig,
                                      @Nullable VelocityConfigDTO velocityConfigDTO,
                                      @Nullable Map<String, SortingOrder> sortBy,
                                      int pageSize) {
        return PaginationUtils.streamThrowingRuntime(0, 1, pageNumber ->
                list(company, filter, jiraSprintFilter, linkedJiraIssuesFilter, ouConfig, velocityConfigDTO, sortBy, pageNumber, pageSize).getRecords());
    }

    public DbListResponse<DbJiraIssue> list(@Nonnull String company,
                                            @Nonnull JiraIssuesFilter filter,
                                            @Nullable JiraSprintFilter jiraSprintFilter,
                                            @Nullable JiraIssuesFilter linkedJiraIssuesFilter,
                                            @Nullable OUConfiguration ouConfig,
                                            @Nullable VelocityConfigDTO velocityConfigDTO,
                                            @Nullable Map<String, SortingOrder> sortBy,
                                            @Nullable Integer pageNumber,
                                            @Nullable Integer pageSize) throws SQLException {

        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(filter, "filter cannot be null.");
        Boolean filterByLastSprint = false;
        if (filter.getFilterByLastSprint() != null) {
            filterByLastSprint = filter.getFilterByLastSprint();
        }
        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        Map<String, List<String>> velocityStageStatusesMap = Map.of();
        boolean needVelocityStageCategory = velocityConfigDTO != null;
        if (needVelocityStageCategory) {
            List<VelocityConfigDTO.Stage> devStages = getSortedDevStages(velocityConfigDTO);
            velocityStageStatusesMap = JiraConditionUtils.getStageStatusesMap(devStages);
        }

        boolean isReleasePresent = isReleaseStageExcluded(velocityStageStatusesMap, filter);
        boolean isClickedOnReleaseStage = false;
        String releaseStageName = StringUtils.EMPTY;
        List<String> terminationStageValues = new ArrayList<>();
        String releaseVersionPreference = PREFER_RELEASE_DEFAULT;

        if(isReleasePresent) {
            List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());

            terminationStageValues = preDevSortedStages.get(preDevSortedStages.size() - 2).getEvent().getValues();
            VelocityConfigDTO.Stage releaseStage = preDevSortedStages.get(preDevSortedStages.size() - 1);
            Map<String, List<String>> stageParam = releaseStage.getEvent().getParams();
            releaseVersionPreference = (stageParam == null ? PREFER_RELEASE_DEFAULT : stageParam.getOrDefault("prefer_release", List.of(PREFER_RELEASE_DEFAULT)).get(0));

            releaseStageName = releaseStage.getName();
            if (filter.getVelocityStages().contains(releaseStageName)) {
                isClickedOnReleaseStage = true;
                filter = filter.toBuilder().velocityStages(List.of()).build();
            }
        }

        Map<String, Object> params = new HashMap<>();
        long currentTime = Instant.now().getEpochSecond();
        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, currentTime, filter.getIngestedAt(), ouConfig);
        boolean isHistorical = CollectionUtils.isNotEmpty(filter.getHistoricalAssignees());
        boolean requireLinkedIssues = JiraIssueReadUtils.isLinkedIssuesRequired(filter);
        boolean isClickedOnSingleStat = CollectionUtils.isNotEmpty(filter.getVelocityStages())
                && filter.getVelocityStages().size() == 1
                && "$$ALL_STAGES$$".equals(filter.getVelocityStages().get(0));

        if (requireLinkedIssues) {
            conditions.get(JIRA_ISSUE_LINKS).add("from_issue_key IN ( select key from issues )");
        }
        Map<String, List<String>> linkedIssuesConditions = Map.of();
        if (linkedJiraIssuesFilter != null) {
            linkedIssuesConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "linked_", linkedJiraIssuesFilter, currentTime, filter.getIngestedAt(), "", ouConfig);
        }
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "issue_created_at";
                })
                .orElse("issue_created_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        boolean requireVersionTableJoin = false;
        String versionColumn = "";
        String versionSelectColumn = "";
        if (sortByKey.equals("version") || sortByKey.equals("fix_version")) {
            versionColumn = sortByKey;
            sortByKey = sortByKey + "_end_date";
            requireVersionTableJoin = true;
            versionSelectColumn = "," + sortByKey;
        }
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        needSlaTimeStuff = needSlaTimeStuff || (filter.getOrFilter() != null && filter.getOrFilter().getExtraCriteria() != null &&
                (filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time)));

        boolean requireSprints = isSprintTblJoinRequired(filter);

        String intervalColumnForTrend = "";
        String selectDistinctStringForTrend = "";
        String rankFilterForTrend = "";
        String intervalFilterForTrend = "";
        String rankColumnForTrend = "";
        String rankColumnForTrendLinkedIssues = "";
        String rankFilterForTrendLinkedIssues = "";
        if (filter.getAcross() != null &&
                filter.getAcross().equals(JiraIssuesFilter.DISTINCT.trend) &&
                StringUtils.isNotEmpty(filter.getAggInterval()) &&
                !AGG_INTERVAL.day.name().equalsIgnoreCase(filter.getAggInterval())) {
            String aggInterval = filter.getAggInterval();
            if (StringUtils.isEmpty(aggInterval) || !AggTimeQueryHelper.isValid(aggInterval)) {
                aggInterval = "day";
            }
            AggTimeQueryHelper.AggTimeQuery trendAggQuery = AggTimeQueryHelper.getAggTimeQueryForList(AggTimeQueryHelper.Options.builder()
                    .columnName("ingested_at")
                    .across(filter.getAcross().toString())
                    .interval(aggInterval)
                    .isBigInt(true)
                    .build());
            intervalColumnForTrend = "," + trendAggQuery.getHelperColumn();
            selectDistinctStringForTrend = "," + trendAggQuery.getSelect();
            String statusInRank = "";
            if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList()))) {
                statusInRank = ", state";
            }
            rankColumnForTrend = ",ROW_NUMBER() OVER(PARTITION BY trend_interval, integration_id, key" + statusInRank
                    + " ORDER BY ingested_at DESC) as rank";
            rankFilterForTrend = "rank = 1";
            intervalFilterForTrend = " AND interval = " + AggTimeQueryHelper.getIntervalFilterString(filter.getAggInterval());
            if (requireLinkedIssues) {
                rankColumnForTrendLinkedIssues = ",ROW_NUMBER() OVER(PARTITION BY Date_trunc('" + aggInterval + "', To_timestamp(ingested_at)), integration_id, key" + statusInRank
                        + " ORDER BY ingested_at DESC) as li_rank";
                rankFilterForTrendLinkedIssues = " WHERE li_rank = 1 ";
                if (CollectionUtils.isNotEmpty(linkedIssuesConditions.get(ISSUES_TABLE))) {
                    linkedIssuesConditions.get(ISSUES_TABLE).remove("ingested_at = :jira_linked_issues_ingested_at");
                }
            }
            conditions.get(ISSUES_TABLE).remove("ingested_at = :jira_ingested_at");
            if (Objects.nonNull(filter.getIngestedAt())) {
                conditions.get(ISSUES_TABLE).add("ingested_at >= :jira_ingested_at_gt");
                conditions.get(ISSUES_TABLE).add("ingested_at < :jira_ingested_at_lt");
                params.put("jira_ingested_at_gt", filter.getIngestedAt());
                params.put("jira_ingested_at_lt", getIngestedAtUpperBound(filter.getIngestedAt(), filter.getAggInterval()));
            }
            params.put("jira_ingested_at_for_trend", filter.getIngestedAt());
            if (StringUtils.isNotEmpty(rankFilterForTrend)) {
                conditions.get(FINAL_TABLE).add(rankFilterForTrend);
            }
        }
        String linkedIssuesWhere = "";
        String linkedSprintWhere = "";
        String linkedVersionWhere = "";
        if (linkedJiraIssuesFilter != null) {
            if (linkedIssuesConditions.get(ISSUES_TABLE).size() > 0) {
                linkedIssuesWhere += " AND " + String.join(" AND ", linkedIssuesConditions.get(ISSUES_TABLE));
            }
            if (linkedIssuesConditions.get(JIRA_ISSUE_SPRINTS).size() > 0) {
                linkedSprintWhere = " WHERE " + String.join(" AND ", linkedIssuesConditions.get(JIRA_ISSUE_SPRINTS));
            }
            if (linkedIssuesConditions.get(JIRA_ISSUE_VERSIONS).size() > 0) {
                linkedVersionWhere = " WHERE " + String.join(" AND ", linkedIssuesConditions.get(JIRA_ISSUE_VERSIONS));
            }
        }

        String usersWhere = "";
        if (conditions.get(USERS_TABLE).size() > 0) {
            usersWhere = " WHERE " + String.join(" AND ", conditions.get(USERS_TABLE));
            needUserTableStuff = true;
        }
        String finalWhere = "";
        if (conditions.get(FINAL_TABLE).size() > 0) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        }

        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }
        String sprintWhere = "";
        if (conditions.get(JIRA_ISSUE_SPRINTS).size() > 0) {
            sprintWhere = " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_SPRINTS));
        }
        String versionWhere = "";
        if (conditions.get(JIRA_ISSUE_VERSIONS).size() > 0) {
            versionWhere = " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_VERSIONS));
        }
        String linkWhere = "";
        if (conditions.get(JIRA_ISSUE_LINKS).size() > 0) {
            linkWhere = " WHERE " + String.join(" AND ", conditions.get(JIRA_ISSUE_LINKS));
        }

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String slaTimeJoinForLinkedIssues = "";
        if (needSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime())) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            if (CollectionUtils.isNotEmpty(filter.getExcludeStages())) {
                slaTimeColumns = getSlaTimeColumns(company, currentTime, "issues");
            }
            slaTimeJoin = getSlaTimeJoinStmt(company, "issues");
            if (requireLinkedIssues) {
                slaTimeJoinForLinkedIssues = getSlaTimeJoinStmt(company, "linked_issues");
            }
        }
        String userTableJoin = "";
        if (needUserTableStuff) {
            userTableJoin = getUserTableJoin(company, usersWhere, "issues");
        }
        boolean needStatusTransitTime = (filter.getFromState() != null && filter.getToState() != null);
        if (STATE_TRANSITION_TIME.equals(sortByKey) && !needStatusTransitTime) {
            throw new SQLException("'from_state' and 'to_state' must be present to sort by state transition time");
        }
        String statusTableTransitJoin = "";
        String statusTableTransitJoinForLinkedIssues = "";
        if (needStatusTransitTime) {
            statusTableTransitJoin = getStatusTableTransitJoin(company, "issues");
            if (requireLinkedIssues) {
                statusTableTransitJoinForLinkedIssues = getStatusTableTransitJoin(company, "linked_issues");
            }
            params.put("from_status", filter.getFromState().toUpperCase());
            params.put("to_status", filter.getToState().toUpperCase());
        }

        String statusTableJoin = "";
        String statusTableJoinForReleaseDrillDown = "";
        String stageBounceJoin = "";
        String versionTableJoinForRelease = "";

        if (
                CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList())) ||
                        (needVelocityStageCategory && !isClickedOnSingleStat)
        ) {
            String statusWhere = "";
            if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList()))) {
                statusWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
                stageBounceJoin = getStageTableJoinForBounce(company, statusWhere, true);
            }
            statusTableJoin = getStatusTableJoinForStages(company, statusWhere, "issues");
        }
        String parentSPjoin = "";
        boolean needParentSPStuff = filter.getParentStoryPoints() != null || PARENT_STORY_POINTS.equals(sortByKey)
                || (filter.getOrFilter() != null && filter.getOrFilter().getParentStoryPoints() != null);
        if (needParentSPStuff) {
            parentSPjoin = getParentSPjoin(company);
        }
        boolean needPriorityOrdering = PRIORITY_ORDER.equals(sortByKey);
        String priorityOrderJoin = "";
        String priorityOrderJoinForLinkedIssues = "";
        if (needPriorityOrdering) {
            priorityOrderJoin = getPriorityOrderJoinForList(company, "tbl");
            if (requireLinkedIssues) {
                priorityOrderJoinForLinkedIssues = getPriorityOrderJoinForList(company, "req_linked_issues");
            }
            sortByKey = " prior_order.priority_order ";
            sortOrder = SortingOrder.DESC.equals(sortOrder) ? SortingOrder.ASC : SortingOrder.DESC;
        }

        String ticketCategoryColumn = "";
        boolean needTicketCategory = filter.getTicketCategorizationSchemeId() != null || CollectionUtils.isNotEmpty(filter.getTicketCategorizationFilters());
        if (needTicketCategory) {
            String ticketCategorySql = queryBuilder.generateTicketCategorySql(company, params, filter, currentTime);
            ticketCategoryColumn = String.format(", (%s) as ticket_category ", ticketCategorySql);
            log.debug("ticketCategoryColumn={}", ticketCategoryColumn);
        }
        String velocityStageCategoryColumn = "";
        List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> statusCategoryMetadataList = List.of();
        if (needVelocityStageCategory) {
            statusCategoryMetadataList = jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
            String velocityStageSql = queryBuilder.generateVelocityStageSql(velocityStageStatusesMap, params, statusCategoryMetadataList);
            velocityStageCategoryColumn = StringUtils.isNotEmpty(velocityStageSql) ? String.format(", (%s) as velocity_stage ", velocityStageSql) : StringUtils.EMPTY;
            log.debug("velocityStageCategoryColumn={}", velocityStageCategoryColumn);

        }

        boolean needParentIssueTypeJoin = needParentIssueTypeJoin(filter);
        String parentIssueTypeJoinStmt = "";
        String parentIssueTypeColumn = "";
        if (needParentIssueTypeJoin) {
            parentIssueTypeJoinStmt = getParentIssueTypeJoinStmt(company, filter, "issues");
            parentIssueTypeColumn = ", parent_issue_type";
        }

        String linkTableJoin = requireLinkedIssues ? getLinksTableJoinStmt(company, linkWhere) : "";
        String sprintTableJoin = requireSprints ? getSprintJoinStmt(company, filter, INNER_JOIN, sprintWhere, "issues") : "";
        String sprintTableJoinForLinkedIssues = (requireSprints && requireLinkedIssues) ? getSprintJoinStmt(company, filter,
                LEFT_OUTER_JOIN, linkedSprintWhere, "linked_issues") : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, filter, sprintWhere) : "";
        String versionTableJoin = requireVersionTableJoin ? getVersionsJoinStmt(company, versionColumn, INNER_JOIN, versionWhere, "issues") : "";
        String versionTableJoinForLinkedIssues = (requireVersionTableJoin && requireLinkedIssues) ? getVersionsJoinStmt(company, versionColumn,
                LEFT_OUTER_JOIN, linkedVersionWhere, "linked_issues") : "";

        String latestIngestedAtJoin = "";
        String joinAssigneeHistorical = "";
        if (isHistorical) {
            joinAssigneeHistorical = getIssuesAssigneeTableJoinStmt(company);
        }
        String baseWithClause = StringUtils.isNotEmpty(sprintsWithClause) ? " , issues AS " : "WITH issues AS ";
        // add condition for run in our case only
        String baseWithSql = baseWithClause
                + "( SELECT  distinct(key) as jira_issue_key, issues.* "
                + intervalColumnForTrend
                + selectDistinctStringForTrend
                + parentIssueTypeColumn
                + " FROM " + company + "." + ISSUES_TABLE + " AS issues"
                + queryBuilder.releaseTableJoinToBaseSql(company, filter)
                + joinAssigneeHistorical
                + parentIssueTypeJoinStmt
                + issuesWhere
                + " )";

        List<DbJiraIssue> results = new ArrayList<>();
        Map<String, DbJiraIssue> releaseResultMap = null;
        String releaseIntermediateSql = StringUtils.EMPTY;
        if (pageSize > 0) {
            String selectStmt = " SELECT * FROM "
                    + (isClickedOnSingleStat ? "( SELECT issues.*" : "( SELECT distinct (issues.id), issues.*")
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + ticketCategoryColumn
                    + (isClickedOnSingleStat ? "" : velocityStageCategoryColumn)
                    + rankColumnForTrend
                    + (requireLinkedIssues ? ",to_issue_key" : "")
                    + (needParentIssueTypeJoin ? ",parent_issue_type" : "");
            String selectStmtForLinkedIssues = " SELECT * FROM "
                    + (isClickedOnSingleStat ? "( SELECT linked_issues.*" : "( SELECT distinct (linked_issues.id), linked_issues.*")
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + ticketCategoryColumn
                    + (isClickedOnSingleStat ? "" : velocityStageCategoryColumn);
            String linkedIssuesSql = "";
            if (requireLinkedIssues) {
                linkedIssuesSql = selectStmtForLinkedIssues
                        + intervalColumnForTrend
                        + rankColumnForTrendLinkedIssues
                        + " FROM " + company + "." + ISSUES_TABLE + " linked_issues"
                        + slaTimeJoinForLinkedIssues
                        + sprintTableJoinForLinkedIssues
                        + versionTableJoinForLinkedIssues
                        + statusTableTransitJoinForLinkedIssues
                        + " WHERE key IN ( ";
            }
            String intermediateSql = linkedIssuesSql
                    + (requireLinkedIssues ? "SELECT to_issue_key FROM (" : "")
                    + selectStmt
                    + " FROM issues "
                    + userTableJoin
                    + linkTableJoin
                    + slaTimeJoin
                    + sprintTableJoin
                    + statusTableTransitJoin
                    + statusTableJoin
                    + versionTableJoin
                    + stageBounceJoin
                    + latestIngestedAtJoin
                    + joinAssigneeHistorical
                    + issuesWhere
                    + intervalFilterForTrend
                    + " ) as tbl"
                    + parentSPjoin
                    + priorityOrderJoin
                    + finalWhere
                    + (requireLinkedIssues ? ") li_tbl ) " + linkedIssuesWhere + ") req_linked_issues" + rankFilterForTrendLinkedIssues + priorityOrderJoinForLinkedIssues : "");
            String sql = sprintsWithClause
                    + baseWithSql
                    + intermediateSql
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS LAST, key ASC"
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            List<DbJiraIssue> tmpResults = template.query(sql, params, DbJiraIssueConverters.listRowMapper(needStatusTransitTime, needPriorityOrdering, needTicketCategory, true, false, false, false));
            //TODO : FE is sending "excludeStages" at the moment, so using it. Need to figure out how to user excludeStages() and / or excludeStatuses() filter here

            List<DbJiraIssue> releaseResults = new ArrayList<>();

            if(isReleasePresent) {
                String velocityStageSql = " '" + releaseStageName + "' ";
                String velocityStageCategoryColumnRelease = String.format(", (%s) as velocity_stage ", velocityStageSql);
                velocityStageCategoryColumnRelease += " , Greatest(release_end_time - last_stage_start_time, 0) as release_time";
                velocityStageCategoryColumnRelease += " , release_end_time ";

                String finalWhereRelease = finalWhere;
                if(StringUtils.isNotEmpty(finalWhere)
                    && finalWhere.contains("velocity_stage IN (:jira_velocity_stages)"))
                    finalWhereRelease = finalWhere.replace("velocity_stage IN (:jira_velocity_stages)" , " 1 = 1 ");


                statusTableJoinForReleaseDrillDown = getStatusTableJoinForReleaseDrillDown(company, "", "issues", terminationStageValues);
                versionTableJoinForRelease = getVersionTableJoinStmtForReleaseStage(company, "fix_version", INNER_JOIN, versionWhere, "issues", (isClickedOnReleaseStage || CollectionUtils.isEmpty(tmpResults)));
                if(!(isClickedOnReleaseStage || CollectionUtils.isEmpty(tmpResults))) {
                    params.put("issue_keys_for_release", tmpResults.stream().map(DbJiraIssue::getKey).distinct().collect(Collectors.toList()));
                }

                String selectStmtRelease = " SELECT * FROM "
                        + (isClickedOnSingleStat ? "( SELECT issues.*" : "( SELECT distinct (issues.id), issues.*")
                        + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                        + versionSelectColumn
                        + slaTimeColumns
                        + ticketCategoryColumn
                        + velocityStageCategoryColumnRelease
                        + rankColumnForTrend
                        + (requireLinkedIssues ? ",to_issue_key" : "")
                        + (needParentIssueTypeJoin ? ",parent_issue_type" : "");

                releaseIntermediateSql = linkedIssuesSql
                        + (requireLinkedIssues ? "SELECT to_issue_key FROM (" : "")
                        + selectStmtRelease
                        + " FROM issues "
                        + userTableJoin
                        + linkTableJoin
                        + slaTimeJoin
                        + sprintTableJoin
                        + statusTableTransitJoin
                        + statusTableJoinForReleaseDrillDown
                        + versionTableJoinForRelease
                        + stageBounceJoin
                        + latestIngestedAtJoin
                        + joinAssigneeHistorical
                        + issuesWhere
                        + intervalFilterForTrend
                        + " ) as tbl"
                        + parentSPjoin
                        + priorityOrderJoin
                        + finalWhereRelease
                        + (requireLinkedIssues ? ") li_tbl ) " + linkedIssuesWhere + ") req_linked_issues" + rankFilterForTrendLinkedIssues + priorityOrderJoinForLinkedIssues : "");

                String releaseSql = sprintsWithClause
                        + baseWithSql
                        + releaseIntermediateSql
                        + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS LAST, key ASC"
                        + (!isClickedOnReleaseStage ? "" : " OFFSET :skip LIMIT :limit");

                releaseResults = template.query(releaseSql, params, DbJiraIssueConverters.listRowMapper(needStatusTransitTime, needPriorityOrdering, needTicketCategory, true, false, false, false));

                Map<String, List<DbJiraIssue>> releaseResultsMap = releaseResults.stream().collect(Collectors.groupingBy(DbJiraIssue::getKey));

                if (PREFER_RELEASE_DEFAULT.equals(releaseVersionPreference)) {
                    releaseResultMap = releaseResultsMap.entrySet().stream().map(entry -> Collections.min(entry.getValue(), Comparator.comparing(DbJiraIssue::getReleaseEndTime))).collect(Collectors.toMap(DbJiraIssue::getKey, Function.identity()));
                } else {
                    releaseResultMap = releaseResultsMap.entrySet().stream().map(entry -> Collections.max(entry.getValue(), Comparator.comparing(DbJiraIssue::getReleaseEndTime))).collect(Collectors.toMap(DbJiraIssue::getKey, Function.identity()));
                }
                if (isClickedOnReleaseStage) {
                    if (PREFER_RELEASE_DEFAULT.equals(releaseVersionPreference)) {
                        tmpResults = releaseResultsMap.entrySet().stream().map(entry -> Collections.min(entry.getValue(), Comparator.comparing(DbJiraIssue::getReleaseEndTime))).collect(Collectors.toList());
                    } else {
                        tmpResults = releaseResultsMap.entrySet().stream().map(entry -> Collections.max(entry.getValue(), Comparator.comparing(DbJiraIssue::getReleaseEndTime))).collect(Collectors.toList());
                    }

                }
            }

            final Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = statusService.getStatusesForIssues(company, tmpResults, filter.getExcludeStages());
            List<DbJiraSprint> dbJiraSprints = sprintService.filterSprints(company, pageNumber, pageSize, jiraSprintFilter).getRecords();

            if (dbJiraSprints != null && dbJiraSprints.size() == 1) {
                long sprintCompletionDate = dbJiraSprints.get(0).getCompletedDate();
                List<DbJiraIssue> newTmpResults = tmpResults.stream().map(issue -> {
                    List<DbJiraStatus> jiraStatusList = jiraStatuses.getOrDefault(Pair.of(issue.getIntegrationId(), issue.getKey()), List.of());
                    for (DbJiraStatus dbJiraStatus : jiraStatusList) {
                        if (sprintCompletionDate > dbJiraStatus.getStartTime()) {
                            issue = issue.toBuilder().asOfStatus(dbJiraStatus.getStatus()).build();
                            break;
                        }
                    }
                    return issue;
                }).collect(Collectors.toList());
                tmpResults = newTmpResults;
            }

            tmpResults = tmpResults.stream()
                    .map(issue -> issue.toBuilder()
                            .statuses(jiraStatuses.getOrDefault(
                                    Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                            .build())
                    .collect(Collectors.toList());

            final Map<Pair<String, String>, List<DbJiraAssignee>> jiraAssignees = getAssigneesForIssues(company, tmpResults);
            tmpResults = tmpResults.stream()
                    .map(issue -> issue.toBuilder()
                            .assigneeList(jiraAssignees.getOrDefault(
                                    Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                            .build())
                    .collect(Collectors.toList());

            results.addAll(tmpResults);
        }

        Integer count = null;
        if (!isClickedOnReleaseStage) {
            String selectStmt = (isClickedOnSingleStat ? "SELECT issues.*" : "SELECT distinct (issues.id), issues.*")
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + rankColumnForTrend
                    + ticketCategoryColumn
                    + (isClickedOnSingleStat ? "" : velocityStageCategoryColumn)
                    + (requireLinkedIssues ? ",to_issue_key" : "")
                    + (needParentIssueTypeJoin ? ",parent_issue_type" : "");
            String selectStmtForLinkedIssues = (isClickedOnSingleStat ? "SELECT linked_issues.*" : "SELECT distinct (linked_issues.id), linked_issues.*")
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + ticketCategoryColumn
                    + (isClickedOnSingleStat ? "" : velocityStageCategoryColumn);
            String linkedIssuesSql = "";
            if (requireLinkedIssues) {
                linkedIssuesSql = selectStmtForLinkedIssues
                        + intervalColumnForTrend
                        + rankColumnForTrendLinkedIssues
                        + " FROM " + company + "." + ISSUES_TABLE + " linked_issues"
                        + slaTimeJoinForLinkedIssues
                        + sprintTableJoinForLinkedIssues
                        + versionTableJoinForLinkedIssues
                        + statusTableTransitJoinForLinkedIssues
                        + " WHERE key IN ( ";
            }
            String intermediateSql = linkedIssuesSql
                    + (requireLinkedIssues ? "SELECT to_issue_key FROM (" : "")
                    + selectStmt
                    + " FROM issues "
                    + userTableJoin
                    + linkTableJoin
                    + slaTimeJoin
                    + sprintTableJoin
                    + statusTableTransitJoin
                    + statusTableJoin
                    + versionTableJoin
                    + latestIngestedAtJoin
                    + joinAssigneeHistorical
                    + issuesWhere
                    + intervalFilterForTrend
                    + " ) as tbl"
                    + parentSPjoin
                    + priorityOrderJoin
                    + finalWhere
                    + (requireLinkedIssues ? ") " + linkedIssuesWhere + ") req_linked_issues" + rankFilterForTrendLinkedIssues : "");
            String countSql = sprintsWithClause
                    + baseWithSql
                    + (isClickedOnSingleStat ? " SELECT COUNT(distinct(id)) FROM ( " : " SELECT COUNT(*) FROM ( ")
                    + intermediateSql;
            count = template.queryForObject(countSql, params, Integer.class);
        } else {
            String countSql = sprintsWithClause
                    + baseWithSql
                    + " SELECT COUNT(distinct(key)) FROM ( "
                    + releaseIntermediateSql
                    + " ) as intermediate_tbl ";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        if (needVelocityStageCategory) {
            List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadata = jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
            return DbListResponse.of(JiraConditionUtils.getDbJiraIssuesWithVelocityStages(filter, results,
                            velocityConfigDTO, integStatusCategoryMetadata, releaseResultMap, isReleasePresent, false),
                    count);
        }
        return DbListResponse.of(results, count);
    }

    public Map<Pair<String, String>, List<DbJiraAssignee>> getAssigneesForIssues(String company, List<DbJiraIssue> issues) {
        if (CollectionUtils.isEmpty(issues)) {
            return Map.of();
        }
        final List<DbJiraAssignee> assignees = template.query(
                "SELECT * FROM " + company + "." + ASSIGNEES_TABLE +
                        " WHERE issue_key IN (:issue_keys)" +
                        " AND integration_id IN (:integration_ids)",
                Map.of("issue_keys", issues.stream()
                                .map(DbJiraIssue::getKey)
                                .distinct()
                                .collect(Collectors.toList()),
                        "integration_ids", issues.stream()
                                .map(DbJiraIssue::getIntegrationId)
                                .map(NumberUtils::toInt)
                                .distinct()
                                .collect(Collectors.toList())),
                DbJiraIssueConverters.listAssigneeMapper());
        assignees.sort((a1, a2) -> (int) (a2.getStartTime() - a1.getStartTime()));
        return assignees
                .stream()
                .collect(Collectors.groupingBy(
                        dbJiraStatus -> Pair.of(dbJiraStatus.getIntegrationId(), dbJiraStatus.getIssueKey())));
    }


    public List<DbJiraIssue> listJiraIssues(String company,
                                            JiraIssuesFilter filter,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize) throws SQLException {

        Map<String, Object> params = new HashMap<>();
        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        long currentTime = Instant.now().getEpochSecond();

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "issue_created_at";
                })
                .orElse("issue_created_at");

        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);

        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, currentTime, filter.getIngestedAt(), null);

        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }

        String baseSql = " SELECT issues.*  FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + issuesWhere;

        String sql = baseSql + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS LAST, key ASC"
                + " OFFSET :skip LIMIT :limit";

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbJiraIssue> results = new ArrayList<>();
        List<DbJiraIssue> tmpResults = template.query(sql, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));

        //TODO : FE is sending "excludeStages" at the moment, so using it. Need to figure out how to user excludeStages() and / or excludeStatuses() filter here
        final Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = statusService.getStatusesForIssues(company, tmpResults, filter.getExcludeStages());

        tmpResults = tmpResults.stream()
                .map(issue -> issue.toBuilder()
                        .statuses(jiraStatuses.getOrDefault(
                                Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                        .build())
                .collect(Collectors.toList());

        final Map<Pair<String, String>, List<DbJiraAssignee>> jiraAssignees = getAssigneesForIssues(company, tmpResults);
        tmpResults = tmpResults.stream()
                .map(issue -> issue.toBuilder()
                        .assigneeList(jiraAssignees.getOrDefault(
                                Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                        .build())
                .collect(Collectors.toList());

        results.addAll(tmpResults);

        return results;
    }

    public DbListResponse<JiraAssigneeTime> listIssueAssigneesByTime(String company,
                                                                     JiraIssuesFilter filter,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize) {
        Long latestIngestedDate = filter.getIngestedAt();
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                filter.toBuilder().filterByLastSprint(false).build(), currentTime, latestIngestedDate, ouConfig);

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        needSlaTimeStuff = needSlaTimeStuff || (filter.getOrFilter() != null && filter.getOrFilter().getExtraCriteria() != null &&
                (filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time)));
        String usersWhere = "";
        if (conditions.get(USERS_TABLE).size() > 0
                || (OrgUnitHelper.isOuConfigActive(ouConfig)
                && (ouConfig.getStaticUsers() || CollectionUtils.isNotEmpty(ouConfig.getSections())))) {
            var ouUsersSelection = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.JIRA);
            if (Strings.isNotBlank(ouUsersSelection)) {
                usersWhere = MessageFormat.format(", ({0}) o_u WHERE (ju.display_name,ju.integ_id) = (o_u.display_name, o_u.integration_id)", ouUsersSelection);
            } else if (conditions.get(USERS_TABLE).size() > 0) {
                usersWhere = " WHERE " + String.join(" AND ", conditions.get(USERS_TABLE));
            }

            // temp if since dynamic user select is not ready yet for OUs. this prevents using users table stuff if there is no need for it
            if (conditions.get(USERS_TABLE).size() > 0 || Strings.isNotBlank(ouUsersSelection)) {
                needUserTableStuff = true;
            }

        }
        String finalWhere = "";
        if (conditions.get(FINAL_TABLE).size() > 0) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        }
        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }

        List<String> timeAssigneeConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getTimeAssignees())) {
            timeAssigneeConditions.add("a.assignee IN ( :tassignees )");
            params.put("tassignees", filter.getTimeAssignees());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTimeAssignees())) {
            timeAssigneeConditions.add("a.assignee NOT IN ( :excludedtassignees )");
            params.put("excludedtassignees", filter.getExcludeTimeAssignees());
        }

        String timeAssigneesWhere = CollectionUtils.isEmpty(timeAssigneeConditions) ? " " : " AND " + String.join(" AND ", timeAssigneeConditions) + " ";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = getSlaTimeJoinStmt(company, "issues");
        }
        if (needUserTableStuff) {
            userTableJoin = getUserTableJoin(company, usersWhere, "issues");
        }

        String parentSPjoin = "";
        boolean needParentSPStuff = filter.getParentStoryPoints() != null || (filter.getOrFilter() != null && filter.getOrFilter().getParentStoryPoints() != null);
        if (needParentSPStuff) {
            parentSPjoin = getParentSPjoin(company);
        }

        List<JiraAssigneeTime> results = new ArrayList<>();
        if (pageSize > 0) {
            String sql = "WITH issues AS ( SELECT integration_id,key,summary FROM (" +
                    "   SELECT issues.*"
                    + slaTimeColumns
                    + " FROM " + company + "." + ISSUES_TABLE + " as issues"
                    + userTableJoin
                    + slaTimeJoin
                    + issuesWhere
                    + " ) as tbl"
                    + parentSPjoin
                    + finalWhere + " ),"
                    + " assign AS ( SELECT issues.*,a.assignee AS tassignee,(a.end_time - a.start_time) AS time_spent"
                    + " FROM issues INNER JOIN " + company + "." + ASSIGNEES_TABLE + " AS a ON a.issue_key = issues.key"
                    + " AND a.integration_id = issues.integration_id " + timeAssigneesWhere
                    + " ) SELECT * FROM ( SELECT integration_id,key,summary,tassignee,"
                    + "SUM(time_spent) as total FROM assign GROUP BY integration_id,key,summary,tassignee ) AS x"
                    + " ORDER BY total DESC OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params,
                    DbJiraIssueConverters.listIssueAssigneeTimeMapper());
        }
        String countSql = "WITH issues AS ( SELECT integration_id,key,summary FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + ISSUES_TABLE + " as issues"
                + userTableJoin
                + slaTimeJoin
                + issuesWhere + " ) as tbl" + parentSPjoin + finalWhere + " ),"
                + " assign AS ( SELECT issues.*,a.assignee AS tassignee,(a.end_time - a.start_time) AS time_spent"
                + " FROM issues INNER JOIN " + company + "." + ASSIGNEES_TABLE + " AS a ON a.issue_key = issues.key"
                + " AND a.integration_id = issues.integration_id " + timeAssigneesWhere
                + " ) SELECT COUNT(*) FROM ( SELECT integration_id,key,tassignee"
                + " FROM assign GROUP BY integration_id,key,tassignee ) AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }


    /**
     * @param company        tenant id
     * @param issueKeys      list of issueKeys
     * @param integrationIds list of integrationIds
     * @return list of {@link DbJiraStoryPoints} mapped with issueKey and integrationId
     */
    public Map<Pair<String, String>, List<DbJiraStoryPoints>> getStoryPointsForIssues(String company,
                                                                                      List<String> issueKeys,
                                                                                      List<Integer> integrationIds) {
        if (CollectionUtils.isEmpty(issueKeys) || CollectionUtils.isEmpty(integrationIds)) {
            return Map.of();
        }
        final List<DbJiraStoryPoints> storyPoints = template.query(
                "SELECT * FROM " + company + "." + STORY_POINTS_TABLE +
                        " WHERE issue_key IN (:issue_keys)" +
                        " AND integration_id IN (:integration_ids)",
                Map.of("issue_keys", issueKeys,
                        "integration_ids", integrationIds),
                DbJiraIssueConverters.storyPointsRowMapper());
        return storyPoints.stream()
                .collect(Collectors.groupingBy(
                        dbJiraStoryPoints -> Pair.of(dbJiraStoryPoints.getIntegrationId(), dbJiraStoryPoints.getIssueKey())));
    }

    /**
     * @param company        tenant id
     * @param issueKeys      list of issueKeys
     * @param integrationIds list of integrationIds
     * @return list of {@link DbJiraSalesforceCase} mapped with issueKey and integrationId
     */
    public Map<Pair<String, String>, List<DbJiraSalesforceCase>> getSalesforceCaseForIssues(String company,
                                                                                            List<String> issueKeys,
                                                                                            List<Integer> integrationIds) {
        if (CollectionUtils.isEmpty(issueKeys) || CollectionUtils.isEmpty(integrationIds)) {
            return Map.of();
        }
        final List<DbJiraSalesforceCase> salesforceCases = template.query(
                "SELECT * FROM " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                        " WHERE issue_key IN (:issue_keys)" +
                        " AND integration_id IN (:integration_ids)",
                Map.of("issue_keys", issueKeys,
                        "integration_ids", integrationIds),
                DbJiraIssueConverters.salesforceCaseRowMapper());
        return salesforceCases.stream()
                .collect(Collectors.groupingBy(
                        dbJiraSalesforceCase -> Pair.of(dbJiraSalesforceCase.getIntegrationId().toString(),
                                dbJiraSalesforceCase.getIssueKey())));
    }

    /**
     * @param company        tenant id
     * @param issueKeys      list of issueKeys
     * @param integrationIds list of integrationIds
     * @return list of {@link  DbJiraLink} mapped with issueKey and integrationId
     */
    public Map<Pair<String, String>, List<DbJiraLink>> getLinksForIssues(String company, List<String> issueKeys,
                                                                         List<Integer> integrationIds) {
        if (CollectionUtils.isEmpty(issueKeys) || CollectionUtils.isEmpty(integrationIds)) {
            return Map.of();
        }
        final List<DbJiraLink> issueLinks = template.query(
                "SELECT * FROM " + company + "." + JIRA_ISSUE_LINKS +
                        " WHERE from_issue_key IN (:issue_keys)" +
                        " AND integration_id IN (:integration_ids)",
                Map.of("issue_keys", issueKeys,
                        "integration_ids", integrationIds),
                DbJiraIssueConverters.linkRowMapper());
        return issueLinks.stream()
                .collect(Collectors.groupingBy(
                        dbJiraLink -> Pair.of(dbJiraLink.getIntegrationId().toString(), dbJiraLink.getFromIssueKey())));
    }

    public DbListResponse<DbJiraIssue> listAggResult(String company, JiraSprintFilter jiraSprintFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfiguration, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException {

        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);

        Map<String, Object> params = new HashMap<>();
        long currentTime = Instant.now().getEpochSecond();
        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, jiraIssuesFilter, currentTime, jiraIssuesFilter.getIngestedAt(), null);
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "issue_created_at";
                })
                .orElse("issue_created_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);

        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }

        String baseWithClause = "WITH issues AS ";
        String baseWithSql = baseWithClause
                + "( SELECT * FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + issuesWhere
                + " )";

        boolean needSlaTimeStuff = jiraIssuesFilter.getCalculation() != null && jiraIssuesFilter.getCalculation().equals(JiraIssuesFilter.CALCULATION.resolution_time);

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time  ";
            slaTimeJoin = getSlaTimeJoin(company);
        }

        String query = baseWithSql + (needSlaTimeStuff ? " SELECT issco.*,p.*  FROM ( " : "")
                + " SELECT * "
                + slaTimeColumns
                + " FROM issues "
                + slaTimeJoin
                + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS LAST, key ASC";

        List<DbJiraIssue> results;

        String sql = query + " OFFSET :offset LIMIT :limit";
        results = template.query(sql, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));
        log.info("sql : {}", query);
        log.info("params : {}", params);

        String countSql = "SELECT COUNT(*) FROM (" + query + ") AS x ";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    private static final String ISSUES_COUNT_SQL = "SELECT count(*) FROM %s.jira_issues WHERE ingested_at=:ingested_at AND integration_id IN (:integration_ids)";
    public Integer getIssuesCount(final String company, List<Integer> integrationIds, Long ingestedAt) {
        String sql = String.format(ISSUES_COUNT_SQL, company);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ingested_at", ingestedAt);
        params.addValue("integration_ids", integrationIds);
        Integer count = template.query(sql, params, CountQueryConverter.countMapper()).get(0);
        return count;
    }
}
