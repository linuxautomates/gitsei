package io.levelops.commons.databases.services;

import com.google.common.collect.Maps;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.converters.DbZendeskTicketConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitZendesk;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.*;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
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
public class ScmJiraZendeskService extends DatabaseService<DBDummyObj> {
    /**
     * This class only manages jira + scm combined aggs. it does not do any data insertion. only agg reads.
     */
    private static final String ZENDESK_TICKETS = "zendesk_tickets";
    private static final String ZENDESK_TICKET_JIRA_KEYS = "zendesk_ticket_jira_keys";

    private static final String JIRA_USERS_TABLE = "jira_users";
    private static final String JIRA_ISSUES_TABLE = "jira_issues";
    private static final String JIRA_PRIORITIES_SLA_TABLE = "jira_issue_priorities_sla";

    private static final String FILES_TABLE = "scm_files";
    private static final String COMMITS_TABLE = "scm_commits";
    private static final String FILE_COMMITS_TABLE = "scm_file_commits";
    private static final String COMMIT_JIRA_TABLE = "scm_commit_jira_mappings";
    private static final String JIRA_ISSUE_SPRINTS = "jira_issue_sprints";

    public static final String PRS_TABLE = "scm_pullrequests";
    public static final String PULLREQUESTS_WORKITEM_TABLE = "scm_pullrequests_workitem_mappings";

    private static final Map<String, Integer> ZENDESK_TICKET_SORTABLE_COLUMNS =  Map.of("brand", Types.VARCHAR,
            "type", Types.VARCHAR, "subject", Types.VARCHAR, "priority", Types.VARCHAR, "status", Types.VARCHAR,
            "ticket_id", Types.NUMERIC, "assignee", Types.VARCHAR, "submitter", Types.VARCHAR,
            "ticket_created_at", Types.TIMESTAMP, "ticket_updated_at", Types.TIMESTAMP);
    private static final Map<String, Integer> ZENDESK_TICKET_ESCALATION_SORTABLE_COLUMNS = Map.ofEntries(
            Map.entry("brand", Types.VARCHAR), Map.entry("type", Types.VARCHAR), Map.entry("subject", Types.VARCHAR),
            Map.entry("priority", Types.VARCHAR), Map.entry("status", Types.VARCHAR), Map.entry("assignee", Types.VARCHAR),
            Map.entry("submitter", Types.VARCHAR), Map.entry("ticket_id", Types.NUMERIC), Map.entry("escalation_time", Types.NUMERIC),
            Map.entry("key", Types.VARCHAR), Map.entry("ticket_created_at", Types.TIMESTAMP),Map.entry("ticket_updated_at", Types.TIMESTAMP));
    private static final Map<String, Integer> JIRA_ISSUE_SORTABLE_COLUMNS =  Map.of("key", Types.VARCHAR,
            "issue_type", Types.VARCHAR, "reporter", Types.VARCHAR, "assignee", Types.VARCHAR,
            "bounces", Types.NUMERIC,  "issue_created_at", Types.TIMESTAMP, "status", Types.VARCHAR);
    private static final Map<String, Integer> COMMITS_SORTABLE_COLUMNS =  Map.of("additions", Types.NUMERIC,
            "deletions", Types.NUMERIC, "files_ct", Types.NUMERIC, "committer", Types.VARCHAR,
            "message", Types.VARCHAR, "committed_at", Types.TIMESTAMP);
    private static final Map<String, Integer> ESCALATED_FILES_SORTABLE_COLUMNS =  Map.of("repo_id", Types.VARCHAR,
            "project", Types.VARCHAR, "filename", Types.VARCHAR, "num_tickets", Types.NUMERIC,
            "num_commits", Types.NUMERIC, "additions", Types.NUMERIC, "changes", Types.NUMERIC, "deletions", Types.NUMERIC);
    private static final Map<String, Integer> ESCALATED_FILES_REPORT_SORTABLE_COLUMNS =  Map.of("repo_id", Types.VARCHAR,
            "project", Types.VARCHAR, "ct", Types.NUMERIC);
    private static final Map<String, Integer> TOP_COMMITTERS_SORTABLE_COLUMNS =  Map.of("filename", Types.VARCHAR,
            "author", Types.VARCHAR, "no_of_commits", Types.NUMERIC);


    private final ScmAggService scmAggService;
    private final NamedParameterJdbcTemplate template;
    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final ZendeskTicketService zendeskTicketService;

