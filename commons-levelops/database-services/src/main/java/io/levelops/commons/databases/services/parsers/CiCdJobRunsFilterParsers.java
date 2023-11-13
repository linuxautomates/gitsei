package io.levelops.commons.databases.services.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
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
import static io.levelops.commons.databases.services.CiCdAggsService.*;

@Log4j2
public class CiCdJobRunsFilterParsers {

    private final ObjectMapper objectMapper;

    public CiCdJobRunsFilterParsers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public CiCdJobRunsFilter merge(Integer integrationId, CiCdJobRunsFilter requestFilter, Map<String, Object> productFilter) {
        Map<String, Object> excludeFields = (Map<String, Object>) productFilter
                .getOrDefault("exclude", Map.of());
        return CiCdJobRunsFilter.builder()
                .across(requestFilter.getAcross())
                .stacks(requestFilter.getStacks())
                .calculation(requestFilter.getCalculation())
                .integrationIds(List.of(String.valueOf(integrationId)))
                .cicdUserIds(getListOrDefault(productFilter, "cicd_user_ids"))
                .jobNames(getListOrDefault(productFilter, "job_names"))
                .jobNormalizedFullNames(getListOrDefault(productFilter, "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(productFilter, "job_statuses"))
                .instanceNames(getListOrDefault(productFilter, "instance_names"))
                .stageNames(requestFilter.getStageNames())
                .stepNames(requestFilter.getStepNames())
                .stageStatuses(requestFilter.getStageStatuses())
                .stepStatuses(requestFilter.getStepStatuses())
                .startTimeRange(requestFilter.getStartTimeRange() != null ? ImmutablePair.of(requestFilter.getStartTimeRange().getLeft(), requestFilter.getStartTimeRange().getRight()) : null)
                .endTimeRange(requestFilter.getEndTimeRange() != null ? ImmutablePair.of(requestFilter.getEndTimeRange().getLeft(), requestFilter.getEndTimeRange().getRight()) : null)
                .types(CICD_TYPE.parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeStageNames(requestFilter.getExcludeStageNames())
                .excludeStepNames(requestFilter.getExcludeStepNames())
                .excludeStageStatuses(requestFilter.getExcludeStageStatuses())
                .excludeStepStatuses(requestFilter.getExcludeStepStatuses())
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(productFilter, "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(productFilter, "qualified_job_names")))
                .partialMatch(MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match")))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(requestFilter.getAggInterval() != null ?
                        requestFilter.getAggInterval().toString() : null), CICD_AGG_INTERVAL.day))
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> allConditions, String innerSelect, boolean isList, CiCdJobRunsFilter ciCdJobRunsFilter, boolean useAllTriage) {
        List<String> conditions = allConditions.get(CICD_CONDITIONS);
        String whereClause = CollectionUtils.isEmpty(conditions) ? "" : " WHERE " + String.join(" AND ", conditions);

        List<String> triageConditions = allConditions.get(TRIAGE_CONDITIONS);
        String triageWhereClause = CollectionUtils.isEmpty(triageConditions) ? "" : " WHERE " + String.join(" AND ", triageConditions);

        boolean needCiCdJobsTable = false;
        boolean needCiCdJobRunParamsTable = false;
        boolean needCiCdInstancesTable = false;
        boolean needTriageruns = false;
        boolean needCiCdJobRunStageTable = false;
        boolean needCiCdJobRunStageStepsTable = false;
        if (!isList) {
            needCiCdJobsTable = needCiCdJobsTable(ciCdJobRunsFilter);
            needCiCdJobRunParamsTable = needCiCdJobRunParamsTable(ciCdJobRunsFilter);
            needCiCdInstancesTable = needCiCdInstancesTable(ciCdJobRunsFilter);
            needTriageruns = needTriageRuns(ciCdJobRunsFilter);
            needCiCdJobRunStageTable = needCiCdJobRunStageTable(ciCdJobRunsFilter);
            needCiCdJobRunStageStepsTable = needCiCdJobRunStageStepsTable(ciCdJobRunsFilter);
        }
        if (needCiCdInstancesTable)
            needCiCdJobsTable = true;
        String cicdJobsTableJoin = "";
        String cicdJobRunParamsJoin = "";
        String cicdInstancesJoin = "";
        String cicdJobRunStageJoin = "";
        String cicdJobRunStageStepJoin = "";
        String cicdTriageJoin = "";
        String cicdAllTriage = "";
        if (needCiCdJobsTable || isList) {
            cicdJobsTableJoin = " JOIN " + company + ".cicd_jobs as j on r.cicd_job_id = j.id";
        }
        if (needCiCdJobRunParamsTable || isList) {
            cicdJobRunParamsJoin = " LEFT JOIN " + company + ".cicd_job_run_params as p on p.cicd_job_run_id = r.id";
        }
        if (needCiCdInstancesTable || isList) {
            cicdInstancesJoin = " LEFT OUTER JOIN " + company + ".cicd_instances as i on j.cicd_instance_id = i.id";
        }
        if (needCiCdJobRunStageTable || isList) {
            cicdJobRunStageJoin = " LEFT OUTER JOIN " + company + ".cicd_job_run_stages as cicd_job_run_stage on cicd_job_run_stage.cicd_job_run_id = r.id";
        }
        if (needCiCdJobRunStageStepsTable || isList) {
            cicdJobRunStageStepJoin = " LEFT OUTER JOIN " + company + ".cicd_job_run_stage_steps as cicd_job_run_stage_steps on cicd_job_run_stage_steps.cicd_job_run_stage_id = cicd_job_run_stage.id";
        }
        if(needTriageruns || isList ){
            String jobsJoin = cicdJobsTableJoin.isEmpty() ? " JOIN " + company + ".cicd_jobs as j on r.cicd_job_id = j.id " : StringUtils.EMPTY;
            cicdTriageJoin = jobsJoin + "\n"+
                " LEFT JOIN ( select t.id as triage_rule_id, job_run_id, name as triage_rule from "+company+".jenkins_rule_hits j inner join "+company+".triage_rules t on j.rule_id = t.id group by t.id, job_run_id, name ) jen on jen.job_run_id = r.id ";

            if(useAllTriage && innerSelect != null && CollectionUtils.isEmpty(ciCdJobRunsFilter.getStacks())){
                cicdAllTriage = " UNION SELECT  uuid_nil() as id, 0 as duration, NAME AS triage_rule,  t.id AS triage_rule_id FROM "+company+".triage_rules t "+triageWhereClause;
            }
        }
        if (isList) {
            String metadataSelect = " (metadata -> 'env_ids')::text as env_ids,\n" +
                    "            (metadata -> 'infra_ids')::text as infra_ids,\n" +
                    "            (metadata -> 'service_ids')::text as service_ids,\n" +
                    "            (metadata -> 'service_types')::text as service_types,\n" +
                    "            (metadata -> 'tags')::text as tags,\n" +
                    "            trim('\"' from (metadata -> 'branch')::text) as branch,\n" +
                    "            trim('\"' from (metadata -> 'repo_url')::text) as repo_url,\n" +
                    "            (metadata -> 'rollback')::bool as rollback,\n";
            innerSelect = "j.job_name, cicd_job_id, job_run_number, log_gcspath, status, r.start_time,"
                    + metadataSelect +
                    " r.end_time, cicd_user_id, project_name, job_normalized_full_name, i.integration_id, scm_url, scm_commit_ids," +
                    " i.type, i.name as instance_name, i.id as instance_guid";

        }
        return " SELECT DISTINCT(r.id) as id, r.duration, " + innerSelect
                + " FROM " + company + ".cicd_job_runs as r"
                + cicdJobsTableJoin
                + cicdJobRunParamsJoin
                + cicdInstancesJoin
                + cicdJobRunStageJoin
                + cicdJobRunStageStepJoin
                + cicdTriageJoin
                + whereClause
                +cicdAllTriage;
    }

    private boolean needTriageRuns(CiCdJobRunsFilter ciCdJobRunsFilter) {
        if(ciCdJobRunsFilter.getAcross() == CiCdJobRunsFilter.DISTINCT.triage_rule ||
                ( CollectionUtils.isNotEmpty(ciCdJobRunsFilter.getStacks())  && ciCdJobRunsFilter.getStacks().contains( CiCdJobRunsFilter.DISTINCT.triage_rule))
        || CollectionUtils.isNotEmpty(ciCdJobRunsFilter.getTriageRuleNames()) || CollectionUtils.isNotEmpty(ciCdJobRunsFilter.getTriageRuleIds()))
            return true;
        return false;
    }

    private boolean needCiCdJobRunStageTable(CiCdJobRunsFilter filter) {
        return (filter.getAcross() != null && List.of(CiCdJobRunsFilter.DISTINCT.stage_name,
                CiCdJobRunsFilter.DISTINCT.stage_status,
                CiCdJobRunsFilter.DISTINCT.step_name,
                CiCdJobRunsFilter.DISTINCT.step_status).contains(filter.getAcross())) ||
                CollectionUtils.isNotEmpty(filter.getStageNames()) || CollectionUtils.isNotEmpty(filter.getExcludeStageNames()) ||
                CollectionUtils.isNotEmpty(filter.getStageStatuses()) || CollectionUtils.isNotEmpty(filter.getExcludeStageStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getStepNames()) || CollectionUtils.isNotEmpty(filter.getExcludeStepNames()) ||
                CollectionUtils.isNotEmpty(filter.getStepStatuses()) || CollectionUtils.isNotEmpty(filter.getExcludeStepStatuses());
    }

    private boolean needCiCdJobRunStageStepsTable(CiCdJobRunsFilter filter) {
        return (filter.getAcross() != null && List.of(CiCdJobRunsFilter.DISTINCT.step_name,
                CiCdJobRunsFilter.DISTINCT.step_status).contains(filter.getAcross())) ||
                CollectionUtils.isNotEmpty(filter.getStepNames()) || CollectionUtils.isNotEmpty(filter.getExcludeStepNames()) ||
                CollectionUtils.isNotEmpty(filter.getStepStatuses()) || CollectionUtils.isNotEmpty(filter.getExcludeStepStatuses());
    }

    private boolean needCiCdJobsTable(CiCdJobRunsFilter filter) {
        boolean needCiCdJobsTable = false;
        if (CollectionUtils.isNotEmpty(filter.getProjects()) || CollectionUtils.isNotEmpty(filter.getJobNames()) ||
                CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames()) ||
                (MapUtils.isNotEmpty(filter.getPartialMatch()) &&
                        CollectionUtils.containsAny(CICD_PARTIAL_MATCH_COLUMNS, filter.getPartialMatch().keySet()) &&
                        MapUtils.isNotEmpty(filter.getPartialMatch().get("job_normalized_full_name"))) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.job_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.qualified_job_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.project_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.cicd_job_id) ||
                (CollectionUtils.isNotEmpty(filter.getStacks()) &&
                        (filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.job_name) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.qualified_job_name) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.project_name) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.cicd_job_id) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.job_normalized_full_name)))) {

            needCiCdJobsTable = true;
        }
        return needCiCdJobsTable;
    }

    private boolean needCiCdJobRunParamsTable(CiCdJobRunsFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getParameters());
    }

    private boolean needCiCdInstancesTable(CiCdJobRunsFilter filter) {
        boolean needCiCdInstancesTable = false;
        if (CollectionUtils.isNotEmpty(filter.getQualifiedJobNames()) ||
                CollectionUtils.isNotEmpty(filter.getInstanceNames()) ||
                CollectionUtils.isNotEmpty(filter.getIntegrationIds()) ||
                CollectionUtils.isNotEmpty(filter.getTypes()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeQualifiedJobNames()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeTypes()) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.instance_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.qualified_job_name) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.cicd_job_id) ||
                (CollectionUtils.isNotEmpty(filter.getStacks()) &&
                        (filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.instance_name) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.cicd_job_id) ||
                                filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.qualified_job_name)))) {
            needCiCdInstancesTable = true;
        }
        return needCiCdInstancesTable;
    }
}
