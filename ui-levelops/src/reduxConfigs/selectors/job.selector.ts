import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const getJenkinsPipelineJobStagesListState = createSelector(restapiState, apis => {
  return get(apis, ["jenkins_pipeline_job_stages", "list"], {});
});

export const getJenkinsPipelineJobListState = createSelector(restapiState, apis => {
  return get(apis, ["jenkins_pipeline_triage_runs", "list"], {});
});

export const triageMatchingJobsSelector = createSelector(restapiState, apis => {
  return get(apis, ["triage", "matchingJobs"], {});
});
