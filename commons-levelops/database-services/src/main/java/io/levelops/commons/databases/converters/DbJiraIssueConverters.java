package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraLink;
import io.levelops.commons.databases.models.database.jira.DbJiraPrioritySla;
import io.levelops.commons.databases.models.database.jira.DbJiraSalesforceCase;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.JiraAssigneeTime;
import io.levelops.commons.databases.models.database.jira.JiraStatusTime;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class DbJiraIssueConverters {
    public static RowMapper<DbJiraIssue> listRowMapper(boolean hasStateTransitTime, boolean hasPriorityOrder, boolean hasTicketCategory, boolean hasLinkedIssues, boolean hasTicketPortion, boolean hasStoryPointsPortion, boolean hasAssigneeTime) {
        return (rs, rowNumber) -> DbJiraIssue.builder()
                .id(rs.getString("id"))
                .key(rs.getString("key"))
                .priority(rs.getString("priority"))
                .isActive(rs.getBoolean("is_active"))
                .assignee(rs.getString("assignee"))
                .assigneeId(String.valueOf(rs.getObject("assignee_id")))
                .firstAssignee(rs.getString("first_assignee"))
                .firstAssigneeId(String.valueOf(rs.getObject("first_assignee_id")))
                .issueCreatedAt(rs.getLong("issue_created_at"))
                .issueDueAt(rs.getLong("issue_due_at"))
                .issueResolvedAt(rs.getLong("issue_resolved_at"))
                .issueUpdatedAt(rs.getLong("issue_updated_at"))
                .firstCommentAt(rs.getLong("first_comment_at"))
                .firstAssignedAt(rs.getLong("first_assigned_at"))
                .firstAttachmentAt(rs.getLong("first_attachment_at"))
                .bounces(rs.getInt("bounces"))
                .hops(rs.getInt("hops"))
                .customFields(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "jira_issue",
                        rs.getString("custom_fields")))
                .configVersion(rs.getLong("config_version"))
                .createdAt(rs.getLong("created_at"))
                .epic(rs.getString("epic"))
                .parentKey(rs.getString("parent_key"))
                .descSize(rs.getInt("desc_size"))
                .integrationId(rs.getString("integration_id"))
                .issueType(rs.getString("issue_type"))
                .numAttachments(rs.getInt("num_attachments"))
                .project(rs.getString("project"))
                .reporter(rs.getString("reporter"))
                .reporterId(String.valueOf(rs.getObject("reporter_id")))
                .status(rs.getString("status"))
                .summary(rs.getString("summary"))
                .velocityStage(columnPresent(rs, "velocity_stage") ? rs.getString("velocity_stage") : null)
                .releaseTime(columnPresent(rs, "release_time") ? rs.getLong("release_time") : null)
                .releaseEndTime(columnPresent(rs, "release_end_time") ? rs.getLong("release_end_time") : null)
                .fixVersion(columnPresent(rs, "fix_version") ? rs.getString("fix_version") : null)
                .ingestedAt(rs.getLong("ingested_at"))
                .stateTransitionTime(
                        Boolean.TRUE.equals(hasStateTransitTime) ? rs.getLong("state_transition_time") : null)
                .originalEstimate(rs.getLong("original_estimate"))
                .storyPoints(rs.getObject("story_points") != null ? rs.getInt("story_points") : null)
                .versions((rs.getArray("versions") != null &&
                        rs.getArray("versions").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("versions").getArray()) : List.of())
                .fixVersions((rs.getArray("fix_versions") != null &&
                        rs.getArray("fix_versions").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("fix_versions").getArray()) : List.of())
                .labels((rs.getArray("labels") != null &&
                        rs.getArray("labels").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("labels").getArray()) : List.of())
                .components((rs.getArray("components") != null &&
                        rs.getArray("components").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("components").getArray()) : List.of())
                .statusCategory(rs.getString("status_category"))
                .resolution(rs.getString("resolution"))
                .priorityOrder(hasPriorityOrder ? rs.getInt("priority_order") : null)
                .ticketCategory(hasTicketCategory ? rs.getString("ticket_category") : null)
                .asOfStatus(rs.getString("status"))
                .responseTime(columnPresent(rs, "resp_time") ? rs.getLong("resp_time"): null)
                .solveTime(columnPresent(rs,"solve_time") ? rs.getLong("solve_time") : null)
                .sprintIds((rs.getArray("sprint_ids") != null &&
                        rs.getArray("sprint_ids").getArray() != null) ?
                        Arrays.asList((Integer[]) rs.getArray("sprint_ids").getArray()) : List.of())
                .ticketPortion(hasTicketPortion ? rs.getDouble("ticket_portion"):null)
                .storyPointsPortion(hasStoryPointsPortion ? rs.getDouble("story_points_portion"):null)
                .assigneeTime(hasAssigneeTime ? rs.getLong("assignee_time") : null)
                .build();
    }

    public static RowMapper<DbJiraIssue> listJiraReleaseMapper() {
        return (rs, rowNumber) -> DbJiraIssue.builder()
                .id(rs.getString("id"))
                .key(rs.getString("key"))
                .priority(rs.getString("priority"))
                .isActive(rs.getBoolean("is_active"))
                .assignee(rs.getString("assignee"))
                .assigneeId(String.valueOf(rs.getObject("assignee_id")))
                .integrationId(rs.getString("integration_id"))
                .issueType(rs.getString("issue_type"))
                .project(rs.getString("project"))
                .reporter(rs.getString("reporter"))
                .status(rs.getString("status"))
                .velocityStage(columnPresent(rs, "velocity_stage") ? rs.getString("velocity_stage") : null)
                .releaseTime(columnPresent(rs, "release_time") ? rs.getLong("release_time") : null)
                .releaseEndTime(columnPresent(rs, "release_end_time") ? rs.getLong("release_end_time") : null)
                .fixVersion(columnPresent(rs, "fix_version") ? rs.getString("fix_version") : null)
//                .ingestedAt(rs.getLong("ingested_at"))
                .fixVersions((rs.getArray("fix_versions") != null &&
                        rs.getArray("fix_versions").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("fix_versions").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbJiraSprint> listSprintsMapper() {
        return (rs, rowNumber) -> DbJiraSprint.builder()
                .id(rs.getString("id"))
                .sprintId(rs.getInt("sprint_id"))
                .goal(rs.getString("goal"))
                .startDate(rs.getLong("start_date"))
                .endDate(rs.getLong("end_date"))
                .integrationId(rs.getInt("integration_id"))
                .name(rs.getString("name"))
                .completedDate(rs.getLong("completed_at"))
                .updatedAt(rs.getLong("updated_at"))
                .state(rs.getString("state"))
                .build();
    }

    public static RowMapper<DbJiraAssignee> listAssigneeMapper() {
        return (rs, rowNumber) -> DbJiraAssignee.builder()
                .issueKey(rs.getString("issue_key"))
                .startTime(rs.getLong("start_time"))
                .assignee(rs.getString("assignee"))
                .endTime(rs.getLong("end_time"))
                .createdAt(rs.getLong("created_at"))
                .integrationId(rs.getString("integration_id"))
                .build();
    }

    public static RowMapper<DbJiraStatus> listStatusMapper() {
        return (rs, rowNumber) -> DbJiraStatus.builder()
                .issueKey(rs.getString("issue_key"))
                .startTime(rs.getLong("start_time"))
                .status(rs.getString("status"))
                .endTime(rs.getLong("end_time"))
                .createdAt(rs.getLong("created_at"))
                .integrationId(rs.getString("integration_id"))
                .statusId(rs.getString("status_id"))
                .build();
    }

    public static RowMapper<DbJiraStatusMetadata> statusMetadataRowMapper() {
        return (rs, rowNumber) -> DbJiraStatusMetadata.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .statusId(rs.getString("status_id"))
                .status(rs.getString("status"))
                .statusCategory(rs.getString("status_category"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

    public static RowMapper<DbJiraStatusMetadata.IntegStatusCategoryMetadata> statusCategoryToStatusesRowMapper() {
        return (rs, rowNumber) -> DbJiraStatusMetadata.IntegStatusCategoryMetadata.builder()
                .integrationId(rs.getString("integration_id"))
                .statusCategory(rs.getString("status_category"))
                .statuses((rs.getArray("statuses") != null &&
                        rs.getArray("statuses").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("statuses").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbJiraStoryPoints> storyPointsRowMapper() {
        return (rs, rowNumber) -> DbJiraStoryPoints.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .issueKey(rs.getString("issue_key"))
                .storyPoints(rs.getInt("story_points"))
                .startTime(rs.getLong("start_time"))
                .endTime(rs.getLong("end_time"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

    public static RowMapper<DbJiraIssueSprintMapping> sprintMappingRowMapper() {
        return (rs, rowNumber) -> DbJiraIssueSprintMapping.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .issueKey(rs.getString("issue_key"))
                .sprintId(rs.getString("sprint_id"))
                .addedAt(rs.getLong("added_at"))
                .planned(rs.getBoolean("planned"))
                .delivered(rs.getBoolean("delivered"))
                .outsideOfSprint(rs.getBoolean("outside_of_sprint"))
                .ignorableIssueType(rs.getBoolean("ignorable_issue_type"))
                .storyPointsPlanned(rs.getInt("story_points_planned"))
                .storyPointsDelivered(rs.getInt("story_points_delivered"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

    public static RowMapper<DbJiraIssueSprintMapping> sprintMappingRowMapperForIssueAgg() {
        return (rs, rowNumber) -> DbJiraIssueSprintMapping.builder()
                .id(rs.getString("sprint_mapping_id"))
                .integrationId(rs.getString("sprint_mapping_integration_id"))
                .issueKey(rs.getString("sprint_mapping_issue_key"))
                .sprintId(rs.getString("sprint_mapping_sprint_id"))
                .addedAt(rs.getLong("sprint_mapping_added_at"))
                .planned(rs.getBoolean("sprint_mapping_planned"))
                .delivered(rs.getBoolean("sprint_mapping_delivered"))
                .outsideOfSprint(rs.getBoolean("sprint_mapping_outside_of_sprint"))
                .ignorableIssueType(rs.getBoolean("sprint_mapping_ignorable_issue_type"))
                .storyPointsPlanned(rs.getInt("sprint_mapping_story_points_planned"))
                .storyPointsDelivered(rs.getInt("sprint_mapping_story_points_delivered"))
                .build();
    }

    public static RowMapper<JiraAssigneeTime> listIssueAssigneeTimeMapper() {
        return (rs, rowNumber) -> JiraAssigneeTime.builder()
                .assignee(rs.getString("tassignee"))
                .totalTime(rs.getLong("total"))
                .key(rs.getString("key"))
                .integrationId(rs.getString("integration_id"))
                .summary(rs.getString("summary"))
                .build();
    }

    public static RowMapper<JiraStatusTime> listIssueStatusTimeMapper() {
        return (rs, rowNumber) -> JiraStatusTime.builder()
                .status(rs.getString("tstatus"))
                .totalTime(rs.getLong("total"))
                .key(rs.getString("key"))
                .integrationId(rs.getString("integration_id"))
                .summary(rs.getString("summary"))
                .build();
    }

    public static RowMapper<DbJiraUser> listJiraUsersMapper() {
        return (rs, rowNumber) -> DbJiraUser.builder()
                .id(rs.getString("id"))
                .jiraId(rs.getString("jira_id"))
                .displayName(rs.getString("display_name"))
                .accountType(rs.getString("account_type"))
                .integrationId(String.valueOf(rs.getInt("integ_id")))
                .active(rs.getBoolean("active"))
                .createdAt(rs.getLong("created_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> statusMetadataRowMapperForValues() {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(rs.getString("status"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctRowMapper(String key,
                                                                   JiraIssuesFilter.CALCULATION calc,
                                                                   Optional<String> additionalKey) {
        RowMapper<DbJiraIssueSprintMapping> sprintMappingRowMapper = sprintMappingRowMapperForIssueAgg();
        return (rs, rowNumber) -> {
            if (calc == null) {
                //values only
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .build();
            }
            switch (calc) {
                case ticket_count:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalTickets(rs.getLong("ct"))
                            .totalStoryPoints(rs.getLong("total_story_points"))
                            .meanStoryPoints(rs.getDouble("mean_story_points"))
                            .build();
                case age:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalTickets(rs.getLong("count"))
                            .max(rs.getLong("max"))
                            .min(rs.getLong("min"))
                            .mean(rs.getDouble("mean"))
                            .median(rs.getLong("median"))
                            .p90(rs.getLong("p90"))
                            .totalStoryPoints(rs.getLong("total_story_points"))
                            .build();
                case resolution_time:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalTickets(rs.getLong("ct"))
                            .max(rs.getLong("mx"))
                            .min(rs.getLong("mn"))
                            .median(rs.getLong("median"))
                            .p90(rs.getLong("p90"))
                            .mean(rs.getDouble("mean"))
                            .build();
                case stage_times_report:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .stage(rs.getString("state"))
                            .min(rs.getLong("mn"))
                            .max(rs.getLong("mx"))
                            .totalTickets(rs.getLong("ct"))
                            .mean(rs.getDouble("mean_time"))
                            .median(rs.getLong("median"))
                            .p90(rs.getLong("p90"))
                            .p95(rs.getLong("p95"))
                            .build();
                case velocity_stage_times_report:
                    return DbAggregationResult.builder()
                            .key(rs.getString("velocity_stage"))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .min(rs.getLong("mn"))
                            .max(rs.getLong("mx"))
                            .totalTickets(rs.getLong("ct"))
                            .mean(rs.getDouble("mean_time"))
                            .median(rs.getLong("median"))
                            .p90(rs.getLong("p90"))
                            .p95(rs.getLong("p95"))
                            .build();
                case stage_bounce_report:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .stage(rs.getString("state"))
                            .totalTickets(rs.getLong("ct"))
                            .mean(rs.getDouble("mean"))
                            .median(rs.getLong("median"))
                            .build();
                case story_points:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalStoryPoints(rs.getLong("story_points_sum"))
                            .totalUnestimatedTickets(rs.getLong("unestimated_tickets_count"))
                            .totalTickets(rs.getLong("count"))
                            .build();
                case assignees:
                    List<String> assignees = DatabaseUtils.fromSqlArray(rs.getArray("assignees"), String.class).collect(Collectors.toList());
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .assignees(assignees)
                            .total((long) CollectionUtils.size(assignees))
                            .build();
                case sprint_mapping:
                    return DbAggregationResult.builder()
                            .integrationId(rs.getString("sprint_mapping_integration_id"))
                            .sprintId(rs.getString("sprint_mapping_sprint_id"))
                            .sprintName(rs.getString("sprint_mapping_name"))
                            .sprintGoal(rs.getString("sprint_mapping_goal"))
                            .sprintCompletedAt(rs.getLong("sprint_mapping_completed_at"))
                            .sprintStartedAt(rs.getLong("sprint_mapping_start_date"))
                            .sprintMappingAggs(DatabaseUtils.fromSqlArray(rs.getArray("sprint_mappings"), String.class)
                                    //.map(PGobject::getValue)
                                    .filter(StringUtils::isNoneEmpty)
                                    .map(value -> ParsingUtils.parseObject(DefaultObjectMapper.get(), "sprint_mapping", JiraIssueSprintMappingAggResult.class, value))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()))
                            .build();
                case sprint_mapping_count:
                    return DbAggregationResult.builder()
                            .total(rs.getLong("ct"))
                            .build();
                case priority:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .priority(rs.getString("priority"))
                            .priorityOrder(rs.getInt("priority_order"))
                            .build();
                default:
                    return DbAggregationResult.builder()
                            .key(key.equals("none") ? null : rs.getString(key))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .totalTickets(rs.getLong("ct"))
                            .max(rs.getLong("mx"))
                            .min(rs.getLong("mn"))
                            .median(rs.getLong("median"))
                            .build();
            }
        };
    }

    public static RowMapper<DbJiraPrioritySla> priorityRowMapper() {
        return (rs, rowNumber) ->
                DbJiraPrioritySla.builder()
                        .id(rs.getString("id"))
                        .respSla(rs.getLong("resp_sla"))
                        .project(rs.getString("project"))
                        .solveSla(rs.getLong("solve_sla"))
                        .priority(rs.getString("priority"))
                        .taskType(rs.getString("task_type"))
                        .integrationId(rs.getString("integration_id"))
                        .build();
    }

    public static RowMapper<DbAggregationResult> jiraZendeskRowMapper(String key) {
            return (rs, rowNumber) ->
                    DbAggregationResult.builder()
                    .key(key.equals("none") ? null : rs.getString(key))
                    .totalTickets(rs.getLong("ct"))
                    .build();
    }

    public static RowMapper<DbJiraVersion> versionRowMapper() {
        return (rs, rowNumber) ->
                DbJiraVersion.builder()
                        .id(rs.getString("id"))
                        .versionId(rs.getInt("version_id"))
                        .projectId(rs.getInt("project_id"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .integrationId(rs.getInt("integration_id"))
                        .archived(rs.getBoolean("archived"))
                        .released(rs.getBoolean("released"))
                        .overdue(rs.getBoolean("overdue"))
                        .startDate((rs.getTimestamp("start_date") != null)
                                ? (rs.getTimestamp("start_date").toLocalDateTime().toInstant(ZoneOffset.UTC)) : null)
                        .endDate((rs.getTimestamp("end_date") != null)
                                ? (rs.getTimestamp("end_date").toLocalDateTime().toInstant(ZoneOffset.UTC)) : null)
                        .fixVersionUpdatedAt(rs.getLong("fix_version_updated_at"))
                        .build();
    }

    public static RowMapper<DbJiraVersion> versionListRowMapper() {
        return (rs, rowNumber) ->
                DbJiraVersion.builder()
                        .id(rs.getString("id"))
                        .versionId(rs.getInt("version_id"))
                        .projectId(rs.getInt("project_id"))
                        .projectKey((rs.getString("project_key") != null) ? rs.getString("project_key") : null)
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .integrationId(rs.getInt("integration_id"))
                        .archived(rs.getBoolean("archived"))
                        .released(rs.getBoolean("released"))
                        .overdue(rs.getBoolean("overdue"))
                        .startDate((rs.getTimestamp("start_date") != null)
                                ? (rs.getTimestamp("start_date").toLocalDateTime().toInstant(ZoneOffset.UTC)) : null)
                        .endDate((rs.getTimestamp("end_date") != null)
                                ? (rs.getTimestamp("end_date").toLocalDateTime().toInstant(ZoneOffset.UTC)) : null)
                        .build();
    }

    public static RowMapper<DbJiraSalesforceCase> salesforceCaseRowMapper() {
        return (rs, rowNumber) ->
                DbJiraSalesforceCase.builder()
                        .issueKey(rs.getString("issue_key"))
                        .integrationId(rs.getInt("integration_id"))
                        .fieldKey(rs.getString("fieldkey"))
                        .fieldValue(rs.getString("fieldvalue"))
                        .build();
    }

    public static RowMapper<DbJiraLink> linkRowMapper() {
        return (rs, rowNumber) ->
                DbJiraLink.builder()
                        .integrationId(rs.getInt("integration_id"))
                        .fromIssueKey(rs.getString("from_issue_key"))
                        .toIssueKey(rs.getString("to_issue_key"))
                        .relation(rs.getString("relation"))
                        .build();
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }
}
