import { takeLatest, put, take } from "redux-saga/effects";
import { UPDATE_DASHBOARD_WIDGETS } from "reduxConfigs/actions/actionTypes";
import { _dashboardsGetSelector } from "../../selectors/dashboardSelector";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { VELOCITY_CONFIG_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";

export function* getDataForWidgetUpdationSaga(action: any) {
  const { complete } = action;
  const VELOCITY_COMPLETE = complete?.velocity_complete;
  try {
    yield put(
      actionTypes.genericList("velocity_configs", "list", {}, VELOCITY_COMPLETE as any, VELOCITY_CONFIG_LIST_ID)
    );
    yield take(VELOCITY_COMPLETE);
  } catch (e) {
    console.error("Failed to delete widget", e);
  }
}

export function* getDataForWidgetsUpdationWatcherSaga() {
  yield takeLatest(UPDATE_DASHBOARD_WIDGETS, getDataForWidgetUpdationSaga);
}
