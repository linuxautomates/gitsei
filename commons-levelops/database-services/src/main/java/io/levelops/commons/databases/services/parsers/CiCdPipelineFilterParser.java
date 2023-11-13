package io.levelops.commons.databases.services.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdJobRunParameters;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdQualifiedJobNames;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@SuppressWarnings("unchecked")
@Log4j2
public class CiCdPipelineFilterParser {

    private final ObjectMapper objectMapper;

    public CiCdPipelineFilterParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CiCdPipelineJobRunsFilter merge(Integer integrationId, CiCdPipelineJobRunsFilter requestFilter, Map<String, Object> productFilter) {
        Map<String, Object> excludeFields = (Map<String, Object>) productFilter
                .getOrDefault("exclude", Map.of());
        return CiCdPipelineJobRunsFilter.builder()
                .across(requestFilter.getAcross())
                .stacks(requestFilter.getStacks())
                .calculation(requestFilter.getCalculation())
                .cicdUserIds(getListOrDefault(productFilter, "cicd_user_ids"))
                .jobNames(getListOrDefault(productFilter, "job_names"))
                .jobNormalizedFullNames(getListOrDefault(productFilter, "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(productFilter, "job_statuses"))
                .instanceNames(getListOrDefault(productFilter, "instance_names"))
                .startTimeRange(requestFilter.getStartTimeRange() != null ? ImmutablePair.of(requestFilter.getStartTimeRange().getLeft(), requestFilter.getStartTimeRange().getRight()) : null)
                .endTimeRange(requestFilter.getEndTimeRange() != null ? ImmutablePair.of(requestFilter.getEndTimeRange().getLeft(), requestFilter.getEndTimeRange().getRight()) : null)
                .integrationIds(List.of(String.valueOf(integrationId)))
                .types(CICD_TYPE.parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(productFilter, "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(productFilter, "qualified_job_names")))
                .parentCiCdJobIds(getListOrDefault(productFilter, "parent_cicd_job_ids"))
                .jobNamePartial((String) ((Map<String, Object>) productFilter
                        .getOrDefault("partial", Collections.emptyMap()))
                        .getOrDefault("job_name", null))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(requestFilter.getAggInterval() != null ? requestFilter.getAggInterval().toString() : null), CICD_AGG_INTERVAL.day))
                .partialMatch(MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match")))
                .jobIds(getListOrDefault(productFilter, "cicd_job_ids"))
                .ciCdJobRunIds(getListOrDefault(productFilter, "cicd_job_run_ids"))
                .build();
    }

    public String getSqlStmt(String company, List<String> conditions, List<String> unionCondition,
                             Boolean fullDetails, boolean isList) {
        String whereClause = CollectionUtils.isEmpty(conditions) ? "" : " WHERE " + String.join(" AND ", conditions);
        if (isList) {
            String baseWhereClause = (CollectionUtils.isEmpty(conditions)) ? "" : " WHERE " + String.join(" AND ", conditions);
            String unionWhereClause = (CollectionUtils.isEmpty(unionCondition)) ? "" : " WHERE " + String.join(" AND ", unionCondition);
            return "SELECT * FROM (WITH RECURSIVE cte_BOM AS (" + "\n" +
                    " SELECT DISTINCT(r.id) as run_id, j.id AS top_job_id, job_full_name, r.job_run_number" + "\n" +
                    " FROM " + company + ".cicd_job_runs AS r" + "\n" +
                    " JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                    " LEFT JOIN " + company + ".cicd_job_run_params AS p ON p.cicd_job_run_id = r.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_job_run_triggers as t on t.cicd_job_run_id = r.id AND t.type = \'UpstreamCause\'" + "\n" +
                    baseWhereClause + "\n" +
                    " UNION ALL" + "\n" +
                    " SELECT DISTINCT(r.id) as run_id, cte_BOM.top_job_id, j.job_full_name, r.job_run_number" + "\n" +
                    " FROM " + company + ".cicd_job_runs AS r" + "\n" +
                    " JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_job_run_triggers as t on t.cicd_job_run_id = r.id AND t.type = \'UpstreamCause\'" + "\n" +
                    " JOIN cte_BOM ON cte_BOM.job_full_name = t.trigger_id AND cte_BOM.job_run_number = t.job_run_number" + "\n" +
                    (fullDetails ? "" : unionWhereClause) + "\n" +
                    ")" + "\n" +
                    " SELECT DISTINCT(r.id) as id, j.job_name, j.job_full_name, r.log_gcspath, cicd_job_id," +
                    " r.job_run_number, status, start_time, duration, end_time, cicd_user_id, i.url, i.name as instance_name, i.id as instance_guid," +
                    " project_name, job_normalized_full_name, integration_id, scm_url, scm_commit_ids " + "\n" +
                    " FROM cte_BOM" + "\n" +
                    " JOIN " + company + ".cicd_job_runs as r on cte_BOM.run_id = r.id" + "\n" +
                    " JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id) SUB " + "\n";

        } else {
            return "SELECT * FROM (SELECT DISTINCT(r.id) as run_id, j.id AS top_job_id, job_full_name, r.job_run_number" + "\n" +
                    " FROM " + company + ".cicd_job_runs AS r" + "\n" +
                    " JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                    " LEFT JOIN " + company + ".cicd_job_run_params AS p ON p.cicd_job_run_id = r.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id" + "\n" +
                    " LEFT OUTER JOIN " + company + ".cicd_job_run_triggers as t on t.cicd_job_run_id = r.id AND t.type = \'UpstreamCause\'" + "\n" +
                    whereClause + ") SUB ";
        }
    }
}