    @Autowired
    public ScmJiraZendeskService(DataSource dataSource,
                                 ScmAggService aggService,
                                 JiraConditionsBuilder jiraConditionsBuilder,
                                 ZendeskTicketService zdTicketService) {
        super(dataSource);
        this.scmAggService = aggService;
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.zendeskTicketService = zdTicketService;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    private static RowMapper<DbAggregationResult> aggRowMapper(String key) {
        return ((rs, rowNum) -> DbAggregationResult.builder()
                .key(rs.getString(key))
                .totalTickets(rs.getLong("ct"))
                .build());
    }

    private static RowMapper<ZendeskWithJira> buildZendeskCombinedObj() {
        return (rs, rowNumber) -> ZendeskWithJira.builder()
                .id(rs.getString("id"))
                .jiraKeys(Arrays.asList((String[]) rs.getArray("issue_keys").getArray()))
                .brand(rs.getString("brand"))
                .type(rs.getString("type"))
                .subject(rs.getString("subject"))
                .priority(rs.getString("priority"))
                .status(rs.getString("status"))
                .integrationId(rs.getString("integration_id"))
                .ticketId(rs.getLong("ticket_id"))
                .assigneeEmail(rs.getString("assignee"))
                .submitterEmail(rs.getString("submitter"))
                .ticketCreatedAt(rs.getTimestamp("ticket_created_at"))
                .ticketUpdatedAt(rs.getTimestamp("ticket_updated_at")).build();
    }

    private static RowMapper<ZendeskWithJira> buildZendeskWithEscalationTime() {
        return (rs, rowNumber) -> ZendeskWithJira.builder()
                .id(rs.getString("id"))
                .brand(rs.getString("brand"))
                .type(rs.getString("type"))
                .subject(rs.getString("subject"))
                .priority(rs.getString("priority"))
                .status(rs.getString("status"))
                .assigneeEmail(rs.getString("assignee"))
                .submitterEmail(rs.getString("submitter"))
                .integrationId(rs.getString("integration_id"))
                .ticketId(rs.getLong("ticket_id"))
                .escalationTime(rs.getLong("escalation_time"))
                .jiraKey(rs.getString("key"))
                .ticketCreatedAt(rs.getTimestamp("ticket_created_at"))
                .ticketUpdatedAt(rs.getTimestamp("ticket_updated_at"))
                .build();
    }

    private static RowMapper<JiraWithGitZendesk> buildJiraCombinedObj() {
        return (rs, rowNumber) -> JiraWithGitZendesk.builder()
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
                .zendeskTickets(Arrays.asList((Long[]) rs.getArray("zendesk_tickets").getArray()))
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
        return Set.of(JiraIssueService.class, ScmAggService.class);
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
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }

    public DbListResponse<DbAggregationResult> groupZendeskTicketsWithJiraLink(String company,
                                                                               JiraIssuesFilter jiraFilter,
                                                                               ZendeskTicketsFilter ticketsFilter,
                                                                               Map<String, SortingOrder> sortBy,
                                                                               OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        String orderBy = getOrderByStr(sortBy, Map.of("ct", Types.NUMERIC, "status", Types.VARCHAR), "ct");
        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";

        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues AS ( SELECT * FROM ( SELECT issues.key"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + " INNER JOIN " + company + ".zendesk_ticket_jira_keys"
                + " ON issues.key = zendesk_ticket_jira_keys.issue_key"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere + " ) as tbl" + jiraFinalWhere
                + " ), zd_ticks AS ( SELECT zd.status FROM ( SELECT status,issue_key FROM "
                + company + "." + ZENDESK_TICKETS
                + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions + " ) as zd INNER JOIN j_issues ON j_issues.key = zd.issue_key"
                + " ) SELECT COUNT(*) as ct,status FROM zd_ticks GROUP BY status ORDER BY " + orderBy;

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbZendeskTicketConverters.aggRowMapper("status",
                ZendeskTicketsFilter.CALCULATION.ticket_count, Optional.empty()));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> resolvedTicketsTrendReport(String company,
                                                                          JiraIssuesFilter jiraFilter,
                                                                          ZendeskTicketsFilter ticketsFilter,
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

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);

        String query = sprintsWithClause
                + "SELECT COUNT(*) AS ct, resolution_date"
                + " FROM ( SELECT DISTINCT ON (key) key,"
                + " EXTRACT(EPOCH FROM to_timestamp(issue_resolved_at)::date) resolution_date"
                + " FROM ( SELECT issues.* FROM "
                + company + "." + ZENDESK_TICKET_JIRA_KEYS + " keys INNER JOIN ( SELECT id FROM "
                + company + "." + ZENDESK_TICKETS + zendeskConditions
                + " ) zdticks ON zdticks.id = keys.ticket_id INNER JOIN ( SELECT * " + slaTimeColumns + " FROM "
                + company + "." + JIRA_ISSUES_TABLE + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) issues ON issues.key = keys.issue_key " + sprintTableJoin + jiraFinalWhere + " ) issues ORDER BY key ) issues"
                + " GROUP BY resolution_date ORDER BY " + orderBy ;
        log.info("query = {}", query);
        log.info("params = {}", params);
        final List<DbAggregationResult> results = template.query(query, params, aggRowMapper("resolution_date"));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> getZendeskEscalationTimeReport(String company,
                                                                              JiraIssuesFilter jiraFilter,
                                                                              ZendeskTicketsFilter ticketsFilter,
                                                                              Map<String, SortingOrder> sortBy,
                                                                              OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String groupBySql;
        String orderBySql;
        String key;
        Long latestIngestedDate;
        final ZendeskTicketsFilter.DISTINCT DISTINCT = ticketsFilter.getDISTINCT();
        if (ZendeskTicketsFilter.DISTINCT.trend.equals(DISTINCT)) {
            groupBySql = " GROUP BY trend ";
            orderBySql = " trend " + getSortOrder(sortBy, "trend", SortingOrder.ASC) + " ";
            key = "trend";
            latestIngestedDate = null;
        } else {
            groupBySql = " GROUP BY " + DISTINCT.toString();
            orderBySql = " mx " + getSortOrder(sortBy, DISTINCT.toString(), SortingOrder.DESC) + " ";
            key = DISTINCT.name();
            latestIngestedDate = ticketsFilter.getIngestedAt();
        }
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                latestIngestedDate, ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        String aggSql = "MIN(escalation_time) as mn, MAX(escalation_time) as mx, COUNT(id) as ct," +
                " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY escalation_time)" + "," + key;

        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues as (SELECT k.ticket_id, i.* from " + company + "." +
                ZENDESK_TICKET_JIRA_KEYS + " k INNER JOIN (SELECT * " + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE
                + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") i on k.issue_key=i.key " + jiraFinalWhere + " ) SELECT " + aggSql + " FROM (SELECT ticks.*, issues.key" +
                ", EXTRACT(EPOCH FROM ticks.ingested_at) AS trend," +
                " EXTRACT(EPOCH FROM (to_timestamp(issues.issue_created_at) - ticks.ticket_created_at)) AS escalation_time" +
                " FROM (SELECT j_issues.* FROM j_issues INNER JOIN (SELECT ticket_id, min(issue_created_at)" +
                " min_issue_time FROM j_issues GROUP BY ticket_id) t ON j_issues.ticket_id=t.ticket_id AND" +
                " j_issues.issue_created_at = t.min_issue_time) issues "+ sprintTableJoin + " INNER JOIN (SELECT * FROM " + company +
                "." + ZENDESK_TICKETS + zendeskConditions + ") ticks on issues.ticket_id=ticks.id) zd_ticks"
                + groupBySql + " ORDER BY" + orderBySql;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        final List<DbAggregationResult> results = template.query(sql, params,
                DbZendeskTicketConverters.aggRowMapper(key, null, Optional.empty()));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> groupJiraTickets(String company,
                                                                JiraIssuesFilter jiraFilter,
                                                                ZendeskTicketsFilter ticketsFilter,
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
        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        //not supported for partial match
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

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String sql;
        if (commitCount) {
            sql =(StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " j_issues AS ( "
                    + " SELECT key,commit_sha,status FROM ( SELECT issues.*,scm_integ_id,commit_sha"
                    + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + sprintTableJoin
                    + userTableJoin
                    + commitTableJoin
                    + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                    + " ON issues.key = " + ZENDESK_TICKET_JIRA_KEYS + ".issue_key"
                    + slaTimeJoin
                    + jiraIssuesWhere
                    + " ) as tbl " + jiraFinalWhere + " ),"
                    + " zd_ticks AS ( SELECT zendesk_tickets.ticket_id,issue_key FROM "
                    + company + "." + ZENDESK_TICKETS
                    + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                    + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                    + zendeskConditions + " )"
                    + " SELECT COUNT(*) AS ct,status FROM ( SELECT j_issues.commit_sha,status FROM j_issues"
                    + " INNER JOIN zd_ticks ON j_issues.key = zd_ticks.issue_key"
                    + " INNER JOIN " + company + "." + COMMITS_TABLE + " AS y ON j_issues.commit_sha = y.commit_sha"
                    + " GROUP BY j_issues.commit_sha,status ) x "
                    + " GROUP BY status ORDER BY " + orderBy;
        } else {
            sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " j_issues AS ( SELECT key,status FROM ( SELECT *"
                    + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + sprintTableJoin
                    + userTableJoin
                    + slaTimeJoin
                    + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                    + " ON issues.key = " + ZENDESK_TICKET_JIRA_KEYS + ".issue_key"
                    + jiraIssuesWhere + " ) AS tbl " + jiraFinalWhere
                    + " ), zd_ticks AS ( SELECT issue_key FROM "
                    + company + "." + ZENDESK_TICKETS
                    + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                    + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                    + zendeskConditions
                    + " ) SELECT COUNT(DISTINCT(KEY)) AS ct,status FROM j_issues INNER JOIN"
                    + " zd_ticks ON j_issues.key = zd_ticks.issue_key"
                    + " GROUP BY status ORDER BY " + orderBy;
        }
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params,
                DbJiraIssueConverters.jiraZendeskRowMapper(JiraIssuesFilter.DISTINCT.status.toString()));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<ZendeskWithJira> listZendeskTicketsWithEscalationTime(String company,
                                                                                JiraIssuesFilter jiraFilter,
                                                                                ZendeskTicketsFilter ticketsFilter,
                                                                                Integer pageNum,
                                                                                Integer pageSize,
                                                                                Map<String, SortingOrder> sortBy,
                                                                                OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, ZENDESK_TICKET_ESCALATION_SORTABLE_COLUMNS, "escalation_time");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);
        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues as (SELECT k.ticket_id, i.* from " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                " k INNER JOIN (SELECT * " + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE
                + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") i on k.issue_key=i.key " + jiraFinalWhere + " )" +
                " SELECT * FROM (SELECT ticks.*, issues.key," +
                " EXTRACT(EPOCH FROM (ticks.ticket_created_at - to_timestamp(issues.issue_created_at))) AS escalation_time" +
                " FROM (SELECT j_issues.* FROM j_issues INNER JOIN (SELECT ticket_id, min(issue_created_at)" +
                " min_issue_time FROM j_issues GROUP BY ticket_id) t ON j_issues.ticket_id=t.ticket_id AND" +
                " j_issues.issue_created_at = t.min_issue_time) issues " +sprintTableJoin+ " INNER JOIN (SELECT * FROM " + company +
                "." + ZENDESK_TICKETS + zendeskConditions + ") ticks on issues.ticket_id=ticks.id) zd_ticks " +
                " ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        final List<ZendeskWithJira> tickets = template.query(sql, params, buildZendeskWithEscalationTime());
        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues as (SELECT k.ticket_id, i.* from " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                " k INNER JOIN (SELECT * " + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE
                + userTableJoin + slaTimeJoin + jiraIssuesWhere + ") i on k.issue_key=i.key " + jiraFinalWhere + " )" +
                " SELECT count(id) FROM (SELECT ticks.*, issues.key," +
                " EXTRACT(EPOCH FROM (ticks.ticket_created_at - to_timestamp(issues.issue_created_at))) AS escalation_time" +
                " FROM (SELECT j_issues.* FROM j_issues INNER JOIN (SELECT ticket_id, min(issue_created_at)" +
                " min_issue_time FROM j_issues GROUP BY ticket_id) t ON j_issues.ticket_id=t.ticket_id AND" +
                " j_issues.issue_created_at = t.min_issue_time) issues " +sprintTableJoin+ " INNER JOIN (SELECT * FROM " + company +
                "." + ZENDESK_TICKETS + zendeskConditions + ") ticks on issues.ticket_id=ticks.id) zd_ticks ";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(tickets, count);
    }

    public DbListResponse<ZendeskWithJira> listZendeskTickets(String company,
                                                              JiraIssuesFilter jiraFilter,
                                                              ZendeskTicketsFilter ticketsFilter,
                                                              Integer pageNum,
                                                              Integer pageSize,
                                                              Map<String, SortingOrder> sortBy,
                                                              OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, ZENDESK_TICKET_SORTABLE_COLUMNS, "assignee");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);

        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues AS ( SELECT tbl.key FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + " INNER JOIN " + company + ".zendesk_ticket_jira_keys ON issues.key = zendesk_ticket_jira_keys.issue_key"
                + userTableJoin + sprintTableJoin + slaTimeJoin + jiraIssuesWhere + " ) as tbl" + jiraFinalWhere
                + " ), zd_ticks AS ( SELECT * FROM ( SELECT zdt.id,status,issue_key,priority,"
                + "assignee,submitter,ticket_created_at,ticket_updated_at,brand,type,subject,integration_id,zdt.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS + " as zdt INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON zdt.id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions + " ) as zd INNER JOIN j_issues ON j_issues.key = zd.issue_key )"
                + " SELECT id,status,array_agg(issue_key)::text[] as issue_keys,assignee,submitter,ticket_created_at,"
                + "ticket_updated_at,priority,brand,type,subject,integration_id,ticket_id FROM zd_ticks GROUP BY id,status,assignee,submitter,"
                + "ticket_created_at,ticket_updated_at,brand,type,subject,priority,integration_id,ticket_id ORDER BY " + orderBy
                + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<ZendeskWithJira> results = template.query(sql, params,
                buildZendeskCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues AS ( SELECT tbl.key FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + " INNER JOIN " + company + ".zendesk_ticket_jira_keys ON issues.key = zendesk_ticket_jira_keys.issue_key"
                + userTableJoin + sprintTableJoin + slaTimeJoin + jiraIssuesWhere + " ) as tbl" + jiraFinalWhere
                + " ), zd_ticks AS ( SELECT * FROM ( SELECT zdt.id,status,issue_key,priority,"
                + "assignee,submitter,ticket_created_at,ticket_updated_at,brand,type,subject,integration_id,zdt.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS + " as zdt INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON zdt.id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions + " ) as zd INNER JOIN j_issues ON j_issues.key = zd.issue_key )"
                + " SELECT COUNT(*) FROM ( SELECT id,status,array_agg(issue_key)::text[] as issue_keys,assignee,submitter,ticket_created_at,"
                + "ticket_updated_at,priority,brand,type,subject,integration_id,ticket_id FROM zd_ticks GROUP BY id,status,assignee,submitter,"
                + "ticket_created_at,ticket_updated_at,brand,type,subject,priority,integration_id,ticket_id ) AS dataset";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<JiraWithGitZendesk> listJiraTickets(String company,
                                                              JiraIssuesFilter jiraFilter,
                                                              ZendeskTicketsFilter ticketsFilter,
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

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

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

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);

        String sql =(StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT UNNEST(issue_keys) AS issue_key,ticket_id FROM "
                + "( SELECT ticket_id, ARRAY( SELECT issue_key FROM "
                + company + "." + ZENDESK_TICKET_JIRA_KEYS + " WHERE "
                + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id ) AS issue_keys FROM "
                + company + "." + ZENDESK_TICKETS
                + zendeskConditions
                + " ) a ), j_issues AS ( SELECT tbl.id,zd_ticks.ticket_id FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + ") as tbl"
                + " INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT x.zendesk_tickets,y.* FROM ( SELECT id,array_agg(ticket_id)::bigint[] as zendesk_tickets"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + JIRA_ISSUES_TABLE
                + " AS y ON y.id = x.id ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<JiraWithGitZendesk> results = template.query(sql, params, buildJiraCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT UNNEST(issue_keys) AS issue_key,ticket_id FROM "
                + " ( SELECT ticket_id, ARRAY( SELECT issue_key FROM "
                + company + "." + ZENDESK_TICKET_JIRA_KEYS + " WHERE "
                + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id ) AS issue_keys FROM "
                + company + "." + ZENDESK_TICKETS
                + zendeskConditions
                + " ) a ), j_issues AS ( SELECT tbl.id,zd_ticks.ticket_id FROM ( SELECT issues.*"
                + slaTimeColumns + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl"
                + " INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT COUNT(*) AS ct FROM ( SELECT id,array_agg(ticket_id)::bigint[] as zendesk_tickets"
                + " FROM j_issues GROUP BY id ) AS x INNER JOIN " + company + "." + JIRA_ISSUES_TABLE
                + " AS y ON y.id = x.id";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<CommitWithJira> listCommits(String company,
                                                      JiraIssuesFilter jiraFilter,
                                                      ZendeskTicketsFilter ticketsFilter,
                                                      List<String> commitIntegrations,
                                                      Integer pageNum,
                                                      Integer pageSize,
                                                      Map<String, SortingOrder> sortBy,
                                                      OUConfiguration ouConfig) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        String orderBy = getOrderByStr(sortBy, COMMITS_SORTABLE_COLUMNS, "committer");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());

        //jira stuff
        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
        //not supported for partial match

        String scmJiraTableFilter = "";
        if (CollectionUtils.isNotEmpty(commitIntegrations)) {
            scmJiraTableFilter = " WHERE scm_integ_id IN (:scm_integs)";
            params.put("scm_integs", commitIntegrations.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
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

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String commitTableJoin = " INNER JOIN z_scm_links ON issues.key = z_scm_links.issue_key";

        params.put("skip", pageNum * pageSize);
        params.put("limit", pageSize);

        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT issue_key,zendesk_tickets.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS
                + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions
                + " ), z_scm_links AS ( SELECT issue_key,commit_sha,scm_integ_id FROM "
                + company + "." + COMMIT_JIRA_TABLE + scmJiraTableFilter
                + " ), j_issues AS ( SELECT tbl.key,commit_sha FROM ( SELECT issues.*,scm_integ_id,"
                + "z_scm_links.commit_sha" + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + commitTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT x.jira_keys,y.* FROM ( SELECT commit_sha,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY commit_sha ) AS x INNER JOIN " + company + "." + COMMITS_TABLE
                + " AS y ON y.commit_sha = x.commit_sha ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CommitWithJira> results = template.query(sql, params, buildCommitCombinedObj());

        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT issue_key,zendesk_tickets.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS
                + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions
                + " ), z_scm_links AS ( SELECT issue_key,commit_sha,scm_integ_id FROM "
                + company + "." + COMMIT_JIRA_TABLE + scmJiraTableFilter
                + " ), j_issues AS ( SELECT tbl.key,commit_sha FROM ( SELECT issues.*,scm_integ_id,"
                + "z_scm_links.commit_sha" + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + commitTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere
                + " ) SELECT COUNT(*) FROM ( SELECT commit_sha,array_agg(DISTINCT key)::text[] as jira_keys"
                + " FROM j_issues GROUP BY commit_sha ) AS x INNER JOIN " + company + "." + COMMITS_TABLE
                + " AS y ON y.commit_sha = x.commit_sha";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbScmFile> listEscalatedFiles(String company,
                                                        ScmFilesFilter scmFilesFilter,
                                                        JiraIssuesFilter jiraFilter,
                                                        ZendeskTicketsFilter ticketsFilter,
                                                        Integer pageNumber,
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

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());


        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String commitTableJoin = " INNER JOIN z_scm_links ON issues.key = z_scm_links.issue_key";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        List<DbScmFile> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String filesSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " zd_ticks AS ( SELECT issue_key,zendesk_tickets.ticket_id,zendesk_tickets.integration_id FROM "
                    + company + "." + ZENDESK_TICKETS
                    + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                    + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                    + zendeskConditions
                    + " ), z_scm_links AS ( SELECT issue_key,commit_sha,scm_integ_id FROM "
                    + company + "." + COMMIT_JIRA_TABLE
                    + " ), j_issues AS ( SELECT tbl.key,commit_sha, zd_ticks.ticket_id,zd_ticks.integration_id FROM ( SELECT issues.*,scm_integ_id,"
                    + "z_scm_links.commit_sha" + slaTimeColumns
                    + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + sprintTableJoin
                    + userTableJoin
                    + commitTableJoin
                    + slaTimeJoin
                    + jiraIssuesWhere
                    + " ) as tbl INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                    + jiraFinalWhere + "), " +
                    "files_agg AS ( SELECT files.*,commits.*, j_issues.key, j_issues.ticket_id,j_issues.integration_id as zendesk_integration_id FROM " +
                    " ( select * from " + company + "." + FILES_TABLE + filesWhere + " ) AS files " +
                    " INNER JOIN ( SELECT file_id,change ,addition, deletion, commit_sha FROM " +
                    company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON files.id = commits.file_id " +
                    "INNER JOIN j_issues ON commits.commit_sha = j_issues.commit_sha ) ";
            String sql = filesSql +
                    "SELECT * FROM (SELECT COUNT(DISTINCT(ticket_id)) AS num_tickets," +
                    "array_remove(array_agg(DISTINCT ticket_id), NULL)::text[] AS tickets,id" +
                    " FROM files_agg GROUP BY id) t" +
                    " INNER JOIN (SELECT SUM(change) AS changes,SUM(addition) AS additions," +
                    "SUM(deletion) AS deletions,COUNT(*) AS num_commits,repo_id,project,filename,id,integration_id,zendesk_integration_id" +
                    " FROM (SELECT DISTINCT id, commit_sha, change, addition, deletion, repo_id, project, filename, integration_id,zendesk_integration_id FROM files_agg ) x" +
                    " GROUP BY id,repo_id,project,filename,integration_id,zendesk_integration_id ) y ON t.id = y.id" +
                    " ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.filesZendeskRowMapper());
            String countSql = filesSql + " SELECT COUNT(DISTINCT(file_id)) FROM files_agg";
            log.info("countSql = {}", countSql);
            log.info("params = {}", params);
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> getZendeskEscalationFileReport(String company,
                                                                              ScmFilesFilter scmFilesFilter,
                                                                              JiraIssuesFilter jiraFilter,
                                                                              ZendeskTicketsFilter ticketsFilter,
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

        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());


        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        //zendesk stuff
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);
        //zd end

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String commitTableJoin = " INNER JOIN z_scm_links ON issues.key = z_scm_links.issue_key";

        String filesSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT issue_key,zendesk_tickets.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS
                + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions
                + " ), z_scm_links AS ( SELECT issue_key,commit_sha,scm_integ_id FROM "
                + company + "." + COMMIT_JIRA_TABLE
                + " ), j_issues AS ( SELECT tbl.key,commit_sha, zd_ticks.ticket_id FROM ( SELECT issues.*,scm_integ_id,"
                + "z_scm_links.commit_sha" + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + commitTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere + "), " +
                "files_agg AS ( SELECT files.*,commits.*, j_issues.key, j_issues.ticket_id FROM " +
                " ( SELECT * FROM " + company + "." + FILES_TABLE + filesWhere + " ) AS files " +
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
                "SELECT split_part(" + path + ", '/', 1) AS root_module, repo_id, project, COUNT(DISTINCT(ticket_id)) AS ct " +
                "FROM files_agg " + fileCondition + " GROUP BY repo_id, project, root_module ORDER BY " + orderBy;

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        final List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.escalatedZDTicketMapper("root_module"));

        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> getTopCommitters(String company,
                                                                JiraIssuesFilter jiraFilter,
                                                                ZendeskTicketsFilter ticketsFilter,
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
        List<String> zendeskTicketConditions = zendeskTicketService.createWhereClauseAndUpdateParams(company, params,
                ticketsFilter.getIngestedAt(), ticketsFilter.getBrands(), ticketsFilter.getTypes(),
                ticketsFilter.getPriorities(), ticketsFilter.getStatuses(), ticketsFilter.getOrganizations(),
                ticketsFilter.getRequesterEmails(), ticketsFilter.getSubmitterEmails(),
                ticketsFilter.getAssigneeEmails(), ticketsFilter.getIntegrationIds(), ticketsFilter.getAge(),
                ticketsFilter.getExtraCriteria(), ticketsFilter.getTicketCreatedStart(),
                ticketsFilter.getTicketCreatedEnd(), ticketsFilter.getCustomFields(), ticketsFilter.getExcludeCustomFields());
        Map<String, List<String>> commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params,
                scmCommitFilter.getCommitShas(), scmCommitFilter.getRepoIds(), scmCommitFilter.getVcsTypes(), scmCommitFilter.getProjects(),
                scmCommitFilter.getCommitters(), scmCommitFilter.getAuthors(), scmCommitFilter.getDaysOfWeek(), scmCommitFilter.getIntegrationIds(),
                scmCommitFilter.getCommitBranches(), scmCommitFilter.getExcludeCommitShas(), scmCommitFilter.getExcludeRepoIds(), scmCommitFilter.getExcludeProjects(),
                scmCommitFilter.getExcludeCommitters(), scmCommitFilter.getExcludeCommitBranches(), scmCommitFilter.getExcludeAuthors(),  scmCommitFilter.getExcludeDaysOfWeek(), scmCommitFilter.getCommittedAtRange(),
                scmCommitFilter.getLocRange(), scmCommitFilter.getExcludeLocRange(), scmCommitFilter.getPartialMatch(), scmCommitFilter.getExcludePartialMatch(), scmCommitFilter.getFileTypes(), scmCommitFilter.getExcludeFileTypes(),
                scmCommitFilter.getCodeChangeSize(), scmCommitFilter.getCodeChanges(), scmCommitFilter.getCodeChangeUnit(), scmCommitFilter.getTechnologies(), scmCommitFilter.getExcludeTechnologies(), null, "", false, ouConfig, scmCommitFilter.getIds(), scmCommitFilter.getIsApplyOuOnVelocityReport(), scmCommitFilter.getCreatedAtRange());
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
        String commitsWhere = "";
        if (commitConditions.get(COMMITS_TABLE).size() > 0)
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
        String filesWhere = "";
        if (fileConditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", fileConditions.get(FILES_TABLE));
        String zendeskConditions = "";
        if (zendeskTicketConditions.size() > 0)
            zendeskConditions = " WHERE " + String.join(" AND ", zendeskTicketConditions);

        // jira stuff

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));
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
        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";
        String sprintsWithClause = filterByLastSprint ? getSprintAuxTable(company, jiraFilter, jiraSprintsWhere) : "";
        String jiraIssue = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " zd_ticks AS ( SELECT issue_key,zendesk_tickets.ticket_id FROM "
                + company + "." + ZENDESK_TICKETS
                + " INNER JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " ON " + ZENDESK_TICKETS + ".id = " + ZENDESK_TICKET_JIRA_KEYS + ".ticket_id"
                + zendeskConditions
                + " ), j_issues AS ( SELECT tbl.key, zd_ticks.ticket_id FROM ( SELECT issues.* "
                + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + slaTimeJoin
                + jiraIssuesWhere
                + " ) as tbl INNER JOIN zd_ticks ON zd_ticks.issue_key = tbl.key"
                + jiraFinalWhere + ") ";
        String commitsSelect = ScmQueryUtils.COMMITS_SELECT;
        String authorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String committerTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String commitsSQL = " ( SELECT " + commitsSelect + " FROM " + company + "." + COMMITS_TABLE + authorTableJoin +
                committerTableJoin+  " ) a";
        String sql = jiraIssue + " SELECT files.filename, commits.author, count(DISTINCT(file_commits.commit_sha)) as no_of_commits"
                + " FROM ((SELECT * FROM " + company + "." + COMMIT_JIRA_TABLE + " as scjm "
                + " INNER JOIN j_issues ON j_issues.key = scjm.issue_key " + commitJiraWhere
                + ") AS commit_link INNER JOIN (SELECT * FROM "
                + commitsSQL + commitsWhere + ") AS commits ON commits.commit_sha = commit_link.commit_sha"
                + " INNER JOIN (SELECT * FROM " + company + "." + FILE_COMMITS_TABLE + ") AS file_commits ON"
                + " file_commits.commit_sha = commits.commit_sha INNER JOIN (SELECT * FROM " + company + "." + FILES_TABLE
                + filesWhere + ") AS files ON" + " files.id = file_commits.file_id) GROUP BY files.id,files.filename,commits.author"
                + " ORDER BY " + orderBy;

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.commitFileRowMapper());
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbScmPullRequest> listPRsForJiraSprint(String company,
                                                                 JiraIssuesFilter jiraFilter,
                                                                 ScmPrFilter filter,
                                                                 OUConfiguration ouConfig,
                                                                 Integer pageNumber,
                                                                 Integer pageSize) throws SQLException {

        Map<String, Object> params = Maps.newHashMap();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> conditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig);

        boolean needUserTableStuff = false;
        boolean needSlaTimeStuff = jiraFilter.getExtraCriteria() != null &&
                (jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || jiraFilter.getExtraCriteria().contains(JiraIssuesFilter.EXTRA_CRITERIA.missed_response_time));

        String jiraUsersWhere = "";
        if (jiraConditions.get(JIRA_USERS_TABLE).size() > 0) {
            jiraUsersWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_USERS_TABLE));
            needUserTableStuff = true;
        }

        String jiraIssuesWhere = "";
        if (jiraConditions.get(JIRA_ISSUES_TABLE).size() > 0)
            jiraIssuesWhere = " WHERE " + String.join(" AND ", jiraConditions.get(JIRA_ISSUES_TABLE));

        String sprintTableJoin = isSprintTblJoinRequired(jiraFilter) ? getSprintQuery(params, jiraFilter, company) : "";

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

        String prTableConditions = "";
        String prWorkItemTableCondition = "";

        if (conditions.get(PRS_TABLE).size() > 0)
            prTableConditions = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));

        if(conditions.get(PULLREQUESTS_WORKITEM_TABLE).size() > 0)
            prWorkItemTableCondition = " WHERE " + String.join(" AND ", conditions.get(PULLREQUESTS_WORKITEM_TABLE));

        String selctUniqueIdsFromPRTable = "SELECT CONCAT (number, '--' ,project) as uniqueId, * ";
        String selctUniqueIdsFromPRWIMappings = " INNER JOIN ( SELECT CONCAT (pr_id, '--' ,project) as uniqueId, * FROM ";
        String finalWhere = " WHERE pr.uniqueId = prissue.uniqueId ";

        String sql = "SELECT DISTINCT ( pr.* ) FROM (  " + selctUniqueIdsFromPRTable + " FROM "
                + company + "." + PRS_TABLE
                + prTableConditions + ") pr , "
                + " ( SELECT spwm.uniqueId, spwm.scm_integration_id, issues.* "
                + slaTimeColumns
                +" FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin
                + userTableJoin
                + slaTimeJoin
                + selctUniqueIdsFromPRWIMappings
                + company + "." + PULLREQUESTS_WORKITEM_TABLE
                + prWorkItemTableCondition
                + ") as spwm on spwm.workitem_id = issues.key "
                + jiraIssuesWhere
                + " ) as prissue "
                + finalWhere;

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmPullRequest> results = List.of();
        results = template.query(sql, params, DbScmConverters.prRowMapper());

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
                (jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() > 0) ||
                CollectionUtils.size(jiraFilter.getSprintStates()) > 0 || jiraFilter.getAcross() == JiraIssuesFilter.DISTINCT.sprint) {
            sprintConditions = createSprintWhereClause(params, jiraFilter.getSprintIds(),
                    jiraFilter.getSprintNames(), jiraFilter.getSprintFullNames(), jiraFilter.getSprintStates(), jiraFilter.getSprintCount());
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
        return sprintTableJoin;
    }

    private String getSprintAuxTable(String company, JiraIssuesFilter filter, String sprintWhere) {
        String sprintLimitStatement = "";
        if(filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }

        return "WITH spr_dates AS (select COALESCE(start_date, 99999999999) as start_date, sprint_id, name " +
                "from " + company + "." + JIRA_ISSUE_SPRINTS + sprintWhere + sprintLimitStatement + " )";
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
        if(sortableColumns.get(sortByKey) == Types.TIMESTAMP) {
            defaultSortOrder = SortingOrder.ASC;
        }
        String sortOrder = getSortOrder(sortBy, sortByKey, defaultSortOrder);

        if(sortableColumns.get(sortByKey) == Types.VARCHAR) {
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

