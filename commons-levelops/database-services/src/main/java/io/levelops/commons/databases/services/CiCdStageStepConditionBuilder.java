package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class CiCdStageStepConditionBuilder {

    public void prepareCiCdStageStepConditions(CiCdJobRunsFilter filter, Map<String, Object> params, List<String> criterias, String paramSuffix, String company) {


        if (CollectionUtils.isNotEmpty(filter.getStageNames())) {
            criterias.add("cicd_job_run_stage.name IN (:stage_names" + paramSuffix + ")");
            params.put("stage_names" + paramSuffix, filter.getStageNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getStepNames())) {
            criterias.add("cicd_job_run_stage_steps.display_name IN (:step_names" + paramSuffix + ")");
            params.put("step_names" + paramSuffix, filter.getStepNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getStageStatuses())) {
            criterias.add(" LOWER(cicd_job_run_stage.result) " + getStageStepStatusClause("stage_statuses", paramSuffix));
            params.put("stage_statuses" + paramSuffix, filter.getStageStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getStepStatuses())) {
            criterias.add(" LOWER(cicd_job_run_stage_steps.result) " + getStageStepStatusClause("step_statuses", paramSuffix));
            params.put("step_statuses" + paramSuffix, filter.getStepStatuses());
        }


        if (CollectionUtils.isNotEmpty(filter.getExcludeStageNames())) {
            criterias.add("r.id not in (Select cicd_job_run_id from "+company+".cicd_job_run_stages where name IN (:exclude_stage_names" + paramSuffix + "))");
            params.put("exclude_stage_names" + paramSuffix, filter.getExcludeStageNames());
        }

        if (CollectionUtils.isNotEmpty(filter.getExcludeStageStatuses())) {
            if(CollectionUtils.isEmpty(filter.getExcludeStageNames())
                    && CollectionUtils.isNotEmpty(filter.getStageNames())) {
                criterias.add("r.id not in (Select cicd_job_run_id from " + company + ".cicd_job_run_stages where name IN (:exclude_stage_names" + paramSuffix + ") and " +
                        " LOWER(result) " + getStageStepStatusClause("exclude_stage_statuses", paramSuffix) + ")");
                params.put("exclude_stage_names" + paramSuffix, filter.getStageNames());
                params.put("exclude_stage_statuses" + paramSuffix, filter.getExcludeStageStatuses());
            }
        }

        if (CollectionUtils.isNotEmpty(filter.getExcludeStepNames())) {
            criterias.add("r.id not in ( select cicd_job_run_id from " + company + ".cicd_job_run_stages where id in (select cicd_job_run_stage_id from " + company + ".cicd_job_run_stage_steps where display_name in (:exclude_step_names" + paramSuffix + ")))");
            params.put("exclude_step_names" + paramSuffix, filter.getExcludeStepNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeStepStatuses())) {
            if(CollectionUtils.isEmpty(filter.getExcludeStepNames())
                && CollectionUtils.isNotEmpty(filter.getStepNames()) ) {
                criterias.add("r.id not in ( select cicd_job_run_id from " + company + ".cicd_job_run_stages where id in (select cicd_job_run_stage_id from " + company + ".cicd_job_run_stage_steps where display_name in (:exclude_step_names" + paramSuffix + ") and " +
                        " LOWER(result) " + getStageStepStatusClause("exclude_step_statuses", paramSuffix) + "))");
                params.put("exclude_step_names" + paramSuffix, filter.getStepNames());
                params.put("exclude_step_statuses" + paramSuffix, filter.getExcludeStepStatuses());
            }
        }

    }

    private String getStageStepStatusClause(String field, String paramSuffix) {
        return " LIKE ANY(LOWER(ARRAY[:" + field + paramSuffix + " ]::text)::text[])";
    }
}
