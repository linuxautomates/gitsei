package io.levelops.commons.databases.services.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.services.CiCdScmCombinedAggsService.CicdScmSqlCriteria;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdJobRunParameters;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdQualifiedJobNames;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
public class CiCdScmFilterParsers {

    private final ObjectMapper objectMapper;

    public CiCdScmFilterParsers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public CiCdScmFilter merge(Integer integrationId, CiCdScmFilter requestFilter, Map<String, Object> productFilter) {
        Map<String, Object> excludeFields = (Map<String, Object>) productFilter
                .getOrDefault("exclude", Map.of());
        return CiCdScmFilter.builder()
                .across(requestFilter.getAcross())
                .stacks(requestFilter.getStacks())
                .calculation(requestFilter.getCalculation())
                .authors(getListOrDefault(productFilter, "authors"))
                .cicdUserIds(getListOrDefault(productFilter, "cicd_user_ids"))
                .jobNames(getListOrDefault(productFilter, "job_names"))
                .jobNormalizedFullNames(getListOrDefault(productFilter, "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(productFilter, "job_statuses"))
                .instanceNames(getListOrDefault(productFilter, "instance_names"))
                .startTimeRange(requestFilter.getStartTimeRange() != null ? ImmutablePair.of(requestFilter.getStartTimeRange().getLeft(), requestFilter.getStartTimeRange().getRight()) : null)
                .endTimeRange(requestFilter.getEndTimeRange() != null ? ImmutablePair.of(requestFilter.getEndTimeRange().getLeft(), requestFilter.getEndTimeRange().getRight()) : null)
                .jobStartTime(requestFilter.getJobStartTime())
                .jobEndTime(requestFilter.getJobEndTime())
                .integrationIds(List.of(String.valueOf(integrationId)))
                .cicdIntegrationIds(List.of(String.valueOf(integrationId)))
                .types(CICD_TYPE.parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(productFilter, "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(productFilter, "qualified_job_names")))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(requestFilter.getAggInterval() != null ? requestFilter.getAggInterval().toString() : null), CICD_AGG_INTERVAL.day))
                .repos(getListOrDefault(productFilter, "repos"))
                .excludeRepos(getListOrDefault(excludeFields, "repos"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .partialMatch(MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match")))
                .build();
    }

    public String getSqlStmt(String company, CicdScmSqlCriteria conditions, String innerSelect, boolean isList) {
        String cicdWhereClause = (CollectionUtils.isEmpty(conditions.getCicdCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getCicdCriteria());
        String scmWhereClause = (CollectionUtils.isEmpty(conditions.getScmCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getScmCriteria());
        if (isList) {
            innerSelect = " j.job_name, cicd_job_id, job_run_number, status," +
                    " end_time, duration, cicd_user_id, i.name as instance_name, i.id as instance_guid," +
                    "project_name, job_normalized_full_name, i.integration_id as cicd_integration_id, scm_url, scm_commit_ids \n ";
        }
        return "SELECT * FROM (SELECT r.id as run_id, start_time" + (StringUtils.isEmpty(innerSelect) ? "" : "," + innerSelect) +
                " FROM " + company + ".cicd_job_runs as r\n" +
                "JOIN " + company + ".cicd_jobs as j on r.cicd_job_id = j.id \n" +
                (conditions.isUsingParamsTable() ? " LEFT JOIN " + company + ".cicd_job_run_params as p on p.cicd_job_run_id = r.id " : " ") +
                " LEFT OUTER JOIN " + company + ".cicd_instances as i on j.cicd_instance_id = i.id\n" +
                cicdWhereClause +
                ") a " +
                "join " + company + ".cicd_scm_mapping as m on m.cicd_job_run_id = a.run_id\n" +
                "join " + company + ".scm_commits as c on c.id = m.commit_id\n" +
                scmWhereClause;
    }
}
