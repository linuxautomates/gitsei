import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeLatest } from "redux-saga/effects";
import { USER_TRELLIS_PERMISSION_UPDATE } from "reduxConfigs/actions/actionTypes";
import { restapiData, usersList, usersUpdate } from "reduxConfigs/actions/restapi";
import { TrellisUserPermissionService } from "services/restapi";

function* userTrellisPermissionUpdateSaga(action: any) {
  const { user, isUpdateType } = action;
  try {
    // @ts-ignore
    const trellisServices = new TrellisUserPermissionService();
    const scopeAction = user?.scopes?.dev_productivity_write ? "assign-scopes" : "remove-scopes";
    const filter = {
      filter: {
        target_emails: [user.email]
      }
    };
    // @ts-ignore
    yield call(trellisServices.update, scopeAction, filter);
    if (isUpdateType) {
      yield put(usersList({ page: 0, page_size: 50 }));
      notification.success({ message: "Trellis access updated successfully" });
    } else {
      yield put(restapiData(true, "trellis_user_permission", "update", user.email));
    }
  } catch (e) {
    if (!isUpdateType) {
      yield put(restapiData(false, "trellis_user_permission", "update", user.email));
    }
    user.scopes = {};
    yield put(usersUpdate(user.id, user));
    handleError({
      showNotfication: true,
      message: "Failed to updated trellis permission access",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* userTrellisPermissionUpdateSagaWatcher() {
  yield takeLatest(USER_TRELLIS_PERMISSION_UPDATE, userTrellisPermissionUpdateSaga);
}
