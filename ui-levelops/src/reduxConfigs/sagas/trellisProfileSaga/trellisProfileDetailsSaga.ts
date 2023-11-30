import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import {
  trellisProfileLoadFailedAction,
  trellisProfileLoadSuccessfulAction
} from "reduxConfigs/actions/restapi/trellisProfileActions";
import { _integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { TrellisProfileServices } from "services/restapi";

const ON_INTEGRATIONS_LOADED = "ON_INTEGRATIONS_LOADED";

function* trellisProfileDetailsSaga(action: any) {
  try {
    const trellisServices = new TrellisProfileServices();

    // @ts-ignore
    const response = yield call(trellisServices.get, action.id);
    if (response.error) {
      yield put(trellisProfileLoadFailedAction(response.error));
    }
    yield put(trellisProfileLoadSuccessfulAction(response.data));
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to load the profile.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });

    yield put(trellisProfileLoadFailedAction(e));
  }
}

export function* trellisProfileDetailsWatcherSaga() {
  yield takeLatest(trellisProfileActions.TRELLIS_PROFILE_READ, trellisProfileDetailsSaga);
}
