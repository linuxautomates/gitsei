package io.levelops.commons.databases.services.parsers;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

@Log4j2
public class ScmIssuesFilterParser {

    private static final String ISSUES_TABLE = "scm_issues";
    private static final String FINAL_TABLE = "final_table";

    @SuppressWarnings("unchecked")
    public ScmIssueFilter merge(Integer integrationId, ScmIssueFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Object> filterExclude = MapUtils.emptyIfNull((Map<String, Object>) productFilter.get("exclude"));
        List<String> excludeStates = (List<String>) filterExclude.getOrDefault("states", List.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        return ScmIssueFilter.builder()
                .calculation(reqFilter.getCalculation())
                .across(reqFilter.getAcross())
                .extraCriteria(MoreObjects.firstNonNull(
                        getListOrDefault(productFilter, "hygiene_types"),
                        List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(ScmIssueFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .repoIds(getListOrDefault(productFilter, "repo_ids"))
                .creators(getListOrDefault(productFilter, "creators"))
                .projects(getListOrDefault(productFilter,"projects"))
                .assignees(getListOrDefault(productFilter, "assignees"))
                .labels(getListOrDefault(productFilter, "labels"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeCreators(getListOrDefault(excludedFields, "creators"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                .excludeStates(getListOrDefault(excludedFields, "states"))
                .excludeLabels(getListOrDefault(excludedFields, "labels"))
                .excludeStates(excludeStates)
                .states(getListOrDefault(productFilter, "states"))
                .issueClosedRange(reqFilter.getIssueClosedRange())
                .issueCreatedRange(reqFilter.getIssueCreatedRange())
                .integrationIds(List.of(String.valueOf(integrationId)))
                .title((String) productFilter.getOrDefault("title", null))
                .partialMatch(partialMatchMap)
                .aggInterval(reqFilter.getAggInterval())
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, ScmIssueFilter scmIssueFilter,
                             long currentTime, boolean isListQuery) {
        String issuesWhere = "";
        String finalWhere = "";
        String intervalColumn = "";
        String resolutionTimeColumn = "";
        if (isListQuery) {
            if (conditions.get(ISSUES_TABLE).size() > 0)
                issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
            if (conditions.get(FINAL_TABLE).size() > 0)
                finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
            return "SELECT * FROM (SELECT issues.*,"
                    + "(extract(epoch FROM COALESCE(first_comment_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM issue_created_at)) AS resp_time FROM "
                    + company + "." + ISSUES_TABLE + " AS issues"
                    + issuesWhere + " ) a" + finalWhere;
        }
        if (conditions.get(ISSUES_TABLE).size() > 0)
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        if (conditions.get(FINAL_TABLE).size() > 0)
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        boolean needResolutionTimeReport = false;
        AggTimeQueryHelper.AggTimeQuery issueModAggQuery;
        AGG_INTERVAL aggInterval = scmIssueFilter.getAggInterval() == null ? AGG_INTERVAL.day : scmIssueFilter.getAggInterval();
        switch (scmIssueFilter.getAcross()) {
            case issue_closed:
                conditions.get(ISSUES_TABLE).add("issue_closed_at IS NOT NULL");
            case issue_created:
            case issue_updated:
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(scmIssueFilter.getAcross().toString() +
                                "_at", scmIssueFilter.getAcross().toString(), aggInterval.toString(), false);
                intervalColumn = issueModAggQuery.getHelperColumn();
                break;
            case first_comment:
                conditions.get(ISSUES_TABLE).add("first_comment_at IS NOT NULL");
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(scmIssueFilter.getAcross().toString() +
                                "_at", scmIssueFilter.getAcross().toString(), aggInterval.toString(), false);
                intervalColumn = issueModAggQuery.getHelperColumn();
                break;
            default:
        }
        if (scmIssueFilter.getCalculation() != null && scmIssueFilter.getCalculation().
                equals(ScmIssueFilter.CALCULATION.resolution_time)) {
            needResolutionTimeReport = true;
        }
        if (needResolutionTimeReport) {
            resolutionTimeColumn = ",(extract(epoch FROM COALESCE(issue_closed_at,TO_TIMESTAMP(" +
                    currentTime + ")))-extract(epoch FROM issue_created_at)) AS solve_time";
        }

        return "SELECT * FROM (SELECT issues.*,"
                + "(extract(epoch FROM COALESCE(first_comment_at,TO_TIMESTAMP(" + currentTime + ")))"
                + "-extract(epoch FROM issue_created_at)) AS resp_time"
                + (needResolutionTimeReport ? resolutionTimeColumn : "")
                + intervalColumn
                + " FROM " + company + "." + ISSUES_TABLE + " AS issues"
                + issuesWhere
                + " ) a" + finalWhere;
    }
}
