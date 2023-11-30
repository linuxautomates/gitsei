import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import { DORA_LEAD_TIME_GET_DATA } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestDoraLeadTimeForChangeReportsService } from "services/restapi";
import { doraLeadTimeForChangeTransformer } from "./helper";

function* doraLeadTimeForChangeSaga(action: getWidgetAPIActionType) {
  try {
    const service = new RestDoraLeadTimeForChangeReportsService();

    // @ts-ignore
    const response = yield call(service.list, action.filter);
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      const data = response.data;
      yield put(getWidgetDataSuccessAction(action.widgetId, doraLeadTimeForChangeTransformer(data)));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: get(e, ["message"], ""),
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(getWidgetDataFailedAction(action.widgetId, e));
  }
}

export function* doraLeadTimeForChangeWatcherSaga() {
  yield takeEvery(DORA_LEAD_TIME_GET_DATA, doraLeadTimeForChangeSaga);
}
