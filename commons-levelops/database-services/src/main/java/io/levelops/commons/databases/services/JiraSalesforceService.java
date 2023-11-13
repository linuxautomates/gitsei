package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbSalesforceCaseConverters;
import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitSalesforce;
import io.levelops.commons.databases.models.database.combined.SalesforceWithJira;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraSalesforceService extends DatabaseService<DBDummyObj> {

    private static final String SALESFORCE_CASES = "salesforce_cases";

    private static final String JIRA_USERS_TABLE = "jira_users";
    private static final String JIRA_ISSUES_TABLE = "jira_issues";
    private static final String JIRA_PRIORITIES_SLA_TABLE = "jira_issue_priorities_sla";
    private static final String JIRA_ISSUE_SALESFORCE_CASES = "jira_issue_salesforce_cases";
    private static final String COMMIT_JIRA_TABLE = "scm_commit_jira_mappings";
    private static final String COMMITS_TABLE = "scm_commits";
    private static final String FILE_COMMITS_TABLE = "scm_file_commits";
    private static final String FILES_TABLE = "scm_files";
    private static final String JIRA_ISSUE_SPRINTS = "jira_issue_sprints";

    private final SalesforceCaseService salesforceCaseService;
    private final ScmAggService scmAggService;
    private final NamedParameterJdbcTemplate template;
    private final JiraConditionsBuilder jiraConditionsBuilder;

    private static final Map<String, Integer> SALESFORCE_CASES_SORTABLE_COLUMNS = Map.of("case_id", Types.VARCHAR,
            "subject", Types.VARCHAR, "contact", Types.VARCHAR, "creator", Types.VARCHAR, "origin", Types.VARCHAR,
            "status", Types.VARCHAR, "type", Types.VARCHAR, "priority", Types.VARCHAR,
            "reason", Types.VARCHAR, "sf_created_at", Types.TIMESTAMP);
    private static final Map<String, Integer> SALESFORCE_CASES_ESCALATION_SORTABLE_COLUMNS = Map.of("case_id", Types.VARCHAR,
            "subject", Types.VARCHAR, "contact", Types.VARCHAR, "creator", Types.VARCHAR, "origin", Types.VARCHAR,
            "reason", Types.VARCHAR, "sf_created_at", Types.TIMESTAMP, "sf_modified_at", Types.TIMESTAMP,
            "escalation_time", Types.TIMESTAMP, "priority", Types.VARCHAR);
    private static final Map<String, Integer> JIRA_ISSUE_SORTABLE_COLUMNS =  Map.of("key", Types.VARCHAR,
            "issue_type", Types.VARCHAR, "reporter", Types.VARCHAR, "assignee", Types.VARCHAR,
            "bounces", Types.NUMERIC,  "issue_created_at", Types.TIMESTAMP, "status", Types.VARCHAR);
    private static final Map<String, Integer> COMMITS_SORTABLE_COLUMNS =  Map.of("additions", Types.NUMERIC,
            "deletions", Types.NUMERIC, "files_ct", Types.NUMERIC, "committer", Types.VARCHAR,
            "message", Types.VARCHAR, "author", Types.VARCHAR, "committed_at", Types.TIMESTAMP);
    private static final Map<String, Integer> ESCALATED_FILES_SORTABLE_COLUMNS =  Map.of("repo_id", Types.VARCHAR,
            "project", Types.VARCHAR, "filename", Types.VARCHAR, "num_cases", Types.NUMERIC,
            "num_commits", Types.NUMERIC, "additions", Types.NUMERIC, "changes", Types.NUMERIC, "deletions", Types.NUMERIC);
    private static final Map<String, Integer> ESCALATED_FILES_REPORT_SORTABLE_COLUMNS =  Map.of("repo_id", Types.VARCHAR,
            "project", Types.VARCHAR, "ct", Types.NUMERIC);
    private static final Map<String, Integer> TOP_COMMITTERS_SORTABLE_COLUMNS =  Map.of("filename", Types.VARCHAR,
            "author", Types.VARCHAR, "no_of_commits", Types.NUMERIC);

    @Autowired
    public JiraSalesforceService(DataSource dataSource,
                                 ScmAggService scmAggService,
                                 JiraConditionsBuilder jiraConditionsBuilder,
                                 SalesforceCaseService salesforceCaseService) {
        super(dataSource);
        this.salesforceCaseService = salesforceCaseService;
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.scmAggService = scmAggService;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key) {
        return ((rs, rowNum) -> DbAggregationResult.builder()
                .key(rs.getString(key))
                .totalTickets(rs.getLong("ct"))
                .build());
    }

    private static RowMapper<JiraWithGitSalesforce> buildJiraCombinedObj() {
        return (rs, rowNumber) -> JiraWithGitSalesforce.builder()
                .id(rs.getString("id"))
                .key(rs.getString("key"))
                .issueType(rs.getString("issue_type"))
                .reporter(rs.getString("reporter"))
                .assignee(rs.getString("assignee"))
                .bounces(rs.getInt("bounces"))
                .issueCreatedAt(rs.getLong("issue_created_at"))
                .status(rs.getString("status"))
                .components(Arrays.asList((String[]) rs.getArray("components").getArray()))
                .labels(Arrays.asList((String[]) rs.getArray("labels").getArray()))
                .customFields(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "jira_issue",
                        rs.getString("custom_fields")))
                .salesforceCases(Arrays.asList((String[]) rs.getArray("sf_cases").getArray()))
                .build();
    }

    private static RowMapper<SalesforceWithJira> buildSalesforceCombinedObj() {
        return (rs, rowNumber) -> SalesforceWithJira.builder()
                .caseId(rs.getString("case_id"))
                .subject(rs.getString("subject"))
                .contact(rs.getString("contact"))
                .creator(rs.getString("creator"))
                .origin(rs.getString("origin"))
                .status(rs.getString("status"))
                .type(rs.getString("type"))
                .priority(rs.getString("priority"))
                .reason(rs.getString("reason"))
                .caseCreatedAt(rs.getTimestamp("sf_created_at"))
                .caseModifiedAt(rs.getTimestamp("sf_modified_at"))
                .jiraIssues(Arrays.asList((String[]) rs.getArray("jira_keys").getArray())).build();
    }

    private static RowMapper<SalesforceWithJira> buildSalesforceWithEscalationTime() {
        return (rs, rowNumber) -> SalesforceWithJira.builder()
                .caseId(rs.getString("case_id"))
                .subject(rs.getString("subject"))
                .contact(rs.getString("contact"))
                .creator(rs.getString("creator"))
                .origin(rs.getString("origin"))
                .status(rs.getString("status"))
                .type(rs.getString("type"))
                .priority(rs.getString("priority"))
                .reason(rs.getString("reason"))
                .escalationTime(rs.getLong("escalation_time"))
                .jiraKey(rs.getString("key"))
                .caseCreatedAt(rs.getTimestamp("sf_created_at"))
                .caseModifiedAt(rs.getTimestamp("sf_modified_at"))
                .build();
    }

    private static RowMapper<CommitWithJira> buildCommitCombinedObj() {
        return (rs, rowNumber) -> CommitWithJira.builder()
                .id(rs.getString("id"))
                .additions(rs.getInt("additions"))
                .deletions(rs.getInt("deletions"))
                .filesCt(rs.getInt("files_ct"))
                .committer(rs.getString("committer"))
                .message(rs.getString("message"))
                .author(rs.getString("author"))
                .commitUrl(rs.getString("commit_url"))
                .commitSha(rs.getString("commit_sha"))
                .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                .jiraKeys(Arrays.asList((String[]) rs.getArray("jira_keys").getArray()))
                .build();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(SalesforceCaseService.class, JiraIssueService.class);
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

    public DbListResponse<DbAggregationResult> groupJiraTicketsByStatus(String company,
                                                                        JiraIssuesFilter jiraFilter,
                                                                        SalesforceCaseFilter salesforceFilter,
                                                                        List<String> commitIntegrations,
                                                                        Boolean withCommit,
                                                                        Boolean commitCount,
                                                                        Map<String, SortingOrder> sortBy,
                                                                        OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, Map.of("ct", Types.NUMERIC, "status", Types.VARCHAR), "ct");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(),
                salesforceFilter.getPriorities(), salesforceFilter.getStatuses(), salesforceFilter.getContacts(),
                salesforceFilter.getTypes(), salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(),
                salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());

        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }

        String scmJiraTableFilter = "";
        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            scmJiraTableFilter = "scm_integ_id IN (:scm_integs)";
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        //jira stuff
        String commitTableJoin = "";
        if (commitCount) {
            commitTableJoin = " INNER JOIN ( SELECT issue_key,commit_sha,scm_integ_id FROM "
                    + company + "." + COMMIT_JIRA_TABLE
                    + ((scmJiraTableFilter.length() > 0) ? " WHERE " : "") + scmJiraTableFilter
                    + " ) AS z_scm_links ON issues.key = z_scm_links.issue_key";
        } else if (withCommit) {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " j_scm_links"
                    + " WHERE issues.key = j_scm_links.issue_key" +
                    ((scmJiraTableFilter.length() > 0) ? " AND " : "") + scmJiraTableFilter + " )");
        } else {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("NOT EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " j_scm_links"
                    + " WHERE issues.key = j_scm_links.issue_key" +
                    ((scmJiraTableFilter.length() > 0) ? " AND " : "") + scmJiraTableFilter + " )");
        }

        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }

        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String query;
        if (commitCount) {
            query = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " sf_cases AS ( SELECT issue_key,sf.id FROM " +
                    "( SELECT id,case_number FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf"
                    + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                    + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue"
                    + " ), j_issues AS ( SELECT key,commit_sha,status FROM ( SELECT issues.*,commit_sha"
                    + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + userTableJoin
                    + commitTableJoin
                    + sprintTableJoin
                    + slaTimeJoin
                    + jiraIssuesWhere
                    + " ) as tbl INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                    + jiraFinalWhere
                    + " ) SELECT COUNT(*) AS ct,status FROM ("
                    + " SELECT commit_sha,status,array_agg(DISTINCT key)::text[] as jira_keys FROM j_issues"
                    + " GROUP BY commit_sha,status ) AS x INNER JOIN " + company + "." + COMMITS_TABLE
                    + " AS y ON y.commit_sha = x.commit_sha GROUP BY status ORDER BY " + orderBy;
        } else {
            query = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " sf_cases AS ( SELECT issue_key,sf.id FROM ( SELECT id,case_number"
                    + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf"
                    + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                    + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue"
                    + " ), j_issues AS ( SELECT tbl.key,status FROM ( SELECT issues.*"
                    + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + userTableJoin + slaTimeJoin + sprintTableJoin
                    + jiraIssuesWhere
                    + " ) as tbl INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                    + jiraFinalWhere
                    + " ) SELECT COUNT(DISTINCT(key)) as ct,status FROM j_issues GROUP BY status";
        }

        final List<DbAggregationResult> results = template.query(query, params, aggRowMapper("status"));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> groupSalesforceCasesWithJiraLinks(String company,
                                                                                 JiraIssuesFilter jiraFilter,
                                                                                 SalesforceCaseFilter salesforceFilter,
                                                                                 Map<String, SortingOrder> sortBy,
                                                                                 OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, Map.of("ct", Types.NUMERIC, "status", Types.VARCHAR),
                "ct");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(),
                salesforceFilter.getPriorities(), salesforceFilter.getStatuses(), salesforceFilter.getContacts(),
                salesforceFilter.getTypes(), salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(),
                salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());

        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }

        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String query = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,status,sf.id FROM ( SELECT id,status,case_number "
                + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf"
                + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue"
                + " ), j_issues AS ( SELECT sf_cases.status,sf_cases.id FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + userTableJoin + sprintTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT COUNT(DISTINCT(id)) as ct,status FROM j_issues GROUP BY status ORDER BY " + orderBy;

        final List<DbAggregationResult> results = template.query(query, params, aggRowMapper("status"));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> resolvedTicketsTrendReport(String company,
                                                                          JiraIssuesFilter jiraFilter,
                                                                          SalesforceCaseFilter salesforceFilter,
                                                                          List<String> commitIntegrations,
                                                                          Boolean withCommit,
                                                                          Map<String, SortingOrder> sortBy,
                                                                          OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, Map.of("ct", Types.NUMERIC, "resolution_date", Types.TIMESTAMP),
                "resolution_date");

        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());

        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String scmJiraTableFilter = "";
        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            scmJiraTableFilter = " AND scm_integ_id IN (:scm_integs)";
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        //jira stuff
        if (withCommit) {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " z_scm_links"
                    + " WHERE jira_issues.key = z_scm_links.issue_key" + scmJiraTableFilter + " )");
        } else {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("NOT EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " z_scm_links"
                    + " WHERE jira_issues.key = z_scm_links.issue_key" + scmJiraTableFilter + " )");
        }


        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";

        jiraConditions.get(JIRA_ISSUES_TABLE).add(" issue_resolved_at IS NOT NULL");
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String query = sprintsWithClause
                + " SELECT count(*) as ct, EXTRACT(EPOCH FROM to_timestamp(issue_resolved_at)::date) resolution_date"
                + " FROM ( SELECT DISTINCT ON (key) key, issue_resolved_at FROM ( SELECT issues.* FROM "
                + company + "." + JIRA_ISSUE_SALESFORCE_CASES + " keys INNER JOIN ( SELECT case_number AS cn FROM "
                + company + "." + SALESFORCE_CASES + salesforceConditions
                + " ) cases ON cases.cn = keys.fieldvalue INNER JOIN ( SELECT * FROM " + slaTimeColumns
                + company + "." + JIRA_ISSUES_TABLE + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + ") issues ON issues.key=keys.issue_key AND keys.integration_id=issues.integration_id"
                + sprintTableJoin + jiraFinalWhere + " ) issues ORDER BY key, issue_resolved_at ) issues " +
                " GROUP BY issue_resolved_at ORDER BY " + orderBy ;
        final List<DbAggregationResult> result = template.query(query, params, aggRowMapper("resolution_date"));
        return DbListResponse.of(result, result.size());
    }

    public DbListResponse<SalesforceWithJira> listSalesforceCases(String company,
                                                                  JiraIssuesFilter jiraFilter,
                                                                  SalesforceCaseFilter salesforceFilter,
                                                                  Integer pageNum,
                                                                  Integer pageSize,
                                                                  Map<String, SortingOrder> sortBy,
                                                                  OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, SALESFORCE_CASES_SORTABLE_COLUMNS, "creator");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());

        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        params.put("integration_ids",
                salesforceFilter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String query = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,sf.id FROM ( SELECT case_number,id "
                + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf"
                + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue"
                + " ), j_issues AS ( SELECT tbl.key,sf_cases.id FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT x.jira_keys,y.* FROM ( SELECT id,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + SALESFORCE_CASES
                + " AS y ON y.id = x.id ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
        List<SalesforceWithJira> results = template.query(query, params,
                buildSalesforceCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,sf.id FROM ( SELECT case_number,id "
                + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf"
                + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue"
                + " ), j_issues AS ( SELECT tbl.key,sf_cases.id FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT COUNT(*) FROM ( SELECT id,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + SALESFORCE_CASES
                + " AS y ON y.id = x.id";
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<JiraWithGitSalesforce> listJiraTickets(String company,
                                                                 JiraIssuesFilter jiraFilter,
                                                                 SalesforceCaseFilter salesforceFilter,
                                                                 List<String> commitIntegrations,
                                                                 Boolean withCommit,
                                                                 Integer pageNum,
                                                                 Integer pageSize,
                                                                 Map<String, SortingOrder> sortBy,
                                                                 OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, JIRA_ISSUE_SORTABLE_COLUMNS, "assignee");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());

        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        String scmJiraTableFilter = "";
        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            scmJiraTableFilter = " AND scm_integ_id IN (:scm_integs)";
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        //jira stuff
        if (withCommit) {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " z_scm_links"
                    + " WHERE issues.key = z_scm_links.issue_key" + scmJiraTableFilter + " )");
        } else {
            jiraConditions.get(JIRA_ISSUES_TABLE).add("NOT EXISTS( SELECT commit_sha,issue_key FROM "
                    + company + "." + COMMIT_JIRA_TABLE + " z_scm_links"
                    + " WHERE issues.key = z_scm_links.issue_key" + scmJiraTableFilter + " )");
        }

        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        params.put("integration_ids",
                salesforceFilter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";


        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT UNNEST(issue_keys) as issue_key, case_number FROM "
                + "( SELECT case_number,ARRAY( SELECT issue_key FROM " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " WHERE " + SALESFORCE_CASES + ".case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue ) AS issue_keys "
                + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions
                + " ) a ), j_issues AS ( SELECT tbl.id, sf_cases.case_number FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl"
                + " INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT x.sf_cases, y.* FROM ( SELECT id, array_agg(DISTINCT case_number)::text[] as sf_cases"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + JIRA_ISSUES_TABLE
                + " AS y ON y.id = x.id WHERE integration_id IN (:integration_ids) ORDER BY " + orderBy
                + " OFFSET :skip LIMIT :limit";
        List<JiraWithGitSalesforce> results = template.query(sql, params, buildJiraCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT UNNEST(issue_keys) as issue_key, case_number FROM "
                + "( SELECT case_number,ARRAY( SELECT issue_key FROM " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " WHERE " + SALESFORCE_CASES + ".case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue ) AS issue_keys "
                + " FROM " + company + "." + SALESFORCE_CASES + salesforceConditions
                + " ) a ), j_issues AS ( SELECT tbl.id, sf_cases.case_number FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl"
                + " INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT COUNT(*) FROM ( SELECT id, array_agg(DISTINCT case_number)::text[] as sf_cases"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + JIRA_ISSUES_TABLE
                + " AS y ON y.id = x.id WHERE integration_id IN (:integration_ids)";
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> getCaseEscalationTimeReport(String company,
                                                                           JiraIssuesFilter jiraFilter,
                                                                           SalesforceCaseFilter salesforceFilter,
                                                                           Map<String, SortingOrder> sortBy,
                                                                           OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String groupBySql;
        String orderBySql;
        String key = "";
        String rowKey = "";
        Optional<String> additionalKey = Optional.empty();
        String intervalColumn = "";
        String selectDistinctString = "";
        Long latestIngestedDate;
        final SalesforceCaseFilter.DISTINCT DISTINCT = salesforceFilter.getAcross();
        if (SalesforceCaseFilter.DISTINCT.trend.equals(DISTINCT)) {
            AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery("cases.ingested_at",
                    DISTINCT.toString(), salesforceFilter.getAggInterval() != null ? salesforceFilter.getAggInterval().toString() : null, false);
            selectDistinctString = ticketModAggQuery.getSelect();
            intervalColumn = ticketModAggQuery.getHelperColumn();
            additionalKey = Optional.of(ticketModAggQuery.getIntervalKey());
            groupBySql = " GROUP BY " + ticketModAggQuery.getGroupBy();
            orderBySql = ticketModAggQuery.getOrderBy();
            key = "";
            rowKey = "trend";
            latestIngestedDate = null;
        } else {
            groupBySql = " GROUP BY " + DISTINCT.toString();
            orderBySql = " mx " + getSortOrder(sortBy, DISTINCT.name(), SortingOrder.DESC);
            key = DISTINCT.name();
            rowKey = key;
            latestIngestedDate = salesforceFilter.getIngestedAt();
        }
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                latestIngestedDate);
        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";

        jiraConditions.get(JIRA_ISSUES_TABLE).add(" issue_resolved_at IS NOT NULL");
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String aggSql = "MIN(escalation_time) as mn, MAX(escalation_time) as mx, COUNT(id) as ct," +
                " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY escalation_time)" + "," + key + selectDistinctString;
        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_issues AS (SELECT issues.*, j_cases.fieldvalue FROM (SELECT * FROM " + slaTimeColumns + company
                + "." + JIRA_ISSUES_TABLE + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") issues INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                " j_cases ON issues.key=j_cases.issue_key AND issues.integration_id=j_cases.integration_id " + jiraFinalWhere + " )" +
                "SELECT " + aggSql + " FROM (SELECT cases.*,issues.key " + intervalColumn  +
                ",EXTRACT(EPOCH FROM (to_timestamp(issues.issue_created_at) - cases.sf_created_at)) AS " +
                " escalation_time FROM (SELECT sf_issues.* FROM (SELECT fieldvalue, min(issue_created_at) min_issue_time" +
                " FROM sf_issues GROUP BY fieldvalue) t INNER JOIN sf_issues ON t.fieldvalue=sf_issues.fieldvalue" +
                " AND t.min_issue_time=sf_issues.issue_created_at) issues" + sprintTableJoin + " INNER JOIN " +
                "(SELECT * FROM " + company + "." + SALESFORCE_CASES + salesforceConditions +
                " ) cases on issues.fieldvalue=cases.case_number) sf_cases " + groupBySql + " ORDER BY " + orderBySql;
        final List<DbAggregationResult> results = template.query(sql, params,
                DbSalesforceCaseConverters.distinctRowMapper(rowKey, null, additionalKey));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> filesReport(String company,
                                                           ScmFilesFilter scmFilesFilter,
                                                           JiraIssuesFilter jiraFilter,
                                                           SalesforceCaseFilter salesforceFilter,
                                                           Map<String, SortingOrder> sortBy,
                                                           OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, ESCALATED_FILES_REPORT_SORTABLE_COLUMNS, "ct");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseConditions = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(), scmFilesFilter.getFilename(),
                scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(), scmFilesFilter.getCommitStartTime(),
                scmFilesFilter.getCommitEndTime(), null);

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        //jira stuff
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end
        String salesforceConditions = "";
        if (ListUtils.emptyIfNull(salesforceCaseConditions.get(SALESFORCE_CASES)).size() > 0)
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseConditions.get(SALESFORCE_CASES));
        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String commitTableJoin = " INNER JOIN " + company + "." + COMMIT_JIRA_TABLE
                + " AS commits_link ON issues.key = commits_link.issue_key";
        String filesSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,sf.id, sf.case_number FROM ( SELECT case_number,id  " +
                "FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf " +
                "INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue ), " +
                "j_issues AS ( SELECT distinct tbl.key,sf_cases.case_number FROM ( SELECT issues.* " + slaTimeColumns +
                " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues "
                + sprintTableJoin + userTableJoin + commitTableJoin + slaTimeJoin + jiraIssuesWhere
                + ") as tbl" +
                " INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key " + jiraFinalWhere + "), " +
                "files_agg AS ( SELECT files.*,commits.*, j_issues.key, j_issues.case_number FROM " +
                " ( select * from " + company + "." + FILES_TABLE + filesWhere + " ) AS files " +
                " INNER JOIN ( SELECT file_id,change ,addition, deletion, commit_sha FROM " +
                company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON files.id = commits.file_id " +
                "INNER JOIN ( SELECT issue_key,commit_sha FROM " + company + "." + COMMIT_JIRA_TABLE + " ) AS mapping " +
                "ON mapping.commit_sha = commits.commit_sha INNER JOIN j_issues ON mapping.issue_key = j_issues.key ) ";

        String path =
                StringUtils.isEmpty(scmFilesFilter.getModule())
                        ? "filename" : "substring(filename from " + (scmFilesFilter.getModule().length() + 2) + ")";
        String fileCondition = "";
        if (!scmFilesFilter.getListFiles()) {
            fileCondition = " where position('/' IN " + path + " ) > 0 ";
        }
        String sql = filesSql +
                "select split_part(" + path + ", '/', 1) as root_module, repo_id, project, COUNT(DISTINCT(case_number)) as ct " +
                "FROM files_agg " + fileCondition + " group by repo_id, project, root_module " +
                "order by " + orderBy + " , root_module asc";

        final List<DbAggregationResult> results = template.query(sql, params,
                DbScmConverters.escalatedSFCasesMapper("root_module"));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<SalesforceWithJira> listCasesWithEscalationTime(String company,
                                                                          JiraIssuesFilter jiraFilter,
                                                                          SalesforceCaseFilter salesforceFilter,
                                                                          Integer pageNum,
                                                                          Integer pageSize,
                                                                          Map<String, SortingOrder> sortBy,
                                                                          OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, SALESFORCE_CASES_ESCALATION_SORTABLE_COLUMNS, "escalation_time");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseCondition = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());
        String salesforceConditions = "";
        if (!salesforceCaseCondition.isEmpty()) {
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseCondition.get(SALESFORCE_CASES));
        }
        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        boolean needUserTableStuff = false;
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";

        jiraConditions.get(JIRA_ISSUES_TABLE).add(" issue_resolved_at IS NOT NULL");
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_issues AS (SELECT issues.*, j_cases.fieldvalue FROM (SELECT * " + slaTimeColumns + " FROM " + company
                + "." + JIRA_ISSUES_TABLE + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") issues "+ sprintTableJoin +" INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                " j_cases ON issues.key=j_cases.issue_key AND issues.integration_id=j_cases.integration_id " + jiraFinalWhere + " )" +
                "SELECT * FROM (SELECT cases.*,issues.key,EXTRACT(EPOCH FROM (to_timestamp(issues.issue_created_at) - cases.sf_created_at))" +
                " AS escalation_time FROM (SELECT sf_issues.* FROM (SELECT fieldvalue, min(issue_created_at) min_issue_time" +
                " FROM sf_issues GROUP BY fieldvalue) t INNER JOIN sf_issues ON t.fieldvalue=sf_issues.fieldvalue" +
                " AND t.min_issue_time=sf_issues.issue_created_at) issues INNER JOIN " +
                "(SELECT * FROM " + company + "." + SALESFORCE_CASES + salesforceConditions +
                " ) cases on issues.fieldvalue=cases.case_number) sf_cases ORDER BY " + orderBy +
                " OFFSET :skip LIMIT :limit";
        final List<SalesforceWithJira> cases = template.query(sql, params, buildSalesforceWithEscalationTime());
        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_issues AS (SELECT issues.*, j_cases.fieldvalue FROM (SELECT * " + slaTimeColumns + " FROM " + company
                + "." + JIRA_ISSUES_TABLE + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") issues " + sprintTableJoin + " INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                " j_cases ON issues.key=j_cases.issue_key AND issues.integration_id=j_cases.integration_id " + jiraFinalWhere + " )" +
                "SELECT count(*) FROM (SELECT cases.*,issues.key,EXTRACT(EPOCH FROM (to_timestamp(issues.issue_created_at) - cases.sf_created_at))" +
                " AS escalation_time FROM (SELECT sf_issues.* FROM (SELECT fieldvalue, min(issue_created_at) min_issue_time" +
                " FROM sf_issues GROUP BY fieldvalue) t INNER JOIN sf_issues ON t.fieldvalue=sf_issues.fieldvalue" +
                " AND t.min_issue_time=sf_issues.issue_created_at) issues INNER JOIN " +
                "(SELECT * FROM " + company + "." + SALESFORCE_CASES + salesforceConditions +
                " ) cases on issues.fieldvalue=cases.case_number) sf_cases ";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(cases, count);
    }

    public DbListResponse<CommitWithJira> listCommits(String company,
                                                      JiraIssuesFilter jiraFilter,
                                                      SalesforceCaseFilter salesforceFilter,
                                                      List<String> commitIntegrations,
                                                      Integer pageNum,
                                                      Integer pageSize,
                                                      Map<String, SortingOrder> sortBy,
                                                      OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, COMMITS_SORTABLE_COLUMNS, "author");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseConditions = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());
        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end
        String salesforceConditions = "";
        if (ListUtils.emptyIfNull(salesforceCaseConditions.get(SALESFORCE_CASES)).size() > 0)
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseConditions.get(SALESFORCE_CASES));
        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            jiraConditions.get(JIRA_ISSUES_TABLE).add(" scm_integ_id IN (:scm_integs) ");
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        String commitTableJoin = " INNER JOIN " + company + "." + COMMIT_JIRA_TABLE
                + " AS commits_link ON issues.key = commits_link.issue_key";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " issues_jira AS ( SELECT f.*, s.fieldvalue sf_case from "
                + company + "." + JIRA_ISSUES_TABLE
                + " f INNER JOIN " + " ((SELECT * FROM " + company + "." + SALESFORCE_CASES + salesforceConditions +
                ") cases INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " jira_cases ON cases.case_number = jira_cases.fieldValue) s ON f.key=s.issue_key"
                + " ), j_issues AS ( SELECT tbl.key,commit_sha FROM ( SELECT issues.*,commits_link.commit_sha,"
                + "commits_link.scm_integ_id" + slaTimeColumns + " FROM issues_jira as issues"
                + userTableJoin
                + sprintTableJoin
                + commitTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl"
                + jiraFinalWhere
                + " ) SELECT x.jira_keys,y.* FROM ( SELECT commit_sha,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY commit_sha ) AS x INNER JOIN " + company + "." + COMMITS_TABLE
                + " AS y ON y.commit_sha = x.commit_sha ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
        List<CommitWithJira> results = template.query(sql, params, buildCommitCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " issues_jira AS ( SELECT f.*, s.fieldvalue sf_case from "
                + company + "." + JIRA_ISSUES_TABLE
                + " f INNER JOIN " + " ((SELECT * FROM " + company + "." + SALESFORCE_CASES + salesforceConditions +
                ") cases INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " jira_cases ON cases.case_number = jira_cases.fieldValue) s ON f.key=s.issue_key"
                + " ), j_issues AS ( SELECT tbl.key,commit_sha FROM ( SELECT issues.*,commits_link.commit_sha,"
                + "commits_link.scm_integ_id" + slaTimeColumns + " FROM issues_jira as issues"
                + userTableJoin
                + sprintTableJoin
                + commitTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl"
                + jiraFinalWhere
                + " ) SELECT COUNT(*) FROM ( SELECT commit_sha,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY commit_sha ) AS x INNER JOIN " + company + "." + COMMITS_TABLE
                + " AS y ON y.commit_sha = x.commit_sha";
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbScmFile> listFiles(String company,
                                               ScmFilesFilter scmFilesFilter,
                                               JiraIssuesFilter jiraFilter,
                                               SalesforceCaseFilter salesforceFilter,
                                               Integer pageNum,
                                               Integer pageSize,
                                               Map<String, SortingOrder> sortBy,
                                               OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, ESCALATED_FILES_SORTABLE_COLUMNS, "num_commits");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseConditions = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(), salesforceFilter.getPriorities(),
                salesforceFilter.getStatuses(), salesforceFilter.getContacts(), salesforceFilter.getTypes(),
                salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(), salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(), scmFilesFilter.getFilename(),
                scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(), scmFilesFilter.getCommitStartTime(),
                scmFilesFilter.getCommitEndTime(), null);

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        //jira stuff

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match

        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end
        String salesforceConditions = "";
        if (ListUtils.emptyIfNull(salesforceCaseConditions.get(SALESFORCE_CASES)).size() > 0)
            salesforceConditions = " WHERE " + String.join(" AND ", salesforceCaseConditions.get(SALESFORCE_CASES));
        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String commitTableJoin = " INNER JOIN " + company + "." + COMMIT_JIRA_TABLE
                + " AS commits_link ON issues.key = commits_link.issue_key";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        List<DbScmFile> results = List.of();
        String filesSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,sf.id, sf.case_number FROM ( SELECT case_number,id  " +
                "FROM " + company + "." + SALESFORCE_CASES + salesforceConditions + " ) as sf " +
                "INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue ), " +
                "j_issues AS ( SELECT distinct tbl.key,sf_cases.case_number FROM ( SELECT issues.* " + slaTimeColumns +
                " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues "
                + sprintTableJoin + userTableJoin + commitTableJoin + slaTimeJoin + jiraIssuesWhere
                + ") as tbl" +
                " INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key " + jiraFinalWhere + "), " +
                "files_agg AS ( SELECT files.*,commits.*, j_issues.key, j_issues.case_number FROM " +
                " ( select * from " + company + "." + FILES_TABLE + filesWhere + " ) AS files " +
                " INNER JOIN ( SELECT file_id,change ,addition, deletion, commit_sha FROM " +
                company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON files.id = commits.file_id " +
                "INNER JOIN ( SELECT issue_key,commit_sha FROM " + company + "." + COMMIT_JIRA_TABLE + " ) AS mapping " +
                "ON mapping.commit_sha = commits.commit_sha INNER JOIN j_issues ON mapping.issue_key = j_issues.key ) ";
        if (pageSize > 0) {
            String sql = filesSql +
                    "SELECT COUNT(DISTINCT(case_number)) as num_cases,array_remove(array_agg(DISTINCT case_number), NULL)::text[] as cases," +
                    "SUM(change) as changes,SUM(addition) as additions,SUM(deletion) as deletions, COUNT(*) as num_commits," +
                    "repo_id,project,filename,id,integration_id FROM files_agg GROUP BY id,repo_id,project,filename,integration_id " +
                    " ORDER BY " + orderBy +
                    " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbScmConverters.filesSalesforceRowMapper());
        }
        String countSql = filesSql + " SELECT COUNT(DISTINCT(file_id)) FROM files_agg";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> getTopCommitters(String company,
                                                                JiraIssuesFilter jiraFilter,
                                                                SalesforceCaseFilter salesforceFilter,
                                                                ScmCommitFilter scmCommitFilter,
                                                                ScmFilesFilter scmFilesFilter,
                                                                List<String> commitIntegrations,
                                                                Map<String, SortingOrder> sortBy,
                                                                OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, TOP_COMMITTERS_SORTABLE_COLUMNS, "no_of_commits");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();

        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> salesforceCaseConditions = salesforceCaseService.createWhereClauseAndUpdateParams(
                params, salesforceFilter.getExtraCriteria(), salesforceFilter.getCaseIds(), salesforceFilter.getCaseNumbers(),
                salesforceFilter.getPriorities(), salesforceFilter.getStatuses(), salesforceFilter.getContacts(),
                salesforceFilter.getTypes(), salesforceFilter.getIntegrationIds(), salesforceFilter.getAccounts(),
                salesforceFilter.getAge(), salesforceFilter.getSFCreatedRange(), salesforceFilter.getSFUpdatedRange(),
                salesforceFilter.getIngestedAt());
        Map<String, List<String>> commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params,
                scmCommitFilter.getCommitShas(), scmCommitFilter.getRepoIds(), scmCommitFilter.getVcsTypes(), scmCommitFilter.getProjects(),
                scmCommitFilter.getCommitters(), scmCommitFilter.getAuthors(), scmCommitFilter.getDaysOfWeek(), scmCommitFilter.getIntegrationIds(),
                scmCommitFilter.getCommitBranches(), scmCommitFilter.getExcludeCommitShas(), scmCommitFilter.getExcludeRepoIds(), scmCommitFilter.getExcludeProjects(),
                scmCommitFilter.getExcludeCommitters(), scmCommitFilter.getExcludeCommitBranches(), scmCommitFilter.getExcludeAuthors(), scmCommitFilter.getExcludeDaysOfWeek(),
                scmCommitFilter.getCommittedAtRange(), scmCommitFilter.getLocRange(), scmCommitFilter.getExcludeLocRange(), scmCommitFilter.getPartialMatch(), scmCommitFilter.getExcludePartialMatch(), scmCommitFilter.getFileTypes(), scmCommitFilter.getExcludeFileTypes(),
                scmCommitFilter.getCodeChangeSize(), scmCommitFilter.getCodeChanges(), scmCommitFilter.getCodeChangeUnit(), scmCommitFilter.getTechnologies(), scmCommitFilter.getExcludeTechnologies(), null, "", false, ouConfig, scmCommitFilter.getIds(), scmCommitFilter.getIsApplyOuOnVelocityReport(),
                scmCommitFilter.getCreatedAtRange());
        Map<String, List<String>> fileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);
        String commitJiraWhere = "";
        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            commitJiraWhere = " WHERE scm_integ_id IN (:scm_integs)";
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        String salesforceWhere = "";
        if (ListUtils.emptyIfNull(salesforceCaseConditions.get(SALESFORCE_CASES)).size() > 0)
            salesforceWhere = " WHERE " + String.join(" AND ", salesforceCaseConditions.get(SALESFORCE_CASES));
        String commitsWhere = "";
        if (commitConditions.get(COMMITS_TABLE).size() > 0)
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
        String filesWhere = "";
        if (fileConditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", fileConditions.get(FILES_TABLE));

        // jira stuff

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        boolean requireSprints = isSprintTblJoinRequired(jiraFilter);
        //not supported for partial match
        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }
        String jiraFinalWhere = "";
        if (jiraConditions.get("final_table").size() > 0)
            jiraFinalWhere = " WHERE " + String.join(" AND ", jiraConditions.get("final_table"));
        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));
        String jiraSprintsWhere = "";
        if (jiraConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
            jiraSprintsWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUE_SPRINTS));
        //jira stuff end

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        String userTableJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + JIRA_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = issues.project AND p.prio = issues.priority AND p.integid = issues.integration_id"
                    + " AND p.ttype = issues.issue_type";
        }
        if (needUserTableStuff) {
            userTableJoin = " INNER JOIN ( SELECT display_name,integ_id FROM " + company + "."
                    + JIRA_USERS_TABLE + jiraUsersWhere + " ) AS u ON u.display_name = issues.assignee AND"
                    + " u.integ_id = issues.integration_id";
        }
        String sprintTableJoin = requireSprints ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String commitsSelect = ScmQueryUtils.COMMITS_SELECT;
        String authorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String committerTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);

        String jiraIssue = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " sf_cases AS ( SELECT issue_key,sf.id, sf.case_number FROM ( SELECT case_number,id  " +
                "FROM " + company + "." + SALESFORCE_CASES + salesforceWhere + " ) as sf " +
                "INNER JOIN " + company + "." + JIRA_ISSUE_SALESFORCE_CASES
                + " ON sf.case_number = " + JIRA_ISSUE_SALESFORCE_CASES + ".fieldValue ), " +
                "j_issues AS ( SELECT distinct tbl.key,sf_cases.case_number FROM ( SELECT issues.* " + slaTimeColumns +
                " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues "
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + ") as tbl" +
                " INNER JOIN sf_cases ON sf_cases.issue_key = tbl.key " + jiraFinalWhere + ") ";

        String commitsSQL = " ( SELECT " + commitsSelect + " FROM " + company + "." + COMMITS_TABLE +
                authorTableJoin + committerTableJoin +  " ) a";

        String sql = jiraIssue + " SELECT files.filename, commits.author, COUNT(file_commits.commit_sha) as no_of_commits FROM"
                + " ((SELECT * FROM " + company + "." + FILES_TABLE + " , j_issues " + filesWhere + ") AS files INNER JOIN " + company
                + "." + FILE_COMMITS_TABLE + " file_commits ON files.id = file_commits.file_id INNER JOIN (SELECT * FROM "
                + commitsSQL + commitsWhere + ") AS commits ON commits.commit_sha = file_commits.commit_sha"
                + " INNER JOIN ( SELECT * FROM " + company + "." + COMMIT_JIRA_TABLE + commitJiraWhere + ") commits_link "
                + "ON commits_link.commit_sha = commits.commit_sha INNER JOIN j_issues ON commits_link.issue_key = j_issues.key)"
                + " GROUP BY files.filename, commits.author ORDER BY " + orderBy ;

        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.commitFileRowMapper());
        return DbListResponse.of(results, results.size());
    }

    private Map<String, List<String>> createSprintWhereClause(Map<String, Object> params,
                                                              List<String> sprintIds,
                                                              List<String> sprintNames,
                                                              List<String> sprintFullNames,
                                                              List<String> sprintStates,
                                                              Integer sprintCount) {
        List<String> sprintTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sprintIds)) {
            sprintTableConditions.add("sprint_id IN (:sprint_ids)");
            params.put("sprint_ids",
                    sprintIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(sprintNames)) {
            sprintTableConditions.add("name IN (:sprint_names)");
            params.put("sprint_names", sprintNames);
        }
        if (CollectionUtils.isNotEmpty(sprintFullNames)) {
            sprintTableConditions.add("name IN (:sprint_full_names)");
            params.put("sprint_full_names", sprintFullNames);
        }
        if (CollectionUtils.isNotEmpty(sprintStates)) {
            sprintTableConditions.add("state IN (:sprint_states)");
            params.put("sprint_states", sprintStates);
        }
        if (sprintCount != null && sprintCount > 0) {
            sprintTableConditions.add("end_date IS NOT NULL");
        }
        return Map.of(JIRA_ISSUE_SPRINTS, sprintTableConditions);
    }

    private String getSprintQuery(Map<String, Object> params, JiraIssuesFilter jiraFilter, String company) {
        Map<String, List<String>> sprintConditions;
        String sprintIncludeJoinCondition = "";
        String sprintExcludeJoinCondition = "";
        String sprintIncludeQuery = "";
        String sprintExcludeQuery = "";
        String sprintTableJoin;
        String sprintWhere = "";
        boolean includeSprint = false;
        boolean excludeSprint = false;

        String sprintLimitStatement = "";
        if(jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + jiraFilter.getSprintCount();
        }

        if (CollectionUtils.size(jiraFilter.getSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getSprintNames()) > 0 ||
                (jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() >0 ) ||
                CollectionUtils.size(jiraFilter.getSprintStates()) > 0 || jiraFilter.getAcross() == JiraIssuesFilter.DISTINCT.sprint) {
            sprintConditions = createSprintWhereClause(params, jiraFilter.getSprintIds(), jiraFilter.getSprintNames(),
                    jiraFilter.getSprintFullNames(), jiraFilter.getSprintStates(), jiraFilter.getSprintCount());
            if (sprintConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
                sprintWhere = " WHERE " + String.join(" AND ", sprintConditions.get(JIRA_ISSUE_SPRINTS));
            sprintIncludeQuery = " array( select sprint_id from " + company + "." + JIRA_ISSUE_SPRINTS
                    + sprintWhere
                    + sprintLimitStatement + " ) AS inc_sprints";
            sprintIncludeJoinCondition = " spr.inc_sprints && issues.sprint_ids ";
            includeSprint = true;
        }
        if (CollectionUtils.size(jiraFilter.getExcludeSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getExcludeSprintNames()) > 0 ||
                CollectionUtils.size(jiraFilter.getExcludeSprintStates()) > 0) {
            sprintConditions = createSprintWhereClause(params, jiraFilter.getExcludeSprintIds(),
                    jiraFilter.getExcludeSprintNames(), jiraFilter.getExcludeSprintFullNames(), jiraFilter.getExcludeSprintStates(), jiraFilter.getSprintCount());
            if (sprintConditions.get(JIRA_ISSUE_SPRINTS).size() > 0)
                sprintWhere = " WHERE " + String.join(" AND ", sprintConditions.get(JIRA_ISSUE_SPRINTS));
            sprintExcludeQuery = " array( select sprint_id from " + company + "." + JIRA_ISSUE_SPRINTS
                    + sprintWhere
                    + sprintLimitStatement + " ) AS exc_sprints";
            sprintExcludeJoinCondition = " NOT spr.exc_sprints && issues.sprint_ids ";
            excludeSprint = true;
        }
        if (includeSprint && excludeSprint) {
            sprintTableJoin = " INNER JOIN ( SELECT " + sprintIncludeQuery + "," + sprintExcludeQuery + " ) AS spr ON " + sprintIncludeJoinCondition + " AND " + sprintExcludeJoinCondition;
        } else {
            sprintTableJoin = " INNER JOIN ( SELECT " + sprintIncludeQuery + sprintExcludeQuery + " ) AS spr ON " + sprintIncludeJoinCondition + sprintExcludeJoinCondition;
        }
        if (jiraFilter.getFilterByLastSprint() != null && jiraFilter.getFilterByLastSprint()) {
            sprintTableJoin += " AND (SELECT max(start_date) FROM " + company + "." + JIRA_ISSUE_SPRINTS
                    + " spr WHERE spr.sprint_id = ANY(sprint_ids)) = ANY(ARRAY(SELECT start_date FROM spr_dates spr))";
        }
        return sprintTableJoin;
    }

    private String getSprintAuxTable(String company, JiraIssuesFilter filter, String sprintWhere) {
        String sprintLimitStatement = "";
        if(filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }

        return "WITH spr_dates AS (select COALESCE(start_date, 99999999999) as start_date, sprint_id, name " +
                "from " + company + "." + JIRA_ISSUE_SPRINTS + sprintWhere + sprintLimitStatement + " ) ";
    }

    private boolean isSprintTblJoinRequired(JiraIssuesFilter jiraFilter) {
        return CollectionUtils.size(jiraFilter.getSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getSprintNames()) > 0 ||
                CollectionUtils.size(jiraFilter.getSprintStates()) > 0 || jiraFilter.getAcross() == JiraIssuesFilter.DISTINCT.sprint ||
                CollectionUtils.size(jiraFilter.getExcludeSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getExcludeSprintNames()) > 0 ||
                (jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() > 0) ||
                CollectionUtils.size(jiraFilter.getExcludeSprintStates()) > 0;
    }

    private String getOrderByStr(Map<String, SortingOrder> sortBy, Map<String, Integer> sortableColumns,
                                 String defaultSortKey) {
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if(sortableColumns.containsKey(entry.getKey())) {
                        return entry.getKey();
                    }
                    return defaultSortKey;
                })
                .orElse(defaultSortKey);

        SortingOrder defaultSortOrder = SortingOrder.DESC;
        if(sortableColumns.containsKey(sortByKey) && sortableColumns.get(sortByKey) == Types.TIMESTAMP) {
            defaultSortOrder = SortingOrder.ASC;
        }
        String sortOrder = getSortOrder(sortBy, sortByKey, defaultSortOrder);

        if(sortableColumns.containsKey(sortByKey) && sortableColumns.get(sortByKey) == Types.VARCHAR) {
            sortByKey = "lower(" + sortByKey + ")";
        }
        return " " + sortByKey + " " + sortOrder + " ";
    }

    private String getSortOrder(Map<String, SortingOrder> sortBy, String key, SortingOrder defaultOrder) {
        if (MapUtils.isEmpty(sortBy)) {
            return SortingOrder.DESC.toString();
        }

        return sortBy.getOrDefault(key, defaultOrder).toString();
    }
}