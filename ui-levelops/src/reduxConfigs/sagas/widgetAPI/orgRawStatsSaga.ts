import { issueContextTypes, severityTypes } from "bugsnag";
import { OrgRawStatsDataType } from "dashboard/reports/dev-productivity/org-raw-stats-report/types";
import { rawStatsDaysColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { widgetAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedAction,
  getWidgetDataSuccessAction
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { RestOrgRawStats } from "services/restapi/devProductivity.services";
import { convertToDays } from "utils/timeUtils";
import { getRawStatRatingAndCount } from "./helper";
// import { transformData } from "./helper";

const transformData = (data: Array<OrgRawStatsDataType>) =>
  (data ?? [])
    .map((data: OrgRawStatsDataType) => {
      const valuesObj = getRawStatRatingAndCount(data?.raw_stats);
      return {
        ...data,
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
function* orgRawStatsSaga(action: getWidgetAPIActionType) {
  if (action.reportType !== "org_raw_stats_report") {
    return;
  }
  try {
    const orgRawStatsServices = new RestOrgRawStats();

    // @ts-ignore
    const response = yield call(orgRawStatsServices.list, action.filter);
    if (response.error) {
      yield put(getWidgetDataFailedAction(action.widgetId, response.error));
    } else {
      yield put(getWidgetDataSuccessAction(action.widgetId, transformData(response.data.records)));
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

export function* orgRawStatsWatcherSaga() {
  yield takeEvery(widgetAPIActions.GET_DATA, orgRawStatsSaga);
}
