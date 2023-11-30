import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep } from "lodash";
import { call, put, takeLatest } from "redux-saga/effects";
import { PUT_DEV_PROD_PROFILE } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { RestDevProdParentService } from "services/restapi";

function* trellisProfileOUUpdateSaga(action: any): any {
  try {
    const trellisServices = new RestDevProdParentService();
    let response = undefined;
    // @ts-ignore
    const isTrellisEnabled = cloneDeep(action?.payload?.parent_profile?.isTrellisEnabled);
    if (action?.payload?.parent_profile?.isTrellisEnabled !== undefined) {
      delete action.payload.parent_profile.isTrellisEnabled;
    }
    // call trellis disable api
    if (!isTrellisEnabled) {
      response = yield call(trellisServices.patch, action.payload?.parent_profile?.associated_ou_ref_ids?.[0]) as any;
    } else {
      response = yield call(trellisServices.update, action.payload) as any;
    }
    yield put(restapiData(response?.data, "trellis_profile_ou", "update", action?.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "update", action?.id));

    if (response?.error) {
      notification.error({
        message: !isTrellisEnabled
          ? "Failed to create profile of Collection."
          : "Failed to update profile of Collection."
      });
      yield put(restapiError(response?.error, "trellis_profile_ou", "update", action?.id));
    } else {
      yield put(restapiError(false, "trellis_profile_ou", "update", action?.id));
      const { location } = action;
      if (location && location?.history) {
        location?.history?.push(location?.routeForRedirect);
      }
      if (location?.showCreateMessage) {
        notification.success({ message: "Collection Created Successfully" });
      } else {
        notification.success({ message: "Collection Updated Successfully" });
      }
    }
  } catch (e) {
    yield put(restapiError(true, "trellis_profile_ou", "update", action.id));
    yield put(restapiLoading(false, "trellis_profile_ou", "update", action?.id));
    handleError({
      showNotfication: true,
      message: "Failed to update profile of Collection.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS_OU,
        data: { e, action }
      }
    });
  }
}

export function* trellisProfileOUUpdateWatcherSaga() {
  yield takeLatest(PUT_DEV_PROD_PROFILE, trellisProfileOUUpdateSaga);
}
