package io.levelops.commons.databases.services.jira;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.UUIDUtils;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.JiraIssueService.ASSIGNEES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_LINKS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SALESFORCE_CASES;
import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_SLA_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.STATUSES_TABLE;

@Log4j2
@Service
public class JiraIssueWriteService {

    private static final int DEFAULT_BULK_EPIC_STORY_POINTS_READ_PAGE_SIZE = 1000;
    private static final int DEFAULT_BULK_EPIC_STORY_POINTS_WRITE_PAGE_SIZE = 100;
    public static final String UNDEFINED_STATUS_ID = "UNDEFINED";
    private final NamedParameterJdbcTemplate template;
    private final DataSourceTransactionManager transactionManager;
    private JiraIssueReadService readService;
    private ObjectMapper objectMapper;
    private final JiraIssueAggService aggService;

    public JiraIssueWriteService(DataSource dataSource,
                                 ObjectMapper objectMapper,
                                 JiraIssueAggService aggService,
                                 JiraIssueReadService readService) throws SQLException {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.objectMapper = objectMapper;
        this.aggService = aggService;
        this.readService = readService;
    }

    public String insert(String company, DbJiraIssue issue) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(issue, "issue cannot be null.");
        Validate.notBlank(issue.getIntegrationId(), "issue.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(issue.getKey(), "issue.getKey() cannot be null or empty.");
        Validate.notNull(issue.getIngestedAt(), "issue.getIngestedAt() cannot be null or empty.");

        String insertedRowId = insertIssue(company, issue);

        if (issue.getOldIssueKey() != null) {
            try {
                insertJiraLinkedIssueRelation(company, issue.getIntegrationId(), issue.getOldIssueKey(), issue.getKey(), "moved");
            } catch (Exception e) {
                log.warn("Failed to insert old issue key for company={}, integration_id={}, issue_key={}, old_issue_key={}, ingested_at={}",
                        company, issue.getIntegrationId(), issue.getKey(), issue.getOldIssueKey(), issue.getIngestedAt(), e);
            }
        }

        try {
            markOldIssuesAsInactive(company, issue);
        } catch (Exception e) {
            log.warn("Failed to mark old issues as inactive for company={}, integration_id={}, issue_key={}, ingested_at={}",
                    company, issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt(), e);
        }

        try {
            insertPrioritySla(company, issue);
        } catch (Exception e) {
            log.warn("Failed to insert priority sla for company={}, integration_id={}, issue_key={}, ingested_at={}",
                    company, issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt(), e);
        }

        try {
            insertAssignees(company, issue);
        } catch (Exception e) {
            log.warn("Failed to insert assignees for company={}, integration_id={}, issue_key={}, ingested_at={}",
                    company, issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt(), e);
        }

        try {
            insertStatuses(company, issue);
        } catch (Exception e) {
            log.warn("Failed to insert statuses for company={}, integration_id={}, issue_key={}, ingested_at={}",
                    company, issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt(), e);
        }

        TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            insertSalesforceFields(company, issue);
            cleanUpRemovedSalesforceFields(company, issue);
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.warn("Failed to insert Salesforce fields for company={}, integration_id={}, issue_key={}, ingested_at={}",
                    company, issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt(), e);
        }

