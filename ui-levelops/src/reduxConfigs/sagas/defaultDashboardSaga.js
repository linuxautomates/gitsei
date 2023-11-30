import { put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { DASHBOARD_DEFAULT } from "../actions/actionTypes";
import { get } from "lodash";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { sanitizeObjectCompletely } from "utils/commonUtils";

const restapiState = state => state.restapiReducer;

export function* dashboardDefaultEffectSage(action) {
  const defaultDashKey = `defaultDashKey-${action.id}`;
  const defaultDashAction = `defaultDashAction-${action.id}`;

  // setting loading and error true at the beginning
  yield put(actionTypes.restapiError(true, action.uri, action.method, action.id));
  yield put(actionTypes.restapiLoading(true, action.uri, action.method, action.id));
  const selectedWorkspace = yield select(getSelectedWorkspace);
  yield put(
    actionTypes.dashboardsList(
      {
        filter: sanitizeObjectCompletely({
          default: true,
          workspace_id: !!selectedWorkspace?.id ? parseInt(selectedWorkspace?.id) : ""
        })
      },
      defaultDashKey,
      defaultDashAction
    )
  );

  yield take(defaultDashAction);

  const state = yield select(restapiState);

  const dashboard = get(state, ["dashboards", "list", defaultDashKey, "data", "records"], []);

  if (dashboard.length > 0) {
    yield put(actionTypes.restapiData({ id: dashboard[0].id, exists: true }, action.uri, action.method, action.id));
  } else {
    // getting security dashboard
    const securityDashKey = `securityDashKey-${action.id}`;
    const securityDashAction = `securityDashAction-${action.id}`;
    yield put(
      actionTypes.dashboardsList(
        {
          filter: sanitizeObjectCompletely({
            type: "security",
            workspace_id: !!selectedWorkspace?.id ? parseInt(selectedWorkspace?.id) : ""
          })
        },
        securityDashKey,
        securityDashAction
      )
    );

    yield take(securityDashAction);

    const newState = yield select(restapiState);
    const securityDash = get(newState, ["dashboards", "list", securityDashKey, "data", "records"], []);

    if (securityDash.length > 0) {
      yield put(
        actionTypes.restapiData({ id: securityDash[0].id, exists: true }, action.uri, action.method, action.id)
      );
    } else {
      // geting any dashboard
      const dashKey = `dashKey-${action.id}`;
      const dashAction = `dashAction-${action.id}`;
      yield put(
        actionTypes.dashboardsList(
          sanitizeObjectCompletely({
            filter: {
              workspace_id: !!selectedWorkspace?.id ? parseInt(selectedWorkspace?.id) : ""
            }
          }),
          dashKey,
          dashAction
        )
      );

      yield take(dashAction);

      const listState = yield select(restapiState);
      const dashboards = get(listState, ["dashboards", "list", dashKey, "data", "records"], []);

      if (dashboards.length > 0) {
        yield put(
          actionTypes.restapiData({ id: dashboards[0].id, exists: true }, action.uri, action.method, action.id)
        );
      } else {
        yield put(actionTypes.restapiData({ id: undefined, exists: false }, action.uri, action.method, action.id));
      }
    }
  }
  yield put(actionTypes.restapiError(false, action.uri, action.method, action.id));
  yield put(actionTypes.restapiLoading(false, action.uri, action.method, action.id));
}

export function* defaultDashboardWatcherSaga() {
  yield takeLatest([DASHBOARD_DEFAULT], dashboardDefaultEffectSage);
}
