package io.levelops.commons.databases.models.filters;

import io.levelops.commons.databases.models.database.SortingOrder;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class CiCdJobRunTestsFilter {

    List<String> cicdUserIds;
    List<String> jobNames;
    List<String> jobStatuses;
    List<String> testSuites;
    List<String> testStatuses;
    List<String> jobRunNumbers;
    List<String> jobRunIds;
    List<String> instanceNames;
    List<String> integrationIds;
    List<CICD_TYPE> types;
    List<String> projects;
    ImmutablePair<Long, Long> startTimeRange;
    ImmutablePair<Long, Long> endTimeRange;
    Set<UUID> orgProductsIds;
    Map<String, SortingOrder> sortBy;

    List<String> excludeJobNames;
    List<String> excludeJobStatuses;
    List<String> excludeTestSuites;
    List<String> excludeTestStatuses;
    List<String> excludeInstanceNames;
    List<String> excludeProjects;
    List<String> excludeJobRunIds;
    List<String> excludeJobRunNumbers;
    List<String> excludeCiCdUserIds;
    List<CICD_TYPE> excludeTypes;
    DISTINCT DISTINCT;

    CALCULATION CALCULATION;
    CICD_AGG_INTERVAL aggInterval;

    public enum DISTINCT {
        job_status,
        job_name,
        cicd_user_id,
        job_run_number,
        test_status,
        test_suite,
        job_run_id,
        instance_name,
        job_end,
        project_name,
        trend;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        duration,
        count;

        public static CiCdJobRunsFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CiCdJobRunsFilter.CALCULATION.class, st);
        }
    }
}
