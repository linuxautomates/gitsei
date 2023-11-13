package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraLink;
import io.levelops.commons.databases.models.database.jira.DbJiraPrioritySla;
import io.levelops.commons.databases.models.database.jira.DbJiraSalesforceCase;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraSprintDistMetric;
import io.levelops.commons.databases.services.jira.JiraIssueAggService;
import io.levelops.commons.databases.services.jira.JiraIssuePrioritySlaService;
import io.levelops.commons.databases.services.jira.JiraIssueReadService;
import io.levelops.commons.databases.services.jira.JiraIssueSprintService;
import io.levelops.commons.databases.services.jira.JiraIssueStatusService;
import io.levelops.commons.databases.services.jira.JiraIssueUserService;
import io.levelops.commons.databases.services.jira.JiraIssueVersionService;
import io.levelops.commons.databases.services.jira.JiraIssueWriteService;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Log4j2
@Service
public class JiraIssueService extends DatabaseService<DbJiraIssue> {

    public static final Set<String> FIELD_SIZE_COLUMNS = Set.of("key", "project", "summary", "priority", "assignee",
            "epic", "reporter", "status", "issue_type", "first_assignee");
    public static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("key", "project", "summary", "assignee", "epic",
            "reporter", "issue_type", "first_assignee", "status", "priority", "sprint_name", "sprint_full_name", "sprint_full_names", "status_category");
    public static final Set<String> PARTIAL_MATCH_ARRAY_COLUMNS = Set.of("components", "labels", "versions", "fix_versions");
    public static final String FINAL_TABLE = "final_table";
    public static final String USERS_TABLE = "jira_users";
    public static final String ISSUES_TABLE = "jira_issues";
    public static final String PRIORITIES_TABLE = "jira_priorities";
    public static final String STATUSES_TABLE = "jira_issue_statuses";
    public static final String ASSIGNEES_TABLE = "jira_issue_assignees";
    public static final String PRIORITIES_SLA_TABLE = "jira_issue_priorities_sla";
    public static final String JIRA_ISSUE_SALESFORCE_CASES = "jira_issue_salesforce_cases";
    public static final String STATE_TRANSITION_TIME = "state_transition_time";
    public static final String PARENT_STORY_POINTS = "parent_story_points";
    public static final String STORY_POINTS_TABLE = "jira_issue_story_points";
    public static final String PRIORITY_ORDER = "priority";
    public static final String JIRA_ISSUE_SPRINTS = "jira_issue_sprints";
    public static final String JIRA_ISSUE_VERSIONS = "jira_issue_versions";
    public static final String JIRA_ISSUE_LINKS = "jira_issue_links";
    public static final String JIRA_PROJECTS = "jiraprojects";
    public static final String JIRA_STATUS_METADATA = "jira_status_metadata";

    public static final Set<String> SORTABLE_COLUMNS = Set.of("bounces", "hops", "resp_time",
            "solve_time", "issue_created_at", "issue_due_at", "issue_due_relative_at", "desc_size", "num_attachments",
            "first_attachment_at", "issue_resolved_at", "issue_updated_at", "first_comment_at",
            "version", "fix_version", "ingested_at", "story_points",
            PRIORITY_ORDER, PARENT_STORY_POINTS, STATE_TRANSITION_TIME);
    public static final List<JiraIssuesFilter.DISTINCT> TIMESTAMP_SORTABLE_COLUMNS = List.of(JiraIssuesFilter.DISTINCT.trend,
            JiraIssuesFilter.DISTINCT.issue_created, JiraIssuesFilter.DISTINCT.issue_updated, JiraIssuesFilter.DISTINCT.issue_due,
            JiraIssuesFilter.DISTINCT.issue_due_relative, JiraIssuesFilter.DISTINCT.issue_resolved, JiraIssuesFilter.DISTINCT.issue_updated);
    public static final List<JiraIssuesFilter.DISTINCT> USER_BASED_COLUMNS = List.of(JiraIssuesFilter.DISTINCT.assignee,
            JiraIssuesFilter.DISTINCT.reporter, JiraIssuesFilter.DISTINCT.first_assignee);
    public static final String INNER_JOIN = "INNER JOIN";
    public static final String LEFT_OUTER_JOIN = "LEFT OUTER JOIN";


