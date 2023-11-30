import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  saveTrellisProfileFailedAction,
  saveTrellisProfileSuccessfulAction
} from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfileServices } from "services/restapi";

function* trellisProfileCreateSaga(action: any) {
  try {
    const trellisServices = new TrellisProfileServices();
    // @ts-ignore
    const response = yield call(trellisServices.create, action.data);
    if (response.error) {
      yield put(saveTrellisProfileFailedAction(response.error));
    } else {
      yield put(saveTrellisProfileSuccessfulAction());
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to save the profile.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* trellisProfileCreateWatcherSaga() {
  yield takeLatest(trellisProfileActions.TRELLIS_PROFILE_CREATE, trellisProfileCreateSaga);
}
