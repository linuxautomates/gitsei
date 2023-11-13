package io.levelops.commons.databases.services.jira.utils;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JiraIssueQueryBuilder {

    public static final String DEFAULT_TICKET_CATEGORY = "Other";
    public static final String DONE_STATUS_CATEGORY = "DONE";
    public static final String IGNORE_TERMINAL_STAGE = "Ignore_Terminal_Stage";
    private final JiraConditionsBuilder conditionsBuilder;

    @Autowired
    public JiraIssueQueryBuilder(JiraConditionsBuilder conditionsBuilder) {
        this.conditionsBuilder = conditionsBuilder;
    }

    public String generateTicketCategorySql(String company, Map<String, Object> params, JiraIssuesFilter filter, Long currentTime) {
        if (CollectionUtils.isEmpty(filter.getTicketCategorizationFilters())) {
            return String.format("'%s'", DEFAULT_TICKET_CATEGORY);
        }
        MutableInt nbCategory = new MutableInt(0);
        List<String> categoryCases = ListUtils.emptyIfNull(filter.getTicketCategorizationFilters()).stream()
                .sorted(Comparator.comparingInt(cat -> MoreObjects.firstNonNull(cat.getIndex(), Integer.MAX_VALUE)))
                .map(category -> {
                    String paramPrefix = String.format("ticket_category_%d_", nbCategory.getAndIncrement());
                    Map<String, List<String>> whereClauseAndUpdateParams = conditionsBuilder
                            .createWhereClauseAndUpdateParams(company, params, paramPrefix, category.getFilter(), currentTime,
                                    null /* ingested at will be set on the final table filters */,
                                    "",
                                    null /* categories should not be OU dependant - the final table filters will take care of OU stuff */);
                    String categoryWhereClause = whereClauseAndUpdateParams.values().stream()
                            .flatMap(Collection::stream)
                            .collect(Collectors.joining(" AND "));
                    if (StringUtils.isEmpty(categoryWhereClause)) {
                        return null;
                    }
                    String categoryName = category.getName().replaceAll("'", "''");
                    return String.format(" WHEN (%s) THEN '%s' ", categoryWhereClause, categoryName);
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

    public String generateVelocityStageSql(Map<String, List<String>> devMap, Map<String, Object> params,
                                           List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryMetadataList) {
        if (MapUtils.isEmpty(devMap)) {
            return StringUtils.EMPTY;
        }
        List<String> terminalStatusCategoryCases = List.of();
        if (CollectionUtils.isNotEmpty(integStatusCategoryMetadataList)) {
            terminalStatusCategoryCases = integStatusCategoryMetadataList.stream()
                    .filter(Objects::nonNull)
                    .filter(dbJiraStatusCategoryToStatuses -> DONE_STATUS_CATEGORY.equalsIgnoreCase(dbJiraStatusCategoryToStatuses.getStatusCategory()))
                    .map(dbJiraStatusCategoryToStatuses -> {
                        String integrationId = dbJiraStatusCategoryToStatuses.getIntegrationId();
                        List<String> doneStatuses = dbJiraStatusCategoryToStatuses.getStatuses();
                        String paramPrefixForIgnore = String.format("status_category_%s_", integrationId);
                        List<String> doneStatusCategoryParams = doneStatuses.stream()
                                .map(status -> status.replaceAll("'", "''"))
                                .collect(Collectors.toList());
                        params.put(paramPrefixForIgnore, doneStatusCategoryParams);
                        params.put("integ_id_" + integrationId, Integer.valueOf(integrationId));
                        String categoryWhereClause = " state IN (:" + paramPrefixForIgnore + ") AND integration_id = :integ_id_" + integrationId;
                        return String.format(" WHEN (%s) THEN '%s' ", categoryWhereClause, IGNORE_TERMINAL_STAGE);
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        MutableInt nbStage = new MutableInt(0);
        List<String> categoryCases = devMap.entrySet().stream().map(entry -> {
                    String stageName = entry.getKey();
                    List<String> statusesList = entry.getValue();
                    String paramPrefix = String.format("velocity_stage_%d_", nbStage.getAndIncrement());
                    List<String> statusParams = statusesList.stream().map(status -> status.replaceAll("'", "''")).collect(Collectors.toList());
                    params.put(paramPrefix, statusParams);
                    String categoryWhereClause = " state IN (:" + paramPrefix + ")";
                    if (StringUtils.isEmpty(categoryWhereClause)) {
                        return null;
                    }
                    return String.format(" WHEN (%s) THEN '%s' ", categoryWhereClause, stageName);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(terminalStatusCategoryCases)) {
            List<String> list = new ArrayList<>();
            list.addAll(categoryCases);
            list.addAll(terminalStatusCategoryCases); // PROP-101 putting ignored statuses at the end so that status explicitly defined in the profile are not ignored
            return "CASE " +
                    String.join("", list) +
                    String.format(" ELSE '%s' ", DEFAULT_TICKET_CATEGORY) +
                    " END";
        }

        return "CASE " +
                String.join("", categoryCases) +
                String.format(" ELSE '%s' ", DEFAULT_TICKET_CATEGORY) +
                " END";
    }

    public String releaseTableJoinToBaseSql(String company, JiraIssuesFilter filter) {
        return filter.getIssueReleasedRange() != null && filter.getIssueReleasedRange().getLeft() != null && filter.getIssueReleasedRange().getRight() != null
                ? " INNER JOIN ("
                    + " SELECT integration_id AS integ_id, name AS fix_version  FROM " + company + ".jira_issue_versions"
                    + " WHERE end_date >= to_timestamp(" + filter.getIssueReleasedRange().getLeft() + ") AND end_date "
                    + " <= to_timestamp(" + filter.getIssueReleasedRange().getRight() + ") AND released = true ) versions ON"
                    + " versions.integ_id = issues.integration_id AND versions.fix_version = ANY(issues.fix_versions) "
                : " ";
    }
}
