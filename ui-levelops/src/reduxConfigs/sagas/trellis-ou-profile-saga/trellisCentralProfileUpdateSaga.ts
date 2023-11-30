import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { PUT_DEV_PROD_CENTRAL_PROFILE } from "reduxConfigs/actions/actionTypes";
import { getDevProdCentralProfile } from "reduxConfigs/actions/devProdParentActions";
import { restapiClear, restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { RestDevProdParentService } from "services/restapi";

function* trellisCentralProfileUpdateSaga(action: any): any {
  try {
    const trellisServices = new RestDevProdParentService();
    let response = undefined;
    response = yield call(trellisServices.centralProfileUpdate, action.payload, action.payload?.id) as any;
    yield put(restapiData(response?.data, "trellis_profile_ou", "update", action?.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "update", action?.id));
    if (response?.error) {
      notification.error({ message: "Failed update central profile." });
      yield put(restapiError(response?.error, "trellis_profile_ou", "update", action?.id));
    } else {
      yield put(restapiError(false, "trellis_profile_ou", "update", action?.id));
      notification.success({ message: "Trellis central profile updated successfully" });
    }
    yield put(restapiClear("trellis_profile_ou", "get", "central_profile"));
    yield put(getDevProdCentralProfile({}, "central_profile"));
  } catch (e) {
    yield put(restapiError(true, "trellis_profile_ou", "update", action.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "update", action?.id));
    handleError({
      showNotfication: true,
      message: "Failed update central profile.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS_OU,
        data: { e, action }
      }
    });
  }
}

export function* trellisCentralProfileUpdateWatcherSaga() {
  yield takeLatest(PUT_DEV_PROD_CENTRAL_PROFILE, trellisCentralProfileUpdateSaga);
}
