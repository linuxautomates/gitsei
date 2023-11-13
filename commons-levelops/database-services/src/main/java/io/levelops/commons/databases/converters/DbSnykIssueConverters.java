package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.snyk.DbSnykIssue;
import io.levelops.commons.databases.models.filters.SnykIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DbSnykIssueConverters {

    public static RowMapper<DbSnykIssue> listRowMapper() {
        return (rs, rowNum) -> DbSnykIssue.builder()
                    .integrationId(rs.getString("integration_id"))
                    .orgId(rs.getString("org_id"))
                    .org(rs.getString("org"))
                    .projectId(rs.getString("project_id"))
                    .projectName(rs.getString("project"))
                    .issueId(rs.getString("issue_id"))
                    .url(rs.getString("url"))
                    .title(rs.getString("title"))
                    .type(StringUtils.defaultIfEmpty(rs.getString("type"), "").toUpperCase())
                    .packageName(rs.getString("package"))
                    .version(rs.getString("version"))
                    .severity(rs.getString("severity"))
                    .language(rs.getString("language"))
                    .packageManager(rs.getString("package_manager"))
                    .ignored(rs.getBoolean("ignored"))
                    .patched(rs.getBoolean("patched"))
                    .exploitMaturity(rs.getString("exploit_maturity"))
                    .upgradable(rs.getBoolean("upgradable"))
                    .patchable(rs.getBoolean("patchable"))
                    .pinnable(rs.getBoolean("pinnable"))
                    .cvssv3(rs.getString("cvssv3"))
                    .cvssScore(rs.getDouble("cvssv_score"))
                    .disclosureTime(rs.getDate("disclosure_time"))
                    .publicationTime(rs.getDate("publication_time"))
                    .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String groupByKey, SnykIssuesFilter.Calculation calculation) {
        return (rs, rowNum) -> {
            if (calculation == SnykIssuesFilter.Calculation.scores) {
                return DbAggregationResult.builder()
                        .key(getGroupByKeyResponse(groupByKey, rs))
                        .total(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .mean(rs.getDouble("mean"))
                        .build();

            } else {
                return DbAggregationResult.builder()
                        .key(getGroupByKeyResponse(groupByKey, rs))
                        .total(rs.getLong("ct"))
                        .build();
            }
        };
    }

    private static String getGroupByKeyResponse(String groupByKey, ResultSet rs) throws SQLException {
        switch (groupByKey) {
            case "upgradable":
            case "patchable":
            case "pinnable":
            case "ignored":
            case "patched":
                String keyInUpperCase = groupByKey.toUpperCase();
                boolean recordValue = rs.getBoolean(groupByKey);
                boolean wasNull = rs.wasNull();
                if (wasNull)
                    return "UNDEFINED";
                else
                    return recordValue ? keyInUpperCase : "NOT_" + keyInUpperCase;
            default:
                return rs.getString(groupByKey);
        }
    }
}
