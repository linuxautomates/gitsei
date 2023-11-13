package io.levelops.commons.databases.services.jira;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.INNER_JOIN;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.PARENT_STORY_POINTS;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITY_ORDER;
import static io.levelops.commons.databases.services.JiraIssueService.STATE_TRANSITION_TIME;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;

import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getSortedDevStages;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isSprintTblJoinRequired;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStatusTableTransitJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getUserTableJoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeColumns;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStageTableJoinForBounce;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getPriorityOrderJoinForList;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getParentSPjoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getParentIssueTypeJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSprintJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSprintAuxTable;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getStatusTableJoinForReleaseDrillDown;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getVersionTableJoinStmtForReleaseStage;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.needParentIssueTypeJoin;

@Log4j2
@Service
public class JiraIssueReleaseService {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private static final Set<String> SORTABLE_COLUMNS = Set.of(
            "bounces", "hops", "story_points", "issue_created_at", "reporter", "status", "issue_type",
            "summary", "project", "labels", "assignee", "components", "key", PRIORITY_ORDER
    );

    private final NamedParameterJdbcTemplate template;
    private final JiraIssueStatusService statusService;
    private final JiraIssueQueryBuilder queryBuilder;
    private final JiraIssueSprintService sprintService;
    private final JiraConditionsBuilder conditionsBuilder;
    private final JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;

