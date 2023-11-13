package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemTimelineQueryCriteria;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmAggService.*;

@Log4j2
@Service
@SuppressWarnings("unused")
public class ScmIssueMgmtService extends DatabaseService<DBDummyObj> {

    private static final String WORKITEMS_TABLE = "issue_mgmt_workitems";
    private static final String WORKITEMS_PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";
    private static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    private static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";

    private static final String COMMIT_WORKITEMS_TABLE = "scm_commit_workitem_mappings";
    private static final String COMMIT_JIRA_TABLE = "scm_commit_jira_mappings";
    private static final String FILES_TABLE = "scm_files";
    private static final String FILE_COMMITS_TABLE = "scm_file_commits";

    private static final String PULLREQUESTS_JIRA_TABLE = "scm_pullrequests_jira_mappings";
    private static final String PULLREQUESTS_WORKITEM_TABLE = "scm_pullrequests_workitem_mappings";

    private static final String JIRA_USERS_TABLE = "jira_users";
    private static final String JIRA_ISSUES_TABLE = "jira_issues";
    private static final String JIRA_ISSUE_SPRINTS = "jira_issue_sprints";
    private static final String JIRA_PRIORITIES_SLA_TABLE = "jira_issue_priorities_sla";

    private static final Map<String, Integer> FILE_SORTABLE_COLUMNS = Map.of("num_issues", Types.NUMERIC,
            "repo_id", Types.VARCHAR, "project", Types.VARCHAR, "filename", Types.VARCHAR);

    private final ScmAggService scmAggService;
    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final NamedParameterJdbcTemplate template;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;

    public ScmIssueMgmtService(DataSource dataSource, ScmAggService scmAggService, JiraConditionsBuilder jiraConditionsBuilder, WorkItemFieldsMetaService workItemFieldsMetaService) {
        super(dataSource);
        this.scmAggService = scmAggService;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.workItemFieldsMetaService = workItemFieldsMetaService;
    }

