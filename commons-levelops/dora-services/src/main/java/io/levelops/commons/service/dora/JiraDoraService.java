package io.levelops.commons.service.dora;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraSprintConditionsBuilder;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.PARENT_STORY_POINTS;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.INNER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITY_ORDER;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_SLA_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.STATE_TRANSITION_TIME;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.ASSIGNEES_TABLE;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getVersionsJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isSprintTblJoinRequired;

@Log4j2
@Service
public class JiraDoraService {

    private static final String JIRA_ISSUES_TABLE = "jira_issues";
    private static final String JIRA_FINAL_TABLE = "final_table";
    public static final String INTERVAL_FIELD = "interval";
    private static final String LEFT_OUTER_JOIN = "LEFT OUTER JOIN";
    private static final Integer DEFAULT_PAGE_SIZE = 100;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("bounces", "hops", "resp_time",
            "solve_time", "issue_created_at", "issue_due_at", "issue_due_relative_at", "desc_size", "num_attachments",
            "first_attachment_at", "issue_resolved_at", "issue_updated_at", "first_comment_at",
            "version", "fix_version", "ingested_at", "story_points",
            PRIORITY_ORDER, PARENT_STORY_POINTS, STATE_TRANSITION_TIME);
    public static final String COUNT_FIELD = "ct";
    private static final String TIME_SERIES_QUERY = "SELECT\n" +
            "  EXTRACT(\n" +
            "    EPOCH\n" +
            "    FROM\n" +
            "      trend_interval\n" +
            "  ) as trend," +
            "  CONCAT(\n" +
            "    (date_part('day', trend_interval)),\n" +
            "    '-',\n" +
            "    (date_part('month', trend_interval)),\n" +
            "    '-',\n" +
            "    (date_part('year', trend_interval))\n" +
            "  ) as " + INTERVAL_FIELD + ",\n" +
            "  COUNT(key) as " + COUNT_FIELD + "\n" +
            "FROM\n" +
            "  (\n" +
            "    SELECT\n" +
            "      %s,\n" +
            "      DATE_TRUNC('day', %s) as trend_interval\n" +
            "    FROM\n" +
            "    %s\n" +
            "  ) a\n" +
            "GROUP BY\n" +
            "  trend_interval\n" +
            "ORDER BY\n" +
            "  trend_interval DESC,\n" +
            "  ct DESC NULLS FIRST;";

    private static final String COUNT_QUERY = "SELECT\n" +
            "  COUNT(key) as " + COUNT_FIELD + "\n" +
            "FROM\n" +
            "  (\n" +
            "    SELECT\n" +
            "      DISTINCT(key)\n" +
            "    FROM\n" +
            "    %s\n" +
            "  ) a;";
    private final NamedParameterJdbcTemplate template;

    private final JiraConditionsBuilder conditionsBuilder;

    private final DataSource dataSource;

    @Autowired
    public JiraDoraService(final DataSource dataSource, JiraConditionsBuilder conditionsBuilder) {
        this.dataSource = dataSource;
        template = new NamedParameterJdbcTemplate(dataSource);
        this.conditionsBuilder = conditionsBuilder;
    }

    public String insert(String company, DbJiraIssue t) throws SQLException {
        return null;
    }

    private String getParentSPJoin(String company, String prefix) {
        return " INNER JOIN "
                + "( SELECT key AS parent_key, story_points AS parent_story_points, ingested_at as parent_ingested_at "
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " "
                + " WHERE story_points IS NOT NULL "
                + " AND integration_id IN (:" + prefix + "integration_ids) "
                + " ) parent_issues ON epic = parent_issues.parent_key AND ingested_at = parent_issues.parent_ingested_at";
    }

