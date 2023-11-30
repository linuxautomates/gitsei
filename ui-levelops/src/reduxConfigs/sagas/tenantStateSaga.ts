import { takeLatest, call } from "redux-saga/effects";
import { TENANT_STATE } from "../actions/actionTypes";
import { restapiEffectSaga } from "./restapiSaga";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const restapiState = (state: any) => state.restapiReducer;

export function* dashboardTenantStateEffectSaga(action: any) {
  try {
    //api call tenantState when ready
    yield call(restapiEffectSaga, {
      uri: action.uri,
      method: action.method,
      id: action.id
    });
    //put mock data for now till api ready
    // yield put(actionTypes.restapiData({ "auto_events": ["CS_USER_CREATED","DEFAULT_USER_CREATED","DEMO_DASHBOARD_CREATED","DORA_DASHBOARD_CREATED"]}, action.uri, action.method, action.id));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.APIS,
        data: { e, action }
      }
    });
  }
}

export function* teanantStateWatcherSaga() {
  yield takeLatest([TENANT_STATE], dashboardTenantStateEffectSaga);
}
