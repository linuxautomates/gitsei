package io.levelops.commons.databases.services;

import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GithubCardFilterParser {

    private static final String PROJECTS_TABLE = "github_projects";
    private static final String COLUMNS_TABLE = "github_project_columns";
    private static final String CARDS_TABLE = "github_cards";
    private static final String STATUSES_TABLE = "github_cards_statuses";
    private static final String ISSUES_TABLE = "scm_issues";

    @SuppressWarnings("unchecked")
    public GithubCardFilter merge(Integer integrationId, GithubCardFilter reqFilter, Map<String, Object> productFilter) {
        Boolean isPrivateProject = getFilterValue("private_project", Boolean.class, productFilter).orElse(null);
        Boolean isArchivedCard = getFilterValue("archived_card", Boolean.class, productFilter).orElse(null);
        Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) productFilter.get("exclude"));
        List<String> excludeColumns = (List<String>) filterExclude.getOrDefault("columns", List.of());
        return GithubCardFilter.builder()
                .projects(getListOrDefault(productFilter, "projects"))
                .projectCreators(getListOrDefault(productFilter, "project_creators"))
                .organizations(getListOrDefault(productFilter, "organizations"))
                .projectStates(getListOrDefault(productFilter, "project_states"))
                .privateProject(isPrivateProject)
                .across(reqFilter.getAcross())
                .calculation(reqFilter.getCalculation())
                .columns(getListOrDefault(productFilter, "columns"))
                .currentColumns(getListOrDefault(productFilter, "current_columns"))
                .cardIds(getListOrDefault(productFilter, "cards_ids"))
                .cardCreators(getListOrDefault(productFilter, "card_creators"))
                .integrationIds(List.of(String.valueOf(integrationId)))
                .archivedCard(isArchivedCard)
                .excludeColumns(excludeColumns)
                .sort(reqFilter.getSort())
                .repoIds(getListOrDefault(productFilter, "repos"))
                .labels(getListOrDefault(productFilter, "labels"))
                .assignees(getListOrDefault(productFilter, "assignees"))
                .issueClosedRange(reqFilter.getIssueClosedRange())
                .issueCreatedRange(reqFilter.getIssueCreatedRange())
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, GithubCardFilter githubCardFilter,
                             boolean checkIfList, long currentTime) {
        if (checkIfList) {
            String projectsWhere = "";
            if (conditions.get(PROJECTS_TABLE).size() > 0)
                projectsWhere = " WHERE " + String.join(" AND ", conditions.get(PROJECTS_TABLE));
            String columnsWhere = "";
            if (conditions.get(COLUMNS_TABLE).size() > 0)
                columnsWhere = " WHERE " + String.join(" AND ", conditions.get(COLUMNS_TABLE));
            String cardsWhere = "";
            if (conditions.get(CARDS_TABLE).size() > 0)
                cardsWhere = " WHERE " + String.join(" AND ", conditions.get(CARDS_TABLE));
            String issuesWhere = "";
            if (conditions.get(ISSUES_TABLE).size() > 0)
                issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
            String projectSQL = " ( SELECT id as project_row_id FROM " + company + "."
                    + PROJECTS_TABLE + projectsWhere + " ) AS projects ";
            String columnsJoin = " INNER JOIN ( SELECT id as column_row_id,project_id FROM "
                    + company + "." + COLUMNS_TABLE + columnsWhere + " ) AS columns ON projects.project_row_id=columns.project_id ";
            String cardsJoin = " INNER JOIN ( SELECT * FROM " + company + "." + CARDS_TABLE + cardsWhere
                    + " ) as cards ON cards.current_column_id=columns.column_row_id";
            String issuesJoin = " INNER JOIN (SELECT id, title as issue_title, state as issue_state, issue_id as issues_issue_id," +
                    "repo_id as issues_repo_id,number as issues_number,labels as issues_labels,"
                    + " assignees as issues_assignees,issue_closed_at,issue_created_at FROM " + company
                    + "." + ISSUES_TABLE + issuesWhere + " ) AS issues ON issues.issues_issue_id = cards.issue_id ";
            return "SELECT * FROM ( " + projectSQL + columnsJoin + cardsJoin + issuesJoin + " ) x ";

        }
        boolean needResolutionTimeReport = false;
        boolean needTimeAcrossColumnReport = false;
        String selectDistinctString = "", groupByString = "";
        GithubCardFilter.CALCULATION calculation = githubCardFilter.getCalculation() == null ? GithubCardFilter.CALCULATION.count
                : githubCardFilter.getCalculation();
        AGG_INTERVAL aggInterval = githubCardFilter.getAggInterval() == null ? AGG_INTERVAL.day : githubCardFilter.getAggInterval();
        if (calculation == GithubCardFilter.CALCULATION.resolution_time) {
            needResolutionTimeReport = true;
        }
        if (calculation == GithubCardFilter.CALCULATION.stage_times_report) {
            needTimeAcrossColumnReport = true;
        }
        String intervalColumn = "";
        AggTimeQueryHelper.AggTimeQuery issueModAggQuery;
        switch (githubCardFilter.getAcross()) {
            case project:
            case project_creator:
            case organization:
            case card_creator:
                groupByString = githubCardFilter.getAcross().toString();
                selectDistinctString = githubCardFilter.getAcross().toString();
                break;
            case repo_id:
                groupByString = "issues_repo_id";
                selectDistinctString = "issues_repo_id";
                break;
            case column:
                groupByString = "column_name";
                selectDistinctString = "column_name";
                break;
            case assignee:
                groupByString = "issues_assignees";
                selectDistinctString = "issues_assignees"; //unnest for assignees as that is an array
                break;
            case label:
                groupByString = "issues_label";
                selectDistinctString = "issues_labels"; //unnest for labels as that is an array
                break;
            case issue_closed:
                conditions.get(ISSUES_TABLE).add("issue_closed_at IS NOT NULL");
            case issue_created:
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(githubCardFilter.getAcross().toString() +
                                "_at", githubCardFilter.getAcross().toString(), aggInterval.toString(), false, true);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = githubCardFilter.getAcross() + "_interval";
                selectDistinctString = githubCardFilter.getAcross() + "_interval";
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
        String projectsWhere = "";
        if (conditions.get(PROJECTS_TABLE).size() > 0)
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(PROJECTS_TABLE));
        String columnsWhere = "";
        if (conditions.get(COLUMNS_TABLE).size() > 0)
            columnsWhere = " WHERE " + String.join(" AND ", conditions.get(COLUMNS_TABLE));
        String cardsWhere = "";
        if (conditions.get(CARDS_TABLE).size() > 0)
            cardsWhere = " WHERE " + String.join(" AND ", conditions.get(CARDS_TABLE));
        String statusesWhere = "";
        if (conditions.get(STATUSES_TABLE).size() > 0)
            statusesWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0)
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        String projectJoin = " INNER JOIN ( SELECT id, project_id, project, organization, "
                + " creator as project_creator, integration_id as project_integration_id FROM " + company + "."
                + PROJECTS_TABLE + projectsWhere + " ) AS projects ON statuses.project_id"
                + " = projects.project_id AND statuses.integration_id=projects.project_integration_id ";
        String columnsJoin = " INNER JOIN ( SELECT id, project_id, column_id,name as column_name FROM "
                + company + "." + COLUMNS_TABLE + columnsWhere + " ) AS columns ON"
                + " statuses.column_id = columns.column_id AND projects.id=columns.project_id ";
        String cardsJoin = " INNER JOIN ( SELECT id, current_column_id,issue_id,creator as card_creator,"
                + " card_id as project_card_id, card_created_at FROM " + company + "." + CARDS_TABLE
                + cardsWhere + " ) AS cards ON"
                + " statuses.card_id=cards.project_card_id AND columns.id=cards.current_column_id ";
        String issuesJoin = " INNER JOIN (SELECT id,, title as issue_title, state as issue_state, " +
                "issue_id as issues_issue_id,repo_id as issues_repo_id,number as issues_number,labels as issues_labels,"
                + " assignees as issues_assignees,issue_closed_at,issue_created_at FROM " + company
                + "." + ISSUES_TABLE + issuesWhere + " ) AS issues ON issues.issues_issue_id = cards.issue_id ";
        String resolutionTimeColumn = "";
        if (needResolutionTimeReport) {
            resolutionTimeColumn = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                    + "-min(COALESCE(start_time,card_created_at)) as solve_time ";
            if (CollectionUtils.isNotEmpty(githubCardFilter.getExcludeColumns()))
                resolutionTimeColumn = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                        + "-min(COALESCE(start_time,card_created_at))-COALESCE((SELECT SUM(end_time - start_time)"
                        + " AS exclude_time FROM " + company + "." + STATUSES_TABLE
                        + " WHERE integration_id IN (:integration_ids)"
                        + " AND column_id IN (Select column_id from " + company + "." + COLUMNS_TABLE
                        + " WHERE name IN (:exclude_columns))),0) AS solve_time ";
        }
        String columnTime = "";
        if (needTimeAcrossColumnReport) {
            columnTime = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                    + "-min(COALESCE(start_time,card_created_at)) as time_spent ";
        }
        String needResolutionSql = needResolutionTimeReport ? " SELECT " + selectDistinctString 
                + resolutionTimeColumn : "";
        String needColumnSql = needTimeAcrossColumnReport ? " SELECT " + selectDistinctString + columnTime : "";
        return   needResolutionSql + needColumnSql
                + (StringUtils.isEmpty(needResolutionSql)  && StringUtils.isEmpty(needColumnSql) ? "  SELECT * " : " FROM ( SELECT * ")
                + intervalColumn
                + " FROM " + company + "." + STATUSES_TABLE + " as statuses "
                + projectJoin
                + columnsJoin
                + cardsJoin
                + issuesJoin
                + statusesWhere
                + (needResolutionTimeReport || needTimeAcrossColumnReport ? ") temp GROUP BY card_id," + groupByString : "");
    }


    public <T> Optional<T> getFilterValue(String key, Class<T> clazz, Map<String, Object> filter) {
        return Optional.ofNullable(filter)
                .map(f -> f.get(key))
                .map(clazz::cast);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<String> getListOrDefault(Map<String, Object> filter, String key) {
        try {
            var value = (Collection) MapUtils.emptyIfNull(filter).getOrDefault(key, Collections.emptyList());
            if (value == null || value.size() < 1){
                return List.of();
            }
            if (value.size() > 0 && !(value.iterator().next() instanceof String)){
                return (List<String>) value.stream().map(i -> i.toString()).collect(Collectors.toList());
            }
            // to handle list or sets
            return (List<String>) value.stream().collect(Collectors.toList());
        } catch (ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter parameter: " + key);
        }
    }
}
