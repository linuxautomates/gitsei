import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  saveTrellisProfileFailedAction,
  saveTrellisProfileSuccessfulAction
} from "reduxConfigs/actions/restapi/trellisProfileActions";
import { TrellisProfileServices } from "services/restapi";

function* trellisProfilePartialUpdateSaga(action: any) {
  try {
    const trellisServices = new TrellisProfileServices();

    // @ts-ignore
    const response = yield call(trellisServices.patch, action.id, action.data);
    if (response.error) {
      yield put(saveTrellisProfileFailedAction(response.error));
    }
    yield put(saveTrellisProfileSuccessfulAction());
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

    yield put(saveTrellisProfileFailedAction(e));
  }
}

export function* trellisProfilePartialUpdateWatcherSaga() {
  yield takeLatest(trellisProfileActions.TRELLIS_PROFILE_PARTIAL_UPDATE, trellisProfilePartialUpdateSaga);
}
