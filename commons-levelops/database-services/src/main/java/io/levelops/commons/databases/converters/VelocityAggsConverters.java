package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbHistogramBucket;
import io.levelops.commons.databases.models.response.VelocityCountByRating;
import io.levelops.commons.databases.models.response.VelocityStageResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class VelocityAggsConverters {
    private static final boolean AGGS_RESULTS = true;
    private static final boolean VALUES_RESULTS = false;

    private static List<DbAggregationResult> parseMedianAndCount(ResultSet rs, int rowNumber, Map<Integer, VelocityConfigDTO.Stage> offsetStageMap, List<Integer> sortedOffsets, Map<UUID, String> cicdIdAndJobNameMap, boolean aggsResult) throws SQLException {
        List<DbAggregationResult> result = new ArrayList<>();
        for(Integer offset : sortedOffsets) {
            Long median = null;
            Double mean = null;
            Long count = null;
            Long p90 = null;
            Long p95 = null;

            if(aggsResult) {
                String medianKey = String.format("s%d_median", offset);
                median = rs.getLong(medianKey);

                String meanKey = String.format("s%d_mean", offset);
                mean = rs.getDouble(meanKey);

                String countKey = String.format("s%d_count", offset);
                count = rs.getLong(countKey);

                String p90Key = String.format("s%d_p90", offset);
                p90 = rs.getLong(p90Key);

                String p95Key = String.format("s%d_p95", offset);
                p95 = rs.getLong(p95Key);
            } else {
                //For Values i.e. Not Aggs Result, if stage is not applicable, resultset("s0") or resultset("s1") etc. will be null.
                //Return null NOT 0.
                String calcKey = String.format("s%d", offset);
                Long calcValue = (rs.getObject(calcKey) != null) ? rs.getLong(calcKey) : null;
                median = calcValue;
                mean = (calcValue != null) ? calcValue.doubleValue() : null;
            }

            VelocityConfigDTO.Stage stage =  offsetStageMap.get(offset);
            DbAggregationResult.DbAggregationResultBuilder bldr = DbAggregationResult.builder()
                    .key(offsetStageMap.get(offset).getName())
                    .additionalKey(stage.getEvent().buildEventDescription(cicdIdAndJobNameMap))
                    .median(median)
                    .mean(mean)
                    .count(count)
                    .p90(p90)
                    .p95(p95)
                    .velocityStageResult(VelocityStageResult.builder()
                            .lowerLimitUnit(stage.getLowerLimitUnit()).lowerLimitValue(stage.getLowerLimitValue())
                            .upperLimitUnit(stage.getUpperLimitUnit()).upperLimitValue(stage.getUpperLimitValue())
                            .rating(stage.calculateRating(median))
                            .build());
            
            DbAggregationResult dbAggregationResult = bldr.build();
            result.add(dbAggregationResult);
        }
        return result;
    }
    private static String parseKey(ResultSet rs, int rowNumber, VelocityFilter velocityFilter) throws SQLException {
        VelocityFilter.STACK stack = velocityFilter.getStacks().get(0);
        String key = null;
        switch (stack) {
            case issue_type:
            case issue_priority:
            case issue_component:
            case issue_project:
            case issue_label:
            case issue_epic:
                key = rs.getString("stack");
                break;
            default:
                throw new RuntimeException(String.format("Stack %s is not supported!", stack));
        }
        return key;
    }
    public static RowMapper<List<DbAggregationResult>> mapVelocityAgg(VelocityFilter velocityFilter, Map<Integer, VelocityConfigDTO.Stage> offsetStageNameMap, Map<UUID, String> cicdIdAndJobNameMap) {
        return (rs, rowNumber) -> {
            List<Integer> sortedOffsets = offsetStageNameMap.keySet().stream().sorted().collect(Collectors.toList());
            List<DbAggregationResult> result = new ArrayList<>();

            if(velocityFilter.getAcross() == VelocityFilter.DISTINCT.trend) {
                List<DbAggregationResult> data = parseMedianAndCount(rs, rowNumber, offsetStageNameMap, sortedOffsets, cicdIdAndJobNameMap, AGGS_RESULTS);
                String trendKey = String.valueOf(rs.getLong("trend"));
                result.add(DbAggregationResult.builder().key(trendKey).data(data).build());
            } else  {
                if(CollectionUtils.isNotEmpty(velocityFilter.getStacks())) {
                    List<DbAggregationResult> stacks = parseMedianAndCount(rs, rowNumber, offsetStageNameMap, sortedOffsets, cicdIdAndJobNameMap, AGGS_RESULTS);
                    String key = parseKey(rs, rowNumber, velocityFilter);
                    result.add(DbAggregationResult.builder().key(key).stacks(stacks).build());
                } else {
                    result = parseMedianAndCount(rs, rowNumber, offsetStageNameMap, sortedOffsets, cicdIdAndJobNameMap, AGGS_RESULTS);
                }
            }

            return result;
        };
    }

    public static RowMapper<DbAggregationResult> mapVelocityValues(VelocityFilter velocityFilter, Map<Integer, VelocityConfigDTO.Stage> offsetStageNameMap, Map<UUID, String> cicdIdAndJobNameMap) {
        return (rs, rowNumber) -> {
            List<Integer> sortedOffsets = offsetStageNameMap.keySet().stream().sorted().collect(Collectors.toList());

            List<DbAggregationResult> data = parseMedianAndCount(rs, rowNumber, offsetStageNameMap, sortedOffsets, cicdIdAndJobNameMap, VALUES_RESULTS);
            String uid = rs.getString("u_id");
            String key = rs.getString("key");
            String title = rs.getString("title");
            String organization = rs.getString("org");
            String project = rs.getString("project");
            List<String> repoIds = (rs.getArray("repo_id") != null &&
                    rs.getArray("repo_id").getArray() != null) ?
                    Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of();
            String integrationId = rs.getString("integration_id");
            String prLink = columnPresent(rs, "pr_link") ? rs.getString("pr_link") : null;
            Long totalLeadTime = (rs.getObject("t") == null) ? null : (long) rs.getDouble("t");

            return DbAggregationResult.builder()
                    .key(uid)
                    .additionalKey(key)
                    .title(title)
                    .organization(organization)
                    .project(project)
                    .repoIds(repoIds)
                    .integrationId(integrationId)
                    .total(totalLeadTime)
                    .data(data)
                    .prLink(prLink)
                    .build();
        };
    }

    public static RowMapper<DbHistogramBucket> mapVelocityHistogram() {
        return (rs, rowNumber) -> {
            Integer bucketNumber = rs.getInt("bucket");
            Double intervalLower = (rs.getObject("min") != null) ? rs.getDouble("min") : null;
            Double intervalUpper = (rs.getObject("max") != null) ? rs.getDouble("max") : null;
            Long frequency = rs.getLong("cnt");

            DbHistogramBucket result = DbHistogramBucket.builder()
                    .bucketNumber(bucketNumber)
                    .intervalLower(intervalLower)
                    .intervalUpper(intervalUpper)
                    .frequency(frequency)
                    .build();
            log.info("histogram result = {}", result);
            return result;
        };
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

    public static RowMapper<VelocityCountByRating> mapVelocityCountByRating() {
        return (rs, rowNumber) -> {
            VelocityConfigDTO.Rating rating = VelocityConfigDTO.Rating.fromString(rs.getString("rating"));
            Long frequency = rs.getLong("cnt");
            VelocityCountByRating result = VelocityCountByRating.builder()
                    .rating(rating)
                    .count(frequency)
                    .build();
            log.info("rating result = {}", result);
            return result;
        };
    }
}
