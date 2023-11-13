package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.OktaGroupsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Log4j2
@Service
public class JiraOktaService extends DatabaseService<DBDummyObj> {

    private static final String JIRA_ISSUES_TABLE = "jira_issues";

    private static final String OKTA_USERS = "okta_users";
    private static final String OKTA_GROUPS = "okta_groups";

    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final OktaAggService oktaAggService;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public JiraOktaService(DataSource dataSource,
                           JiraConditionsBuilder jiraConditionsBuilder,
                           OktaAggService oktaAggService) {
        super(dataSource);
        this.oktaAggService = oktaAggService;
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    private static RowMapper<DbAggregationResult> buildOktaGroupIssueCount() {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(rs.getString("name"))
                .count(rs.getLong("count"))
                .build();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(JiraIssueService.class, OktaAggService.class);
    }

    @Override
    public String insert(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DBDummyObj> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<DBDummyObj> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }

    public DbListResponse<DbAggregationResult> groupJiraIssuesWithOkta(String company,
                                                                       JiraIssuesFilter jiraFilter,
                                                                       OktaGroupsFilter oktaGroupsFilter,
                                                                       OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        List<String> oktaGroupConditions = oktaAggService.createWhereClauseAndUpdateParams(params,
                oktaGroupsFilter.getObjectClass(), oktaGroupsFilter.getTypes(), oktaGroupsFilter.getMembers(),
                oktaGroupsFilter.getGroupIds(), oktaGroupsFilter.getIntegrationIds(), oktaGroupsFilter.getNames());

        String jiraIssuesWhere = null;
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        //jira stuff end

        //okta
        String oktaConditions = "";
        if (oktaGroupConditions.size() > 0)
            oktaConditions = " WHERE " + String.join(" AND ", oktaGroupConditions);
        //okta end

        String sql = "";
        if (jiraIssuesWhere == null) {
            sql = "SELECT B.name, " +
                    "       Sum(B.count) AS count " +
                    "FROM   (SELECT A.*, " +
                    "               (SELECT Count(*) " +
                    "                FROM  " + "(select DISTINCT ON (key) * from " + company + "." + JIRA_ISSUES_TABLE +
                    "                                                       order by key,ingested_at desc)" + " AS J " +
                    "                WHERE  J.assignee = A.display_name) " +
                    "        FROM   (SELECT a.user_id, " +
                    "                       a.group_id, " +
                    "                       a.NAME, " +
                    "                       Concat_ws(' ', b.first_name, b.middle_name, b.last_name) " +
                    "                       AS " +
                    "                       display_name " +
                    "                FROM   (SELECT Unnest(members) AS user_id, " +
                    "                               group_id, " +
                    "                               NAME " +
                    "                        FROM   " + company + "." + OKTA_GROUPS + oktaConditions + ") AS a " +
                    "                       LEFT JOIN " + company + "." + OKTA_USERS + " AS b " +
                    "                              ON a.user_id = b.user_id) AS A) AS B " +
                    "GROUP  BY name; ";
        } else {
            sql = "SELECT B.name, " +
                    "       Sum(B.count) AS count " +
                    "FROM   (SELECT A.*, " +
                    "               (SELECT Count(*) " +
                    "                FROM  " + "(select DISTINCT ON (key) * from " + company + "." + JIRA_ISSUES_TABLE +
                    "                                                       order by key,ingested_at desc)" + " AS J " +
                    jiraIssuesWhere +
                    "                AND  J.assignee = A.display_name) " +
                    "        FROM   (SELECT a.user_id, " +
                    "                       a.group_id, " +
                    "                       a.NAME, " +
                    "                       Concat_ws(' ', b.first_name, b.middle_name, b.last_name) " +
                    "                       AS " +
                    "                       display_name " +
                    "                FROM   (SELECT Unnest(members) AS user_id, " +
                    "                               group_id, " +
                    "                               NAME " +
                    "                        FROM   " + company + "." + OKTA_GROUPS + oktaConditions + ") AS a " +
                    "                       LEFT JOIN " + company + "." + OKTA_USERS + " AS b " +
                    "                              ON a.user_id = b.user_id) AS A) AS B " +
                    "GROUP  BY name; ";
        }
        List<DbAggregationResult> results = template.query(sql, params, buildOktaGroupIssueCount());
        return DbListResponse.of(results, results.size());
    }

}