    public DbListResponse<DbScmFile> listIssueMgmtScmFiles(String company, ScmFilesFilter scmFilesFilter,
                                                           WorkItemsFilter workItemsFilter,
                                                           WorkItemsMilestoneFilter milestoneFilter,
                                                           Map<String, SortingOrder> sortBy,
                                                           final OUConfiguration ouConfig,
                                                           Integer pageNumber,
                                                           Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
            try {
                workItemCustomFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(), true,
                        null, null, null, null, null, 0,
                        1000).getRecords();
            } catch (SQLException e) {
                log.error("Error while querying workitem field meta table. Reason: " + e.getMessage());
            }
        }
        Query workItemSelectionCriteria =
                WorkItemQueryCriteria.getSelectionCriteria(company, workItemsFilter, workItemCustomFields, null, null, ouConfig);
        String workItemsWhere = "";
        if (CollectionUtils.isNotEmpty(workItemSelectionCriteria.getCriteria().getConditions())) {
            workItemsWhere = " WHERE " + String.join(" AND ", workItemSelectionCriteria.getCriteria().getConditions());
        }
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);
        params.putAll(workItemSelectionCriteria.getCriteria().getQueryParams());
        String orderBy = getOrderByStr(sortBy, FILE_SORTABLE_COLUMNS, "num_issues");

        String filesWhere = "";
        if (scmFileConditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        String prWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PRS_TABLE))) {
            prWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PRS_TABLE));
        }
        String prWorkitemMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PULLREQUESTS_WORKITEM_TABLE))) {
            prWorkitemMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PULLREQUESTS_WORKITEM_TABLE));
        }

        boolean needSlaTimeStuff = workItemsFilter.getExtraCriteria() != null &&
                (workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));

        String slaTimeColumns = "";
        String slaTimeJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,workitem_type as wtype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + WORKITEMS_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id"
                    + " AND p.wtype = workitems.workitem_type";
        }
        String sprintTableJoin = milestoneFilter.isSpecified() ? getWorkItemSprintQuery(milestoneFilter, company) : "";
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbScmFile> results = List.of();
        if (pageSize > 0) {
            String sql = " WITH j_workitems AS ( \n" +
                    "SELECT * FROM ( \n" +
                    "SELECT workitems.*" + slaTimeColumns + " FROM " + company + "." + WORKITEMS_TABLE + " as workitems"
                    + sprintTableJoin + slaTimeJoin + workItemsWhere
                    + " ) as tbl \n"
                    + " ), scm_files_commits_cte AS ( \n" +
                    "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                    "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                    + " ) AS files \n"
                    + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                    + " INNER JOIN ( SELECT workitem_id as scm_commit_workitem_mappings_workitem_id,commit_sha FROM " + company + "." + COMMIT_WORKITEMS_TABLE + " ) AS mapping ON mapping.commit_sha = commits.commit_sha \n"
                    + " INNER JOIN j_workitems ON mapping.scm_commit_workitem_mappings_workitem_id = j_workitems.workitem_id \n"
                    + " ), scm_files_pr_cte AS ( \n" +
                    "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                    "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                    + " ) AS files \n"
                    + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                    + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests " + prWhere + " ) as pr ON files.integration_id=pr.integration_id AND (commits.commit_sha=pr.merge_sha OR commits.commit_sha = ANY(pr.commit_shas)) \n"
                    + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests_workitem_mappings " + prWorkitemMappingsWhere + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                    + " INNER JOIN j_workitems ON prjm.workitem_id=j_workitems.workitem_id \n"
                    + " ), scm_files_cte AS ( \n"
                    + "select * from scm_files_commits_cte \n"
                    + "UNION \n"
                    + "select * from scm_files_pr_cte \n"
                    + " ) \n"
                    + " SELECT COUNT(DISTINCT(workitem_id)) as num_issues,array_remove(array_agg(DISTINCT workitem_id), NULL)::text[] as keys,repo_id,project,filename,id,integration_id \n"
                    + " FROM scm_files_cte GROUP BY id,repo_id,project,filename,integration_id ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.filesJiraRowMapper());
        }
        String countSql = " WITH j_workitems AS ( \n" +
                "SELECT * FROM ( \n" +
                "SELECT workitems.*" + slaTimeColumns + " FROM " + company + "." + WORKITEMS_TABLE + " as workitems"
                + sprintTableJoin + slaTimeJoin + workItemsWhere
                + " ) as tbl \n"
                + " ), scm_files_commits_cte AS ( \n" +
                "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " INNER JOIN ( SELECT workitem_id as scm_commit_workitem_mappings_workitem_id,commit_sha FROM " + company + "." + COMMIT_WORKITEMS_TABLE + " ) AS mapping ON mapping.commit_sha = commits.commit_sha \n"
                + " INNER JOIN j_workitems ON mapping.scm_commit_workitem_mappings_workitem_id = j_workitems.workitem_id \n"
                + " ), scm_files_pr_cte AS ( \n" +
                "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests " + prWhere + " ) as pr ON files.integration_id=pr.integration_id AND (commits.commit_sha=pr.merge_sha OR commits.commit_sha = ANY(pr.commit_shas)) \n"
                + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests_workitem_mappings " + prWorkitemMappingsWhere + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                + " INNER JOIN j_workitems ON prjm.workitem_id=j_workitems.workitem_id \n"
                + " ), scm_files_cte AS ( \n"
                + "select * from scm_files_commits_cte \n"
                + "UNION \n"
                + "select * from scm_files_pr_cte \n"
                + " ) \n"
                + " SELECT COUNT(DISTINCT(file_id)) FROM scm_files_cte";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> listIssueMgmtScmModules(String company,
                                                                       ScmFilesFilter scmFilesFilter,
                                                                       WorkItemsFilter workItemsFilter,
                                                                       WorkItemsMilestoneFilter milestoneFilter,
                                                                       Map<String, SortingOrder> sortBy,
                                                                       final OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        List<DbWorkItemField> workItemCustomFields = null;
        if(MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
            try {
                workItemCustomFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(), true,
                        null, null, null, null, null, 0,
                        1000).getRecords();
            } catch (SQLException e) {
                log.error("Error while querying workitem field meta table. Reason: " + e.getMessage());
            }
        }
        Query workItemSelectionCriteria =
                WorkItemQueryCriteria.getSelectionCriteria(company, workItemsFilter, workItemCustomFields, null, null, ouConfig);
        String workItemsWhere = "";
        if (CollectionUtils.isNotEmpty(workItemSelectionCriteria.getCriteria().getConditions())) {
            workItemsWhere = " WHERE " + String.join(" AND ", workItemSelectionCriteria.getCriteria().getConditions());
        }
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);

        String orderBy = getOrderByStr(sortBy, Map.of("num_issues", Types.NUMERIC,
                "repo_id", Types.VARCHAR, "project", Types.VARCHAR), "num_issues");

        String filesWhere = "";
        if (scmFileConditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        String scmCommitWorkItemMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.getOrDefault(COMMIT_WORKITEMS_TABLE, null))) {
            scmCommitWorkItemMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(COMMIT_WORKITEMS_TABLE));
        }

        String prWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PRS_TABLE))) {
            prWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PRS_TABLE));
        }
        String prWorkitemMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PULLREQUESTS_WORKITEM_TABLE))) {
            prWorkitemMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PULLREQUESTS_WORKITEM_TABLE));
        }

        boolean needSlaTimeStuff = workItemsFilter.getExtraCriteria() != null &&
                (workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || workItemsFilter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));

        params.putAll(workItemSelectionCriteria.getCriteria().getQueryParams());
        String slaTimeColumns = "";
        String slaTimeJoin = "";
        if (needSlaTimeStuff) {
            slaTimeColumns = ",(COALESCE(first_comment_at," + currentTime + ")-issue_created_at) AS resp_time"
                    + ",(COALESCE(issue_resolved_at," + currentTime + ")-issue_created_at) AS solve_time,p.*";
            slaTimeJoin = " LEFT OUTER JOIN ("
                    + " SELECT solve_sla,resp_sla,project as proj,task_type as ttype,priority as prio,"
                    + "integration_id as integid FROM " + company + "." + WORKITEMS_PRIORITIES_SLA_TABLE + " )"
                    + " AS p ON p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id"
                    + " AND p.ttype = workitems.workitem_type";
        }
        String sprintTableJoin = milestoneFilter.isSpecified() ? getWorkItemSprintQuery(milestoneFilter, company) : "";

        String path =
                StringUtils.isEmpty(scmFilesFilter.getModule())
                        ? "filename" : "substring(filename from " + (scmFilesFilter.getModule().length() + 2) + ")";
        String fileCondition = "";
        if (!scmFilesFilter.getListFiles()) {
            fileCondition = " where position('/' IN " + path + " ) > 0 ";
        }

        String sql = " WITH j_workitems AS ( \n" +
                "SELECT * FROM ( \n" +
                "SELECT workitems.*" + slaTimeColumns + " FROM " + company + "." + WORKITEMS_TABLE + " as workitems"
                + sprintTableJoin + slaTimeJoin + workItemsWhere
                + " ) as tbl \n"
                + " ), scm_files_commits_cte AS ( \n" +
                "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " INNER JOIN ( SELECT workitem_id as scm_commit_workitem_mappings_workitem_id,commit_sha FROM " + company + "." + COMMIT_WORKITEMS_TABLE + " ) AS mapping ON mapping.commit_sha = commits.commit_sha \n"
                + " INNER JOIN j_workitems ON mapping.scm_commit_workitem_mappings_workitem_id = j_workitems.workitem_id \n"
                + " ), scm_files_pr_cte AS ( \n" +
                "SELECT files.*,commits.file_id,commits.commit_sha,j_workitems.workitem_id FROM ( \n" +
                "SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests " + prWhere + " ) as pr ON files.integration_id=pr.integration_id AND (commits.commit_sha=pr.merge_sha OR commits.commit_sha = ANY(pr.commit_shas)) \n"
                + " LEFT JOIN ( SELECT * FROM " + company + ".scm_pullrequests_workitem_mappings " + prWorkitemMappingsWhere + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                + " INNER JOIN j_workitems ON prjm.workitem_id=j_workitems.workitem_id \n"
                + " ), scm_files_cte AS ( \n"
                + "select * from scm_files_commits_cte \n"
                + "UNION \n"
                + "select * from scm_files_pr_cte \n"
                + " ) \n"
                + " SELECT COUNT(DISTINCT(workitem_id)) as num_issues, split_part(" + path + ", '/', 1) as root_module, repo_id, project"
                + " FROM scm_files_cte " + fileCondition + " GROUP BY repo_id,project,root_module ORDER BY " + orderBy + " , root_module asc";

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.modulesJiraRowMapper("root_module"));
        return DbListResponse.of(results, results.size());
    }

    private String getWorkItemSprintQuery(WorkItemsMilestoneFilter milestoneFilter, String company) {
        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(WorkItemTimelineQueryCriteria
                .getSelectionCriteria(WorkItemsTimelineFilter.builder().build(), null).getSelectFields());
        timelineSelectFields.addAll(WorkItemQueryCriteria.getFieldNames(milestoneSelectFields));
        Query fetchMilestones = Query.builder().select(milestoneSelectFields)
                .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND)
                .build();

        Query fetchTimelines = Query.builder().select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "timelines"))
                .build();
        String intermediateSQL = fetchTimelines.toSql() + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                + "milestones.milestone_integration_id = timelines.integration_id ";
        intermediateSQL = " INNER JOIN ( " + intermediateSQL + " ) as tmp ON tmp.timeline_workitem_id = wi.workitem_id AND"
                + " tmp.timeline_integration_id = wi.integration_id ";
        return intermediateSQL;
    }

    public DbListResponse<DbScmFile> listScmFiles(String company,
                                                  ScmFilesFilter scmFilesFilter,
                                                  JiraIssuesFilter jiraFilter,
                                                  Map<String, SortingOrder> sortBy,
                                                  OUConfiguration ouConfig,
                                                  Integer pageNumber,
                                                  Integer pageSize) {
        boolean filterByLastSprint = false;
        if (jiraFilter.getFilterByLastSprint() != null) {
            filterByLastSprint = jiraFilter.getFilterByLastSprint();
        }

        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, List<String>> jiraConditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params,
                jiraFilter, currentTime, jiraFilter.getIngestedAt(), ouConfig);
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);

        String orderBy = getOrderByStr(sortBy, FILE_SORTABLE_COLUMNS, "num_issues");

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        String prWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PRS_TABLE))) {
            prWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PRS_TABLE));
        }
        String prJiraMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PULLREQUESTS_JIRA_TABLE))) {
            prJiraMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PULLREQUESTS_JIRA_TABLE));
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
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        List<DbScmFile> results = List.of();
        if (pageSize > 0) {
            String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                    + " j_issues AS ( \n"
                    + " SELECT * FROM ( \n"
                    + " SELECT issues.*" + slaTimeColumns
                    + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                    + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                    + " ) as tbl"
                    + jiraFinalWhere + " \n"
                    + " ), scm_files_commits_cte AS ( \n"
                    + " SELECT files.*,commits.file_id,commits.commit_sha,j_issues.key FROM ( \n"
                    + " SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                    + " ) AS files \n"
                    + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                    + " INNER JOIN ( SELECT issue_key,commit_sha FROM " + company + "." + COMMIT_JIRA_TABLE + " ) AS mapping ON mapping.commit_sha = commits.commit_sha \n"
                    + " INNER JOIN j_issues ON mapping.issue_key = j_issues.key \n"
                    + " ), scm_files_pr_cte AS ( \n"
                    + " SELECT files.*,commits.file_id,commits.commit_sha,j_issues.key FROM ( \n"
                    + " SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                    + " ) AS files \n"
                    + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                    + " INNER JOIN ( SELECT * FROM " + company + ".scm_pullrequests " + prWhere + " ) as pr ON files.integration_id=pr.integration_id AND (commits.commit_sha=pr.merge_sha OR commits.commit_sha = ANY(pr.commit_shas)) \n"
                    + " INNER JOIN ( SELECT * FROM " + company + ".scm_pullrequests_jira_mappings " + prJiraMappingsWhere + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                    + " INNER JOIN j_issues ON prjm.issue_key = j_issues.key \n"
                    + " ), scm_files_cte AS ( \n"
                    + "select * from scm_files_commits_cte \n"
                    + "UNION \n"
                    + "select * from scm_files_pr_cte \n"
                    + " ) \n"
                    + " SELECT COUNT(DISTINCT(key)) as num_issues,array_remove(array_agg(DISTINCT key), NULL)::text[] as keys,"
                    + "repo_id,project,filename,id,integration_id FROM scm_files_cte GROUP BY id,repo_id,project,filename,integration_id"
                    + " ORDER BY " + orderBy + " OFFSET :skip LIMIT :limit";

            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.filesJiraRowMapper());
        }
        String countSql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues AS ( \n"
                + " SELECT * FROM ( \n"
                + " SELECT issues.*" + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl"
                + jiraFinalWhere + " \n"
                + " ), scm_files_commits_cte AS ( \n"
                + " SELECT files.*,commits.file_id,commits.commit_sha,j_issues.key FROM ( \n"
                + " SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " INNER JOIN ( SELECT issue_key,commit_sha FROM " + company + "." + COMMIT_JIRA_TABLE + " ) AS mapping ON mapping.commit_sha = commits.commit_sha \n"
                + " INNER JOIN j_issues ON mapping.issue_key = j_issues.key \n"
                + " ), scm_files_pr_cte AS ( \n"
                + " SELECT files.*,commits.file_id,commits.commit_sha,j_issues.key FROM ( \n"
                + " SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere
                + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id,committed_at,commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " INNER JOIN ( SELECT * FROM " + company + ".scm_pullrequests " + prWhere + " ) as pr ON files.integration_id=pr.integration_id AND (commits.commit_sha=pr.merge_sha OR commits.commit_sha = ANY(pr.commit_shas)) \n"
                + " INNER JOIN ( SELECT * FROM " + company + ".scm_pullrequests_jira_mappings " + prJiraMappingsWhere + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                + " INNER JOIN j_issues ON prjm.issue_key = j_issues.key \n"
                + " ), scm_files_cte AS ( \n"
                + "select * from scm_files_commits_cte \n"
                + "UNION \n"
                + "select * from scm_files_pr_cte \n"
                + " ) \n"
                + " SELECT COUNT(DISTINCT(file_id)) FROM scm_files_cte";

        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> listScmModules(String company,
                                                              ScmFilesFilter scmFilesFilter,
                                                              JiraIssuesFilter jiraFilter,
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
        Map<String, List<String>> scmFileConditions = scmAggService.createFilesWhereClauseAndUpdateParams(params,
                scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), null);

        String orderBy = getOrderByStr(sortBy, Map.of("num_issues", Types.NUMERIC,
                "repo_id", Types.VARCHAR, "project", Types.VARCHAR), "num_issues");

        String filesWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILES_TABLE));
        String fileCommitsWhere = "";
        if (scmFileConditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(FILE_COMMITS_TABLE));

        String scmCommitJiraMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.getOrDefault(COMMIT_JIRA_TABLE, null))) {
            scmCommitJiraMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(COMMIT_JIRA_TABLE));
        }

        String prWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PRS_TABLE))) {
            prWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PRS_TABLE));
        }
        String prJiraMappingsWhere = "";
        if (CollectionUtils.isNotEmpty(scmFileConditions.get(PULLREQUESTS_JIRA_TABLE))) {
            prJiraMappingsWhere = " WHERE " + String.join(" AND ", scmFileConditions.get(PULLREQUESTS_JIRA_TABLE));
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
        String path =
                StringUtils.isEmpty(scmFilesFilter.getModule())
                        ? "filename" : "substring(filename from " + (scmFilesFilter.getModule().length() + 2) + ")";
        String fileCondition = "";
        if (!scmFilesFilter.getListFiles()) {
            fileCondition = " where position('/' IN " + path + " ) > 0 ";
        }

        String sql = (StringUtils.isNotEmpty(sprintsWithClause) ? sprintsWithClause + "," : " WITH ")
                + " j_issues AS ( \n"
                + " SELECT * FROM ( \n"
                + " SELECT issues.KEY" + slaTimeColumns
                + " FROM " + company + "." + JIRA_ISSUES_TABLE + " as issues"
                + sprintTableJoin + userTableJoin + slaTimeJoin + jiraIssuesWhere
                + " ) as tbl"
                + jiraFinalWhere + " \n"
                + " ), j_commits AS ( \n"
                + " SELECT files.*, commits.file_id, commits.commit_sha FROM ( \n"
                + " SELECT id,integration_id,filename,repo_id,project FROM " + company + "." + FILES_TABLE + filesWhere + " ) AS files \n"
                + " INNER JOIN ( SELECT file_id, commit_sha FROM " + company + "." + FILE_COMMITS_TABLE + fileCommitsWhere + " ) AS commits ON commits.file_id = files.id \n"
                + " ), scm_files_commits_cte AS ( \n"
                + " SELECT j_commits.filename,j_commits.repo_id,j_commits.project,j_issues.key FROM ( \n"
                + " SELECT issue_key,commit_sha FROM " + company + "." + COMMIT_JIRA_TABLE + scmCommitJiraMappingsWhere + " ) AS mapping \n"
                + " INNER JOIN j_commits ON mapping.commit_sha = j_commits.commit_sha \n"
                + " INNER JOIN j_issues ON mapping.issue_key = j_issues.key \n"
                + " ), scm_files_pr_cte AS ( \n"
                + " SELECT j_commits.filename,j_commits.repo_id,j_commits.project,j_issues.key FROM ( \n"
                + " SELECT id, integration_id, merge_sha, commit_shas, project, number FROM "+ company + ".scm_pullrequests " + prWhere + " ) as pr \n"
                + " INNER JOIN j_commits ON j_commits.integration_id = pr.integration_id AND ( j_commits.commit_sha = pr.merge_sha OR j_commits.commit_sha = ANY ( pr.commit_shas ) ) \n"
                + " INNER JOIN ( SELECT pr_uuid, issue_key FROM " + company + ".scm_pullrequests_jira_mappings " + prJiraMappingsWhere
                + " ) AS prjm ON pr.id=prjm.pr_uuid\n"
                + " INNER JOIN j_issues ON prjm.issue_key = j_issues.key \n"
                + " ), scm_files_cte AS ( \n"
                + "select * from scm_files_commits_cte \n"
                + "UNION \n"
                + "select * from scm_files_pr_cte \n"
                + " ) \n"
                + " SELECT COUNT(DISTINCT(key)) as num_issues, split_part(" + path + ", '/', 1) as root_module, repo_id, project"
                + " FROM scm_files_cte " + fileCondition + " GROUP BY repo_id,project,root_module "
                + " ORDER BY " + orderBy + " , root_module asc";

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.modulesJiraRowMapper("root_module"));
        return DbListResponse.of(results, results.size());
    }

    private String getSprintAuxTable(String company, JiraIssuesFilter filter, String sprintWhere) {
        String sprintLimitStatement = "";
        if (filter.getSprintCount() != null && filter.getSprintCount() > 0) {
            sprintLimitStatement = " order by end_date desc limit " + filter.getSprintCount();
        }

        return "WITH spr_dates AS (select COALESCE(start_date, 99999999999) as start_date, sprint_id, name " +
                "from " + company + "." + JIRA_ISSUE_SPRINTS + sprintWhere + sprintLimitStatement + " )";
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
        if (jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() > 0) {
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

    private String getOrderByStr(Map<String, SortingOrder> sortBy, Map<String, Integer> sortableColumns,
                                 String defaultSortKey) {
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (sortableColumns.containsKey(entry.getKey())) {
                        return entry.getKey();
                    }
                    return defaultSortKey;
                })
                .orElse(defaultSortKey);

        SortingOrder defaultSortOrder = SortingOrder.DESC;
        if (sortableColumns.get(sortByKey) == Types.TIMESTAMP) {
            defaultSortOrder = SortingOrder.ASC;
        }
        String sortOrder = getSortOrder(sortBy, sortByKey, defaultSortOrder);

        if (sortableColumns.get(sortByKey) == Types.VARCHAR) {
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

    private boolean isSprintTblJoinRequired(JiraIssuesFilter jiraFilter) {
        return CollectionUtils.size(jiraFilter.getSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getSprintNames()) > 0 ||
                CollectionUtils.size(jiraFilter.getSprintStates()) > 0 || jiraFilter.getAcross() == JiraIssuesFilter.DISTINCT.sprint ||
                CollectionUtils.size(jiraFilter.getExcludeSprintIds()) > 0 || CollectionUtils.size(jiraFilter.getExcludeSprintNames()) > 0 ||
                (jiraFilter.getSprintCount() != null && jiraFilter.getSprintCount() > 0) ||
                CollectionUtils.size(jiraFilter.getExcludeSprintStates()) > 0;
    }

    public List<DbScmCommit> getScmCommitsForJira(String company, String key){

        String sql = "select * from "+company+"."+COMMITS_TABLE+" where commit_sha in ( "+
                " select commit_sha from "+company+"."+COMMIT_JIRA_TABLE+" where issue_key in (:issue_key) ) order by committed_at";
        Map<String, Object> params = new HashMap<>();
        params.put("issue_key", key);

        return template.query(sql, params, DbScmConverters.commitRowMapper());
    }

    public List<DbScmPullRequest> getScmPRsForJira(String company, String key){

        String sql = "select * from "+company+"."+PRS_TABLE+" where id in ( "+
                " select pr_uuid from "+company+"."+PULLREQUESTS_JIRA_TABLE+" where issue_key in (:issue_key) ) order by pr_created_at";
        Map<String, Object> params = new HashMap<>();
        params.put("issue_key", key);

        return template.query(sql, params, DbScmConverters.prRowMapper());
    }


    public List<DbScmCommit> getScmCommitsForWorkItem(String company, String key){

        String sql = "select * from "+company+"."+COMMITS_TABLE+" where commit_sha in ( "+
                " select commit_sha from "+company+"."+COMMIT_WORKITEMS_TABLE+" where workitem_id in (:issue_key) ) order by committed_at";
        Map<String, Object> params = new HashMap<>();
        params.put("issue_key", key);

        return template.query(sql, params, DbScmConverters.commitRowMapper());
    }

    public List<DbScmPullRequest> getScmPRsForWorkItem(String company, String key){

        String sql = "select * from "+company+"."+PRS_TABLE+" where id in ( "+
                " select pr_uuid from "+company+"."+PULLREQUESTS_WORKITEM_TABLE+" where workitem_id in (:issue_key) ) order by pr_created_at";
        Map<String, Object> params = new HashMap<>();
        params.put("issue_key", key);

        return template.query(sql, params, DbScmConverters.prRowMapper());
    }
    @Override
    public String insert(String company, DBDummyObj t) throws SQLException {
        return null;
    }

    @Override
    public Boolean update(String company, DBDummyObj t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DBDummyObj> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DBDummyObj> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return null;
    }
}
