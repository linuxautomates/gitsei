import { all, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { get } from "lodash";

import { TRIAGE_MATCHING_JOGS } from "reduxConfigs/actions/actionTypes";
import { getJenkinsPipelineJobListState, getJenkinsPipelineJobStagesListState } from "../selectors/job.selector";

export function* triageMatchingJobsEffectSage(action) {
  const URI = "triage";
  const METHOD = "matchingJobs";
  const ID = action.work_item.id || "0";

  let error = false;

  try {
    const { work_item } = action;

    const { job_ids, job_ids_with_stag, stag_ids } = work_item.cicd_mappings.reduce(
      (acc, obj) => {
        const jobId = obj.cicd_job_run_id;
        const stagId = obj.cicd_job_run_stage_id;
        if (jobId) {
          acc.job_ids[jobId] = true;
          if (stagId) {
            acc.stag_ids[stagId] = true;
            acc.job_ids_with_stag[jobId] = true;
          } else {
            acc.job_ids_without_stg[jobId] = true;
          }
        }
        return acc;
      },
      { job_ids: {}, job_ids_with_stag: {}, stag_ids: {}, job_ids_without_stg: {} }
    );
    const jobIds = Object.keys(job_ids);
    const jobIdsWithStag = Object.keys(job_ids_with_stag);
    const jobIdsWithoutStag = Object.keys(job_ids);
    const stagIds = Object.keys(stag_ids);

    const filters = { filter: { cicd_job_run_ids: jobIds } };
    const stagesFilter = jobId => ({
      page: 0,
      page_size: 500,
      sort: [],
      filter: { job_run_id: jobId, partial: {} },
      across: ""
    });
    const JOB_IDS_WITH_STAG_ACTION = "JOB_IDS_WITH_STAG_ACTION";
    const JOB_RUN_ACTION = "JOB_RUN_ACTION";

    yield put(actionTypes.genericList("jenkins_pipeline_triage_runs", "list", filters, JOB_RUN_ACTION, work_item.id));
    yield take(JOB_RUN_ACTION);

    yield all(
      jobIdsWithStag.map(jobId =>
        put(
          actionTypes.genericList(
            "jenkins_pipeline_job_stages",
            "list",
            stagesFilter(jobId),
            `${JOB_IDS_WITH_STAG_ACTION}-${jobId}`,
            jobId
          )
        )
      )
    );
    yield all(jobIdsWithStag.map(jobId => take(`${JOB_IDS_WITH_STAG_ACTION}-${jobId}`)));

    const jobStagListState = yield select(getJenkinsPipelineJobStagesListState);
    const jobRunsListState = yield select(getJenkinsPipelineJobListState);

    let jobStagDetails = [];
    let allJobs = [];
    for (let j = 0; j < jobIdsWithStag.length; j++) {
      const jobId = jobIdsWithStag[j];
      const stagJobs = get(jobStagListState, [jobId, "data", "records"], []);
      const Jobs = get(jobRunsListState, [ID, "data", "records"], []);
      const reqJob = Jobs.find(record => record.id === jobId);
      const matchingStages = stagJobs
        .filter(rec => stagIds.includes(rec.id))
        .map(rec => ({
          id: jobId,
          cicd_job_run_stage_id: rec.id,
          cicd_job_run_stage_name: rec.name,
          job_name: reqJob.job_name,
          job_run_number: reqJob.job_run_number
        }));
      jobStagDetails.push(...matchingStages);
    }

    for (let j = 0; j < jobIdsWithoutStag.length; j++) {
      const jobId = jobIdsWithoutStag[j];
      const stagJobs = get(jobRunsListState, [ID, "data", "records"], []);
      const reqJob = stagJobs.find(record => record.id === jobId);
      if (reqJob) {
        jobStagDetails.push({
          id: reqJob.id,
          job_name: reqJob.job_name,
          job_run_number: reqJob.job_run_number
        });
      }
    }

    let jobDetails = get(jobRunsListState, [work_item.id, "data", "records"], []);
    if (jobStagDetails && jobStagDetails.length > 0) {
      jobDetails.forEach(job => {
        const hasStags = jobStagDetails.filter(js => {
          return js.cicd_job_run_id === job.id && stagIds.includes(js.id);
        });
        if (hasStags && hasStags.length > 0) {
          hasStags.forEach(stag => {
            allJobs.push({ ...job, cicd_job_run_stage_id: stag.id, cicd_job_run_stage_name: stag.name });
          });
        } else {
          allJobs.push(job);
        }
      });
    }

    yield put(actionTypes.restapiData(jobStagDetails, URI, METHOD, ID));
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
    yield put(actionTypes.restapiError(error, URI, METHOD, ID));
  }
}

export function* triageMatchingJobsWatcherSaga() {
  yield takeLatest([TRIAGE_MATCHING_JOGS], triageMatchingJobsEffectSage);
}