    public DoraResponseDTO getTimeSeriesDataForDeployment(String company, JiraIssuesFilter filter, JiraIssuesFilter requestFilter, String calculationField, OUConfiguration ouConfig) throws SQLException, BadRequestException {
        ImmutablePair<Long, Long> calculationTimeRange;
        if ("issue_resolved_at".equals(calculationField)) {
            calculationTimeRange = requestFilter.getIssueResolutionRange();
        } else if("issue_updated_at".equals(calculationField)) {
            calculationTimeRange = requestFilter.getIssueUpdatedRange();
        } else {
            calculationTimeRange = requestFilter.getIssueReleasedRange();
        }
        if(calculationTimeRange == null || calculationTimeRange.getLeft() == null || calculationTimeRange.getRight() == null){
            throw new BadRequestException("Error computing DORA metric, please provide Issue Resolution range.");
        }
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> whereConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, null, filter.getIngestedAt(), ouConfig);
        Map<String, List<String>> whereConditionsForRequest = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "req_", requestFilter, null, requestFilter.getIngestedAt(), "", null);
        String whereClause = "";
        if (whereConditions.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", whereConditions.get(JIRA_ISSUES_TABLE));
        }
        if (whereConditionsForRequest.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_ISSUES_TABLE));
        }
        String issuesStmt = "SELECT %s, " + calculationField + " FROM ( SELECT * FROM " + company + "." + JIRA_ISSUES_TABLE;
        String innerSelectKey = "key";
        String outerSelectKey = "DISTINCT(key)";

        if("released_in".equals(calculationField)) {

            String versionsWhereClause = "";
            if (whereConditionsForRequest.get(JIRA_ISSUE_VERSIONS).size() > 0) {
                versionsWhereClause = String.join(" AND ", whereConditionsForRequest.get(JIRA_ISSUE_VERSIONS));
                versionsWhereClause += " AND ";
            }

            issuesStmt += " INNER JOIN ( SELECT name, integration_id as intg_id, "
                    + " end_date::timestamp with time zone AT TIME ZONE 'UTC' AS released_in FROM " + company + ".jira_issue_versions "
                    + " WHERE " + versionsWhereClause
                    + " released ) release ON release.intg_id = integration_id AND release.name = ANY(fix_versions)";
            innerSelectKey = "name";
            outerSelectKey = "distinct(name) AS key";
        }

        issuesStmt = String.format(issuesStmt, innerSelectKey);
        issuesStmt += whereClause + " ) as finaltable";

        if (filter.getParentStoryPoints() != null) {
            issuesStmt += getParentSPJoin(company, "jira_");
            if(whereConditions.get(JIRA_FINAL_TABLE).size() > 0){
                issuesStmt += " AND " + String.join(" AND ", whereConditions.get(JIRA_FINAL_TABLE));
            }
        }
        else if (requestFilter.getParentStoryPoints() != null) {
            issuesStmt += getParentSPJoin(company, "req_jira_");
            if(whereConditionsForRequest.get(JIRA_FINAL_TABLE).size() > 0){
                issuesStmt += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_FINAL_TABLE));
            }
        }
        issuesStmt = "( " + issuesStmt + " ) issues";
        String sqlStmt = String.format(
                TIME_SERIES_QUERY,
                outerSelectKey,
                "released_in".equals(calculationField)
                        ? calculationField
                        : "to_timestamp(" + calculationField + ")::timestamp without time zone",
                issuesStmt
        );

        log.info(" Jira Deployment Frequency SQL" + sqlStmt);
        log.info("params = {}", params);

        List<DoraTimeSeriesDTO.TimeSeriesData> tempResults = template.query(sqlStmt, params, DoraCalculationUtils.getTimeSeries());

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDayFailedDeployment = DoraCalculationUtils.fillRemainingDates(calculationTimeRange.getLeft(),
                calculationTimeRange.getRight(),
                tempResults);

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesByDayFailedDeployment);
        Integer total = CollectionUtils.emptyIfNull(filledTimeSeriesByDayFailedDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();
        DoraSingleStateDTO stats = DoraSingleStateDTO.builder().totalDeployment(total).build();

        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats).build();
    }

    public DbListResponse<JiraReleaseResponse> getDeploymentForJiraRelease(
            String company, JiraIssuesFilter filter, JiraIssuesFilter requestFilter, OUConfiguration ouConfig, Integer pageNumber, Integer pageSize
    ) throws BadRequestException {
        ImmutablePair<Long, Long> calculationTimeRange = requestFilter.getIssueReleasedRange();
        if(calculationTimeRange == null || calculationTimeRange.getLeft() == null || calculationTimeRange.getRight() == null){
            throw new BadRequestException("Error computing DORA metric, please provide Issue Resolution range.");
        }
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> whereConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, null, filter.getIngestedAt(), ouConfig);
        Map<String, List<String>> whereConditionsForRequest = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "req_", requestFilter, null, requestFilter.getIngestedAt(), "", null);
        String whereClause = "";
        if (whereConditions.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause += " AND " + String.join(" AND ", whereConditions.get(JIRA_ISSUES_TABLE));
        }
        if (whereConditionsForRequest.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_ISSUES_TABLE));
        }
        if (whereConditionsForRequest.get(JIRA_ISSUE_VERSIONS).size() > 0) {
            whereClause += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_ISSUE_VERSIONS));
        }

        String sqlStmt = "SELECT release.name AS fix_version, count(issue.key), issue.project, "
                + " extract( epoch from (end_date) ) AS released_date "
                + " FROM " + company + ".jira_issues issue INNER JOIN ( SELECT name, integration_id AS intg_id, "
                + " end_date FROM " + company + ".jira_issue_versions WHERE released )"
                + " release ON release.intg_id = issue.integration_id AND release.name = ANY(fix_versions) ";

        sqlStmt += whereClause
                + " GROUP BY fix_version, project, released_date";
        String sql = "SELECT fix_version, STRING_AGG(result_tbl.project, ', ') as project, sum(count) as count, released_date FROM ("
                + sqlStmt + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize)
                + " ) result_tbl GROUP BY fix_version, released_date";
        log.info(" Jira Deployment Frequency for Jira Release SQL" + sql);
        log.info("params = {}", params);

        List<JiraReleaseResponse> result = template.query(sql, params, DoraCalculationUtils.getDFForJiraRelease());

        String countSql = "SELECT count(*) FROM ( "
                + " SELECT fix_version, STRING_AGG(result_tbl.project, ', ') as project, sum(count) as count, released_date FROM ( "
                + sqlStmt
                + " ) result_tbl GROUP BY fix_version, released_date "
                + " ) as count_tbl";
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(result, count);
    }

    public Long getCountForDeployment(String company, JiraIssuesFilter filter, JiraIssuesFilter requestFilter) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> whereConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, null, null, null);
        Map<String, List<String>> whereConditionsForRequest = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "req_", requestFilter, null, null, "",null);
        String whereClause = "";
        if (whereConditions.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", whereConditions.get(JIRA_ISSUES_TABLE));
        }
        if (whereConditionsForRequest.get(JIRA_ISSUES_TABLE).size() > 0) {
            whereClause += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_ISSUES_TABLE));
        }
        String issuesStmt = "SELECT key FROM ( SELECT * FROM " + company + "." + JIRA_ISSUES_TABLE + whereClause + " ) as finaltable";
        if (filter.getParentStoryPoints() != null) {
            issuesStmt += getParentSPJoin(company, "jira_");
            if(whereConditions.get(JIRA_FINAL_TABLE).size() > 0){
                issuesStmt += " AND " + String.join(" AND ", whereConditions.get(JIRA_FINAL_TABLE));
            }
        }
        else if (requestFilter.getParentStoryPoints() != null) {
            issuesStmt += getParentSPJoin(company, "req_jira_");
            if(whereConditionsForRequest.get(JIRA_FINAL_TABLE).size() > 0){
                issuesStmt += " AND " + String.join(" AND ", whereConditionsForRequest.get(JIRA_FINAL_TABLE));
            }
        }
        issuesStmt = "( " + issuesStmt + " ) issues";
        String sqlStmt = String.format(COUNT_QUERY, issuesStmt);
        return template.queryForObject(sqlStmt, params, Long.class);
    }

    public DbListResponse<DbJiraIssue> getDrillDownData(String company,
                                            JiraSprintFilter jiraSprintFilter,
                                            JiraIssuesFilter filter,
                                            Optional<JiraIssuesFilter> linkedJiraIssuesFilter,
                                            JiraSprintFilter workflowJiraSprintFilter,
                                            JiraIssuesFilter workflowFilter,
                                            Optional<JiraIssuesFilter> workflowLinkedJiraIssuesFilter,
                                            OUConfiguration ouConfig,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize) throws SQLException {
        boolean filterByLastSprint = false;
        if (filter.getFilterByLastSprint() != null) {
            filterByLastSprint = filter.getFilterByLastSprint();
        }
        else if(workflowFilter.getFilterByLastSprint() != null){
            filterByLastSprint = workflowFilter.getFilterByLastSprint();
        }
        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = new HashMap<>();
        long currentTime = Instant.now().getEpochSecond();
        Map<String, List<String>> requestConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, null, filter.getIngestedAt(), ouConfig);
        Map<String, List<String>> workflowConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, workflowFilter, null, workflowFilter.getIngestedAt(), ouConfig);
        for(var condition: requestConditions.entrySet()){
            String key = condition.getKey();
            List<String> values = condition.getValue();
            List<String> workflowFilterValues = workflowConditions.get(key);
            HashSet<String> finalValues = new HashSet<>();
            finalValues.addAll(values);
            finalValues.addAll(workflowFilterValues);
            conditions.put(key, new ArrayList<>(finalValues));
        }
        boolean isHistorical = CollectionUtils.isNotEmpty(filter.getHistoricalAssignees()) || CollectionUtils.isNotEmpty(workflowFilter.getHistoricalAssignees());
        boolean requireLinkedIssues = (filter.getLinks() != null && CollectionUtils.isNotEmpty(filter.getLinks())) ||
                (filter.getExcludeLinks() != null && CollectionUtils.isNotEmpty(filter.getExcludeLinks()));
        boolean workflowRequireLinkedIssues = (workflowFilter.getLinks() != null && CollectionUtils.isNotEmpty(workflowFilter.getLinks())) ||
                (workflowFilter.getExcludeLinks() != null && CollectionUtils.isNotEmpty(workflowFilter.getExcludeLinks()));
        if (requireLinkedIssues || workflowRequireLinkedIssues) {
            conditions.get(JIRA_ISSUE_LINKS).add("from_issue_key IN ( select key from issues )");
        }
        Map<String, List<String>> linkedIssuesConditions = new HashMap<>();
        Map<String, List<String>> reqLinkedIssuesConditions = Map.of();
        Map<String, List<String>> workflowLinkedIssuesConditions = Map.of();
        if (linkedJiraIssuesFilter.isPresent()) {
            reqLinkedIssuesConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "linked_", linkedJiraIssuesFilter.get(), null, null, "", ouConfig);
        }
        if (workflowLinkedJiraIssuesFilter.isPresent()) {
            workflowLinkedIssuesConditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, "linked_", linkedJiraIssuesFilter.get(), null, null, "", ouConfig);
        }
        for(var condition: reqLinkedIssuesConditions.entrySet()){
            String key = condition.getKey();
            List<String> values = condition.getValue();
            List<String> workflowFilterValues = workflowLinkedIssuesConditions.get(key);
            HashSet<String> finalValues = new HashSet<>();
            finalValues.addAll(values);
            finalValues.addAll(workflowFilterValues);
            linkedIssuesConditions.put(key, new ArrayList<>(finalValues));
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

        boolean workflowNeedSlaTimeStuff = workflowFilter.getExtraCriteria() != null &&
                (workflowFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || workflowFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        workflowNeedSlaTimeStuff = workflowNeedSlaTimeStuff || (workflowFilter.getOrFilter() != null && workflowFilter.getOrFilter().getExtraCriteria() != null &&
                (workflowFilter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || workflowFilter.getOrFilter().getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time)));

        boolean requireSprints = isSprintTblJoinRequired(filter) || isSprintTblJoinRequired(workflowFilter);

        String intervalColumnForTrend = "";
        String selectDistinctStringForTrend = "";
        String intervalFilterForTrend = "";
        String rankColumnForTrend = "";
        String rankColumnForTrendLinkedIssues = "";
        String rankFilterForTrendLinkedIssues = "";
        String linkedIssuesWhere = "";
        String linkedSprintWhere = "";
        String linkedVersionWhere = "";
        if (linkedJiraIssuesFilter.isPresent()) {
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
        if (needSlaTimeStuff || workflowNeedSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime()) || Boolean.TRUE.equals(workflowFilter.getIncludeSolveTime())) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
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
        String stageBounceJoin = "";
        if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList()))) {
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

        String velocityStageCategoryColumn = "";

        String linkTableJoin = requireLinkedIssues || workflowRequireLinkedIssues ? getLinksTableJoinStmt(company, linkWhere) : "";
        String sprintTableJoin = requireSprints ? getSprintJoinStmt(company, filter, workflowFilter, INNER_JOIN, sprintWhere, "issues") : "";
        String sprintTableJoinForLinkedIssues = (requireSprints && requireLinkedIssues) ? getSprintJoinStmt(company, filter,
                workflowFilter, LEFT_OUTER_JOIN, linkedSprintWhere, "linked_issues") : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, filter, workflowFilter, sprintWhere) : "";
        String versionTableJoin = requireVersionTableJoin ? getVersionsJoinStmt(company, versionColumn, INNER_JOIN, versionWhere, "issues") : "";
        String versionTableJoinForLinkedIssues = (requireVersionTableJoin && requireLinkedIssues) ? getVersionsJoinStmt(company, versionColumn,
                LEFT_OUTER_JOIN, linkedVersionWhere, "linked_issues") : "";

        String latestIngestedAtJoin = "";
        String joinAssigneeHistorical="";
        if(isHistorical){
            joinAssigneeHistorical=getIssuesAssigneeTableJoinStmt(company);
        }
        String baseWithClause = StringUtils.isNotEmpty(sprintsWithClause) ? " , issues AS " : "WITH issues AS ";
        String baseWithSql = baseWithClause
                + "( SELECT issues.* " + intervalColumnForTrend +  selectDistinctStringForTrend + " FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + joinAssigneeHistorical
                + issuesWhere
                + " )";

        List<DbJiraIssue> results = new ArrayList<>();
        if (pageSize > 0) {
            String selectStmt = " SELECT * FROM "
                    + "( SELECT distinct on (issues.key, key) issues.*"
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + ticketCategoryColumn
                    + velocityStageCategoryColumn
                    + rankColumnForTrend
                    + (requireLinkedIssues ? ",to_issue_key" : "");
            String selectStmtForLinkedIssues = " SELECT * FROM "
                    + "( SELECT distinct on (linked_issues.key, key) linked_issues.*"
                    + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                    + versionSelectColumn
                    + slaTimeColumns
                    + ticketCategoryColumn
                    + velocityStageCategoryColumn;
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
                    + versionTableJoin
                    + statusTableTransitJoin
                    + statusTableJoin
                    + stageBounceJoin
                    + latestIngestedAtJoin
                    +joinAssigneeHistorical
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
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS LAST"
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            List<DbJiraIssue> tmpResults = template.query(sql, params, DbJiraIssueConverters.listRowMapper(needStatusTransitTime, needPriorityOrdering, false, true, false, false,false));
            final Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = getStatusesForIssues(company, tmpResults);

            List<DbJiraSprint> dbJiraSprints = filterSprints(company, pageNumber, pageSize, jiraSprintFilter, workflowJiraSprintFilter).getRecords();

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
        String selectStmt = "SELECT distinct on (issues.key, key) issues.*"
                + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                + versionSelectColumn
                + slaTimeColumns
                + rankColumnForTrend
                + ticketCategoryColumn
                + velocityStageCategoryColumn
                + (requireLinkedIssues ? ",to_issue_key" : "");
        String selectStmtForLinkedIssues = "SELECT distinct on (linked_issues.key, key) linked_issues.*"
                + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                + versionSelectColumn
                + slaTimeColumns
                + ticketCategoryColumn
                + velocityStageCategoryColumn;
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
                + versionTableJoin
                + statusTableTransitJoin
                + statusTableJoin
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
                + " SELECT COUNT(*) FROM ( "
                + intermediateSql;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }


    private String replaceCharVaryingArrayTypeFields(String whereClause){
        whereClause = whereClause.replace("labels &&", "labels::text[] &&");
        whereClause = whereClause.replace("versions &&", "versions::text[] &&");
        whereClause = whereClause.replace("components &&", "components::text[] &&");
        return whereClause;
    }

    public static String getSprintJoinStmt(String company, JiraIssuesFilter filter, JiraIssuesFilter workflowFilter, String joinType, String sprintWhere, String issuesTableAlias) {
        String sprintLimitStatement = "";
        if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }
        else if (workflowFilter.getSprintCount() != null && workflowFilter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + workflowFilter.getSprintCount();
        }
        return " " + joinType + " (Select integration_id as integ_id,sprint_id,name as sprint,state as" +
                " sprint_state,start_date as sprint_creation_date" + " FROM " + company + "." + JIRA_ISSUE_SPRINTS
                + sprintWhere
                + sprintLimitStatement + " ) sprints ON" +
                " sprints.integ_id=" + issuesTableAlias + ".integration_id AND sprints.sprint_id=ANY(" + issuesTableAlias + ".sprint_ids)";
    }

    @NotNull
    private String getSlaTimeJoinStmt(String company, String issuesTableAlias) {
        return " LEFT OUTER JOIN ("
                + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                + "integration_id as integid FROM " + company + "." + PRIORITIES_SLA_TABLE + " )"
                + " AS p ON p.proj = " + issuesTableAlias + ".project "
                + " AND p.prio = " + issuesTableAlias + ".priority"
                + " AND p.integid = " + issuesTableAlias + ".integration_id"
                + " AND p.ttype = " + issuesTableAlias + ".issue_type";
    }

    @NotNull
    private String getParentSPjoin(String company) {
        return " INNER JOIN " +
                "( SELECT key AS parent_key, story_points AS parent_story_points, ingested_at as parent_ingested_at " +
                " FROM " + company + "." + ISSUES_TABLE + " " +
                " WHERE story_points IS NOT NULL " +
                " AND integration_id IN (:jira_integration_ids) " +
                " ) parent_issues ON epic = parent_issues.parent_key AND ingested_at = parent_issues.parent_ingested_at";
    }

    @NotNull
    private String getUserTableJoin(String company, String usersWhere, String issuesTableAlias) {
        return " INNER JOIN ( SELECT ju.display_name,ju.active,ju.integ_id FROM " + company + "."
                + USERS_TABLE + " ju" + usersWhere + " ) AS u ON u.display_name = " + issuesTableAlias + ".assignee AND"
                + " u.integ_id = " + issuesTableAlias + ".integration_id";
    }

    @NotNull
    private String getStatusTableJoinForStages(String company, String statusWhere, String issuesTableAlias) {
        return " INNER JOIN ( SELECT integration_id as integ_id, issue_key,end_time-start_time as time_spent," +
                "status as state from " + company + "." + STATUSES_TABLE + statusWhere +
                " ) s ON s.integ_id=" + issuesTableAlias + ".integration_id AND s.issue_key=" + issuesTableAlias + ".key ";
    }

    private String getStageTableJoinForBounce(String company, String statusWhere, boolean isList) {
        String statusGroupBy = "";
        String statusAlias = "";
        if(!isList) {
            statusGroupBy = ",status ";
            statusAlias = statusGroupBy + "as bounce_status";
        }
        return " INNER JOIN ( SELECT integration_id as integ_id, issue_key " + statusAlias + ", count(*) as count" +
                " from " + company + "." + STATUSES_TABLE + statusWhere +
                " GROUP  BY issue_key,integration_id" + statusGroupBy + " ) q ON q.integ_id=issues.integration_id AND q.issue_key= issues.key ";
    }

    private String getStatusTableTransitJoin(String company, String issuesTableAlias) {
        return " INNER JOIN " +
                "(SELECT t1.issue_key, t1.integration_id AS status_integ_id, to_time - from_time AS state_transition_time" +
                " FROM " +
                "  (SELECT issue_key, integration_id, max(start_time) to_time " +
                "   FROM " + company + "." + STATUSES_TABLE +
                "   WHERE status = :to_status " +
                "   GROUP BY issue_key, integration_id" +
                "  ) t1 " +
                "  INNER JOIN " +
                "  (SELECT issue_key, integration_id, min(start_time) from_time" +
                "   FROM " + company + "." + STATUSES_TABLE +
                "   WHERE status = :from_status GROUP BY issue_key, integration_id" +
                "  ) t2 " +
                "  ON t1.issue_key = t2.issue_key AND t1.integration_id = t2.integration_id " +
                "  WHERE from_time < to_time" +
                ") statuses ON " + issuesTableAlias + ".key=statuses.issue_key AND " + issuesTableAlias + ".integration_id=statuses.status_integ_id";
    }

    @NotNull
    private String getPriorityOrderJoinForList(String company, String finalTableAlias) {
        return " LEFT JOIN (" +
                "   SELECT DISTINCT priority_order, integration_id, priority, project" +
                "   FROM " + company + "." + PRIORITIES_TABLE +
                "   WHERE integration_id IN (:jira_integration_ids)" +
                "   AND scheme = 'default'" +
                "   AND project = '_levelops_default_'" +
                " ) AS prior_order " +
                " ON " + finalTableAlias + ".integration_id = prior_order.integration_id" +
                " AND UPPER(" + finalTableAlias + ".priority) = UPPER(prior_order.priority) ";
    }

    private String getSprintAuxTable(String company, JiraIssuesFilter filter, JiraIssuesFilter workflowFilter, String sprintWhere) {
        String sprintLimitStatement = "";
        if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }
        else if (workflowFilter.getSprintCount() != null && workflowFilter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + workflowFilter.getSprintCount();
        }

        return "WITH spr_dates AS (select COALESCE(start_date, 99999999999) as start_date, sprint_id, name " +
                "from " + company + "." + JIRA_ISSUE_SPRINTS + sprintWhere + sprintLimitStatement + " )";
    }

    @NotNull
    private String getLinksTableJoinStmt(String company, String linkWhere) {
        return " INNER JOIN (SELECT from_issue_key, to_issue_key, integration_id as intg_id, relation FROM "
                + company + "." + JIRA_ISSUE_LINKS + linkWhere
                + ") links ON issues.key = links.from_issue_key AND issues.integration_id = links.intg_id";
    }

    @NotNull
    private String getIssuesAssigneeTableJoinStmt(String company) {
        return "" +
                "        join (\n" +
                "          select a.integration_id historical_assignee_integration_id, a.issue_key, assignee as historical_assignee from\n" +
                company + ".jira_issue_assignees as a\n" +
                "        ) as historical_assignees\n" +
                "        on issues.integration_id = historical_assignees.historical_assignee_integration_id and\n" +
                "           issues.key = historical_assignees.issue_key";
    }

    public DbListResponse<DbJiraSprint> filterSprints(String company, Integer pageNumber, Integer pageSize, JiraSprintFilter filter, JiraSprintFilter workflowFilter) {
        int limit = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : JiraSprintFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        JiraSprintConditionsBuilder.generateSprintsConditions(conditions, null, params, filter);
        JiraSprintConditionsBuilder.generateSprintsConditions(conditions, null, params, workflowFilter);

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + JIRA_ISSUE_SPRINTS +
                where +
                " ORDER BY sprint_id ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        String countSql = "SELECT count(*) FROM " + company + "." + JIRA_ISSUE_SPRINTS + where ;
        List<DbJiraSprint> results=template.query(sql, params, DbJiraIssueConverters.listSprintsMapper());
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    private Map<Pair<String, String>, List<DbJiraStatus>> getStatusesForIssues(String company,
                                                                               List<DbJiraIssue> issues) {
        if (CollectionUtils.isEmpty(issues)) {
            return Map.of();
        }
        final List<DbJiraStatus> statuses = template.query(
                "SELECT * FROM " + company + "." + STATUSES_TABLE +
                        " WHERE issue_key IN (:issue_keys)" +
                        " AND integration_id IN (:integration_ids) ORDER BY start_time DESC",
                Map.of("issue_keys", issues.stream()
                                .map(DbJiraIssue::getKey)
                                .distinct()
                                .collect(Collectors.toList()),
                        "integration_ids", issues.stream()
                                .map(DbJiraIssue::getIntegrationId)
                                .map(NumberUtils::toInt)
                                .distinct()
                                .collect(Collectors.toList())),
                DbJiraIssueConverters.listStatusMapper());
        return statuses
                .stream()
                .collect(Collectors.groupingBy(
                        dbJiraStatus -> Pair.of(dbJiraStatus.getIntegrationId(), dbJiraStatus.getIssueKey())));
    }

    private Map<Pair<String, String>, List<DbJiraAssignee>> getAssigneesForIssues(String company,
                                                                                  List<DbJiraIssue> issues) {
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
        assignees.sort((a1,a2) -> (int) (a2.getStartTime() - a1.getStartTime()));
        return assignees
                .stream()
                .collect(Collectors.groupingBy(
                        dbJiraStatus -> Pair.of(dbJiraStatus.getIntegrationId(), dbJiraStatus.getIssueKey())));
    }

}
