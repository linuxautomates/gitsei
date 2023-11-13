package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class DbIssuesMilestoneConverters {

    public static RowMapper<DbIssuesMilestone> milestoneRowMapper() {
        return (rs, rowNumber) -> DbIssuesMilestone.builder()
                .id(rs.getObject("id", UUID.class))
                .fieldType(rs.getString("field_type"))
                .fieldValue(rs.getString("field_value"))
                .parentFieldValue(rs.getString("parent_field_value"))
                .name(rs.getString("name"))
                .projectId((rs.getString("project_id")))
                .state(rs.getString("state"))
                .integrationId(rs.getInt("integration_id"))
                .startDate(rs.getTimestamp("start_date"))
                .endDate(rs.getTimestamp("end_date"))
                .completedAt(rs.getTimestamp("completed_at"))
                .build();
    }
}
