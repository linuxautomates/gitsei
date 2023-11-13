package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DbIssueMgmtSprintMappingConverters {

    public static DbIssueMgmtSprintMapping mapToDbIssueMgmtSprintMapping(ResultSet rs) throws SQLException {
        return DbIssueMgmtSprintMapping.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .workitemId(rs.getString("workitem_id"))
                .sprintId(rs.getString("sprint_id"))
                .addedAt(rs.getLong("added_at"))
                .removedAt(rs.getLong("removed_at"))
                .planned(rs.getBoolean("planned"))
                .delivered(rs.getBoolean("delivered"))
                .outsideOfSprint(rs.getBoolean("outside_of_sprint"))
                .ignorableWorkitemType(rs.getBoolean("ignorable_workitem_type"))
                .storyPointsPlanned(rs.getFloat("story_points_planned"))
                .storyPointsDelivered(rs.getFloat("story_points_delivered"))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

    public static RowMapper<Pair<DbIssueMgmtSprintMapping, DbIssuesMilestone>> sprintMappingAndMilestoneRowMapper() {
        return (rs, rowNumber) -> Pair.of(DbIssueMgmtSprintMapping.builder()
                        .id(rs.getString("sprint_mapping_id"))
                        .integrationId(rs.getString("sprint_mapping_integration_id"))
                        .workitemId(rs.getString("sprint_mapping_workitem_id"))
                        .sprintId(rs.getString("sprint_mapping_sprint_id"))
                        .addedAt(rs.getLong("sprint_mapping_added_at"))
                        .removedAt(rs.getLong("sprint_mapping_removed_at"))
                        .planned(rs.getBoolean("sprint_mapping_planned"))
                        .delivered(rs.getBoolean("sprint_mapping_delivered"))
                        .outsideOfSprint(rs.getBoolean("sprint_mapping_outside_of_sprint"))
                        .ignorableWorkitemType(rs.getBoolean("sprint_mapping_ignorable_workitem_type"))
                        .storyPointsPlanned(rs.getFloat("sprint_mapping_story_points_planned"))
                        .storyPointsDelivered(rs.getFloat("sprint_mapping_story_points_delivered"))
                        .createdAt(DateUtils.toInstant(rs.getTimestamp("sprint_mapping_created_at")))
                        .build(),
                DbIssuesMilestone.builder()
                        .id(UUID.fromString(rs.getString("milestone_id")))
                        .fieldType(rs.getString("milestone_field_type"))
                        .fieldValue(rs.getString("milestone_field_value"))
                        .parentFieldValue(rs.getString("milestone_parent_field_value"))
                        .name(rs.getString("milestone_name"))
                        .integrationId(rs.getInt("milestone_integration_id"))
                        .projectId(rs.getString("milestone_project_id"))
                        .state(rs.getString("milestone_state"))
                        .startDate(rs.getTimestamp("milestone_start_date"))
                        .endDate(rs.getTimestamp("milestone_end_date"))
                        .completedAt(rs.getTimestamp("milestone_completed_at"))
                        .attributes(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                                "milestone_attributes",
                                rs.getString("milestone_attributes")))
                        .build()
        );
    }
}
