import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import {
  DORA_LEAD_TIME_FOR_CHANGE_GET_DATA,
  DORA_MEAN_TIME_TO_RESTORE_GET_DATA
} from "reduxConfigs/actions/actionTypes";
import {
  getWidgetaDataAction,
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestDoraLeadTimeForChangeService, RestDoraMeanTimeForChangeService } from "services/restapi";

function* doraLeadTimeSaga(action: getWidgetAPIActionType) {
  try {
    const service = new RestDoraLeadTimeForChangeService();
    yield put(getWidgetaDataAction(action.reportType, action.widgetId, action.filter));
    // @ts-ignore
    const response = yield call(service.list, action.filter);
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      const data = response.data;
      const records = get(data, ["records"], []);
      yield put(getWidgetDataSuccessAction(action.widgetId, records));
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

export function* doraLeadTimeWidgetWatcherSaga() {
  yield takeEvery(DORA_LEAD_TIME_FOR_CHANGE_GET_DATA, doraLeadTimeSaga);
}

function* doraMTTRSaga(action: getWidgetAPIActionType) {
  try {
    const service = new RestDoraMeanTimeForChangeService();
    yield put(getWidgetaDataAction(action.reportType, action.widgetId, action.filter));
    // @ts-ignore
    const response = yield call(service.list, action.filter);
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      const data = response.data;
      const records = get(data, ["records"], []);
      yield put(getWidgetDataSuccessAction(action.widgetId, records));
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

export function* doraMeanTimeToRestoreWatcherSaga() {
  yield takeEvery(DORA_MEAN_TIME_TO_RESTORE_GET_DATA, doraMTTRSaga);
}
