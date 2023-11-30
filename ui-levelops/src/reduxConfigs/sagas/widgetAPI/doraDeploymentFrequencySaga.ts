import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { widgetAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestDoraChangeFailureReportsService, RestDoraDeploymentFrequencyReportsService } from "services/restapi";

function* depoymentFrequencySaga(action: getWidgetAPIActionType) {
  if (action.reportType !== "deployment_frequency_report") {
    return;
  }
  try {
    const depoymentFreqServices = new RestDoraDeploymentFrequencyReportsService();

    // @ts-ignore
    const response = yield call(depoymentFreqServices.list, action.filter);
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

export function* doraDepoymentFrequencyWatcherSaga() {
  yield takeEvery(widgetAPIActions.GET_DATA, depoymentFrequencySaga);
}