    private final NamedParameterJdbcTemplate template;
    private final JiraIssueWriteService writeService;
    private final JiraIssueReadService readService;
    private final JiraIssueAggService aggService;
    private final JiraIssueUserService userService;
    private final JiraIssueSprintService sprintService;
    private final JiraIssueVersionService versionService;
    private final JiraIssuePrioritySlaService prioritySlaService;
    private final JiraIssueStatusService statusService;

    @Autowired
    public JiraIssueService(DataSource dataSource,
                            JiraIssueWriteService writeService,
                            JiraIssueReadService readService,
                            JiraIssueAggService aggService,
                            JiraIssueUserService userService,
                            JiraIssueSprintService sprintService,
                            JiraIssueVersionService versionService,
                            JiraIssuePrioritySlaService prioritySlaService,
                            JiraIssueStatusService statusService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.writeService = writeService;
        this.readService = readService;
        this.aggService = aggService;
        this.userService = userService;
        this.sprintService = sprintService;
        this.versionService = versionService;
        this.prioritySlaService = prioritySlaService;
        this.statusService = statusService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, JiraFieldService.class);
    }

    //region insert
    @Override
    public String insert(String company, DbJiraIssue issue) throws SQLException {
        return writeService.insert(company, issue);
    }

    public void bulkUpdateEpicStoryPoints(String company, String integrationId, Long ingestedDate)
            throws SQLException {
        writeService.bulkUpdateEpicStoryPoints(company, integrationId, ingestedDate);
    }

    public boolean bulkUpdateEpicStoryPointsSinglePage(String company, String integrationId, Long ingestedDate, int readPageSize, int readOffset, int writePageSize) throws SQLException {
        return writeService.bulkUpdateEpicStoryPointsSinglePage(company, integrationId, ingestedDate, readPageSize, readOffset, writePageSize);
    }

    public Boolean insertJiraLinkedIssueRelation(String company, String integrationId, String fromIssueKey, String toIssueKey, String relation) {
        return writeService.insertJiraLinkedIssueRelation(company, integrationId, fromIssueKey, toIssueKey, relation);
    }

    public Boolean insertJiraUser(String company, DbJiraUser user) {
        return userService.insertJiraUser(company, user);
    }
    //endregion

    // region sprints

    /**
     * Inserts new sprints into jira_issue_sprint table on conflict doest nothing
     *
     * @param company tenant id
     * @param sprint  {@link DbJiraSprint} object
     * @return return true if insert successfully else false
     */
    public Optional<String> insertJiraSprint(String company, DbJiraSprint sprint) {
        return sprintService.insertJiraSprint(company, sprint);
    }

    public Stream<DbJiraSprint> streamSprints(String company, JiraSprintFilter filter) {
        return sprintService.streamSprints(company, filter);
    }

    public DbListResponse<DbJiraSprint> filterSprints(String company, Integer pageNumber, Integer pageSize, JiraSprintFilter filter) {
        return sprintService.filterSprints(company, pageNumber, pageSize, filter);
    }

    public Optional<DbJiraSprint> getSprint(String company, int integrationId, int sprintId) {
        return sprintService.getSprint(company, integrationId, sprintId);
    }
    //endregion

    //region version

    public boolean insertJiraVersion(String company, DbJiraVersion version) {
        return versionService.insertJiraVersion(company, version);
    }

    public Optional<DbJiraVersion> getJiraVersion(String company, Integer integrationId, Integer versionId) {
        return versionService.getJiraVersion(company, integrationId, versionId);
    }

    public List<DbJiraVersion> getVersionsForIssues(String company, List<String> versionNames, List<Integer> integrationIds,
                                                    List<String> projectKeys) {
        return versionService.getVersionsForIssues(company, versionNames, integrationIds, projectKeys);
    }
    //endregion

    // region priority sla
    public Boolean updatePrioritySla(String company, DbJiraPrioritySla prioritySla) {
        return prioritySlaService.updatePrioritySla(company, prioritySla);
    }

    public Integer bulkUpdatePrioritySla(String company,
                                         List<String> ids,
                                         List<String> integrationIds,
                                         List<String> projects,
                                         List<String> issueTypes,
                                         List<String> priorities,
                                         Long respSla,
                                         Long solveSla) {
        return prioritySlaService.bulkUpdatePrioritySla(company, ids, integrationIds, projects, issueTypes, priorities, respSla, solveSla);
    }

    public DbListResponse<DbJiraPrioritySla> listPrioritiesSla(String company,
                                                               List<String> integrationIds,
                                                               List<String> projects,
                                                               List<String> issueTypes,
                                                               List<String> priorities,
                                                               Integer pageNumber,
                                                               Integer pageSize) {
        return prioritySlaService.listPrioritiesSla(company, integrationIds, projects, issueTypes, priorities, pageNumber, pageSize);
    }

    //endregion


    //we dont support updates because the insert does all the work
    @Override
    public Boolean update(String company, DbJiraIssue issue) {
        throw new UnsupportedOperationException();
    }

    // region read

    //we dont support this get because the filter requires: integration_id + key + ingestedAt
    @Override
    public Optional<DbJiraIssue> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Optional<DbJiraIssue> get(String company, String key, String integrationId, Long ingestedAt)
            throws SQLException {
        return readService.get(company, key, integrationId, ingestedAt);
    }

    @Override
    public DbListResponse<DbJiraIssue> list(String company, Integer pageNumber,
                                            Integer pageSize) throws SQLException {
        return list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder().build(), null, Collections.emptyMap(), pageNumber, pageSize);
    }

    public DbListResponse<DbJiraIssue> list(String company,
                                            JiraSprintFilter jiraSprintFilter,
                                            JiraIssuesFilter filter,
                                            OUConfiguration ouConfig,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize) throws SQLException {
        return list(company, jiraSprintFilter, filter, Optional.ofNullable(JiraIssuesFilter.builder().build()), ouConfig, Optional.empty(), sortBy, pageNumber, pageSize);
    }

    public DbListResponse<DbJiraIssue> list(String company,
                                            JiraSprintFilter jiraSprintFilter,
                                            JiraIssuesFilter filter,
                                            OUConfiguration ouConfig,
                                            Optional<VelocityConfigDTO> optVelocityConfigDTO,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize) throws SQLException {
        return list(company, jiraSprintFilter, filter, Optional.ofNullable(JiraIssuesFilter.builder().build()), ouConfig, optVelocityConfigDTO, sortBy, pageNumber, pageSize);
    }

    /**
     * @deprecated Optionals should not be used as parameters. Use {@link JiraIssueService#list(String, JiraIssuesFilter, JiraSprintFilter, JiraIssuesFilter, OUConfiguration, VelocityConfigDTO, Map, Integer, Integer)}
     */
    @Deprecated
    public DbListResponse<DbJiraIssue> list(String company,
                                            JiraSprintFilter jiraSprintFilter,
                                            JiraIssuesFilter filter,
                                            Optional<JiraIssuesFilter> linkedJiraIssuesFilter,
                                            OUConfiguration ouConfig,
                                            Optional<VelocityConfigDTO> optVelocityConfigDTO,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize)
            throws SQLException {
        return readService.list(company, filter, jiraSprintFilter, linkedJiraIssuesFilter.orElse(null), ouConfig, optVelocityConfigDTO.orElse(null), sortBy, pageNumber, pageSize);
    }

    public DbListResponse<DbJiraIssue> list(@Nonnull String company,
                                            @Nonnull JiraIssuesFilter filter,
                                            @Nullable JiraSprintFilter jiraSprintFilter,
                                            @Nullable JiraIssuesFilter linkedJiraIssuesFilter,
                                            @Nullable OUConfiguration ouConfig,
                                            @Nullable VelocityConfigDTO optVelocityConfigDTO,
                                            @Nullable Map<String, SortingOrder> sortBy,
                                            @Nullable Integer pageNumber,
                                            @Nullable Integer pageSize) throws SQLException {
        return readService.list(company, filter, jiraSprintFilter, linkedJiraIssuesFilter, ouConfig, optVelocityConfigDTO, sortBy, pageNumber, pageSize);
    }

    public Stream<DbJiraIssue> stream(@Nonnull String company,
                                      @Nonnull JiraIssuesFilter filter,
                                      @Nullable JiraSprintFilter jiraSprintFilter,
                                      @Nullable JiraIssuesFilter linkedJiraIssuesFilter,
                                      @Nullable OUConfiguration ouConfig,
                                      @Nullable VelocityConfigDTO velocityConfigDTO,
                                      @Nullable Map<String, SortingOrder> sortBy) {
        return readService.stream(company, filter, jiraSprintFilter, linkedJiraIssuesFilter, ouConfig, velocityConfigDTO, sortBy);
    }

    public List<DbJiraUser> listJiraUsers(String company, List<Integer> integrationIds, List<String> displayNames) {
        return userService.listJiraUsers(company, integrationIds, displayNames);
    }

    public List<DbJiraIssue> listJiraIssues(String company,
                                            JiraIssuesFilter filter,
                                            Map<String, SortingOrder> sortBy,
                                            Integer pageNumber,
                                            Integer pageSize) throws SQLException {
        return readService.listJiraIssues(company, filter, sortBy, pageNumber, pageSize);
    }

    public DbListResponse<DbJiraIssue> listAggResult(String company, JiraSprintFilter jiraSprintFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfiguration, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException {
        return readService.listAggResult(company, jiraSprintFilter, jiraIssuesFilter, ouConfiguration, sortBy, pageNumber, pageSize);
    }

    public boolean doIssueStatusOverlap(String company, DbJiraIssue issue) {
        return statusService.doIssueStatusOverlap(company, issue);
    }

    //endregion


    /**
     * @param company        tenant id
     * @param issueKeys      list of issueKeys
     * @param integrationIds list of integrationIds
     * @return list of {@link DbJiraStoryPoints} mapped with issueKey and integrationId
     */
    public Map<Pair<String, String>, List<DbJiraStoryPoints>> getStoryPointsForIssues(String company,
                                                                                      List<String> issueKeys,
                                                                                      List<Integer> integrationIds) {
        return readService.getStoryPointsForIssues(company, issueKeys, integrationIds);
    }

    public Map<Pair<String, String>, List<DbJiraSalesforceCase>> getSalesforceCaseForIssues(String company,
                                                                                            List<String> issueKeys,
                                                                                            List<Integer> integrationIds) {
        return readService.getSalesforceCaseForIssues(company, issueKeys, integrationIds);
    }

    public Map<Pair<String, String>, List<DbJiraLink>> getLinksForIssues(String company, List<String> issueKeys,
                                                                         List<Integer> integrationIds) {
        return readService.getLinksForIssues(company, issueKeys, integrationIds);
    }

    public Map<String, DbJiraStatus> getHistoricalStatusForIssues(String company, String integrationId, List<String> issueKeys, long targetTime) {
        return statusService.getHistoricalStatusForIssues(company, integrationId, issueKeys, targetTime);
    }

    public DbListResponse<JiraAssigneeTime> listIssueAssigneesByTime(String company,
                                                                     JiraIssuesFilter filter,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize) {
        return readService.listIssueAssigneesByTime(company, filter, ouConfig, pageNumber, pageSize);
    }

    public DbListResponse<JiraStatusTime> listIssueStatusesByTime(String company,
                                                                  JiraIssuesFilter filter,
                                                                  OUConfiguration ouConfig,
                                                                  Integer pageNumber,
                                                                  Integer pageSize) {
        return statusService.listIssueStatusesByTime(company, filter, ouConfig, pageNumber, pageSize);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              JiraIssuesFilter filter,
                                                              List<JiraIssuesFilter.DISTINCT> stacks,
                                                              String configTableKey,
                                                              OUConfiguration ouConfig,
                                                              Map<String, List<String>> velocityStageStatusesMap)
            throws SQLException {
        return aggService.stackedGroupBy(company, filter, stacks, configTableKey, ouConfig, velocityStageStatusesMap, null);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              JiraIssuesFilter filter,
                                                              List<JiraIssuesFilter.DISTINCT> stacks,
                                                              String configTableKey,
                                                              OUConfiguration ouConfig,
                                                              VelocityConfigDTO velocityConfigDTO,
                                                              Map<String, List<String>> velocityStageStatusesMap)
            throws SQLException {
        return aggService.stackedGroupBy(company, filter, stacks, configTableKey, ouConfig, velocityStageStatusesMap, velocityConfigDTO);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   JiraIssuesFilter filter,
                                                                   Boolean valuesOnly,
                                                                   String configTableKey,
                                                                   OUConfiguration ouConfig,
                                                                   Map<String, List<String>> velocityStageStatusesMap) throws SQLException {
        return aggService.groupByAndCalculate(company, filter, valuesOnly, configTableKey, ouConfig, velocityStageStatusesMap);
    }


    public DbListResponse<JiraSprintDistMetric> getSprintDistributionReport(String company, JiraSprintFilter.CALCULATION calculation,
                                                                            JiraSprintFilter sprintFilter) {
        return aggService.getSprintDistributionReport(company, calculation, sprintFilter);
    }

    public void deleteAndUpdateStatuses(String company, DbJiraIssue issue) {

        statusService.deleteAndUpdateStatuses(company, issue);
    }

    public int cleanUpOldData(String company, Long currentTime, Long olderThanSeconds) {
        return writeService.cleanUpOldData(company, currentTime, olderThanSeconds);
    }

    //we dont support delete because insert does the work and cascade deletes do the other work
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + ISSUES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    key VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    custom_fields JSONB NOT NULL,\n" +
                        "    config_version BIGINT,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    summary VARCHAR NOT NULL,\n" +
                        "    components VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    labels VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    versions VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    fix_versions VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    sprint_ids INTEGER[] NOT NULL DEFAULT '{}'::INTEGER[],\n" +
                        "    resolution VARCHAR,\n" +
                        "    status_category VARCHAR,\n" +
                        "    original_estimate BIGINT,\n" +
                        "    desc_size INTEGER NOT NULL,\n" +
                        "    priority VARCHAR NOT NULL,\n" +
                        "    assignee VARCHAR,\n" +
                        "    assignee_id UUID REFERENCES "
                        + company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    epic VARCHAR,\n" +
                        "    parent_key VARCHAR,\n" +
                        "    parent_issue_type VARCHAR,\n" +
                        "    parent_labels VARCHAR[] NOT NULL DEFAULT '{}'::VARCHAR[],\n" +
                        "    reporter VARCHAR NOT NULL,\n" +
                        "    reporter_id UUID REFERENCES "
                        + company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    status VARCHAR NOT NULL,\n" +
                        "    issue_type VARCHAR NOT NULL,\n" +
                        "    hops INTEGER NOT NULL,\n" +
                        "    bounces INTEGER NOT NULL,\n" +
                        "    num_attachments INTEGER NOT NULL,\n" +
                        "    issue_created_at BIGINT NOT NULL,\n" +
                        "    issue_updated_at BIGINT NOT NULL,\n" +
                        "    issue_resolved_at BIGINT,\n" +
                        "    issue_due_at BIGINT,\n" +
                        "    first_assigned_at BIGINT,\n" +
                        "    first_assignee VARCHAR,\n" +
                        "    first_assignee_id UUID REFERENCES "
                        + company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    first_attachment_at BIGINT,\n" +
                        "    first_comment_at BIGINT,\n" +
                        "    story_points INTEGER,\n" +
                        "    ingested_at BIGINT NOT NULL,\n" + //this will just contain a timestamp to the DAY
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (integration_id,key,ingested_at),\n" +
                        "    is_active boolean NOT NULL DEFAULT true" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_ingested_at_integration_id_compound_idx " +
                        "on " + company + "." + ISSUES_TABLE + "(ingested_at,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_custom_fields_idx " +
                        "on " + company + "." + ISSUES_TABLE + " USING GIN(custom_fields)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_components_idx " +
                        "on " + company + "." + ISSUES_TABLE + " USING GIN(components)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_labels_idx " +
                        "on " + company + "." + ISSUES_TABLE + " USING GIN(labels)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_ingested_at_idx " +
                        "on " + company + "." + ISSUES_TABLE + "(ingested_at)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_status_idx on " + company + "." + ISSUES_TABLE + "(status)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_project_idx on " + company + "." + ISSUES_TABLE + "(project)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_issue_created_at_idx on " + company + "." + ISSUES_TABLE + "(issue_created_at)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_integration_id_idx on " + company + "." + ISSUES_TABLE + "(integration_id)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_assignee_idx on " + company + "." + ISSUES_TABLE + "(assignee)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_first_assignee_idx on " + company + "." + ISSUES_TABLE + "(first_assignee)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_epic_idx on " + company + "." + ISSUES_TABLE + "(epic)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_reporter_idx on " + company + "." + ISSUES_TABLE + "(reporter)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_issue_type_idx on " + company + "." + ISSUES_TABLE + "(issue_type)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_versions_idx on " + company + "." + ISSUES_TABLE + " USING GIN(versions)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_fix_versions_idx on " + company + "." + ISSUES_TABLE + " USING GIN(fix_versions)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_parent_issue_type_idx on " + company + "." + ISSUES_TABLE + "(parent_issue_type)",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_parent_labels_idx on " + company + "." + ISSUES_TABLE + " USING GIN(parent_labels)",

                //supporting tables
                //priorities sla table
                "CREATE TABLE IF NOT EXISTS " + company + "." + PRIORITIES_SLA_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    priority VARCHAR NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    task_type VARCHAR NOT NULL,\n" +
                        "    resp_sla BIGINT NOT NULL DEFAULT 86400,\n" +
                        "    solve_sla BIGINT NOT NULL DEFAULT 86400,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    UNIQUE (priority,task_type,project,integration_id)\n" +
                        ")",
                //status duration table
                "CREATE TABLE IF NOT EXISTS " + company + "." + STATUSES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    status VARCHAR NOT NULL,\n" +
                        "    issue_key VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    start_time BIGINT NOT NULL,\n" +
                        "    end_time BIGINT NOT NULL,\n" +
                        "    status_id VARCHAR NOT NULL,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (issue_key,integration_id,status,start_time)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + STATUSES_TABLE + "_compound_idx " +
                        "on " + company + "." + STATUSES_TABLE + "(issue_key,integration_id)",
                //jira users
                "CREATE TABLE IF NOT EXISTS " + company + "." + USERS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    jira_id VARCHAR NOT NULL,\n" +
                        "    display_name VARCHAR NOT NULL,\n" +
                        "    account_type VARCHAR NOT NULL,\n" +
                        "    integ_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    active BOOLEAN NOT NULL,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (integ_id,jira_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + USERS_TABLE + "_compound_idx on "
                        + company + "." + USERS_TABLE + "(integ_id,display_name)",
                //assignee duration table
                "CREATE TABLE IF NOT EXISTS " + company + "." + ASSIGNEES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    assignee VARCHAR NOT NULL,\n" +
                        "    issue_key VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    start_time BIGINT NOT NULL,\n" +
                        "    end_time BIGINT NOT NULL,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (integration_id,assignee,issue_key,start_time)\n" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + JIRA_ISSUE_SALESFORCE_CASES + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    issue_key VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    fieldkey VARCHAR NOT NULL,\n" +
                        "    fieldvalue VARCHAR NOT NULL,\n" +
                        "    UNIQUE (integration_id,issue_key,fieldkey,fieldvalue)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + ASSIGNEES_TABLE + "_compound_idx on "
                        + company + "." + ASSIGNEES_TABLE + "(integration_id,issue_key)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + JIRA_ISSUE_SPRINTS + "( "
                        + " id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), "
                        + " sprint_id INTEGER NOT NULL, "
                        + " name VARCHAR, "
                        + " integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) on DELETE CASCADE, "
                        + " state VARCHAR, "
                        + " goal VARCHAR, "
                        + " start_date BIGINT, "
                        + " end_date BIGINT, "
                        + " completed_at BIGINT, "
                        + " updated_at BIGINT NOT NULL DEFAULT extract(epoch from now()), "
                        + " UNIQUE(integration_id, sprint_id))",
                "CREATE TABLE IF NOT EXISTS " + company + "." + JIRA_ISSUE_VERSIONS + "( "
                        + " id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), "
                        + " version_id INTEGER NOT NULL, "
                        + " project_id INTEGER, "
                        + " name VARCHAR, "
                        + " description VARCHAR, "
                        + " integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) on DELETE CASCADE, "
                        + " archived BOOLEAN, "
                        + " released BOOLEAN, "
                        + " overdue BOOLEAN, "
                        + " start_date TIMESTAMP WITH TIME ZONE, "
                        + " end_date TIMESTAMP WITH TIME ZONE, "
                        + " fix_version_updated_at BIGINT NOT NULL DEFAULT extract(epoch from now()), "
                        + " UNIQUE(integration_id, version_id))",
                "CREATE TABLE IF NOT EXISTS " + company + "." + JIRA_ISSUE_LINKS + "( "
                        + " id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), "
                        + " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,"
                        + " from_issue_key VARCHAR NOT NULL, "
                        + " to_issue_key VARCHAR NOT NULL, "
                        + " relation VARCHAR NOT NULL, "
                        + " UNIQUE(integration_id, from_issue_key, to_issue_key, relation))",
                          " CREATE INDEX IF NOT EXISTS jira_issue_links_to_issue_key_idx on "+company+".jira_issue_links(to_issue_key)",
                          " CREATE INDEX IF NOT EXISTS jira_issue_links_from_issue_key_idx on "+company+".jira_issue_links(from_issue_key)");
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}