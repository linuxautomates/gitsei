import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { widgetAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestDoraChangeFailureReportsService } from "services/restapi";

function* doraChangeFailureSaga(action: getWidgetAPIActionType) {
  if (action.reportType !== "change_failure_rate") {
    return;
  }
  try {
    const changeFailureServices = new RestDoraChangeFailureReportsService();
    const widgetId = action.widgetId || "";

    // @ts-ignore
    const response = yield call(changeFailureServices.list, {
      ...action.filter,
      widget_id: widgetId
    });
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      yield put(getWidgetDataSuccessAction(action.widgetId, response.data));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });

    yield put(getWidgetDataFailedAction(action.widgetId, e));
  }
}

export function* doraChangeFailureWatcherSaga() {
  yield takeEvery(widgetAPIActions.GET_DATA, doraChangeFailureSaga);
}
