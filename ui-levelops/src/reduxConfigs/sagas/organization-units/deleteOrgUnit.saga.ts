import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { ORGANIZATION_UNIT_DELETE } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "../restapiSaga";

function* deleteOrgUnitSaga(action: { type: string; filters: any; id: string }): any {
  const { filters } = action;
  try {
    yield call(restapiEffectSaga, {
      uri: "organization_unit_management",
      method: "list",
      data: filters,
      id: action.id
    });
    const restState = yield select(restapiState);
    const records = get(restState, ["organization_unit_management", "list", action.id, "data", "records"], []);
    if (records.length) {
      notification.error({
        message:
          "A parent Collection cannot be deleted when there are dependents. Please delete its child Collections first and retry."
      });
      yield put(genericRestAPISet({ loading: false }, "organization_unit_management", "bulkDelete", action.id));
    } else {
      notification.info({ message: "Deleting Collection..." });
      yield call(restapiEffectSaga, {
        uri: "organization_unit_management",
        method: "bulkDelete",
        payload: [get(filters, ["filter", "parent_ref_id"])],
        id: action.id
      });
      const restState = yield select(restapiState);
      const status = get(restState, ["organization_unit_management", "bulkDelete", action.id, "data"]);
      const error = get(restState, ["organization_unit_management", "bulkDelete", action.id, "error"]);
      if (status === "ok") {
        notification.success({ message: "Collections Deleted successfully" });
      }
      if (error) {
        throw new Error();
      }
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to delete collection",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* deleteOrgUnitSagaWatcher() {
  yield takeEvery(ORGANIZATION_UNIT_DELETE, deleteOrgUnitSaga);
}
