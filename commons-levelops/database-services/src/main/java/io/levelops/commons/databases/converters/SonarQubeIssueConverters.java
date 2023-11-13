package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.databases.models.filters.SonarQubeIssueFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class SonarQubeIssueConverters {

    public static RowMapper<DbSonarQubeIssue> listRowMapper() {
        return ((rs, rowNum) -> buildIssue(rs));
    }

    public static ResultSetExtractor<DbSonarQubeIssue> rowMapper() {
        return (rs -> {
            if (!rs.next())
                return null;
            return buildIssue(rs);
        });
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, SonarQubeIssueFilter.CALCULATION calculation) {
        return ((rs, rowNum) -> {
            if (calculation == SonarQubeIssueFilter.CALCULATION.effort) {
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .max(rs.getLong("maxi"))
                        .totalIssues(rs.getLong("ct"))
                        .min(rs.getLong("mini"))
                        .sum(rs.getLong("sum")).build();
            } else {
                return DbAggregationResult.builder()
                        .key(key.equals("none") ? null : rs.getString(key))
                        .totalIssues(rs.getLong("ct"))
                        .build();
            }
        });
    }

    private static DbSonarQubeIssue buildIssue(ResultSet rs) throws SQLException {
        return DbSonarQubeIssue.builder()
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .type(rs.getString("type"))
                .organization(rs.getString("organization"))
                .key(rs.getString("key"))
                .severity(rs.getString("severity"))
                .component(rs.getString("component"))
                .status(rs.getString("status"))
                .message(rs.getString("message"))
                .effort(rs.getString("effort"))
                .debt(rs.getString("debt"))
                .author(rs.getString("author"))
                .tags(convertSQLArrayToList(rs.getArray("tags")))
                .ingestedAt(rs.getTimestamp("ingested_at"))
                .creationDate(rs.getTimestamp("issue_creation_date"))
                .updationDate(rs.getTimestamp("issue_updation_date"))
                .build();
    }

    private static List<String> convertSQLArrayToList(java.sql.Array tags) throws SQLException {
        return (tags != null &&
                tags.getArray() != null) ?
                Arrays.asList((String[]) tags.getArray()) : List.of();
    }
}