    public JiraIssueReleaseService(DataSource dataSource,
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

    public DbListResponse<JiraReleaseResponse> jiraReleaseTableReport(@Nonnull String company,
                                                                      @Nonnull JiraIssuesFilter filter,
                                                                      @Nullable JiraSprintFilter jiraSprintFilter,
                                                                      @Nullable OUConfiguration ouConfig,
                                                                      @Nullable VelocityConfigDTO velocityConfigDTO,
                                                                      @Nullable Integer pageNumber,
                                                                      @Nullable Integer pageSize) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(filter, "filter cannot be null.");
        Boolean filterByLastSprint = false;

        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        Map<String, List<String>> velocityStageStatusesMap = Map.of();
        boolean needVelocityStageCategory = velocityConfigDTO != null;
        if (needVelocityStageCategory) {
            List<VelocityConfigDTO.Stage> devStages = getSortedDevStages(velocityConfigDTO);
            velocityStageStatusesMap = JiraConditionUtils.getStageStatusesMap(devStages);
        }

        String releaseStageName = StringUtils.EMPTY;
        List<String> terminationStageValues = new ArrayList<>();

        List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                .stream()
                .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                .collect(Collectors.toList());

        terminationStageValues = preDevSortedStages.get(preDevSortedStages.size() - 2).getEvent().getValues();
        VelocityConfigDTO.Stage releaseStage = preDevSortedStages.get(preDevSortedStages.size() - 1);

        releaseStageName = releaseStage.getName();

        Map<String, Object> params = new HashMap<>();
        long currentTime = Instant.now().getEpochSecond();

        String sortByKey = "issue_created_at";
        String versionSelectColumn = "";
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
        String intervalFilterForTrend = "";
        String rankColumnForTrend = "";

        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, currentTime, filter.getIngestedAt(), ouConfig);
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

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        if (needSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime())) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            if (CollectionUtils.isNotEmpty(filter.getExcludeStages())) {
                slaTimeColumns = getSlaTimeColumns(company, currentTime, "issues");
            }
            slaTimeJoin = getSlaTimeJoinStmt(company, "issues");
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
        if (needStatusTransitTime) {
            statusTableTransitJoin = getStatusTableTransitJoin(company, "issues");
            params.put("from_status", filter.getFromState().toUpperCase());
            params.put("to_status", filter.getToState().toUpperCase());
        }

        String statusTableJoinForReleaseDrillDown = "";
        String stageBounceJoin = "";
        String versionTableJoinForRelease = "";

        if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList())) || needVelocityStageCategory) {
            String statusWhere = "";
            if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList()))) {
                statusWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
                stageBounceJoin = getStageTableJoinForBounce(company, statusWhere, true);
            }
        }
        String parentSPjoin = "";
        boolean needParentSPStuff = filter.getParentStoryPoints() != null || PARENT_STORY_POINTS.equals(sortByKey)
                || (filter.getOrFilter() != null && filter.getOrFilter().getParentStoryPoints() != null);
        if (needParentSPStuff) {
            parentSPjoin = getParentSPjoin(company);
        }
        boolean needPriorityOrdering = PRIORITY_ORDER.equals(sortByKey);
        String priorityOrderJoin = "";
        if (needPriorityOrdering) {
            priorityOrderJoin = getPriorityOrderJoinForList(company, "tbl");
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
        if (needParentIssueTypeJoin) {
            parentIssueTypeJoinStmt = getParentIssueTypeJoinStmt(company, filter, "issues");
        }

        String sprintTableJoin = requireSprints ? getSprintJoinStmt(company, filter, INNER_JOIN, sprintWhere, "issues") : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, filter, sprintWhere) : "";

        String latestIngestedAtJoin = "";
        String baseWithSql = "WITH issues AS "
                + "( SELECT issues.* " + intervalColumnForTrend + selectDistinctStringForTrend + " FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + issuesWhere
                + " AND array_length(fix_versions, 1) > 0  )";

        Map<String, List<DbJiraIssue>> groupedIssues = new HashMap<>();
        AtomicReference<Map<String, DbJiraIssue>> releaseResultMap = null;
        String releaseIntermediateSql = StringUtils.EMPTY;
        List<DbJiraIssue> releaseResults = new ArrayList<>();

        String velocityStageSql = " '" + releaseStageName + "' ";
        String velocityStageCategoryColumnRelease = String.format(", (%s) as velocity_stage ", velocityStageSql);
        velocityStageCategoryColumnRelease += " , Greatest(release_end_time - last_stage_start_time, 0) as release_time";
        velocityStageCategoryColumnRelease += " , release_end_time, fix_version ";

        String finalWhereRelease = finalWhere;
        if (StringUtils.isNotEmpty(finalWhere)
                && finalWhere.contains("velocity_stage IN (:jira_velocity_stages)"))
            finalWhereRelease = finalWhere.replace("velocity_stage IN (:jira_velocity_stages)", " 1 = 1 ");

        statusTableJoinForReleaseDrillDown = getStatusTableJoinForReleaseDrillDown(company, "", "issues", terminationStageValues);
        versionTableJoinForRelease = getVersionTableJoinStmtForReleaseStage(company, "fix_version", INNER_JOIN, versionWhere, "issues", true);

        String selectStmtRelease = " SELECT * FROM "
                + "( SELECT distinct (issues.id), issues.* "
                + (needStatusTransitTime ? ",statuses.state_transition_time" : "")
                + versionSelectColumn
                + slaTimeColumns
                + ticketCategoryColumn
                + velocityStageCategoryColumnRelease
                + rankColumnForTrend
                + (needParentIssueTypeJoin ? ",parent_issue_type" : "");

        releaseIntermediateSql = selectStmtRelease
                + " FROM issues "
                + userTableJoin
                + slaTimeJoin
                + sprintTableJoin
                + statusTableTransitJoin
                + statusTableJoinForReleaseDrillDown
                + versionTableJoinForRelease
                + stageBounceJoin
                + latestIngestedAtJoin
                + parentIssueTypeJoinStmt
                + issuesWhere
                + intervalFilterForTrend
                + " ) as tbl"
                + parentSPjoin
                + priorityOrderJoin
                + finalWhereRelease;

        String releaseSql = sprintsWithClause
                + baseWithSql
                + releaseIntermediateSql;

        releaseResults = template.query(releaseSql, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));

        List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadata = jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
        final Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = statusService.getStatusesForIssues(company, releaseResults, filter.getExcludeStages());
        List<DbJiraIssue> issueListWithStatuses = releaseResults.stream()
                .map(issue -> issue.toBuilder()
                        .statuses(jiraStatuses.getOrDefault(
                                Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                        .build())
                .collect(Collectors.toList());

        final Integer finalPageNumber = pageNumber;
        final Integer finalPageSize = pageSize;
        List<DbJiraSprint> dbJiraSprints = sprintService.filterSprints(
                company, finalPageNumber, finalPageSize, jiraSprintFilter
        ).getRecords();
        if (dbJiraSprints != null && dbJiraSprints.size() == 1) {
            long sprintCompletionDate = dbJiraSprints.get(0).getCompletedDate();
            List<DbJiraIssue> newTmpResults = issueListWithStatuses.stream().map(issue -> {
                List<DbJiraStatus> jiraStatusList = jiraStatuses.getOrDefault(Pair.of(issue.getIntegrationId(), issue.getKey()), List.of());
                for (DbJiraStatus dbJiraStatus : jiraStatusList) {
                    if (sprintCompletionDate > dbJiraStatus.getStartTime()) {
                        issue = issue.toBuilder().asOfStatus(dbJiraStatus.getStatus()).build();
                        break;
                    }
                }
                return issue;
            }).collect(Collectors.toList());
            issueListWithStatuses = newTmpResults;
        }

        Map<String, List<DbJiraIssue>> tmpGroupedIssues = issueListWithStatuses.stream()
                .collect(Collectors.groupingBy(DbJiraIssue::getFixVersion));

        tmpGroupedIssues.forEach((fixVersion, issueList) -> {

            HashSet<String> seen = new HashSet<>();
            issueList.removeIf(issue -> !seen.add(issue.getKey()));

            Map<String, DbJiraIssue> releaseResultsMap = issueList.stream().collect(Collectors.toMap(DbJiraIssue::getKey, issue -> issue));

            List<DbJiraIssue> dbJiraIssues = JiraConditionUtils.getDbJiraIssuesWithVelocityStages(filter, issueList,
                    velocityConfigDTO, integStatusCategoryMetadata, releaseResultsMap, true, false);

            groupedIssues.put(fixVersion, dbJiraIssues);
        });

        List<JiraReleaseResponse> finalReleaseResults = new ArrayList<>();
        groupedIssues.forEach((fixVersion, issueList) -> {
            long totalSum = issueList.stream()
                    .mapToLong(DbJiraIssue::getVelocityStageTotalTime).sum();
            int issueCount = issueList.size();
            long averageTime = totalSum / issueCount;
            JiraReleaseResponse result = JiraReleaseResponse.builder()
                    .name(fixVersion)
                    .averageTime(averageTime)
                    .issueCount(issueCount)
                    .project(issueList.stream().map(DbJiraIssue::getProject).findFirst().get())
                    .releaseEndTime(issueList.stream().map(DbJiraIssue::getReleaseEndTime).findFirst().get())
                    .build();

            finalReleaseResults.add(result);
        });

        finalReleaseResults.sort(Comparator.comparing(JiraReleaseResponse::getReleaseEndTime));

        return DbListResponse.of(
                finalReleaseResults.stream()
                        .skip((long) pageNumber * pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toList()),
                finalReleaseResults.size()
        );
    }


    public DbListResponse<DbJiraIssue> drilldownListReport(@Nonnull String company,
                                                           @Nonnull JiraIssuesFilter filter,
                                                           @Nullable OUConfiguration ouConfig,
                                                           @Nullable VelocityConfigDTO velocityConfigDTO,
                                                           @Nullable Map<String, SortingOrder> sortBy,
                                                           @Nullable Integer pageNumber,
                                                           @Nullable Integer pageSize) throws SQLException {

        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(filter, "filter cannot be null.");
        Boolean filterByLastSprint = false;
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        sortBy = MapUtils.emptyIfNull(sortBy);
        Map<String, List<String>> velocityStageStatusesMap = Map.of();
        boolean needVelocityStageCategory = velocityConfigDTO != null;
        if (needVelocityStageCategory) {
            List<VelocityConfigDTO.Stage> devStages = getSortedDevStages(velocityConfigDTO);
            velocityStageStatusesMap = JiraConditionUtils.getStageStatusesMap(devStages);
        }

        String releaseStageName = StringUtils.EMPTY;
        List<String> terminationStageValues = new ArrayList<>();

        String sortKey = "";
        if (!sortBy.isEmpty()) {
            String sortByKeyDrilldown = "";
            sortByKeyDrilldown = sortBy.entrySet()
                    .stream().findFirst()
                    .map(entry -> {
                        if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                            return entry.getKey();
                        }
                        return "issue_created_at";
                    })
                    .orElse("issue_created_at");
            SortingOrder sortOrder = sortBy.getOrDefault(sortByKeyDrilldown, SortingOrder.DESC);
            sortKey = " ORDER BY " + sortByKeyDrilldown + " " + sortOrder.toString() + " NULLS LAST";
        }

        List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                .stream()
                .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                .collect(Collectors.toList());

        terminationStageValues = preDevSortedStages.get(preDevSortedStages.size() - 2).getEvent().getValues();
        VelocityConfigDTO.Stage releaseStage = preDevSortedStages.get(preDevSortedStages.size() - 1);

        releaseStageName = releaseStage.getName();

        Map<String, Object> params = new HashMap<>();
        long currentTime = Instant.now().getEpochSecond();
        Map<String, List<String>> conditions = conditionsBuilder.createWhereClauseAndUpdateParams(company, params, filter, currentTime, filter.getIngestedAt(), ouConfig);

        String sortByKey = "issue_created_at";
        String versionSelectColumn = "";
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
        String intervalFilterForTrend = "";
        String rankColumnForTrend = "";

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

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        if (needSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime())) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            if (CollectionUtils.isNotEmpty(filter.getExcludeStages())) {
                slaTimeColumns = getSlaTimeColumns(company, currentTime, "issues");
            }
            slaTimeJoin = getSlaTimeJoinStmt(company, "issues");
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
        if (needStatusTransitTime) {
            statusTableTransitJoin = getStatusTableTransitJoin(company, "issues");
            params.put("from_status", filter.getFromState().toUpperCase());
            params.put("to_status", filter.getToState().toUpperCase());
        }

        String statusTableJoinForReleaseDrillDown = "";
        String stageBounceJoin = "";
        String versionTableJoinForRelease = "";

        if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList())) || needVelocityStageCategory) {
            String statusWhere = "";
            if (CollectionUtils.isNotEmpty(conditions.getOrDefault(STATUSES_TABLE, Collections.emptyList()))) {
                statusWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
                stageBounceJoin = getStageTableJoinForBounce(company, statusWhere, true);
            }
        }
        String parentSPjoin = "";
        boolean needParentSPStuff = filter.getParentStoryPoints() != null || PARENT_STORY_POINTS.equals(sortByKey)
                || (filter.getOrFilter() != null && filter.getOrFilter().getParentStoryPoints() != null);
        if (needParentSPStuff) {
            parentSPjoin = getParentSPjoin(company);
        }
        boolean needPriorityOrdering = PRIORITY_ORDER.equals(sortByKey);
        String priorityOrderJoin = "";
        if (needPriorityOrdering) {
            priorityOrderJoin = getPriorityOrderJoinForList(company, "tbl");
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
        if (needParentIssueTypeJoin) {
            parentIssueTypeJoinStmt = getParentIssueTypeJoinStmt(company, filter, "issues");
        }

        String sprintTableJoin = requireSprints ? getSprintJoinStmt(company, filter, INNER_JOIN, sprintWhere, "issues") : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, filter, sprintWhere) : "";

        String latestIngestedAtJoin = "";
        String baseWithSql = "WITH issues AS "
                + "( SELECT issues.* " + intervalColumnForTrend + selectDistinctStringForTrend + " FROM "
                + company + "." + ISSUES_TABLE + " AS issues"
                + issuesWhere
                + " AND array_length(fix_versions, 1) > 0 )";

        String releaseIntermediateSql = StringUtils.EMPTY;
        String releaseIntermediateSqlForCount = StringUtils.EMPTY;
        String linkedIssuesSql = "";
        List<DbJiraIssue> releaseResults = new ArrayList<>();

        String velocityStageSql = " '" + releaseStageName + "' ";
        String velocityStageCategoryColumnRelease = String.format(", (%s) as velocity_stage ", velocityStageSql);
        velocityStageCategoryColumnRelease += " , Greatest(release_end_time - last_stage_start_time, 0) as release_time";
        velocityStageCategoryColumnRelease += " , release_end_time, fix_version ";

        String finalWhereRelease = finalWhere;
        if (StringUtils.isNotEmpty(finalWhere) && finalWhere.contains("velocity_stage IN (:jira_velocity_stages)")) {
            finalWhereRelease = finalWhere.replace("velocity_stage IN (:jira_velocity_stages)", " 1 = 1 ");
        }
        if (StringUtils.isNotEmpty(finalWhere) && finalWhere.contains("velocity_stage NOT IN (:not_jira_velocity_stages)")) {
            finalWhereRelease = finalWhere.replace("velocity_stage NOT IN (:not_jira_velocity_stages)", " 1 = 1 ");
        }

        statusTableJoinForReleaseDrillDown = getStatusTableJoinForReleaseDrillDown(company, "", "issues", terminationStageValues);
        versionTableJoinForRelease = getVersionTableJoinStmtForReleaseStage(company, "fix_version", INNER_JOIN, versionWhere, "issues", true);

        String selectStmtRelease = "( SELECT distinct (issues.id) as unique_id, issues.* "
                + (needStatusTransitTime ? " , statuses.state_transition_time" : "")
                + versionSelectColumn
                + slaTimeColumns
                + ticketCategoryColumn
                + velocityStageCategoryColumnRelease
                + rankColumnForTrend
                + (needParentIssueTypeJoin ? ", parent_issue_type" : "");

        String selectStmtReleaseForCount = " SELECT distinct(tbl.key) FROM "
                + selectStmtRelease;

        selectStmtRelease = " SELECT * FROM "
                + selectStmtRelease;

        releaseIntermediateSql = " FROM issues "
                + userTableJoin
                + slaTimeJoin
                + sprintTableJoin
                + statusTableTransitJoin
                + statusTableJoinForReleaseDrillDown
                + versionTableJoinForRelease
                + stageBounceJoin
                + latestIngestedAtJoin
                + parentIssueTypeJoinStmt
                + issuesWhere
                + intervalFilterForTrend
                + " ) as tbl";

        releaseIntermediateSqlForCount = linkedIssuesSql
                + selectStmtReleaseForCount
                + releaseIntermediateSql
                + finalWhereRelease
                + " group by(key, fix_version) "
                + parentSPjoin
                + priorityOrderJoin;

        releaseIntermediateSql = linkedIssuesSql
                + selectStmtRelease
                + releaseIntermediateSql
                + parentSPjoin
                + priorityOrderJoin
                + finalWhereRelease;

        String releaseSql = sprintsWithClause
                + baseWithSql
                + releaseIntermediateSql
                + sortKey;

        releaseResults = template.query(releaseSql, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));

        String countSql = sprintsWithClause
                + baseWithSql
                + " SELECT COUNT(key) FROM ( "
                + releaseIntermediateSqlForCount
                + ") as drilldown";

        Integer count = template.queryForObject(countSql, params, Integer.class);

        List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadata = jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
        final Map<Pair<String, String>, List<DbJiraStatus>> jiraStatuses = statusService.getStatusesForIssues(company, releaseResults, filter.getExcludeStages());
        List<DbJiraIssue> issueListWithStatuses = releaseResults.stream()
                .map(issue -> issue.toBuilder()
                        .statuses(jiraStatuses.getOrDefault(
                                Pair.of(issue.getIntegrationId(), issue.getKey()), null))
                        .build())
                .collect(Collectors.toList());

        HashSet<String> seen = new HashSet<>();
        issueListWithStatuses.removeIf(i -> !seen.add(i.getKey() + "_" + i.getFixVersion()));

        Map<String, DbJiraIssue> releaseResultsMap = issueListWithStatuses
                .stream()
                .collect(Collectors.toMap(issue -> issue.getKey() + "_" + issue.getFixVersion(), issue -> issue));

        List<DbJiraIssue> dbJiraIssues = JiraConditionUtils.getDbJiraIssuesWithVelocityStagesForReleaseReport(
                filter, issueListWithStatuses, velocityConfigDTO, integStatusCategoryMetadata, releaseResultsMap, true, false
        );

        return DbListResponse.of(
                dbJiraIssues.stream()
                        .skip((long) pageNumber * pageSize)
                        .limit(pageSize)
                        .collect(Collectors.toList()),
                count
        );
    }
}
