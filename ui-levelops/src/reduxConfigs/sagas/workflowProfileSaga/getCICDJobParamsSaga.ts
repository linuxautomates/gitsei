import { call, put, takeEvery } from "redux-saga/effects";
import { cicdJobParamsActions } from "reduxConfigs/actions/actionTypes";
import { cicdJobParamsFailedAction, cicdJobParamsSuccessAction } from "reduxConfigs/actions/restapi/workFlowNewAction";
import { JobParamsService } from "services/restapi/jobParams.service";

function* getCICDJobParams(action: any) {
  const jobParamService = new JobParamsService();
  try {
    // @ts-ignore
    const response = yield call(jobParamService.getParams, action.payload);

    if (response.error) {
      yield put(cicdJobParamsFailedAction(action.id, response.error));
    } else {
      yield put(cicdJobParamsSuccessAction(action.id, response.data));
    }
  } catch (e) {
    yield put(cicdJobParamsFailedAction(action.id, e));
  }
}

export function* getCICDJobParamsSaga() {
  yield takeEvery(cicdJobParamsActions.GET_CICD_JOB_PARAMS, getCICDJobParams);
}
