package io.levelops.commons.databases.models.filters;

import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class TestRailsTestsFilter {

    List<String> projects;
    List<String> milestones;
    List<String> testPlans;
    List<String> testRuns;
    List<String> assignees;
    List<String> integrationIds;
    List<String> statuses;
    List<String> priorities;
    List<String> testTypes;
    Integer acrossLimit;
    String customAcross;
    ImmutablePair<Long, Long> createdOnTimeRange;
    List<String> customStacks;
    Map<String, Object> customCaseFields;
    Map<String, Object> excludeCustomCaseFields;
    ImmutablePair<Long, Long> createdAtTimeRange;

    DISTINCT DISTINCT;

    CALCULATION CALCULATION;

    public enum DISTINCT {
        project,
        milestone,
        test_plan,
        test_run,
        type,
        status,
        trend,
        priority,
        assignee,
        custom_case_field,
        none;

        public static DISTINCT fromString(String across) {
            if (across != null && across.startsWith("custom_")) {
                return custom_case_field;
            }
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        test_count,
        test_case_count,
        estimate,
        estimate_forecast;

        public static CALCULATION fromString(String calculation) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, calculation);
        }
        public static CALCULATION fromDefaultListRequest(DefaultListRequest request) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, MapUtils.emptyIfNull(request.getFilter())
                    .getOrDefault("metric", "test_count").toString());
        }
    }

}
