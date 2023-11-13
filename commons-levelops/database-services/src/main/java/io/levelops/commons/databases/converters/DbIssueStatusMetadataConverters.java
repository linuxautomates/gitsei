package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class DbIssueStatusMetadataConverters {
    public static RowMapper<DbIssueStatusMetadata> issueStatusMetadataRowMapper() {
        return (rs, rowNumber) -> DbIssueStatusMetadata.builder()
                .id(rs.getObject("id", UUID.class))
                .integrationId(rs.getString("integration_id"))
                .projectId(rs.getString("project_id"))
                .status(rs.getString("status"))
                .statusCategory(rs.getString("status_category"))
                .statusId((rs.getString("status_id")))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}
