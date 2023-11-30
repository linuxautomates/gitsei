import { get } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { CODE_VOL_VS_DEPLOYMENT_VALUES } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiLoading, restapiError } from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "./restapiSaga";

import axios from "axios";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

export function* CodeVolVsDeployemntValueEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}) {
  const actionId = action.id;
  let integrationIds = get(action, ["data", "integration_ids"], []);
  let code_vol_vs_deployment_filters: any = {};

  try {
    yield put(restapiLoading(true, action.uri, action.method, actionId.toString()));
    const deploy_supported_fields = [
      "cicd_user_id",
      "job_status",
      "job_name",
      "project_name",
      "instance_name",
      "job_normalized_full_name"
    ];
    const build_supported_fields = [
      "cicd_user_id",
      "job_status",
      "job_name",
      "repo",
      "project_name",
      "instance_name",
      "job_normalized_full_name"
    ];

    const deployFilter = {
      fields: deploy_supported_fields,
      integration_ids: integrationIds,
      filter: {
        cicd_integration_ids: integrationIds,
        integration_ids: integrationIds
      }
    };

    const buildFilter = {
      fields: build_supported_fields,
      integration_ids: integrationIds,
      filter: {
        cicd_integration_ids: integrationIds,
        integration_ids: integrationIds
      }
    };
    const apicalls = [
      {
        uri: "cicd_filter_values",
        method: "list",
        id: actionId,
        data: buildFilter
      },
      {
        uri: "jenkins_jobs_filter_values",
        method: "list",
        id: actionId,
        data: deployFilter
      }
    ];

    yield all(apicalls.map((apiCall: any) => call(restapiEffectSaga, apiCall)));
    //@ts-ignore
    let restState: any = yield select(restapiState);
    const deployValues = get(restState, ["jenkins_jobs_filter_values", "list", action.id, "data", "records"], []).map(
      (item: any) => {
        const key = Object.keys(item)[0];
        return { [`deploy_${key}`]: item[key] };
      }
    );

    const buildValues = get(restState, ["cicd_filter_values", "list", action.id, "data", "records"], []).map(
      (item: any) => {
        const key = Object.keys(item)[0];
        return { [`build_${key}`]: item[key] };
      }
    );

    code_vol_vs_deployment_filters.records = [...deployValues, ...buildValues];
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(restapiError(true, action.uri, action.method, actionId));
  } finally {
    yield put(restapiData(code_vol_vs_deployment_filters, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId.toString()));
  }
}

export function* CodeVolVsDeployemntValueWatcherSaga() {
  yield takeEvery([CODE_VOL_VS_DEPLOYMENT_VALUES], CodeVolVsDeployemntValueEffectSaga);
}
