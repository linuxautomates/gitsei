import { issueContextTypes, severityTypes } from "bugsnag";
import { DevRawStatsDataType } from "dashboard/reports/dev-productivity/individual-raw-stats-report/types";
import { rawStatsDaysColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { widgetAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestDevRawStats } from "services/restapi";
import { convertToDays } from "utils/timeUtils";
import { getRawStatRatingAndCount } from "./helper";
// import { transformData } from "./helper";

const transformData = (data: Array<DevRawStatsDataType>) =>
  (data ?? [])
    .map((data: DevRawStatsDataType) => {
      const valuesObj = getRawStatRatingAndCount(data?.raw_stats);
      return {
        ...data,
        ...(data.ou_attributes ?? {}),
        ...(valuesObj ?? {})
      };
    })
    .map((data: any) => {
      const newObject = {};
      const keys = Object.keys(data);
      keys.forEach((key: string) => {
        let value = data[key];
        if (rawStatsDaysColumns.includes(key)) {
          value = convertToDays(value);
        }
        // @ts-ignore
        newObject[key] = value;
      });
      return newObject;
    });
function* devRawStatsSaga(action: getWidgetAPIActionType) {
  if (action.reportType !== "individual_raw_stats_report") {
    return;
  }
  try {
    const devRawStatsServices = new RestDevRawStats();

    // @ts-ignore
    const response = yield call(devRawStatsServices.list, action.filter);
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      yield put(getWidgetDataSuccessAction(action.widgetId, transformData(response.data.records?.[0]?.records)));
      // yield put(getWidgetDataSuccessAction(action.widgetId, transformData(response.data.records)));
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

export function* devRawStatsWatcherSaga() {
  yield takeEvery(widgetAPIActions.GET_DATA, devRawStatsSaga);
}
