package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.databases.models.filters.TestRailsTestsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public class DbTestRailsConverters {

    public static RowMapper<DbTestRailsTest> listRowMapper(ObjectMapper mapper) {
        return (((rs, rowNum) -> buildDbTestRailsTest(rs, mapper)));
    }

    private static DbTestRailsTest buildDbTestRailsTest(ResultSet rs, ObjectMapper mapper) throws SQLException {
        return DbTestRailsTest.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .testId(rs.getInt("test_id"))
                .caseId(rs.getInt("case_id"))
                .runId(rs.getInt("test_run_id"))
                .milestoneId(rs.getInt("milestone_id"))
                .estimate(rs.getLong("estimate"))
                .estimateForecast(rs.getLong("estimate_forecast"))
                .type(rs.getString("type"))
                .status(rs.getString("status"))
                .refs(rs.getString("refs"))
                .title(rs.getString("title"))
                .priority(rs.getString("priority"))
                .assignee(rs.getString("assignee"))
                .testRun(rs.getString("test_run"))
                .testPlan(rs.getString("test_plan"))
                .milestone(rs.getString("milestone"))
                .project(rs.getString("project"))
                .customCaseFields(MapUtils.emptyIfNull(ParsingUtils.parseJsonObject(mapper, "custom_case_fields", rs.getString("custom_case_fields"))))
                .defects((rs.getArray("defects") != null &&
                        rs.getArray("defects").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("defects").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbTestRailsTest> listTestCaseRowMapper(ObjectMapper mapper) {
        return (((rs, rowNum) -> buildDbTestRailsTestCase(rs, mapper)));
    }
    private static DbTestRailsTest buildDbTestRailsTestCase(ResultSet rs, ObjectMapper mapper) throws SQLException {
        return DbTestRailsTest.builder()
                .caseId(rs.getInt("case_id"))
                .integrationId(rs.getString("integration_id"))
                .project(rs.getString("project"))
                .milestone(rs.getString("milestone"))
                .title(rs.getString("title"))
                .type(rs.getString("type"))
                .priority(rs.getString("priority"))
                .refs(rs.getString("refs"))
                .estimate(rs.getLong("estimate"))
                .estimateForecast(rs.getLong("estimate_forecast"))
                .customCaseFields(MapUtils.emptyIfNull(ParsingUtils.parseJsonObject(mapper, "custom_case_fields", rs.getString("custom_case_fields"))))
                .build();
    }

    public static RowMapper<DbTestRailsCaseField> listRowMapperForCaseField() {
        return (((rs, rowNum) -> buildDbTestRailsCaseField(rs)));
    }

    public static DbTestRailsCaseField buildDbTestRailsCaseField(ResultSet rs) throws  SQLException{
        return DbTestRailsCaseField.builder()
                .id(rs.getString("id"))
                .caseFieldId(rs.getInt("case_field_id"))
                .label(rs.getString("label"))
                .name(rs.getString("name"))
                .systemName(rs.getString("system_name"))
                .type(rs.getString("type"))
                .isActive(rs.getObject("is_active", Boolean.class))
                .isGlobal(rs.getObject("is_global", Boolean.class))
                .integrationId(String.valueOf(rs.getInt("integration_id")))
                .projectIds((rs.getArray("project_ids") != null &&
                        rs.getArray("project_ids").getArray() != null) ?
                        Arrays.asList((Integer[]) rs.getArray("project_ids").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbTestRailsTest> listTestRowMapper(ObjectMapper mapper){
        return ((rs, rowNum) ->
             DbTestRailsTest.builder()
                    .id(rs.getString("id"))
                    .assignee(rs.getString("assignee"))
                    .caseId(rs.getInt("case_id"))
                    .estimate(rs.getLong("estimate"))
                    .estimateForecast(rs.getLong("estimate_forecast"))
                    .milestoneId(rs.getInt("milestone_id"))
                    .priority(rs.getString("priority"))
                    .testRun(rs.getString("run_id"))
                    .status(rs.getString("status"))
                    .testId(rs.getInt("test_id"))
                    .title(rs.getString("title"))
                    .type(rs.getString("type"))
                    .refs(rs.getString("refs"))
                     .customCaseFields(MapUtils.emptyIfNull(ParsingUtils.parseJsonObject(mapper, "custom case fields", rs.getString("custom_case_fields"))))
                    .build()
        );
    }
    public static RowMapper<DbAggregationResult> aggRowMapper(ObjectMapper mapper,String key, TestRailsTestsFilter.CALCULATION CALCULATION) {
        return ((rs, rowNum) -> {
            if (TestRailsTestsFilter.CALCULATION.test_count.equals(CALCULATION) || TestRailsTestsFilter.CALCULATION.test_case_count.equals(CALCULATION)) {
                return DbAggregationResult.builder()
                        .key(key.contains("trend") ?
                                String.valueOf(rs.getTimestamp(key)!= null ? rs.getTimestamp(key).toLocalDateTime().toEpochSecond(ZoneOffset.UTC) : 0)
                                : rs.getString(key))
                        .stacks(doesColumnExist("stacks", rs) ? ParsingUtils.parseList(mapper, "stacks", DbAggregationResult.class, rs.getString("stacks")) : null)
                        .totalTests(rs.getLong("ct"))
                        .build();
            } else {
                return DbAggregationResult.builder()
                        .key(key.contains("trend") ?
                                String.valueOf(rs.getTimestamp(key).toInstant().getEpochSecond())
                                : rs.getString(key))
                        .totalTests(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .stacks(doesColumnExist("stacks", rs) ? ParsingUtils.parseList(mapper, "stacks", DbAggregationResult.class, rs.getString("stacks")) : null)
                        .build();
            }
        });
    }

    public static boolean doesColumnExist(String columnName, ResultSet rs) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
