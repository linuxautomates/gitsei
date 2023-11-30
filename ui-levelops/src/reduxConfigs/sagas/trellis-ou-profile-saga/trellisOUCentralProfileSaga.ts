import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { GET_DEV_PROD_CENTRAL_PROFILE } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { RestDevProdParentService } from "services/restapi";

function* trellisOUCentralProfileSaga(action: any) {
  try {
    const trellisServices = new RestDevProdParentService();
    // @ts-ignore
    const response = yield call(trellisServices.get);
    yield put(restapiData(response?.data, "trellis_profile_ou", "get", action?.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "get", action?.id));
    if (response?.error) {
      notification.error({ message: "Failed get central profile for OU" });
      yield put(restapiError(response?.error, "trellis_profile_ou", "get", action?.id));
    } else {
      yield put(restapiError(false, "trellis_profile_ou", "get", action?.id));
    }
  } catch (e) {
    yield put(restapiError(true, "trellis_profile_ou", "get", action?.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "get", action?.id));

    handleError({
      showNotfication: true,
      message: "Failed get central profile for OU.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS_OU,
        data: { e, action }
      }
    });
  }
}

export function* trellisOUCentralProfileSagaWatcherSaga() {
  yield takeLatest(GET_DEV_PROD_CENTRAL_PROFILE, trellisOUCentralProfileSaga);
}