        return insertedRowId;
    }

    private String insertIssue(String company, DbJiraIssue issue) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(issue, "issue cannot be null.");
        Validate.notBlank(issue.getIntegrationId(), "issue.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(issue.getKey(), "issue.getKey() cannot be null or empty.");
        Validate.notNull(issue.getIngestedAt(), "issue.getIngestedAt() cannot be null.");
        Validate.notBlank(issue.getProject(), "issue.getProject() cannot be null or empty.");
        Validate.notBlank(issue.getIssueType(), "issue.getIssueType() cannot be null or empty.");
        Validate.notBlank(issue.getPriority(), "issue.getPriority() cannot be null or empty.");
        Validate.notBlank(issue.getStatus(), "issue.getStatus() cannot be null or empty.");
        Validate.notNull(issue.getIssueCreatedAt(), "issue.getIssueCreatedAt() cannot be null or empty.");
        Validate.notNull(issue.getIssueUpdatedAt(), "issue.getIssueUpdatedAt() cannot be null or empty.");

        String sql = "INSERT INTO " + company + "." + ISSUES_TABLE + " as issues " +
                " (key, integration_id, project, summary, labels, versions, fix_versions, sprint_ids, resolution, status_category, desc_size, issue_created_at, " +
                "  issue_updated_at, status, issue_type, priority, assignee, assignee_id, issue_resolved_at, issue_due_at, reporter, reporter_id, " +
                "  original_estimate, bounces, components, hops, epic, custom_fields, config_version, num_attachments, first_attachment_at, " +
                "  first_comment_at, first_assigned_at, story_points, first_assignee, first_assignee_id, ingested_at, parent_key, parent_issue_type, parent_labels, is_active) " +
                " VALUES " +
                " (:key, :integration_id, :project, :summary, :labels::VARCHAR[], :versions::VARCHAR[], :fix_versions::VARCHAR[], :sprint_ids::INTEGER[], :resolution, :status_category, :desc_size, :issue_created_at, " +
                "  :issue_updated_at, :status, :issue_type, :priority, :assignee, :assignee_id, :issue_resolved_at, :issue_due_at, :reporter, :reporter_id, " +
                "  :original_estimate, :bounces, :components::VARCHAR[], :hops, :epic, :custom_fields::JSONB, :config_version, :num_attachments, :first_attachment_at, " +
                "  :first_comment_at, :first_assigned_at, :story_points, :first_assignee, :first_assignee_id, :ingested_at, :parent_key, :parent_issue_type, :parent_labels::VARCHAR[], :is_active)" +
                " ON CONFLICT (integration_id, key, ingested_at) DO UPDATE SET" +
                "    project = EXCLUDED.project," +
                "    summary = EXCLUDED.summary," +
                "    labels = EXCLUDED.labels," +
                "    versions = EXCLUDED.versions," +
                "    fix_versions = EXCLUDED.fix_versions," +
                "    sprint_ids = EXCLUDED.sprint_ids," +
                "    resolution = EXCLUDED.resolution," +
                "    status_category = EXCLUDED.status_category," +
                "    desc_size = EXCLUDED.desc_size," +
                "    issue_created_at = EXCLUDED.issue_created_at," +
                "    issue_updated_at = EXCLUDED.issue_updated_at," +
                "    status = EXCLUDED.status," +
                "    issue_type = EXCLUDED.issue_type," +
                "    priority = EXCLUDED.priority," +
                "    assignee = EXCLUDED.assignee," +
                "    assignee_id = EXCLUDED.assignee_id," +
                "    issue_resolved_at = EXCLUDED.issue_resolved_at," +
                "    issue_due_at = EXCLUDED.issue_due_at," +
                "    reporter = EXCLUDED.reporter," +
                "    reporter_id = EXCLUDED.reporter_id," +
                "    original_estimate = EXCLUDED.original_estimate," +
                "    bounces = EXCLUDED.bounces," +
                "    components = EXCLUDED.components," +
                "    hops = EXCLUDED.hops," +
                "    epic = EXCLUDED.epic," +
                "    custom_fields = EXCLUDED.custom_fields," +
                "    config_version = EXCLUDED.config_version," +
                "    num_attachments = EXCLUDED.num_attachments," +
                "    first_attachment_at = EXCLUDED.first_attachment_at," +
                "    first_comment_at = EXCLUDED.first_comment_at," +
                "    first_assigned_at = EXCLUDED.first_assigned_at," +
                "    story_points = EXCLUDED.story_points," +
                "    first_assignee = EXCLUDED.first_assignee," +
                "    first_assignee_id = EXCLUDED.first_assignee_id," +
                "    parent_key = EXCLUDED.parent_key," +
                "    parent_issue_type = EXCLUDED.parent_issue_type," +
                "    parent_labels = EXCLUDED.parent_labels" +
                " WHERE" +
                " (issues.project, issues.summary, issues.labels, issues.versions, issues.fix_versions, issues.sprint_ids, issues.resolution, issues.status_category, issues.desc_size," +
                "  issues.issue_created_at, issues.issue_updated_at, issues.status, issues.issue_type, issues.priority, issues.assignee, issues.assignee_id, issues.issue_resolved_at," +
                "  issues.issue_due_at, issues.reporter, issues.reporter_id, issues.original_estimate, issues.bounces, issues.components, issues.hops, issues.epic, issues.custom_fields, issues.config_version, " +
                "  issues.num_attachments, issues.first_attachment_at, issues.first_comment_at, issues.first_assigned_at, issues.story_points, issues.first_assignee, issues.first_assignee_id, " +
                "  issues.parent_key, issues.parent_issue_type, issues.parent_labels" +
                " )" +
                " IS DISTINCT FROM " +
                " (EXCLUDED.project, EXCLUDED.summary, EXCLUDED.labels, EXCLUDED.versions, EXCLUDED.fix_versions, EXCLUDED.sprint_ids, EXCLUDED.resolution, EXCLUDED.status_category, EXCLUDED.desc_size," +
                "  EXCLUDED.issue_created_at, EXCLUDED.issue_updated_at, EXCLUDED.status, EXCLUDED.issue_type, EXCLUDED.priority, EXCLUDED.assignee, EXCLUDED.assignee_id, EXCLUDED.issue_resolved_at, EXCLUDED.issue_due_at," +
                "  EXCLUDED.reporter, EXCLUDED.reporter_id, EXCLUDED.original_estimate, EXCLUDED.bounces, EXCLUDED.components, EXCLUDED.hops, EXCLUDED.epic, EXCLUDED.custom_fields, EXCLUDED.config_version, " +
                "  EXCLUDED.num_attachments, EXCLUDED.first_attachment_at, EXCLUDED.first_comment_at, EXCLUDED.first_assigned_at, EXCLUDED.story_points, EXCLUDED.first_assignee, EXCLUDED.first_assignee_id," +
                "  EXCLUDED.parent_key, EXCLUDED.parent_issue_type, EXCLUDED.parent_labels" +
                " )";

        String isOldIssueKeySql = "SELECT from_issue_key FROM " + company + "." + JIRA_ISSUE_LINKS
                + " WHERE from_issue_key = :key AND integration_id = :integration_id AND relation = 'moved' ";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("key", issue.getKey());
        params.addValue("integration_id", NumberUtils.toInt(issue.getIntegrationId()));
        params.addValue("project", issue.getProject());
        params.addValue("summary", StringUtils.defaultString(issue.getSummary()));
        params.addValue("labels", DatabaseUtils.toSqlArray(issue.getLabels()));
        params.addValue("versions", DatabaseUtils.toSqlArray(issue.getVersions()));
        params.addValue("fix_versions", DatabaseUtils.toSqlArray(issue.getFixVersions()));
        params.addValue("sprint_ids", DatabaseUtils.toSqlArray(issue.getSprintIds()));
        params.addValue("resolution", issue.getResolution());
        params.addValue("status_category", issue.getStatusCategory());
        params.addValue("desc_size", MoreObjects.firstNonNull(issue.getDescSize(), 0));
        params.addValue("issue_created_at", issue.getIssueCreatedAt());
        params.addValue("issue_updated_at", issue.getIssueUpdatedAt());
        params.addValue("status", issue.getStatus());
        params.addValue("issue_type", StringUtils.upperCase(issue.getIssueType()));
        params.addValue("priority", issue.getPriority());
        params.addValue("assignee", issue.getAssignee());
        params.addValue("assignee_id", UUIDUtils.fromString(issue.getAssigneeId()));
        params.addValue("issue_resolved_at", issue.getIssueResolvedAt());
        params.addValue("issue_due_at", issue.getIssueDueAt());
        params.addValue("reporter", StringUtils.defaultString(issue.getReporter(), DbJiraIssue.UNKNOWN));
        params.addValue("reporter_id", UUIDUtils.fromString(issue.getReporterId()));
        params.addValue("original_estimate", issue.getOriginalEstimate());
        params.addValue("bounces", MoreObjects.firstNonNull(issue.getBounces(), 0));
        params.addValue("components", DatabaseUtils.toSqlArray(issue.getComponents()));
        params.addValue("hops", MoreObjects.firstNonNull(issue.getHops(), 0));
        params.addValue("epic", issue.getEpic());
        params.addValue("custom_fields", ParsingUtils.serialize(objectMapper, "custom_fields", issue.getCustomFields(), "{}"));
        params.addValue("config_version", issue.getConfigVersion());
        params.addValue("num_attachments", MoreObjects.firstNonNull(issue.getNumAttachments(), 0));
        params.addValue("first_attachment_at", issue.getFirstAttachmentAt());
        params.addValue("first_comment_at", issue.getFirstCommentAt());
        params.addValue("first_assigned_at", issue.getFirstAssignedAt());
        params.addValue("story_points", issue.getStoryPoints());
        params.addValue("first_assignee", issue.getFirstAssignee());
        params.addValue("first_assignee_id", UUIDUtils.fromString(issue.getFirstAssigneeId()));
        params.addValue("ingested_at", issue.getIngestedAt());
        params.addValue("parent_key", issue.getParentKey());
        params.addValue("parent_issue_type", StringUtils.upperCase(issue.getParentIssueType()));
        params.addValue("parent_labels", DatabaseUtils.toSqlArray(issue.getParentLabels()));

        List<String> oldIssueList = template.queryForList(isOldIssueKeySql, params, String.class);

        params.addValue("is_active", CollectionUtils.isEmpty(oldIssueList));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    private void insertPrioritySla(String company, DbJiraIssue issue) {
        if (StringUtils.isEmpty(issue.getPriority())) {
            return;
        }

        String prioritySlaInsert = "INSERT INTO " + company + "." + PRIORITIES_SLA_TABLE +
                " (priority, task_type, project, integration_id) " +
                " VALUES " +
                " (:priority, :task_type, :project, :integration_id) " +
                " ON CONFLICT (priority, task_type, project, integration_id) DO NOTHING";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("priority", issue.getPriority());
        params.addValue("task_type", issue.getIssueType());
        params.addValue("project", issue.getProject());
        params.addValue("integration_id", NumberUtils.toInt(issue.getIntegrationId()));

        template.update(prioritySlaInsert, params);
    }


    private void insertAssignees(String company, DbJiraIssue issue) {
        if (CollectionUtils.isEmpty(issue.getAssigneeList())) {
            return;
        }

        String assigneeSql = "INSERT INTO " + company + "." + ASSIGNEES_TABLE + " AS assignees " +
                " (issue_key, integration_id, assignee, start_time, end_time) " +
                " VALUES" +
                " (:issue_key, :integration_id, :assignee, :start_time, :end_time) " +
                " ON CONFLICT (integration_id, assignee, issue_key, start_time) DO UPDATE SET" +
                "   end_time = EXCLUDED.end_time " +
                " WHERE " +
                " (assignees.end_time) " +
                " IS DISTINCT FROM " +
                " (EXCLUDED.end_time) ";

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (DbJiraAssignee assignee : issue.getAssigneeList()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("issue_key", assignee.getIssueKey());
            params.addValue("integration_id", NumberUtils.toInt(assignee.getIntegrationId()));
            params.addValue("assignee", assignee.getAssignee());
            params.addValue("start_time", assignee.getStartTime());
            params.addValue("end_time", assignee.getEndTime());
            batchParams.add(params);
        }

        template.batchUpdate(assigneeSql, batchParams.toArray(new MapSqlParameterSource[0]));
    }

    private void insertSalesforceFields(String company, DbJiraIssue issue) {
        if (MapUtils.isEmpty(issue.getSalesforceFields())) {
            return;
        }

        String sql = "INSERT INTO " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                " (issue_key, integration_id, fieldkey, fieldvalue) " +
                " VALUES " +
                " (:issue_key, :integration_id, :fieldkey, :fieldvalue) " +
                " ON CONFLICT (integration_id, issue_key, fieldkey, fieldvalue) DO NOTHING";

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        issue.getSalesforceFields().forEach((key, valueList) -> valueList.forEach(value -> {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("issue_key", issue.getKey());
            params.addValue("integration_id", NumberUtils.toInt(issue.getIntegrationId()));
            params.addValue("fieldkey", key);
            params.addValue("fieldvalue", value);
            batchParams.add(params);
        }));

        template.batchUpdate(sql, batchParams.toArray(new MapSqlParameterSource[0]));
    }

    private void cleanUpRemovedSalesforceFields(String company, DbJiraIssue issue) {

        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        conditions.add("integration_id = :integration_id");
        params.addValue("integration_id", NumberUtils.toInt(issue.getIntegrationId()));

        conditions.add("issue_key = :issue_key");
        params.addValue("issue_key", issue.getKey());

        // if empty, delete everything; otherwise only delete keys not inside the current keys
        if (MapUtils.isNotEmpty(issue.getSalesforceFields())) {
            conditions.add("fieldkey NOT IN (:fieldkeys)");
            params.addValue("fieldkeys", issue.getSalesforceFields().keySet());
        }

        String sql = "DELETE FROM " + company + "." + JIRA_ISSUE_SALESFORCE_CASES +
                " WHERE " + String.join(" AND ", conditions);

        template.update(sql, params);
    }

    private int insertStatuses(String company, DbJiraIssue issue) {
        if (CollectionUtils.isEmpty(issue.getStatuses())) {
            return 0;
        }

        String statusSql = "INSERT INTO " + company + "." + STATUSES_TABLE + " AS statuses " +
                " (issue_key, integration_id, status, start_time, end_time, status_id)" +
                " VALUES " +
                " (:issue_key, :integration_id, :status, :start_time, :end_time, :status_id) " +
                " ON CONFLICT (issue_key, integration_id, status, start_time) DO UPDATE SET " +
                "   end_time = EXCLUDED.end_time, status_id = EXCLUDED.status_id" +
                " WHERE " +
                " (statuses.end_time, statuses.status_id) " +
                " IS DISTINCT FROM " +
                " (EXCLUDED.end_time, EXCLUDED.status_id) ";

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (DbJiraStatus status : issue.getStatuses()) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("issue_key", status.getIssueKey());
            params.addValue("integration_id", NumberUtils.toInt(status.getIntegrationId()));
            params.addValue("status", status.getStatus());
            params.addValue("start_time", status.getStartTime());
            params.addValue("end_time", status.getEndTime());
            params.addValue("status_id", StringUtils.firstNonBlank(status.getStatusId(), UNDEFINED_STATUS_ID));
            batchParams.add(params);
        }

        int[] count = template.batchUpdate(statusSql, batchParams.toArray(new MapSqlParameterSource[0]));
        int upsertCount = (count == null) ? 0 : Arrays.stream(count).sum();
        return upsertCount;
    }

    private void markOldIssuesAsInactive(String company, DbJiraIssue issue) {
        if (StringUtils.isEmpty(issue.getOldIssueKey())) {
            return;
        }
        String updateIssueKeySql = "UPDATE " + company + "." + ISSUES_TABLE +
                " SET is_active = false " +
                " WHERE key = :key " +
                " AND integration_id = :integration_id " +
                " AND ingested_at = :ingested_at ";
        Map<String, Object> params = Map.of(
                "integration_id", NumberUtils.toInt(issue.getIntegrationId()),
                "key", issue.getOldIssueKey(),
                "ingested_at", issue.getIngestedAt()
        );
        template.update(updateIssueKeySql, params);
    }

    public boolean insertJiraLinkedIssueRelation(String company, String integrationId, String fromIssueKey, String toIssueKey, String relation) {
        return BooleanUtils.isTrue(template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //insert jira linked issues relationships
            String jiraLinkedIssuesSql = "INSERT INTO " + company + "." + JIRA_ISSUE_LINKS +
                    " (integration_id, from_issue_key, to_issue_key, relation)" +
                    " VALUES (?, ?, ?, ?)" +
                    " ON CONFLICT (integration_id, from_issue_key, to_issue_key, relation) DO NOTHING";

            try (PreparedStatement insertLinkedIssueRelation = conn.prepareStatement(jiraLinkedIssuesSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertLinkedIssueRelation.setObject(i++, NumberUtils.toInt(integrationId));
                insertLinkedIssueRelation.setObject(i++, fromIssueKey);
                insertLinkedIssueRelation.setObject(i++, toIssueKey);
                insertLinkedIssueRelation.setObject(i, relation);

                return insertLinkedIssueRelation.executeUpdate() > 0;
            }
        })));
    }

    public int cleanUpOldData(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + ISSUES_TABLE + " WHERE ingested_at < :olderThanTime "
                        + "AND EXTRACT(DAY FROM to_timestamp(ingested_at)) NOT IN (1)",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }

    public void bulkUpdateEpicStoryPoints(String company, String integrationId, Long ingestedDate) throws SQLException {
        int offset;
        boolean hasMore;
        for (offset = 0, hasMore = true; hasMore; offset += DEFAULT_BULK_EPIC_STORY_POINTS_READ_PAGE_SIZE) {
            hasMore = bulkUpdateEpicStoryPointsSinglePage(company, integrationId, ingestedDate, DEFAULT_BULK_EPIC_STORY_POINTS_READ_PAGE_SIZE, offset, DEFAULT_BULK_EPIC_STORY_POINTS_WRITE_PAGE_SIZE);
        }
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BulkUpdateStoryPointsRow.BulkUpdateStoryPointsRowBuilder.class)
    public static class BulkUpdateStoryPointsRow {
        @JsonProperty("key")
        String key;
        @JsonProperty("story_points")
        Integer storyPoints;
        @JsonProperty("calculated_story_points")
        Integer calculatedStoryPoints;
    }

    /**
     * Update epics' story points with the sum of the story points of their direct children.
     *
     * @param readPageSize how many epics are read from the db at a time
     * @param readOffset number of records (not pages) to skip ahead
     * @param writePageSize how many epics are updated at a time
     * @return hasNext: true if there is more data to process.
     */
    public boolean bulkUpdateEpicStoryPointsSinglePage(String company, String integrationId, Long ingestedDate,
                                                       int readPageSize, int readOffset, int writePageSize) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(ingestedDate, "ingestedDate cannot be null.");

        // Group issues by their parent epic and sum their story points (calculated_story_points).
        // Join with parent epics to get the current story points value (story_points).
        // This ignores children without story points. It also skips epics without children.
        String readSql = "" +
                " SELECT " +
                "   child.epic as key, " +
                "   parent.story_points as story_points," +
                "   SUM(child.story_points) as calculated_story_points " +
                " FROM ${company}.jira_issues AS child " +
                " JOIN ${company}.jira_issues AS parent" +
                " ON " +
                "   parent.key = child.epic " +
                "   AND parent.ingested_at = child.ingested_at " +
                "   AND parent.integration_id = child.integration_id " +
                " WHERE " +
                "   (child.is_active = true OR child.is_active IS NULL)" +
                "   AND child.ingested_at = :ingested_at " +
                "   AND child.integration_id = :integration_id " +
                "   AND child.story_points > 0 " +
                " GROUP BY child.epic, parent.story_points " +
                " ORDER BY child.epic ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        readSql = StringSubstitutor.replace(readSql, Map.of("company", company));

        MapSqlParameterSource readParams = new MapSqlParameterSource();
        readParams.addValue("ingested_at", ingestedDate);
        readParams.addValue("integration_id", NumberUtils.toInt(integrationId));
        readParams.addValue("issue_type", "EPIC");
        readParams.addValue("skip", readOffset);
        readParams.addValue("limit", readPageSize);

        log.debug("sql = " + readSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", readParams);

        List<BulkUpdateStoryPointsRow> page = template.query(readSql, readParams,
                (ResultSet rs, int rowNum) -> BulkUpdateStoryPointsRow.builder()
                        .key(rs.getString("key"))
                        .storyPoints(rs.getInt("story_points"))
                        .calculatedStoryPoints(rs.getInt("calculated_story_points"))
                        .build());

        String updateSql = StringSubstitutor.replace("" +
                        " UPDATE ${company}.jira_issues " +
                        " SET story_points = :story_points " +
                        " WHERE integration_id = :integration_id" +
                        " AND ingested_at = :ingested_at " +
                        " AND key = :epic" +
                        " AND story_points IS DISTINCT FROM :story_points",
                Map.of("company", company));

        Stream<MapSqlParameterSource> batchUpdateParamsStream = page.stream()
                .filter(row -> {
                    if (StringUtils.isEmpty(row.getKey()) || row.getCalculatedStoryPoints() == null) {
                        // malformed
                        return false;
                    }
                    // always update if db value is missing
                    if (row.getStoryPoints() == null) {
                        return true;
                    }
                    // only update rows where the story points mismatch
                    return !row.getStoryPoints().equals(row.getCalculatedStoryPoints());
                })
                .map(row -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("integration_id", NumberUtils.toInt(integrationId));
                    params.addValue("ingested_at", ingestedDate);
                    params.addValue("epic", row.getKey());
                    params.addValue("story_points", row.getCalculatedStoryPoints());
                    return params;
                });

        log.debug("sql = " + updateSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'

        StreamUtils.forEachPage(batchUpdateParamsStream, writePageSize, batchUpdateParamsPage -> {
            MapSqlParameterSource[] batchUpdateParams = batchUpdateParamsPage.toArray(MapSqlParameterSource[]::new);
            log.debug("params ({})", batchUpdateParams.length);
            template.batchUpdate(updateSql, batchUpdateParams);
        });

        return !page.isEmpty();
    }
}
