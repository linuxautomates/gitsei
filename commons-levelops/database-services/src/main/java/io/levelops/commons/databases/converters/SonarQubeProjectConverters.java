package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeCoverage;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeMeasure;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class SonarQubeProjectConverters {

    public static ResultSetExtractor<String> idMapper() {
        return (rs) -> !rs.next() ? null : rs.getString("id");
    }

    public static ResultSetExtractor<DbSonarQubeProject> rowMapper() {
        return (rs -> {
            if (!rs.next())
                return null;
            return buildProject(rs);
        });
    }

    public static RowMapper<DbSonarQubeMeasure> listMeasureMapper() {
        return ((rs, rowNum) -> DbSonarQubeMeasure.builder()
                .id(rs.getString("id"))
                .parentId(rs.getString("parent_id"))
                .name(rs.getString("name"))
                .value(rs.getString("value"))
                .dataType(rs.getString("dtype"))
                .ingestedAt(rs.getTimestamp("ingested_at"))
                .parent(rs.getString("parent"))
                .repo(rs.getString("repo"))
                .build());
    }

    public static RowMapper<DbSonarQubeCoverage> listCoverageMapper() {
        return ((rs, rowNum) -> DbSonarQubeCoverage.builder()
                .id(rs.getString("id"))
                .parentId(rs.getString("parent_id"))
                .repo(rs.getString("repo"))
                .coverage(rs.getString("coverage"))
                .lines(rs.getString("lines"))
                .line_coverage(rs.getString("line_coverage"))
                .covered_lines(rs.getString("covered_lines"))
                .uncovered_lines(rs.getString("uncovered_lines"))
                .conditions_to_cover(rs.getString("conditions_to_cover"))
                .covered_conditions(rs.getString("covered_conditions"))
                .uncovered_conditions(rs.getString("uncovered_conditions"))
                .new_coverage(rs.getString("new_coverage"))
                .new_lines(rs.getString("new_lines"))
                .new_line_coverage(rs.getString("new_line_coverage"))
                .new_covered_lines(rs.getString("new_covered_lines"))
                .new_uncovered_lines(rs.getString("new_uncovered_lines"))
                .new_conditions_to_cover(rs.getString("new_conditions_to_cover"))
                .new_covered_conditions(rs.getString("new_covered_conditions"))
                .new_uncovered_conditions(rs.getString("new_uncovered_conditions"))
                .build());
    }

    public static RowMapper<DbAggregationResult> aggMeasureMapper(boolean values, String key, String additionalKey) {
        return ((rs, rowNum) -> {
            String returnedKey = rs.getString(key);
            if (values) {
                return DbAggregationResult.builder()
                        .key(returnedKey)
                        .additionalKey(additionalKey != null ? rs.getString(additionalKey) : null)
                        .totalIssues(rs.getLong("ct"))
                        .build();
            } else {
                String metricKey = additionalKey != null ? rs.getString(additionalKey) : null;
                if (metricKey != null) {
                    if (!metricKey.equals("duplicated_lines_density")) {
                        return DbAggregationResult.builder()
                                .key(returnedKey)
                                .additionalKey(metricKey)
                                .total(rs.getLong("ct"))
                                .max(rs.getLong("mx"))
                                .min(rs.getLong("mn"))
                                .median(rs.getLong("percentile_disc"))
                                .sum(rs.getLong("sum"))
                                .build();
                    }
                    return DbAggregationResult.builder()
                            .key(returnedKey)
                            .additionalKey(metricKey)
                            .duplicatedDensity(rs.getFloat("sum"))
                            .build();
                }
                return DbAggregationResult.builder()
                        .key(returnedKey)
                        .additionalKey(metricKey)
                        .total(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .sum(rs.getLong("sum"))
                        .build();
            }
        });
    }

    private static DbSonarQubeProject buildProject(ResultSet rs) throws SQLException {
        return DbSonarQubeProject.builder()
                .integrationId(rs.getString("integration_id"))
                .name(rs.getString("name"))
                .visibility(rs.getString("visibility"))
                .revision(rs.getString("revision"))
                .key(rs.getString("key"))
                .organization(rs.getString("organization"))
                .build();
    }

    public static DateKeyMapper dateKeyMapper() {
        return new DateKeyMapper();
    }

    private static class DateKeyMapper implements ResultSetExtractor<Map<Date, String>> {

        @Override
        public Map<Date, String> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Date, String> map = new HashMap<>();
            while (rs.next()) {
                if (rs.getString("key") != null && rs.getTimestamp("time") != null) {
                    Date date = convert(rs.getTimestamp("time"));
                    String key = rs.getString("key");
                    map.put(date, key);
                }
            }
            return map;
        }

        private Date convert(Timestamp t) {
            if (t == null || t.toInstant() == null)
                return null;
            return new Date(t.toInstant().toEpochMilli());
        }
    }
}
