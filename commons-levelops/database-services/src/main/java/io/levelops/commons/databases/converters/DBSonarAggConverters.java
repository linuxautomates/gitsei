package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.springframework.jdbc.core.RowMapper;

public class DBSonarAggConverters {

    public static RowMapper<DbAggregationResult> rowMapper(){

        return (rs, rowNumber) -> DbAggregationResult.builder()
                .count(rs.getLong("ct"))
                .bugs(rs.getLong("bugs"))
                .vulnerabilities(rs.getLong("vulnerabilities"))
                .codeSmells(rs.getLong("code_smells"))
                .linesAddedCount(rs.getLong("lines_added"))
                .linesChangedCount(rs.getLong("lines_changed"))
                .linesRemovedCount(rs.getLong("lines_deleted"))
                .build();
    }

    public static RowMapper<DbAggregationResult> rowMapperBreakDown(){

        return (rs, rowNumber) -> DbAggregationResult.builder()
                .repoId(rs.getString("repo_id"))
                .title(rs.getString("title"))
                .branch(rs.getString("source_branch"))
                .prNumber(rs.getInt("number"))
                .bugs(rs.getLong("bugs"))
                .vulnerabilities(rs.getLong("vulnerabilities"))
                .codeSmells(rs.getLong("code_smells"))
                .linesAddedCount(rs.getLong("lines_added"))
                .linesChangedCount(rs.getLong("lines_changed"))
                .linesRemovedCount(rs.getLong("lines_deleted"))
                .build();
    }
}
