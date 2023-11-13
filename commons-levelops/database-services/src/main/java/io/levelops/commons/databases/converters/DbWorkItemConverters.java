package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbStackedAggregationResult;
import io.levelops.commons.databases.models.response.IssueMgmtSprintMappingAggResult;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DbWorkItemConverters {

    public static DbWorkItem mapToDbWorkItem(ResultSet rs) throws SQLException {
        return DbWorkItem.builder()
                .id((UUID) rs.getObject("id"))
                .originalEstimate(rs.getFloat("original_estimate"))
                .workItemId(rs.getString("workitem_id"))
                .integrationId(rs.getString("integration_id"))
                .summary(rs.getString("summary"))
                .sprintIds((rs.getArray("sprint_ids") != null &&
                        rs.getArray("sprint_ids").getArray() != null) ?
                        Arrays.asList((UUID[]) rs.getArray("sprint_ids").getArray()) : List.of())
                .priority(rs.getString("priority"))
                .assignee(rs.getString("assignee"))
                .assigneeId(rs.getString("assignee_id"))
                .epic(rs.getString("epic"))
                .parentWorkItemId(rs.getString("parent_workitem_id"))
                .reporter(rs.getString("reporter"))
                .reporterId(rs.getString("reporter_id"))
                .status(rs.getString("status"))
                .workItemType(rs.getString("workitem_type"))
                .ingestedAt(rs.getLong("ingested_at"))
                .project(rs.getString("project"))
                .projectId(rs.getString("project_id"))
                .storyPoint(rs.getObject("story_points") != null ? rs.getFloat("story_points") : null)
                .components((rs.getArray("components") != null &&
                        rs.getArray("components").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("components").getArray()) : List.of())
                .labels((rs.getArray("labels") != null &&
                        rs.getArray("labels").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("labels").getArray()) : List.of())
                .versions((rs.getArray("versions") != null &&
                        rs.getArray("versions").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("versions").getArray()) : List.of())
                .fixVersions((rs.getArray("fix_versions") != null &&
                        rs.getArray("fix_versions").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("fix_versions").getArray()) : List.of())
                .resolution(rs.getString("resolution"))
                .statusCategory(rs.getString("status_category"))
                .ticketCategory(columnPresent(rs, "ticket_category") ? rs.getString("ticket_category") : null)
                .descSize(rs.getInt("desc_size"))
                .hops(rs.getInt("hops"))
                .bounces(rs.getInt("bounces"))
                .numAttachments(rs.getInt("num_attachments"))
                .workItemCreatedAt(rs.getTimestamp("workitem_created_at"))
                .workItemUpdatedAt(rs.getTimestamp("workitem_updated_at"))
                .workItemResolvedAt(rs.getTimestamp("workitem_resolved_at"))
                .workItemDueAt(rs.getTimestamp("workitem_due_at"))
                .firstAttachmentAt(rs.getTimestamp("first_attachment_at"))
                .firstCommentAt(rs.getTimestamp("first_comment_at"))
                .attributes(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "attributes", rs.getString("attributes")))
                .customFields(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "custom_fields", rs.getString("custom_fields")))
                .isActive(rs.getBoolean("is_active"))
                .responseTime(columnPresent(rs, "resp_time") ? rs.getLong("resp_time"): null)
                .solveTime(columnPresent(rs,"solve_time") ? rs.getLong("solve_time") : null)
                .sprintFullNames(columnPresent(rs, "sprint_full_names") ? Arrays.asList((String[]) rs.getArray("sprint_full_names").getArray()) : List.of())
                .build();
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        try{
            rs.findColumn(column);
            return true;
        } catch (SQLException e){
            return false;
        }
    }

    public static RowMapper<DbAggregationResult> statusMetadataRowMapperForValues() {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(rs.getString("status"))
                .build();
    }
    public static RowMapper<DbWorkItem> listRowMapper() {
        return (rs, rowNumber) -> mapToDbWorkItem(rs);
    }

    public static RowMapper<DbAggregationResult> distinctRowMapper(String key, Optional<String> additionalKey, WorkItemsFilter.CALCULATION calc) {
        return (rs, rowNumber) -> getDbAggregationResult(key, additionalKey, calc, rs);
    }

    private static DbAggregationResult getDbAggregationResult(String key, Optional<String> additionalKey,
                                                              WorkItemsFilter.CALCULATION calc, ResultSet rs) throws SQLException {
        switch (calc) {
            case issue_count:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalTickets(rs.getLong("ct"))
                        .build();
            case assign_to_resolve:
            case hops:
            case bounces:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalTickets(rs.getLong("count"))
                        .max(rs.getLong("max"))
                        .min(rs.getLong("min"))
                        .median(rs.getLong("median"))
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
                        .totalTickets(rs.getLong("count"))
                        .max(rs.getLong("max"))
                        .min(rs.getLong("min"))
                        .mean(rs.getDouble("mean"))
                        .median(rs.getLong("median"))
                        .p90(rs.getLong("p90"))
                        .build();
            case story_point_report:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalStoryPoints(rs.getLong("story_points_sum"))
                        .totalUnestimatedTickets(rs.getLong("unestimated_tickets_count"))
                        .totalTickets(rs.getLong("count"))
                        .build();
            case effort_report:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalEffort(rs.getLong("effort_sum"))
                        .totalUnestimatedTickets(rs.getLong("unestimated_tickets_count"))
                        .totalTickets(rs.getLong("count"))
                        .build();
            case sprint_mapping:
                return DbAggregationResult.builder()
                        .integrationId(rs.getString("sprint_mapping_integration_id"))
                        .sprintId(rs.getString("sprint_mapping_sprint_id"))
                        .sprintName(rs.getString("sprint_mapping_name"))
                        .sprintCompletedAt(rs.getLong("sprint_mapping_completed_at"))
                        .sprintStartedAt(rs.getLong("sprint_mapping_start_date"))
                        .issueMgmtSprintMappingAggResults(DatabaseUtils.fromSqlArray(rs.getArray("sprint_mappings"), String.class)
                                //.map(PGobject::getValue)
                                .filter(StringUtils::isNoneEmpty)
                                .map(value -> ParsingUtils.parseObject(DefaultObjectMapper.get(), "sprint_mapping", IssueMgmtSprintMappingAggResult.class, value))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                        .build();
            case sprint_mapping_count:
                return DbAggregationResult.builder()
                        .total(rs.getLong("ct"))
                        .build();
            case stage_times_report:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .stage(rs.getString("state"))
                        .totalTickets(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .mean(rs.getDouble("mean_time"))
                        .median(rs.getLong("median"))
                        .build();
            case stage_bounce_report:
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .stage(rs.getString("stage"))
                        .totalTickets(rs.getLong("ct"))
                        .mean(rs.getDouble("mean"))
                        .median(rs.getLong("median"))
                        .build();
            case assignees:
                List<String> assignees = DatabaseUtils.fromSqlArray(rs.getArray("assignees"), String.class).collect(Collectors.toList());
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .assignees(assignees)
                        .total((long) CollectionUtils.size(assignees))
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
    }

    public static RowMapper<DbStackedAggregationResult> distinctRowMapperForStacks(String key,
                                                                                   Optional<String> additionalKey,
                                                                                   String acrossKey,
                                                                                   Optional<String> acrossAdditionalKey,
                                                                                   WorkItemsFilter.CALCULATION calc) {
        return (rs, rowNumber) -> DbStackedAggregationResult.builder()
                .rowKey(rs.getString(acrossKey))
                .rowAdditionalKey(acrossAdditionalKey.isPresent() ? rs.getString(acrossAdditionalKey.get()) : null)
                .stackedAggResult(getDbAggregationResult(key, additionalKey, calc, rs))
                .build();
    }

    public static RowMapper<DbWorkItemPrioritySLA> prioritySLARowMapper() {
        return (rs, rowNumber) ->
                DbWorkItemPrioritySLA.builder()
                        .id(rs.getString("id"))
                        .priority(rs.getString("priority"))
                        .project(rs.getString("project"))
                        .workitemType(rs.getString("workitem_type"))
                        .respSla(rs.getLong("resp_sla"))
                        .solveSla(rs.getLong("solve_sla"))
                        .integrationId(rs.getString("integration_id"))
                        .build();
    }
}
