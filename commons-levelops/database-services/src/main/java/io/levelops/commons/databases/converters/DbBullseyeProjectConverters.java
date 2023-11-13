package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.bullseye.BullseyeAggregationResult;
import io.levelops.commons.databases.models.database.bullseye.BullseyeSourceFile;
import io.levelops.commons.databases.models.database.bullseye.DbBullseyeBuild;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DbBullseyeProjectConverters {

    private enum Metric {
        FUNCTION,
        DECISION,
        CONDITION
    }

    public static RowMapper<DbBullseyeBuild> projectListRowMapper(boolean cicdJobTableNeeded) {
        return (rs, rowNum) -> {
            DbBullseyeBuild.DbBullseyeBuildBuilder builder = DbBullseyeBuild.builder()
                    .cicdJobRunId(rs.getString("cicd_job_run_id"))
                    .buildId(rs.getString("project_id"))
                    .jobName(rs.getString("project"))
                    .builtAt(rs.getDate("built_at"))
                    .name(rs.getString("name"))
                    .fileHash(rs.getString("file_hash"))
                    .directory(rs.getString("directory"))
                    .functionsCovered(rs.getInt("functions_covered"))
                    .functionsUncovered(getUncovered(Metric.FUNCTION, rs))
                    .totalFunctions(rs.getInt("total_functions"))
                    .functionsPercentageCoverage(getPercentageCoverage(Metric.FUNCTION, rs))
                    .decisionsCovered(rs.getInt("decisions_covered"))
                    .totalDecisions(rs.getInt("total_decisions"))
                    .decisionsUncovered(getUncovered(Metric.DECISION, rs))
                    .decisionsPercentageCoverage(getPercentageCoverage(Metric.DECISION, rs))
                    .conditionsCovered(rs.getInt("conditions_covered"))
                    .totalConditions(rs.getInt("total_conditions"))
                    .conditionsUncovered(getUncovered(Metric.CONDITION, rs))
                    .conditionsPercentageCoverage(getPercentageCoverage(Metric.CONDITION, rs));
            if (cicdJobTableNeeded) {
                builder.cicdJobName(rs.getString("job_name"))
                        .cicdJobFullName(rs.getString("job_full_name"))
                        .cicdJobNormalizedFullName(rs.getString("job_normalized_full_name"));
            }
            return  builder.build();
        };
    }

    public static RowMapper<BullseyeSourceFile> fileListRowMapper() {
        return (rs, rowNum) -> BullseyeSourceFile.builder()
                .name(rs.getString("name"))
                .modificationTime(rs.getDate("modification_time"))
                .functionsCovered(rs.getInt("functions_covered"))
                .totalFunctions(rs.getInt("total_functions"))
                .functionsUncovered(getUncovered(Metric.FUNCTION, rs))
                .functionsPercentageCoverage(getPercentageCoverage(Metric.FUNCTION, rs))
                .decisionsCovered(rs.getInt("decisions_covered"))
                .totalDecisions(rs.getInt("total_decisions"))
                .decisionsUncovered(getUncovered(Metric.DECISION, rs))
                .decisionsPercentageCoverage(getPercentageCoverage(Metric.DECISION, rs))
                .conditionsCovered(rs.getInt("conditions_covered"))
                .totalConditions(rs.getInt("total_conditions"))
                .conditionsUncovered(getUncovered(Metric.CONDITION, rs))
                .conditionsPercentageCoverage(getPercentageCoverage(Metric.CONDITION, rs))
                .build();
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String groupByKey, String additionalKey) {
        return (rs, rowNum) ->
                DbAggregationResult.builder().key(rs.getString(groupByKey))
                        .additionalKey(StringUtils.isEmpty(additionalKey) ? null : rs.getString(additionalKey))
                        .additionalCounts(Map.of("bullseye_coverage_metrics", BullseyeAggregationResult.builder()
                                .functionsCovered(rs.getInt("functions_covered"))
                                .functionsUncovered(getUncovered(Metric.FUNCTION, rs))
                                .totalFunctions(rs.getInt("total_functions"))
                                .functionPercentageCoverage(getPercentageCoverage(Metric.FUNCTION, rs))
                                .decisionsCovered(rs.getInt("decisions_covered"))
                                .decisionsUncovered(getUncovered(Metric.DECISION, rs))
                                .totalDecisions(rs.getInt("total_decisions"))
                                .decisionPercentageCoverage(getPercentageCoverage(Metric.DECISION, rs))
                                .conditionsCovered(rs.getInt("conditions_covered"))
                                .conditionsUncovered(getUncovered(Metric.CONDITION, rs))
                                .totalConditions(rs.getInt("total_conditions"))
                                .conditionPercentageCoverage(getPercentageCoverage(Metric.CONDITION, rs))
                                .build())
                        ).build();
    }

    private static Integer getUncovered(Metric metric, ResultSet rs) throws SQLException {
        switch (metric) {
            case FUNCTION:
                return rs.getInt("total_functions") - rs.getInt("functions_covered");
            case DECISION:
                return rs.getInt("total_decisions") - rs.getInt("decisions_covered");
            default:
                return rs.getInt("total_conditions") - rs.getInt("conditions_covered");
        }
    }

    private static Double getPercentageCoverage(Metric metric, ResultSet rs) throws SQLException {
        switch (metric) {
            case FUNCTION:
                if (rs.getInt("total_functions") == 0)
                    return 0.0;
                return ((rs.getInt("functions_covered") * 1.0) / rs.getInt("total_functions")) * 100;
            case DECISION:
                if (rs.getInt("total_decisions") == 0)
                    return 0.0;
                return ((rs.getInt("decisions_covered") * 1.0) / rs.getInt("total_decisions")) * 100;
            default:
                if (rs.getInt("total_conditions") == 0)
                    return 0.0;
                return ((rs.getInt("conditions_covered") * 1.0) / rs.getInt("total_conditions")) * 100;
        }
    }
}
