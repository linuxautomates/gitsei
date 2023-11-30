import { issueContextTypes, severityTypes } from "bugsnag";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { workflowProfileByOuActions } from "reduxConfigs/actions/actionTypes";
import {
  workflowProfileByOuLoadFailedAction,
  workflowProfileByOuLoadSuccessfulAction,
  workProfileByOuAlreadyPresentAction
} from "reduxConfigs/actions/restapi/workflowProfileByOuAction";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { WorkflowProfileServicesByOu } from "services/restapi";
import { logToBugsnag } from "bugsnag";

function* workflowProfileByOuCreateSaga(action: any): any {
  try {
    const workspaceProfile = yield select(workflowProfileDetailSelector, { queryParamOU: action.id });
    if (workspaceProfile) {
      yield put(workProfileByOuAlreadyPresentAction(action.id));
      return;
    }
    const workflowServicesByOU = new WorkflowProfileServicesByOu();
    // @ts-ignore

    const response = yield call(workflowServicesByOU.get, action.id);
    if (response.error) {
      yield put(workflowProfileByOuLoadFailedAction(action.id, response.error));
    } else {
      yield put(workflowProfileByOuLoadSuccessfulAction(action.id, response.data));
    }
  } catch (e) {
    // we will make notification appear again when we think of the solution for this, PROP-2443
    // for now explicitly calling logToBugsnag
    const bugsnag = {
      // @ts-ignore
      message: (e as any)?.message,
      severity: severityTypes.ERROR,
      context: issueContextTypes.VELOCITY,
      data: { e, action }
    };
    logToBugsnag(bugsnag.message, bugsnag.severity, bugsnag.context, bugsnag.data);
    yield put(workflowProfileByOuLoadFailedAction(action.id, e));
  }
}

export function* workflowProfileByOuCreateWatcherSaga() {
  yield takeLatest(workflowProfileByOuActions.WORKFLOW_PROFILE_READ_BY_OU, workflowProfileByOuCreateSaga);
}
