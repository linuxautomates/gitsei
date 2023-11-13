package io.levelops.commons.databases.services.parsers;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService.CICD_INSTANCES;
import static io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService.CICD_JOBS;
import static io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService.CICD_JOB_RUNS;
import static io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService.CICD_JOB_RUNS_TESTS;

@Log4j2
public class CiCdJobRunTestFilterParser {

    public CiCdJobRunTestsFilter merge(Integer integrationId, CiCdJobRunTestsFilter requestFilter, Map<String, Object> productFilter) {
        Map<String, Object> excludeFields = (Map<String, Object>) productFilter
                .getOrDefault("exclude", Map.of());
        return CiCdJobRunTestsFilter.builder()
                .DISTINCT(requestFilter.getDISTINCT())
                .CALCULATION(requestFilter.getCALCULATION())
                .jobNames(getListOrDefault(productFilter, "job_names"))
                .jobStatuses(getListOrDefault(productFilter, "job_statuses"))
                .jobRunIds(getListOrDefault(productFilter, "job_run_ids"))
                .jobRunNumbers(getListOrDefault(productFilter, "job_run_numbers"))
                .testStatuses(getListOrDefault(productFilter, "test_statuses"))
                .testSuites(getListOrDefault(productFilter, "test_suites"))
                .cicdUserIds(getListOrDefault(productFilter, "cicd_user_ids"))
                .instanceNames(getListOrDefault(productFilter, "instance_names"))
                .integrationIds(List.of(String.valueOf(integrationId)))
                .startTimeRange(requestFilter.getStartTimeRange() != null ? ImmutablePair.of(requestFilter.getStartTimeRange().getLeft(), requestFilter.getStartTimeRange().getRight()) : null)
                .endTimeRange(requestFilter.getEndTimeRange() != null ? ImmutablePair.of(requestFilter.getEndTimeRange().getLeft(), requestFilter.getEndTimeRange().getRight()) : null)
                .types(CICD_TYPE.parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeTestStatuses(getListOrDefault(excludeFields, "test_statuses"))
                .excludeTestSuites(getListOrDefault(excludeFields, "test_suites"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeJobRunNumbers(getListOrDefault(excludeFields, "job_run_numbers"))
                .excludeJobRunIds(getListOrDefault(excludeFields, "job_run_ids"))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(requestFilter.getAggInterval() != null ?
                        requestFilter.getAggInterval().toString() : null), CICD_AGG_INTERVAL.day))
                .build();
    }

    public String getSqlStmt(String company, List<String> conditions, String innerSelect, boolean isList, CiCdJobRunTestsFilter ciCdJobRunTestsFilter) {
        String whereClause = CollectionUtils.isEmpty(conditions) ? "" : " WHERE " + String.join(" AND ", conditions);
        String query;
        if (isList) {
            query = "SELECT t.*, j.job_name, r.status job_status, r.job_run_number, r.cicd_user_id," +
                    " j.job_normalized_full_name, j.project_name, r.start_time, r.end_time FROM ";
        } else {
            query = "SELECT " + (ciCdJobRunTestsFilter.getDISTINCT() == CiCdJobRunTestsFilter.DISTINCT.job_end ?
                    innerSelect + "," : "") + "t.*, j.job_name, j.job_normalized_full_name, j.project_name," +
                    " r.status job_status, r.cicd_user_id, r.job_run_number, i.name, " +
                    "EXTRACT(EPOCH FROM (r.start_time::date)) trend FROM ";
        }
        String joinStmt = company + "." + CICD_JOB_RUNS_TESTS + " t INNER JOIN " + company +
                "." + CICD_JOB_RUNS + " r ON t.cicd_job_run_id = r.id INNER JOIN " + company + "." +
                CICD_JOBS + " j ON r.cicd_job_id = j.id LEFT OUTER JOIN " + company + "." + CICD_INSTANCES +
                " i ON j.cicd_instance_id = i.id" + whereClause;
        return query + joinStmt;
    }

}
