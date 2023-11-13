package io.levelops.commons.databases.services.jira;

import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.FINAL_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.USERS_TABLE;
import static io.levelops.commons.databases.services.jira.JiraIssueWriteService.UNDEFINED_STATUS_ID;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getParentSPjoin;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getSlaTimeJoinStmt;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.getUserTableJoin;

@Log4j2
@Service
public class JiraIssueStatusService {

    private final NamedParameterJdbcTemplate template;
    private final JiraConditionsBuilder conditionsBuilder;

    public JiraIssueStatusService(DataSource dataSource,
                                  JiraConditionsBuilder conditionsBuilder) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.conditionsBuilder = conditionsBuilder;
    }

    public Map<Pair<String, String>, List<DbJiraStatus>> getStatusesForIssues(String company, List<DbJiraIssue> issues, List<String> excludeStatuses) {
        if (CollectionUtils.isEmpty(issues)) {
            return Map.of();
        }
        Map<String, Object> params = new HashMap<>();
        String excludeStatusesCondition = "";
        if(CollectionUtils.isNotEmpty(excludeStatuses)){
            excludeStatusesCondition  = " AND status NOT IN (:exclude_statuses) ";
            params.put("exclude_statuses",excludeStatuses);
        }
        params.put("issue_keys", issues.stream()
                .map(DbJiraIssue::getKey)
                .distinct()
                .collect(Collectors.toList()));
        params.put("integration_ids", issues.stream()
                .map(DbJiraIssue::getIntegrationId)
                .map(NumberUtils::toInt)
                .distinct()
                .collect(Collectors.toList()));
        final List<DbJiraStatus> statuses = template.query(
                "SELECT * FROM " + company + "." + STATUSES_TABLE +
                        " WHERE issue_key IN (:issue_keys)" + excludeStatusesCondition +
                        " AND integration_id IN (:integration_ids) ORDER BY start_time DESC",
                params,
                DbJiraIssueConverters.listStatusMapper());
        return statuses
                .stream()
                .collect(Collectors.groupingBy(
                        dbJiraStatus -> Pair.of(dbJiraStatus.getIntegrationId(), dbJiraStatus.getIssueKey())));
    }

    public Map<Pair<String, String>, List<DbJiraStatus>> getStatusesForIssues(String company, List<DbJiraIssue> issues) {
        return getStatusesForIssues(company, issues, null);
    }

    public Map<String, DbJiraStatus> getHistoricalStatusForIssues(String company, String integrationId, List<String> issueKeys, long targetTime) {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        if (CollectionUtils.isEmpty(issueKeys)) {
            return Map.of();
        }
        String sql = "SELECT * FROM " + company + "." + STATUSES_TABLE +
                " WHERE issue_key IN (:issue_keys) " +
                " AND integration_id = :integration_id " +
                " AND start_time IS NOT NULL AND :target_time >= start_time " +
                " AND end_time   IS NOT NULL AND :target_time <  end_time ";
        Map<String, Object> params = Map.of(
                "issue_keys", issueKeys,
                "integration_id", Integer.valueOf(integrationId),
                "target_time", targetTime);
        final List<DbJiraStatus> statuses = template.query(sql, params, DbJiraIssueConverters.listStatusMapper());
        return ListUtils.emptyIfNull(statuses).stream()
                .collect(Collectors.toMap(DbJiraStatus::getIssueKey, Function.identity(), (a, b) -> (a.getStartTime() > b.getStartTime() ? a : b)));
    }

    public DbListResponse<JiraStatusTime> listIssueStatusesByTime(String company,
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

        String timeStatusesWhere = "";
        if (CollectionUtils.isNotEmpty(filter.getTimeStatuses())) {
            timeStatusesWhere = " WHERE s.status IN ( :tstatuses ) ";
            params.put("tstatuses", filter.getTimeStatuses());
        }

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

        List<JiraStatusTime> results = new ArrayList<>();
        if (pageSize > 0) {
            String sql = "WITH " +
                    " issues AS ( SELECT integration_id,key,summary FROM ( SELECT issues.*"
                    + slaTimeColumns + " FROM " + company + "." + ISSUES_TABLE + " as issues"
                    + userTableJoin + slaTimeJoin
                    + issuesWhere
                    + " ) as tbl" + parentSPjoin + finalWhere + " ),"
                    + " status AS ( SELECT issues.*,s.status AS tstatus,"
                    + "(s.end_time - s.start_time) AS time_spent FROM issues" +
                    " INNER JOIN " + company + "." + STATUSES_TABLE + " AS s " +
                    " ON s.issue_key = issues.key AND s.integration_id = issues.integration_id"
                    + timeStatusesWhere + " )" +
                    " SELECT * FROM (" +
                    "   SELECT integration_id,key,summary,tstatus,SUM(time_spent) as total" +
                    "   FROM status GROUP BY integration_id,key,summary,tstatus " +
                    " ) AS x" +
                    " ORDER BY total DESC" +
                    " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params,
                    DbJiraIssueConverters.listIssueStatusTimeMapper());
        }
        String countSql = "WITH issues AS ( SELECT integration_id,key,summary FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + ISSUES_TABLE + " as issues"
                + userTableJoin + slaTimeJoin
                + issuesWhere + " ) as tbl" + parentSPjoin + finalWhere + " ),"
                + " status AS ( SELECT issues.*,s.status AS tstatus,"
                + "(s.end_time - s.start_time) AS time_spent FROM issues INNER JOIN "
                + company + "." + STATUSES_TABLE + " AS s ON s.issue_key = issues.key AND s.integration_id = issues.integration_id"
                + timeStatusesWhere + " ) SELECT COUNT(*) FROM ( SELECT integration_id,key,tstatus"
                + " FROM status GROUP BY integration_id,key,tstatus ) AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public boolean doIssueStatusOverlap(String company, DbJiraIssue issue) {

        String query = "SELECT 1 as res FROM " + company + "." + STATUSES_TABLE
                + " as jira_status1, " + company + "." + STATUSES_TABLE + " as jira_status2 "
                + " WHERE jira_status1.issue_key = jira_status2.issue_key\n" +
                " AND jira_status1.status = jira_status2.status\n" +
                " AND jira_status1.end_time = jira_status2.start_time\n" +
                " AND jira_status1.integration_id = jira_status2.integration_id\n" +
                " AND  jira_status1.issue_key = :issue_key ";

        Map<String, Object> params = new HashMap<>();
        params.put("issue_key", issue.getKey());

        return !template.queryForList(query, params, Integer.class).isEmpty();
    }

    public void deleteAndUpdateStatuses(String company, DbJiraIssue issue) {

        template.getJdbcOperations().execute(TransactionCallback.of(conn -> {

            String deleteSql = "DELETE FROM " + company + "." + STATUSES_TABLE +
                    " WHERE issue_key = ? " +
                    " AND  integration_id = ? ";

            String statusSql = "INSERT INTO " + company + "." + STATUSES_TABLE +
                    " (issue_key,integration_id,status,start_time,end_time,status_id) VALUES (?,?,?,?,?,?)" +
                    " ON CONFLICT (issue_key,integration_id,status,start_time) DO UPDATE SET end_time=EXCLUDED.end_time, status_id=EXCLUDED.status_id";

            try (PreparedStatement del = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStatuses = conn.prepareStatement(statusSql, Statement.RETURN_GENERATED_KEYS);) {

                del.setString(1, issue.getKey());
                del.setInt(2, NumberUtils.toInt(issue.getIntegrationId()));
                int count = del.executeUpdate();

                log.info("{}, {} statuses deleted for issue {}", company, count, issue.getKey());

                int i = 1;
                if (CollectionUtils.isNotEmpty(issue.getStatuses())) {
                    log.info("{}, inserting {} status records for issue {}", company, issue.getStatuses(), issue.getKey());
                    for (DbJiraStatus status : issue.getStatuses()) {
                        i = 1;
                        insertStatuses.setObject(i++, status.getIssueKey());
                        insertStatuses.setObject(i++, NumberUtils.toInt(status.getIntegrationId()));
                        insertStatuses.setObject(i++, status.getStatus());
                        insertStatuses.setObject(i++, status.getStartTime());
                        insertStatuses.setObject(i++, status.getEndTime());
                        insertStatuses.setObject(i, StringUtils.firstNonBlank(status.getStatusId(), UNDEFINED_STATUS_ID));
                        insertStatuses.addBatch();
                        insertStatuses.clearParameters();
                    }
                    insertStatuses.executeBatch();
                }
                return null;
            }
        }));
    }

}
