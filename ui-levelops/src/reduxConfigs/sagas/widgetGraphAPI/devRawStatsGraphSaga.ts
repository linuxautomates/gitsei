import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { devRawStatGraphAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetGraphAPIActionType,
  getWidgetGraphDataFailedAction,
  getWidgetGraphDataSuccessAction
} from "reduxConfigs/actions/widgetGraphAPIActions";
import { RestDevRawStatsGraph } from "services/restapi";

function* devRawStatsGraphSaga(action: getWidgetGraphAPIActionType) {
  try {
    const devRawStatsServices = new RestDevRawStatsGraph();

    // @ts-ignore
    const response = yield call(devRawStatsServices.list, action.filter);
    if (response.error) {
      yield put(getWidgetGraphDataFailedAction(action.widgetId, response.error));
    } else {
      yield put(getWidgetGraphDataSuccessAction(action.widgetId, response.data));
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

    yield put(getWidgetGraphDataFailedAction(action.widgetId, e));
  }
}

export function* devRawGraphStatsWatcherSaga() {
  yield takeEvery(devRawStatGraphAPIActions.GET_GRAPH_DATA, devRawStatsGraphSaga);
}
